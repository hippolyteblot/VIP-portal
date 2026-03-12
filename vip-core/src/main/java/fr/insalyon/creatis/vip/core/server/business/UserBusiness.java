package fr.insalyon.creatis.vip.core.server.business;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.insalyon.creatis.grida.client.GRIDAClientException;
import fr.insalyon.creatis.grida.client.GRIDAPoolClient;
import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.client.view.CoreConstants;
import fr.insalyon.creatis.vip.core.client.view.CoreConstants.GROUP_ROLE;
import fr.insalyon.creatis.vip.core.client.view.user.UserLevel;
import fr.insalyon.creatis.vip.core.client.view.util.CountryCode;
import fr.insalyon.creatis.vip.core.models.Group;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.server.business.base.CommonBusiness;
import fr.insalyon.creatis.vip.core.server.dao.DAOException;
import fr.insalyon.creatis.vip.core.server.dao.UserDAO;
import fr.insalyon.creatis.vip.core.server.dao.UsersGroupsDAO;

@Service
@Transactional
public class UserBusiness extends CommonBusiness {

    private final UserDAO userDAO;
    private final UsersGroupsDAO usersGroupsDAO;
    private final EmailBusiness emailBusiness;
    private final GRIDAPoolClient gridaPoolClient;
    private final Server server;

    @Autowired
    public UserBusiness(UserDAO userDAO, UsersGroupsDAO usersGroupsDAO, EmailBusiness emailBusiness,
            GRIDAPoolClient gridaPoolClient, Server server) {
        this.userDAO = userDAO;
        this.usersGroupsDAO = usersGroupsDAO;
        this.emailBusiness = emailBusiness;
        this.gridaPoolClient = gridaPoolClient;
        this.server = server;
    }

    public void updateUserEmail(String oldEmail, String newEmail)
            throws VipException {
        emailBusiness.verifyEmail(newEmail);
        try {
            userDAO.updateEmail(oldEmail, newEmail);
        } catch (DAOException e) {
            String errorMessage = "Error changing email from " + newEmail + " to " + newEmail;
            emailBusiness.sendErrorEmailToAdmins(errorMessage, e, oldEmail);
            throw new VipException("Error changing email address", e);
        }

    }

    public User getUserData(String email) throws VipException {
        try {
            return userDAO.get(email);
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public User updateUser(User user) throws VipException {
        try {
            userDAO.update(user);
            return user;
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public Map<Group, CoreConstants.GROUP_ROLE> getUserGroups(String email)
            throws VipException {
        try {
            return usersGroupsDAO.getUserGroups(email);

        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public void setUserGroups(
            String email, Map<String, CoreConstants.GROUP_ROLE> groups)
            throws VipException {
        try {
            usersGroupsDAO.setUserGroups(email, groups);
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public List<String> getAllUserNames() throws VipException {
        return getUsers().stream().map(User::getFullName).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<Boolean> getUserPropertiesGroups(String email)
            throws VipException {
        try {
            return usersGroupsDAO.getUserPropertiesGroups(email);

        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public List<User> getUsers() throws VipException {
        try {
            return userDAO.getUsers();
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public void removeUser(String email, boolean sendNotificationEmail)
            throws VipException {

        try {
            User user = getUser(email);

            gridaPoolClient.removeOperationsByUser(email);

            gridaPoolClient.delete(server.getDataManagerUsersHome() + "/"
                    + user.getFolder(), user.getEmail());
            gridaPoolClient.delete(server.getDataManagerUsersHome() + "/"
                    + user.getFolder() + "_" + CoreConstants.FOLDER_TRASH, user.getEmail());

            userDAO.remove(email);

            if (sendNotificationEmail) {

                String adminsEmailContents = "<html>"
                        + "<head></head>"
                        + "<body>"
                        + "<p>Dear Administrators,</p>"
                        + "<p>The following user removed her/his account:</p>"
                        + "<p><b>First Name:</b> " + user.getFirstName() + "</p>"
                        + "<p><b>Last Name:</b> " + user.getLastName() + "</p>"
                        + "<p><b>Email:</b> " + user.getEmail() + "</p>"
                        + "<p><b>Institution:</b> " + user.getInstitution() + "</p>"
                        + "<p>&nbsp;</p>"
                        + "<p>Best Regards,</p>"
                        + "<p>VIP Team</p>"
                        + "</body>"
                        + "</html>";

                emailBusiness.sendEmailToAdmins("[VIP Admin] Account Removed", adminsEmailContents,
                        true, user.getEmail());
            }
        } catch (GRIDAClientException ex) {
            logger.error("Error removing user {}", email, ex);
            throw new VipException(ex);
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public User getNewUser(String email, String firstName, String lastName, String institution) {
        CountryCode cc = CountryCode.aq;

        String country = "";

        try {
            country = email.substring(email.lastIndexOf('.') + 1);
        } catch (NullPointerException e) {
            logger.warn("Error finding country from email {}", email, e);
        }

        try {
            if (CountryCode.valueOf(country) != null) {
                cc = CountryCode.valueOf(country);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Cannot determine country from email extension {}: user will be mapped to Antartica", country,
                    e);
        }

        return new User(
                firstName.trim(),
                lastName.trim(),
                email.trim(),
                institution.trim(),
                "0000",
                cc, new Timestamp(System.currentTimeMillis()));
    }

    public User getUser(String email) throws VipException {
        try {
            return userDAO.get(email);
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public User getUserWithGroups(String email) throws VipException {
        try {
            User user = userDAO.get(email);
            user.setGroups(usersGroupsDAO.getUserGroups(email));
            return user;
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public void updateTermsOfUse(String email) throws VipException {
        try {
            userDAO.updateTermsOfUse(email, new Timestamp(System.currentTimeMillis()));
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public void updateLastUpdatePublication(String email) throws VipException {
        try {
            userDAO.updateLastUpdatePublication(email, new Timestamp(System.currentTimeMillis()));
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public void updateUser(String email, UserLevel level, CountryCode countryCode,
            int maxRunningSimulations, boolean locked)
            throws VipException {
        try {
            userDAO.update(email, level, countryCode, maxRunningSimulations, locked);
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public void updateUserLastLogin(String email) throws VipException {
        try {
            userDAO.updateLastLogin(email, new Date());
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public List<User> getUsersFromGroup(String groupName) throws VipException {
        try {
            return usersGroupsDAO.getUsersFromGroup(groupName);

        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public String getFromApikey(String email) throws VipException {
        try {
            return userDAO.getUserApikey(email);
        } catch (DAOException e) {
            throw new VipException(e);
        }
    }

    public User getUserWithSession(String email) throws DAOException {
        String session = UUID.randomUUID().toString();
        userDAO.updateSession(email, session);

        return userDAO.get(email);
    }

    public boolean testLastUpdatePublication(String email) throws VipException {
        try {
            if (userDAO.getLastPublicationUpdate(email) == null) {
                return true;
            } else {
                Calendar cal = Calendar.getInstance();
                cal.setTime(userDAO.getLastPublicationUpdate(email));
                cal.add(Calendar.MONTH, server.getNumberMonthsToTestLastPublicationUpdates());
                Timestamp ts = new Timestamp(cal.getTime().getTime());
                return ts.before(new Timestamp(System.currentTimeMillis()));
            }
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public String getUserApikey(String email) throws VipException {
        try {
            return userDAO.getUserApikey(email);
        } catch (DAOException e) {
            throw new VipException(e);
        }
    }

    public void deleteUserApikey(String email) throws VipException {
        try {
            userDAO.updateUserApikey(email, null);
        } catch (DAOException e) {
            throw new VipException(e);
        }
    }

    public String generateNewUserApikey(String email) throws VipException {
        try {
            SecureRandom random = new SecureRandom();
            String apikey = new BigInteger(130, random).toString(32);
            userDAO.updateUserApikey(email, apikey);
            return apikey;
        } catch (DAOException e) {
            throw new VipException(e);
        }
    }

        public void resetNextEmail(String currentEmail) throws VipException {
        try {
            userDAO.updateNextEmail(currentEmail, null);
        } catch (DAOException e) {
            throw new VipException(e);
        }
    }

    public void addUserToGroup(String email, String groupName) throws VipException {
        try {
            usersGroupsDAO.add(email, groupName, GROUP_ROLE.User);
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public void removeUserFromGroup(String email, String groupName)
            throws VipException {
        try {
            usersGroupsDAO.removeUserFromGroup(email, groupName);

        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }
}
