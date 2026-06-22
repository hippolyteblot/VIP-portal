package fr.insalyon.creatis.vip.core.server.business;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.insalyon.creatis.grida.client.GRIDAClientException;
import fr.insalyon.creatis.grida.client.GRIDAPoolClient;
import fr.insalyon.creatis.vip.core.client.DefaultError;
import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.client.view.CoreConstants;
import fr.insalyon.creatis.vip.core.client.view.CoreConstants.GROUP_ROLE;
import fr.insalyon.creatis.vip.core.client.view.user.UserLevel;
import fr.insalyon.creatis.vip.core.client.view.util.CountryCode;
import fr.insalyon.creatis.vip.core.models.Group;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.models.UserAndPassword;
import fr.insalyon.creatis.vip.core.server.business.base.CommonBusiness;
import fr.insalyon.creatis.vip.core.server.dao.DAOException;
import fr.insalyon.creatis.vip.core.server.dao.UserDAO;
import fr.insalyon.creatis.vip.core.server.dao.UsersGroupsDAO;
import fr.insalyon.creatis.vip.core.server.inter.annotations.VIPExternalSafe;
import fr.insalyon.creatis.vip.core.server.model.PrecisePage;

@Service
@Transactional
public class UserBusiness extends CommonBusiness {

    private final UserDAO userDAO;
    private final UsersGroupsDAO usersGroupsDAO;
    private final EmailBusiness emailBusiness;
    private final GRIDAPoolClient gridaPoolClient;
    private final Server server;
    private final EmailTemplateUtils emailTemplateUtils;
    private final PasswordBusiness passwordBusiness;

    @Autowired
    public UserBusiness(UserDAO userDAO, UsersGroupsDAO usersGroupsDAO, EmailBusiness emailBusiness,
            GRIDAPoolClient gridaPoolClient, Server server, EmailTemplateUtils emailTemplateUtils,
            PasswordBusiness passwordBusiness) {
        this.userDAO = userDAO;
        this.usersGroupsDAO = usersGroupsDAO;
        this.emailBusiness = emailBusiness;
        this.gridaPoolClient = gridaPoolClient;
        this.server = server;
        this.emailTemplateUtils = emailTemplateUtils;
        this.passwordBusiness = passwordBusiness;
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

    private void loadMissingFields(User user, User existingUser) {
        // Fields never sent via JSON (no @JsonView, not in ProfileUpdatePayload)
        user.setNextEmail(existingUser.getNextEmail());
        user.setCode(existingUser.getCode());
        user.setSession(existingUser.getSession());
        user.setApiKey(existingUser.getApiKey());
        user.setFailedAuthentications(existingUser.getFailedAuthentications());
        user.setRegistration(existingUser.getRegistration());
        user.setLastLogin(existingUser.getLastLogin());

        // folder is absent from ProfileUpdatePayload but settable via admin API
        if (user.getFolder() == null) {
            user.setFolder(existingUser.getFolder());
        }

        // @JsonView(Admin.class) never sent by non admin callers
        if (user.isConfirmed() == null) user.setConfirmed(existingUser.isConfirmed());
        if (user.isAccountLocked() == null) user.setAccountLocked(existingUser.isAccountLocked());
    }

    @VIPExternalSafe
    public User update(User user) throws VipException {
        User existingUser = getUserWithGroups(user.getEmail());
        Set<Group> groupsToJoin = new HashSet<>(user.getGroups());
        Set<Group> groupsToLeave = new HashSet<>(existingUser.getGroups());

        groupsToJoin.removeAll(existingUser.getGroups());
        groupsToLeave.removeAll(user.getGroups());
    
        // we use JsonView to protect "sensitive"
        // fields from being edited (see @User.class)
        // not all can be handled like that, especially for groups
        if ( ! getUserLevel().equals(UserLevel.Administrator)) {
            if ( ! user.getId().equals(getUser().getId())) {
                // only admin can edit others accounts
                throw new VipException(DefaultError.ACCESS_DENIED);
            }

            // here user try to join private group (forbidden)
            // but it's okay if the user try to leave a private group
            if ( ! groupsToJoin.stream().allMatch(Group::isPublicGroup)) {
                throw new VipException(DefaultError.ACCESS_DENIED);
            }
        }

        // Preserve fields not sent in the JSON payload (hidden by @JsonView or absent).
        // Non-null values from the request are kept; null fields fall back to the existing record.
        loadMissingFields(user, existingUser);
        try {
            userDAO.update(user);

            for (Group group : groupsToLeave) {
                usersGroupsDAO.removeUserFromGroup(user.getEmail(), group.getName());
            }
            for (Group group : groupsToJoin) {
                usersGroupsDAO.add(user.getEmail(), group.getName(), GROUP_ROLE.User);
            }
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

    @VIPExternalSafe
    public List<User> searchUsers(String query, int limit) throws VipException {
        // only administrators and developers can perform this search
        if (!getUserLevel().equals(UserLevel.Administrator) && !getUserLevel().equals(UserLevel.Developer)) {
            throw new VipException(DefaultError.ACCESS_DENIED);
        }

        if (query == null || query.isBlank()) {
            return List.of();
        }

        int safeLimit = Math.max(1, Math.min(limit, 50));
        String normalized = query.trim().toLowerCase();

        // TODO : optimize by doing the filtering in the database instead of in memory
        return getUsers().stream()
                .filter((user) -> matchesQuery(user, normalized))
                .limit(safeLimit)
                // return abbreviated User objects to avoid leaking data
                .map(u -> new User(u.getId(), u.getFirstName(), u.getLastName()))
                .collect(Collectors.toList());
    }

    private boolean matchesQuery(User user, String query) {
        if (user == null) {
            return false;
        }

        Stream<String> fields = Stream.of(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName());

        return fields.filter((value) -> value != null && !value.isBlank())
                .map((value) -> value.toLowerCase())
                .anyMatch((value) -> value.contains(query));
    }

    @VIPExternalSafe
    public void remove(String id, boolean sendNotificationEmail) throws VipException {
        User user = get(id);

        if (user == null) {
            throw new VipException(DefaultError.NOT_FOUND, User.class.getSimpleName(), id);
        }
        if ( ! getUserLevel().equals(UserLevel.Administrator)) {
            if (user.getId() != getUser().getId()) {
                // only admin can remove "other" accounts
                throw new VipException(DefaultError.ACCESS_DENIED);
            }
        }
        try {
    
            gridaPoolClient.removeOperationsByUser(user.getEmail());

            gridaPoolClient.delete(server.getDataManagerUsersHome() + "/"
                    + user.getFolder(), user.getEmail());
            gridaPoolClient.delete(server.getDataManagerUsersHome() + "/"
                    + user.getFolder() + "_" + CoreConstants.FOLDER_TRASH, user.getEmail());

            userDAO.remove(user.getEmail());

            if (sendNotificationEmail) {

                String adminsEmailContents = emailTemplateUtils.removeAccount(user);
                emailBusiness.sendEmailToAdmins("[VIP Admin] Account Removed", adminsEmailContents,
                        true, user.getEmail());
            }
        } catch (GRIDAClientException ex) {
            logger.error("Error removing user id {}", id, ex);
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
                CoreUtil.createUUID(),
                firstName.trim(),
                lastName.trim(),
                email.trim(),
                institution.trim(),
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

    @VIPExternalSafe
    public User getCurrentUser() throws VipException {
        return getUser();
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

    @VIPExternalSafe
    public void updateUserPassword(String userId, String password) throws VipException {
        if (password == null || password.isBlank()) {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, "password", "Password is required!");
        }

        // Ensure user can only change their own password
        User currentUser = getCurrentUser();
        if (!currentUser.getId().equals(userId)) {
            throw new VipException(DefaultError.ACCESS_DENIED);
        }

        passwordBusiness.setPassword(currentUser.getEmail(), password);
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

    @VIPExternalSafe
    public User get(String id) throws VipException {
        // becareful sensitive fields are only removed when processing
        // the json response, before that, all users objects ARE NOT safe
        if ( ! getUserLevel().equals(UserLevel.Administrator) && ! getUser().getId().equals(id)) {
            throw new VipException(DefaultError.ACCESS_DENIED);
        } else {
            User user = userDAO.getById(id);

            if (user != null) {
                user.setGroups(usersGroupsDAO.getUserGroups(user.getEmail()));
            }

            return user;
        }
    }

    @VIPExternalSafe
    public PrecisePage<User> getAll(int offset, int quantity) throws VipException {
        permissions.filter((c) -> c.admin());

        List<User> users = getUsers();

        for (User u: users) {
            u.setGroups(usersGroupsDAO.getUserGroups(u.getEmail()));
        }

        return pageBuilder.doPrecise(offset, quantity, users);
    }
}
