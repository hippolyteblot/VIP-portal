package fr.insalyon.creatis.vip.core.server.business;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.insalyon.creatis.devtools.MD5;
import fr.insalyon.creatis.grida.client.GRIDAClient;
import fr.insalyon.creatis.grida.client.GRIDAClientException;
import fr.insalyon.creatis.vip.core.client.DefaultError;
import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.client.view.CoreConstants;
import fr.insalyon.creatis.vip.core.client.view.CoreConstants.GROUP_ROLE;
import fr.insalyon.creatis.vip.core.client.view.user.UserLevel;
import fr.insalyon.creatis.vip.core.models.Group;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.server.business.base.CommonBusiness;
import fr.insalyon.creatis.vip.core.server.dao.DAOException;
import fr.insalyon.creatis.vip.core.server.dao.UserDAO;
import fr.insalyon.creatis.vip.core.server.dao.UsersGroupsDAO;
import fr.insalyon.creatis.vip.core.server.inter.annotations.VIPExternalSafe;

@Service
public class AuthenticationBusiness extends CommonBusiness {

    private final UserDAO userDAO;
    private final EmailBusiness emailBusiness;
    private final Server server;
    private final GRIDAClient gridaClient;
    private final UsersGroupsDAO usersGroupsDAO;
    private final UserBusiness userBusiness;
    private final GroupBusiness groupBusiness;
    private final EmailTemplateUtils emailTemplateUtils;

    @Autowired
    public AuthenticationBusiness(UserDAO userDAO, EmailBusiness emailBusiness, Server server, GRIDAClient gridaClient, UsersGroupsDAO usersGroupsDAO, UserBusiness userBusiness, GroupBusiness groupBusiness, EmailTemplateUtils emailTemplateUtils) {
        this.userDAO = userDAO;
        this.emailBusiness = emailBusiness;
        this.server = server;
        this.gridaClient = gridaClient;
        this.usersGroupsDAO = usersGroupsDAO;
        this.userBusiness = userBusiness;
        this.groupBusiness = groupBusiness;
        this.emailTemplateUtils = emailTemplateUtils;
    }

    public void signup(User user, String comments, Group group) throws VipException {
        signup(user, comments, false, false, group);
    }

    public void signup(User user, String comments, boolean automaticCreation, boolean mapPrivateGroups, Group group)
            throws VipException {
        this.signup(user, comments, automaticCreation, mapPrivateGroups,
                group == null ? new HashSet<>() : new HashSet<>(Collections.singleton(group)));
    }

    @VIPExternalSafe
    public User signup(User user, String comments, boolean automaticCreation, boolean mapPrivateGroups, Set<Group> groups)
            throws VipException {
        logger.info("Starting signup flow for email='{}' (automaticCreation={}, mapPrivateGroups={})",
                user != null ? user.getEmail() : null, automaticCreation, mapPrivateGroups);

        // should be unauthentified or admin (related to internal methods with asAdminContext)
        if (getUser() != null && ! getUserLevel().equals(UserLevel.Administrator)) { 
            throw new VipException(DefaultError.UNAUTHENTIFIED_ONLY);
        }
        if ( ! user.getGroups().stream().allMatch(Group::isPublicGroup)) {
            throw new VipException(DefaultError.ACCESS_DENIED);
        }
        emailBusiness.verifyEmail(user.getEmail());

        // Build log message
        StringBuilder message = new StringBuilder("Signing up ");
        message.append(". List of undesired countries: ");
        for (String s : server.getUndesiredCountries()) {
            if (!s.trim().isEmpty()) {
                message.append(" ");
                message.append(s);
            }
        }
        message.append(".");
        logger.info(message.toString());

        // Check if country is undesired
        for (String udc : server.getUndesiredCountries()) {
            if (udc.trim().isEmpty()) {
                // An empty config file entry gets here as an empty or
                // whitespace-only string, skip it
                continue;
            }
            if (user.getCountryCode().toString().equals(udc)) {
                logger.error("Undesired country for " + user.getEmail());
                throw new VipException("Error");
            }
        }

        try {
            Timestamp ts = new Timestamp(System.currentTimeMillis());

            if (!automaticCreation) {
                user.setTermsOfUse(ts);
            }
            user.setConfirmed(automaticCreation);
            user.setAccountLocked(false);
            user.setLastLogin(ts);
            user.setRegistration(ts);
            user.setLastUpdatePublications(ts);
            user.setCode(UUID.randomUUID().toString());
            user.setFailedAuthentications(0);

            if (user.getPassword() == null) {
                user.setPassword(null);
            } else {
                user.setPassword(MD5.get(user.getPassword()));
            }
            // normalise user folder : replace accents and non ascii characters by _
            String folder = CoreUtil.getCleanStringAlnum(user.getFirstName().toLowerCase() + "_"
                    + user.getLastName().toLowerCase(), "_");

            while (gridaClient.exist(server.getDataManagerUsersHome() + "/" + folder)) {
                folder += "_" + new Random().nextInt(10000);
            }

            user.setFolder(folder);
            user.setLevel(UserLevel.Beginner);
            user.setId(CoreUtil.createUUID());
            userDAO.add(user);
            userDAO.definePassword(user.getEmail(), user.getPassword());

            // Adding user to groups
            if (groups == null) {
                groups = new HashSet<>();
            }
            StringBuilder groupsString = new StringBuilder();
            for (Group group : groups) {
                if (mapPrivateGroups || automaticCreation || group.isPublicGroup()) {
                    usersGroupsDAO.add(user.getEmail(), group.getName(), GROUP_ROLE.User);
                } else {
                    logger.info("Don't map user " + user.getEmail() + " to private group " + group.getName());
                }
                groupsString.append(group.getName()).append(", ");
            }

                logger.info("Signup persistence succeeded for email='{}' with generatedId='{}'",
                    user.getEmail(), user.getId());

            if (!automaticCreation) {
                String emailContent = emailTemplateUtils.registrationUserEmail(user);
                logger.info("Sending confirmation email to '" + user.getEmail() + "'.");
                emailBusiness.sendEmail("VIP account details", emailContent,
                        new String[] { user.getEmail() }, true, user.getEmail());

                String adminsEmailContents = emailTemplateUtils.registrationAdminEmail(user, groupsString.toString(), comments);
                emailBusiness.sendEmailToAdmins("[VIP Admin] Account Requested", adminsEmailContents,
                        true, user.getEmail());
            } else {
                String adminsEmailContents = emailTemplateUtils.registrationAdminEmailAutomatic(user, groupsString.toString(), comments);

                emailBusiness.sendEmailToAdmins("[VIP Admin] Automatic Account Creation", adminsEmailContents,
                        false, user.getEmail());
            }

            logger.info("Signup flow completed for email='{}'", user.getEmail());
            return user;
        } catch (GRIDAClientException | UnsupportedEncodingException | NoSuchAlgorithmException ex) {
            logger.error("Error signing up user {}", user.getEmail(), ex);
            throw new VipException(ex);
        } catch (DAOException ex) {
            logger.error("DAO error while signing up user {}", user.getEmail(), ex);
            throw new VipException(ex);
        }
    }

    public User signin(String email, String password) throws VipException {
        return signin(email, password, true);
    }

    public User signinWithoutResetingSession(String email, String password)
            throws VipException {
        return signin(email, password, false);
    }

    private User signin(String email, String password, boolean resetSession)
            throws VipException {

        try {
            password = MD5.get(password);

            if (userDAO.authenticate(email, password)) {

                userDAO.resetNFailedAuthentications(email);

                if (resetSession) {
                    return userBusiness.getUserWithSession(email);
                } else {
                    return userDAO.get(email);
                }

            } else {
                userDAO.incNFailedAuthentications(email);
                if (userDAO.getNFailedAuthentications(email) > 5) {
                    userDAO.lock(email);
                }
                logger.error(
                        "Authentication failed to '" + email + "' (email or password incorrect, or user is locked).");
                throw new VipException("Authentication failed (email or password incorrect, or user is locked).");
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            logger.error("Error signing in user {}", email, ex);
            throw new VipException(ex);
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public void signout(String email) throws VipException {
        try {
            String session = UUID.randomUUID().toString();
            userDAO.updateSession(email, session);
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public User activate(String email, String code) throws VipException {
        try {
            if (userDAO.isLocked(email)) {
                logger.error("Activation failed to '" + email + "' (user is locked).");
                throw new VipException("User is locked.");
            }
            if (userDAO.activate(email, code)) {

                User user = userDAO.get(email);
                userDAO.resetNFailedAuthentications(email);

                gridaClient.createFolder(server.getDataManagerUsersHome(),
                        user.getFolder());

                gridaClient.createFolder(server.getDataManagerUsersHome(),
                        user.getFolder() + "_" + CoreConstants.FOLDER_TRASH);

                return user;

            } else {
                userDAO.incNFailedAuthentications(email);
                if (userDAO.getNFailedAuthentications(email) > 5) {
                    userDAO.lock(email);
                }
                logger.error("Activation failed to '" + email + "' (wrong code: " + code + ").");
                throw new VipException("Activation failed.");
            }

        } catch (GRIDAClientException ex) {
            logger.error("Error activating {}", email, ex);
            throw new VipException(ex);
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }


    public User getOrCreateUser(String email, String institution, String groupName)
            throws VipException {

        emailBusiness.verifyEmail(email);

        User user;
        try {
            user = userBusiness.getUserWithSession(email);
        } catch (DAOException ex) {
            //User doesn't exist: let's create an account
            String name = email.substring(0, email.indexOf('@'));
            String firstName = name, lastName = name;

            String[] delimiters = {"\\.", "-", "_"};
            for (String delimiter : delimiters) {
                if (name.contains(".") && name.split(delimiter).length >= 2) {
                    firstName = name.split(delimiter)[0];
                    lastName = name.split(delimiter)[1];
                    break;
                }
            }

            user = userBusiness.getNewUser(email, firstName, lastName, institution);
            try {
                signup(user, "Generated automatically", true, true,
                        groupBusiness.get(groupName));
            } catch (VipException ex2) {
                if (ex2.getMessage().contains("existing")) {
                    //try with a different last name
                    lastName += "_" + System.currentTimeMillis();
                    user = userBusiness.getNewUser(email, firstName, lastName, institution);
                    signup(user, "Generated automatically", true,
                            true, groupBusiness.get(groupName));
                }
            }
            activateUser(user.getEmail());
            try {
                user = userBusiness.getUserWithSession(email);
            } catch (DAOException ex1) {
                throw new VipException(ex1);
            }
        }
        return user;
    }

    public void activateUser(String email) throws VipException {
        try {
            User user = userDAO.get(email);
            activate(email, user.getCode());
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }
}
