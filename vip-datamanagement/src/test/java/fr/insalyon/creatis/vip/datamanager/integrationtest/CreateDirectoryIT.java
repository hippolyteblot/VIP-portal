package fr.insalyon.creatis.vip.datamanager.integrationtest;

import fr.insalyon.creatis.vip.core.client.view.user.UserLevel;
import fr.insalyon.creatis.vip.core.integrationtest.BaseInternalApiSpringIT;
import fr.insalyon.creatis.vip.core.models.Group;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.datamanager.client.DataManagerConstants;
import fr.insalyon.creatis.vip.datamanager.server.controller.dto.StorageCreateDirectoryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CreateDirectoryIT extends BaseInternalApiSpringIT {

    /**
     * when creating a directory.
     * Needs :
     * - a pathinfo on the path (verify not exist)
     * - a pathinfo on the parent (verify exist and a folder) if necessary
     */

    private User basicUser;
    private Group groupTest1;
    private Group groupTest2;

    @Autowired StorageTestConfigurer utils;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        asAdminContext(() -> {
            groupTest1 = createGroup("groupTest1");
            groupTest2 = createGroup("groupTest2");
            basicUser = createUserInGroup(emailUser1, groupTest1.getName());
        });
    }

    public void expectOkOnUserPath(User user, String newDirPath) throws Exception {
        Path path = Path.of("/vip/Home", newDirPath);
        mockMvc.perform(post("/internal/storage/directories")
                        .with(getUserSecurityMock(user))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new StorageCreateDirectoryRequest(
                                path.getParent().toString(),
                                path.getFileName().toString()))))
                .andDo(print())
                .andExpect(status().isCreated());

        utils.verifyCreateFolderForUser(user, newDirPath, 1);
    }

    public void expectOkOnGroupPath(User user, Group group, String newDirPath) throws Exception {
        Path path = Path.of("/vip",  group.getName() + DataManagerConstants.GROUP_APPEND, newDirPath);
        mockMvc.perform(post("/internal/storage/directories")
                        .with(getUserSecurityMock(user))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new StorageCreateDirectoryRequest(
                                path.getParent().toString(),
                                path.getFileName().toString()))))
                .andDo(print())
                .andExpect(status().isCreated());

        utils.verifyCreateFolderForGroup(group, newDirPath, 1);
    }

    public void expectBadRequestOnPath(User user, String newDirPath, Integer expectedErrorCode) throws Exception {
        Path path = Path.of(newDirPath);
        mockMvc.perform(post("/internal/storage/directories")
                        .with(getUserSecurityMock(user))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new StorageCreateDirectoryRequest(
                                path.getParent().toString(),
                                path.getFileName().toString()))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(expectedErrorCode));

        utils.verifyNoCreateFolder();
    }

    public void expectForbiddenOnPath(User user, String newDirPath, Integer expectedErrorCode) throws Exception {
        Path path = Path.of(newDirPath);
        mockMvc.perform(post("/internal/storage/directories")
                        .with(getUserSecurityMock(user))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new StorageCreateDirectoryRequest(
                                path.getParent().toString(),
                                path.getFileName().toString()))))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(expectedErrorCode));

        utils.verifyNoCreateFolder();
    }

    // Creating in Home
    @Test
    public void testDirectoryCreationInHome() throws Exception {
        utils.configureNonExistingElementForUser(basicUser, "newDir");

        expectOkOnUserPath(basicUser, "newDir");
    }

    // Creating in Home subdir
    @Test
    public void testDirectoryCreationInHomeSubdir() throws Exception {
        utils.configureFolderForUser(basicUser, "path/to", (String) null);
        utils.configureNonExistingElementForUser(basicUser, "path/to/newDir");

        expectOkOnUserPath(basicUser, "path/to/newDir");
    }

    // Forbidden for basic user in group
    @Test
    public void testDirectoryCreationKoInGroupForBeginner() throws Exception {
        expectForbiddenOnPath(basicUser, "/vip/groupTest1 (group)/newDir", 9999);
    }

    // allowed for advanced user in group
    @Test
    public void testDirectoryCreationOkInGroupForAdvandced() throws Exception {
        asAdminContext(() -> {
            basicUser.setLevel(UserLevel.Advanced);
            userBusiness.update(basicUser);
        });
        utils.configureNonExistingElementForGroup(groupTest1, "newDir");

        expectOkOnGroupPath(basicUser, groupTest1, "newDir");

        // but only in its group
        Mockito.reset(gridaClient);

        expectForbiddenOnPath(basicUser, "/vip/groupTest2 (group)/newDir", 9999);
    }

     /*
        Error cases :
        - dir/file already exist
        - parent dir does not exist
        - parent dir is a file
        - creating /vip or /vip/something
        - dir not starting with /vip
        - creating with forbidden ..
     */

    @Test
    public void testSomeErrorCases() throws Exception {

        // dir already exist
        utils.configureFolderForUser(basicUser, "newDir", (String) null);

        expectBadRequestOnPath(basicUser, "/vip/Home/newDir", 9999);

        // dir already exist as a file
        Mockito.reset(gridaClient);

        utils.configureFileForUser(basicUser, "newDir");

        expectBadRequestOnPath(basicUser, "/vip/Home/newDir", 9999);

        // Parent dir does not exist
        Mockito.reset(gridaClient);

        utils.configureNonExistingElementForUser(basicUser, "path/to");
        utils.configureNonExistingElementForUser(basicUser, "path/to/newDir");

        expectBadRequestOnPath(basicUser, "/vip/Home/path/to/newDir", 9999);

        // Parent dir exist as a file
        Mockito.reset(gridaClient);

        utils.configureFileForUser(basicUser, "path/to");
        utils.configureNonExistingElementForUser(basicUser, "path/to/newDir");

        expectBadRequestOnPath(basicUser, "/vip/Home/path/to/newDir", 9999);
    }


    @Test
    public void testOtherErrorCases() throws Exception {
        // creating /vip
        expectBadRequestOnPath(basicUser, "/vip", 9999);

        // creating /vip/Home
        Mockito.reset(gridaClient);
        expectBadRequestOnPath(basicUser, "/vip/Home", 9999);

        // creating /vip/group (group)
        Mockito.reset(gridaClient);
        expectBadRequestOnPath(basicUser, "/vip/groupTest1 (group)", 9999);
        Mockito.reset(gridaClient);
        expectBadRequestOnPath(basicUser, "/vip/unknownGroup (group)", 9999);

        // creating /vip/Anything
        Mockito.reset(gridaClient);
        expectBadRequestOnPath(basicUser, "/vip/Anything", 9999);

        // creating /NOTVIP
        Mockito.reset(gridaClient);
        expectBadRequestOnPath(basicUser, "/NOTVIP/Home", 9999);

        // creating /vip/../../something
        Mockito.reset(gridaClient);
        expectBadRequestOnPath(basicUser, "/vip/Home/../../../secret_stuff.txt", 9999);
    }

}
