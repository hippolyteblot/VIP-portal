package fr.insalyon.creatis.vip.api.business;

import static fr.insalyon.creatis.vip.core.client.view.user.UserLevel.Beginner;

import java.sql.Timestamp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.client.view.util.CountryCode;
import fr.insalyon.creatis.vip.core.integrationtest.database.BaseSpringIT;
import fr.insalyon.creatis.vip.core.models.Group;
import fr.insalyon.creatis.vip.core.models.GroupType;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.server.business.ConfigurationBusiness;

public class ApiUserBusinessIT extends BaseSpringIT {

    @Autowired private ApiUserBusiness apiUserBusiness;
    @Autowired private ConfigurationBusiness configurationBusiness;

    private Group group1;
    private User user1;
    private final Timestamp now = new Timestamp(System.currentTimeMillis());

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Create test group
        group1 = new Group("group1", true, GroupType.getDefault());
        asAdminContext(() -> {
           groupBusiness.add(group1); 
        });

        // Create test users
        user1 = new User("firstName", "lastName", "email1@test.fr", "test1@test.fr", "institution", "password", false, "code", "folder", "session", now, now, Beginner, CountryCode.fr, 1, now, now, 0, false, null);
        apiUserBusiness.signup(user1, "comment");

    }

    @Test
    public void testInitialization() throws VipException {
        Assertions.assertEquals(2, userBusiness.getUsers().size(), "Incorrect number of users"); // admin + user1
    }

    @Test
    public void testSignup() throws VipException {
        User user2 = new User("firstName2", "lastName2", "email2@test.fr", "test3@test.fr", "institution", "password", false, "code", "folder", "session", now, now, Beginner, CountryCode.fr, 1, now, now, 0, false, null);
        apiUserBusiness.signup(user2, "comment");
        Assertions.assertEquals(3, userBusiness.getUsers().size(), "Incorrect number of users");
    }

    @Test
    public void testResetPassword() throws VipException {
        apiUserBusiness.resetPassword("email1@test.fr", userBusiness.getUser("email1@test.fr").getCode(), "test new password");
    }

    @Test
    public void testResetCode() throws VipException {
        String oldCode = userBusiness.getUser("email1@test.fr").getCode();
        apiUserBusiness.sendResetCode("email1@test.fr");
        String newCode = userBusiness.getUser("email1@test.fr").getCode();
        Assertions.assertFalse(oldCode.equals(newCode));
    }

}
