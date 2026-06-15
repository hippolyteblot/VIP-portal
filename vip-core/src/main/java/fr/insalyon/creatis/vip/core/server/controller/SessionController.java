package fr.insalyon.creatis.vip.core.server.controller;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.insalyon.creatis.devtools.MD5;
import fr.insalyon.creatis.vip.core.client.DefaultError;
import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.client.view.user.UserLevel;
import fr.insalyon.creatis.vip.core.client.view.util.CountryCode;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.server.business.CoreUtil;
import fr.insalyon.creatis.vip.core.server.business.SessionBusiness;
import fr.insalyon.creatis.vip.core.server.business.UserBusiness;
import fr.insalyon.creatis.vip.core.server.dao.DAOException;
import fr.insalyon.creatis.vip.core.server.dao.UserDAO;
import fr.insalyon.creatis.vip.core.server.model.AuthenticationCredentials;
import fr.insalyon.creatis.vip.core.server.model.Session;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/session")
public class SessionController {

    private static final String DEMO_EMAIL = "demo@vip.local";

    private final SessionBusiness sessionBusiness;
    private final UserBusiness userBusiness;
    private final UserDAO userDAO;
    final private Supplier<User> userProvider;

    @Autowired
    public SessionController(SessionBusiness sessionBusiness, Supplier<User> userProvider, UserBusiness userBusiness, UserDAO userDAO) {
        this.sessionBusiness = sessionBusiness;
        this.userBusiness = userBusiness;
        this.userDAO = userDAO;
        this.userProvider = userProvider;
    }

    @GetMapping
    public Session getSession(HttpServletRequest request, HttpServletResponse response) throws VipException {
        User user = userProvider.get();
        Session session = sessionBusiness.getSession(user);

        try {
            // renew existing cookies
            sessionBusiness.createLoginCookies(request, response, session);
            userBusiness.updateUserLastLogin(user.getEmail());

            return session;
        } catch (UnsupportedEncodingException e) {
            throw new VipException("Failed to retrieve user session!", e);
        }
    }

    @GetMapping("/demo-login")
    public Session demoLogin(HttpServletRequest request, HttpServletResponse response) throws VipException {
        try {
            User user;
            try {
                user = userDAO.get(DEMO_EMAIL);
            } catch (DAOException e) {
                Timestamp now = new Timestamp(System.currentTimeMillis());
                String folder = "demo_user";

                user = new User(
                    CoreUtil.createUUID(), "Demo", "User",
                    DEMO_EMAIL, "Demo Institution",
                    UserLevel.Advanced, CountryCode.fr
                );
                user.setConfirmed(true);
                user.setAccountLocked(false);
                user.setFolder(folder);
                user.setCode(UUID.randomUUID().toString());
                user.setRegistration(now);
                user.setLastLogin(now);
                user.setTermsOfUse(now);
                user.setLastUpdatePublications(now);
                user.setFailedAuthentications(0);
                user.setPassword(MD5.get("demo"));
                user.setApiKey(null);
                userDAO.add(user);
                userDAO.definePassword(DEMO_EMAIL, MD5.get("demo"));
            }

            user = userBusiness.getUserWithSession(DEMO_EMAIL);
            Session session = sessionBusiness.getSession(user);
            sessionBusiness.createLoginCookies(request, response, session);
            userBusiness.updateUserLastLogin(user.getEmail());
            return session;
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | DAOException e) {
            throw new VipException("Failed to create demo session", e);
        }
    }

    @PostMapping
    public Session createSession(@RequestBody @Valid AuthenticationCredentials credentials, HttpServletRequest request,
            HttpServletResponse response)
            throws VipException {
        try {
            Session session = sessionBusiness.signin(credentials);

            sessionBusiness.createLoginCookies(request, response, session);
            return session;
        } catch (UnsupportedEncodingException | VipException e) {
            if (e.getMessage().startsWith("Authentication failed")) {
                throw new VipException(DefaultError.BAD_CREDENTIALS);
            }
            throw new VipException("Failed to create user session!", e);
        }
    }

    @DeleteMapping
    public void deleteSession(HttpServletRequest request, HttpServletResponse response) throws VipException {
        sessionBusiness.signout();
        sessionBusiness.clearLoginCookies(response);
    }
}
