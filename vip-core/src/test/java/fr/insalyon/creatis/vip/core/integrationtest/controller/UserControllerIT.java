package fr.insalyon.creatis.vip.core.integrationtest.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import fr.insalyon.creatis.vip.core.client.DefaultError;
import fr.insalyon.creatis.vip.core.client.view.CoreConstants.GROUP_ROLE;
import fr.insalyon.creatis.vip.core.client.view.user.UserLevel;
import fr.insalyon.creatis.vip.core.client.view.util.CountryCode;
import fr.insalyon.creatis.vip.core.integrationtest.BaseInternalApiSpringIT;
import fr.insalyon.creatis.vip.core.models.Group;
import fr.insalyon.creatis.vip.core.models.GroupType;
import fr.insalyon.creatis.vip.core.models.SignUpForm;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.server.business.CoreUtil;

public class UserControllerIT extends BaseInternalApiSpringIT {

    private User adminUser;
    private User developperUser;
    private User basicUser;
    private User testUser;
    private Group privateGroup;
    private Group publicGroup;
    private SignUpForm form;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        adminUser = createUser(emailUser1, UserLevel.Administrator);
        developperUser = createUser(emailUser2, UserLevel.Developer);
        basicUser = createUser(emailUser3, UserLevel.Beginner);

        asAdminContext(() -> {
            createGroup("private", GroupType.APPLICATION, false);
            createGroup("public", GroupType.APPLICATION, true);

            privateGroup = groupBusiness.get("private");
            publicGroup = groupBusiness.get("public");
        });

        testUser = new User(CoreUtil.createUUID(), "test", "test", "test@insa.fr", "test", UserLevel.Beginner,
                CountryCode.fr);
        testUser.setId(null);
        form = new SignUpForm();
        form.comment = "test";
        form.user = testUser;
    }

    private Map<Group, GROUP_ROLE> asMapGroup(Set<Group> groups) {
        Map<Group, GROUP_ROLE> result = new HashMap<>();

        groups.forEach((g) -> result.put(g, GROUP_ROLE.User));
        return result;
    }

    @Test
    public void add() throws Exception {
        // no crsf token needed (same for session creation)
        // non authentified sign-up with private groups (=forbidden)
        testUser.setGroups(asMapGroup(Set.of(privateGroup, publicGroup)));
        mockMvc.perform(post("/internal/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(form)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value(DefaultError.ACCESS_DENIED.getCode()));

        // non authentified sign-up with public groups (=ok)
        testUser.setGroups(asMapGroup(Set.of(publicGroup)));
        mockMvc.perform(post("/internal/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist()); // check a non-visible field

        asAdminContext(() -> {
            userBusiness.remove(userBusiness.getUser(testUser.getEmail()).getId(), false);
        });

        // authentified users cannot create others "users" (=forbidden) (but admin can)
        mockMvc.perform(post("/internal/users")
                .with(getUserSecurityMock(basicUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(form)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value(DefaultError.UNAUTHENTIFIED_ONLY.getCode()));
        mockMvc.perform(post("/internal/users")
                .with(getUserSecurityMock(developperUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(form)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value(DefaultError.UNAUTHENTIFIED_ONLY.getCode()));
    }

    @Test
    public void update() throws Exception {
        // basic & developer cannot join private groups (=forbidden)
        basicUser.setGroups(asMapGroup(Set.of(privateGroup)));
        mockMvc.perform(put("/internal/users/" + basicUser.getId())
                .with(getUserSecurityMock(basicUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(basicUser)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value(DefaultError.ACCESS_DENIED.getCode()));
        developperUser.setGroups(asMapGroup(Set.of(privateGroup)));
        mockMvc.perform(put("/internal/users/" + developperUser.getId())
                .with(getUserSecurityMock(developperUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(developperUser)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value(DefaultError.ACCESS_DENIED.getCode()));

        // basic & developer can join public groups (=ok)
        basicUser.setGroups(asMapGroup(Set.of(publicGroup)));
        mockMvc.perform(put("/internal/users/" + basicUser.getId())
                .with(getUserSecurityMock(basicUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(basicUser)))
                .andExpect(status().isOk());
        developperUser.setGroups(asMapGroup(Set.of(publicGroup)));
        mockMvc.perform(put("/internal/users/" + developperUser.getId())
                .with(getUserSecurityMock(developperUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(developperUser)))
                .andExpect(status().isOk());

        // basic & developer cannot edit others
        mockMvc.perform(put("/internal/users/" + developperUser.getId())
                .with(getUserSecurityMock(basicUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(developperUser)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value(DefaultError.ACCESS_DENIED.getCode()));
        mockMvc.perform(put("/internal/users/" + basicUser.getId())
                .with(getUserSecurityMock(developperUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(basicUser)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value(DefaultError.ACCESS_DENIED.getCode()));

        // admin can edit others
        mockMvc.perform(put("/internal/users/" + developperUser.getId())
                .with(getUserSecurityMock(adminUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(developperUser)))
                .andExpect(status().isOk());
    }

    @Test
    public void remove() throws Exception {
        // user cannot delete someoneelse (=forbidden)
        mockMvc.perform(delete("/internal/users/" + developperUser.getId())
                .with(getUserSecurityMock(basicUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value(DefaultError.ACCESS_DENIED.getCode()));

        // user delete if self (=ok)
        mockMvc.perform(delete("/internal/users/" + basicUser.getId())
                .with(getUserSecurityMock(basicUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());

        // admin delete another user (=ok)
        mockMvc.perform(delete("/internal/users/" + developperUser.getId())
                .with(getUserSecurityMock(adminUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());
    }

    @Test
    public void getUser() throws Exception {
        // user & developer cannot see others (=forbidden)
        mockMvc.perform(get("/internal/users/" + developperUser.getId())
                .with(getUserSecurityMock(basicUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value(DefaultError.ACCESS_DENIED.getCode()));
        mockMvc.perform(get("/internal/users/" + basicUser.getId())
                .with(getUserSecurityMock(developperUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value(DefaultError.ACCESS_DENIED.getCode()));

        // user & developer see themselves (but filtered by JsonView) (=ok)
        mockMvc.perform(get("/internal/users/" + basicUser.getId())
                .with(getUserSecurityMock(basicUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(basicUser.getId()))
                .andExpect(jsonPath("$.confirmed").doesNotExist())
                .andExpect(jsonPath("$.accountLocked").doesNotExist());
        mockMvc.perform(get("/internal/users/" + developperUser.getId())
                .with(getUserSecurityMock(developperUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(developperUser.getId()))
                .andExpect(jsonPath("$.confirmed").doesNotExist())
                .andExpect(jsonPath("$.accountLocked").doesNotExist());

        // admin can see others accounts with more fields (but not everything) (=ok)
        mockMvc.perform(get("/internal/users/" + developperUser.getId())
                .with(getUserSecurityMock(adminUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(developperUser.getId()))
                .andExpect(jsonPath("$.confirmed").exists())
                .andExpect(jsonPath("$.accountLocked").exists())
                .andExpect(jsonPath("$.nextEmail").doesNotExist())
                .andExpect(jsonPath("$.failedAuthentications").doesNotExist());
        // himself
        mockMvc.perform(get("/internal/users/" + adminUser.getId())
                .with(getUserSecurityMock(adminUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adminUser.getId()))
                .andExpect(jsonPath("$.confirmed").exists())
                .andExpect(jsonPath("$.accountLocked").exists())
                .andExpect(jsonPath("$.nextEmail").doesNotExist())
                .andExpect(jsonPath("$.failedAuthentications").doesNotExist());

        // admin try to see non existing account
        mockMvc.perform(get("/internal/users/dotexist")
                .with(getUserSecurityMock(adminUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value(DefaultError.NOT_FOUND.getCode()));

    }

    @Test
    public void getAll() throws Exception {
        // user & developer cannot use the endpoint (=forbidden)
        mockMvc.perform(get("/internal/users")
                .with(getUserSecurityMock(basicUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value(DefaultError.ACCESS_DENIED.getCode()));
        mockMvc.perform(get("/internal/users")
                .with(getUserSecurityMock(developperUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value(DefaultError.ACCESS_DENIED.getCode()));

        // admin (=ok)
        mockMvc.perform(get("/internal/users")
                .with(getUserSecurityMock(adminUser))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].accountLocked").exists())
                // here we check that the JSONView still applies even if wrapper inside PrecisePage<T>
                .andExpect(jsonPath("$.data[0].nextCode").doesNotExist())
                .andExpect(jsonPath("$.data[0].password").doesNotExist())
                .andExpect(jsonPath("$.data[0].folder").doesNotExist());
    }
}
