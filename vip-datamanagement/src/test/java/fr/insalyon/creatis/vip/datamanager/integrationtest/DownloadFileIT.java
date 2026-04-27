package fr.insalyon.creatis.vip.datamanager.integrationtest;

import fr.insalyon.creatis.grida.common.bean.Operation;
import fr.insalyon.creatis.vip.core.integrationtest.BaseInternalApiSpringIT;
import fr.insalyon.creatis.vip.core.models.Group;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.datamanager.models.StorageDownloadRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class DownloadFileIT extends BaseInternalApiSpringIT {

    /**
     * can download a file or folder
     * Needs for grida mock :
     * - a pathinfo on the path (verify exist and a file)
     * - a get for a download operation
     * - update for a download operation
     * - full info in operation when finished
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

    public void expectDownloadOperationInHome(User user, String homePath, String operationId) throws Exception {
        String operationStatus = "Queued";
        Path path = Path.of("/vip/Home", homePath);
        mockMvc.perform(post("/internal/storage/downloads")
                        .with(getUserSecurityMock(user))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new StorageDownloadRequest(
                                path.toString()))))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpectAll(utils.getOperationMatcher(operationId, operationStatus));
    }

    public void expectOperation(User user, String operationId, String operationStatus) throws Exception {
        mockMvc.perform(get("/internal/storage/operations/" + operationId)
                        .with(getUserSecurityMock(user)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpectAll(utils.getOperationMatcher(operationId, operationStatus));
    }

    // Downloading in Home
    @Test
    public void testDownloadInHome() throws Exception {
        utils.configureFileForUser(basicUser, "file.txt");
        utils.configureOperationForElementInHome(basicUser, "file.txt", "test-operation-id");

        expectDownloadOperationInHome(basicUser, "file.txt", "test-operation-id");

        // also ok in subdir
        resetGridaMocks();
        utils.configureFileForUser(basicUser, "path/to/file.txt");
        utils.configureOperationForElementInHome(basicUser, "path/to/file.txt", "test-operation-id");

        expectDownloadOperationInHome(basicUser, "path/to/file.txt", "test-operation-id");

        // same for folder
        resetGridaMocks();
        utils.configureFolderForUser(basicUser, "somefolder", (String) null);
        utils.configureOperationForElementInHome(basicUser, "somefolder", "test-operation-id");

        expectDownloadOperationInHome(basicUser, "somefolder", "test-operation-id");

        // also ok for folder in subdir
        resetGridaMocks();
        utils.configureFolderForUser(basicUser, "path/to/somefolder", (String) null);
        utils.configureOperationForElementInHome(basicUser, "path/to/somefolder", "test-operation-id");

        expectDownloadOperationInHome(basicUser, "path/to/somefolder", "test-operation-id");
    }

    @Test
    public void testGetOperation() throws Exception {
        utils.configureOperation(basicUser, "test-operation-id", Operation.Status.Running);

        expectOperation(basicUser, "test-operation-id", "Running");
        resetGridaMocks();

        utils.configureOperation(basicUser, "test-operation-id", Operation.Status.Done);

        expectOperation(basicUser, "test-operation-id", "Done");
    }

    @Test
    public void testGetOperationFromSomeoneElse() throws Exception {
        User otherUser = createUser(emailUser2, "otherTestUser");
        utils.configureOperation(otherUser, "test-operation-id", Operation.Status.Running);

        mockMvc.perform(get("/internal/storage/operations/test-operation-id")
                        .with(getUserSecurityMock(basicUser)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(9999));
    }

    // TODO : error cases : unknown operation

    @Test
    public void testDownloadContent() throws Exception {
        Resource testFileResource = getResourceFromClasspath("filesForDownload/testFile.txt");
        // TODO : verify it's actually called only once
        utils.configureOperationWithContent(basicUser, "test-operation-id", testFileResource.getFile().toPath());

        String operationId = "test-operation-id";
        User user = basicUser;

        mockMvc.perform(get("/internal/storage/downloads/" + operationId + "/content")
                        .with(getUserSecurityMock(user)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().string(testFileResource.getContentAsString(StandardCharsets.UTF_8)));

        Mockito.verify(gridaPoolClient, Mockito.times(1)).getOperationById(Mockito.anyString());
    }

}
