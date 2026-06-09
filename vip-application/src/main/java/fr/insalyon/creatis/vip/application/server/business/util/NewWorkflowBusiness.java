package fr.insalyon.creatis.vip.application.server.business.util;

import fr.insalyon.creatis.moteur.plugins.workflowsdb.dao.WorkflowDAO;
import fr.insalyon.creatis.moteur.plugins.workflowsdb.dao.WorkflowsDBDAOException;
import fr.insalyon.creatis.vip.application.client.view.ApplicationError;
import fr.insalyon.creatis.vip.application.client.view.monitor.WorkflowStatus;
import fr.insalyon.creatis.vip.application.models.*;
import fr.insalyon.creatis.vip.application.models.boutiquesTools.BoutiquesInput;
import fr.insalyon.creatis.vip.application.server.business.AppVersionBusiness;
import fr.insalyon.creatis.vip.application.server.business.EngineBusiness;
import fr.insalyon.creatis.vip.application.server.business.ResourceBusiness;
import fr.insalyon.creatis.vip.application.server.business.WorkflowExecutionBusiness;
import fr.insalyon.creatis.vip.core.client.DefaultError;
import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.server.business.EmailBusiness;
import fr.insalyon.creatis.vip.core.server.business.Server;
import fr.insalyon.creatis.vip.core.server.business.base.CommonBusiness;
import fr.insalyon.creatis.vip.datamanager.client.view.DataManagerException;
import fr.insalyon.creatis.vip.datamanager.server.DataManagerUtil;
import fr.insalyon.creatis.vip.datamanager.server.business.ExternalPlatformBusiness;
import fr.insalyon.creatis.vip.datamanager.server.business.LfcPathsBusiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static fr.insalyon.creatis.vip.core.client.CoreModule.user;

@Service
@Transactional
public class NewWorkflowBusiness extends CommonBusiness {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Server server;
    private final WorkflowDAO workflowDAO;
    private final AppVersionBusiness appVersionBusiness;
    private final ExternalPlatformBusiness externalPlatformBusiness;
    private final LfcPathsBusiness lfcPathsBusiness;
    private final ResourceBusiness resourceBusiness;
    private final EngineBusiness engineBusiness;
    private final WorkflowExecutionBusiness workflowExecutionBusiness;
    private final EmailBusiness emailBusiness;

    @Autowired
    public NewWorkflowBusiness(Server server, WorkflowDAO workflowDAO, AppVersionBusiness appVersionBusiness,
                               ExternalPlatformBusiness externalPlatformBusiness,
                               LfcPathsBusiness lfcPathsBusiness, ResourceBusiness resourceBusiness,
                               EngineBusiness engineBusiness, WorkflowExecutionBusiness workflowExecutionBusiness,
                               EmailBusiness emailBusiness) {
        this.server = server;
        this.workflowDAO = workflowDAO;
        this.appVersionBusiness = appVersionBusiness;
        this.externalPlatformBusiness = externalPlatformBusiness;
        this.lfcPathsBusiness = lfcPathsBusiness;
        this.resourceBusiness = resourceBusiness;
        this.engineBusiness = engineBusiness;
        this.workflowExecutionBusiness = workflowExecutionBusiness;
        this.emailBusiness = emailBusiness;
    }

    public synchronized Workflow launch(Workflow workflow) throws VipException {
        checkVIPCapacities(getUser(), workflow.getWorkflowName());

        // TODO check stuff : no id, no status
        // checkWorkflowForLaunch

        AppVersion appVersion = appVersionBusiness.getVersion(workflow.getApplicationName(), workflow.getApplicationVersion());
        List<Map<String, List<String>>> parametersMapList = new ArrayList<>();
        parametersMapList.add(getParameters(appVersion, workflow));

        List<Resource> resources = resourceBusiness.getAvailableForExecution(getUser(), appVersion);
        if (resources.isEmpty()) {
            throw new VipException("There are no ressources available for the moment !");
        }
        Resource resource = resources.getFirst();
        Engine engine = engineBusiness.selectEngine(engineBusiness.getUsableEngines(resource));

        String workflowId;
        try {
            workflowId = workflowExecutionBusiness.launch(workflow.getWorkflowName(), resource, engine, appVersion, parametersMapList);
        } catch (Exception e) {
            String mailSubject = "[VIP] Warn: Workflow submission failed!!";
            String mailContent = "An error occured while submitting a workflow";
            Exception exceptionToRethrow = e;

            if (e instanceof VipException vipEx && ! vipEx.getVipError().equals(DefaultError.GENERIC_ERROR_WITH_MESSAGE)) {
                // not default error code, so it's an intended errors, only warning by mail
                logger.warn("Error occurred during workflow submission. Not disabling, sending mail to admins");
            } else {
                if ( ! (e instanceof VipException)) {
                    logger.error("Unexpected exception while launching a workflow", e);
                    exceptionToRethrow = new VipException(ApplicationError.LAUNCH_ERROR, e);
                }
                logger.warn(
                        "Error occurred during workflow submission. Disabling engine and sending mail to admins");
                mailSubject = "[VIP] Urgent: VIP engine disabled !";
                mailContent = "Engine " + engine.getName() + " has just been disabled.";
                engine.setStatus("disabled");
                engineBusiness.update(engine);
            }
            mailContent += "\n\nException:" + e.getMessage() + "\nStacktrace: " + e.getStackTrace();
            emailBusiness.sendEmailToAdmins(mailSubject, mailContent, true, user.getEmail());
            throw (VipException) exceptionToRethrow;
        }
        logger.info("Launched workflow {}", workflow);

        workflow.setId(workflowId);
        workflow.setStatus(WorkflowStatus.Running);
        workflow.setStartDate(new Date());
        workflow.setUser(getUser());
        addWorkflowInDb(workflow, engine);
        return workflow;
    }

    private void addWorkflowInDb(Workflow workflow, Engine engine) throws VipException {
        fr.insalyon.creatis.moteur.plugins.workflowsdb.bean.Workflow dbWorkflow =
                new fr.insalyon.creatis.moteur.plugins.workflowsdb.bean.Workflow(
                        workflow.getID(),
                        getUser().getFullName(),
                        fr.insalyon.creatis.moteur.plugins.workflowsdb.bean.WorkflowStatus.Running,
                        workflow.getStartDate(),
                        null,
                        workflow.getWorkflowName(),
                        workflow.getApplicationName(),
                        workflow.getApplicationVersion(),
                        null,
                        engine.getName(),
                        null
                );
        try {
            workflowDAO.add(dbWorkflow);
        } catch (WorkflowsDBDAOException e) {
            logger.error("Error launching simulation {}", workflow.getWorkflowName(), e);
            throw new VipException(e);
        }
    }

    private void checkVIPCapacities(User user, String workflowName) throws VipException {
        long runningWorkflows;
        long runningSimulations;
        try {
            runningWorkflows = workflowDAO.getNumberOfRunning(user.getFullName());
            runningSimulations = workflowDAO.getRunning().size();
        } catch (WorkflowsDBDAOException e) {
            logger.error("Error getting workflows stats when launching {}", workflowName, e);
            throw new VipException(e);
        }

        if (runningSimulations >= server.getMaxPlatformRunningSimulations()) {
            logger.warn("Unable to launch execution '{}': max number of"
                            + " running workflows reached in the platform : {}",
                    workflowName, runningSimulations);
            throw new VipException(ApplicationError.PLATFORM_MAX_EXECS);
        } else if (runningWorkflows >= user.getMaxRunningSimulations()) {
            logger.warn("Unable to launch execution '{}': max number of "
                            + "running workflows reached ({}/{}) for user '{}'.",
                    workflowName, runningWorkflows,
                    user.getMaxRunningSimulations(), user);
            throw new VipException(ApplicationError.USER_MAX_EXECS, runningWorkflows);
        }
    }

    private Map<String, List<String>> getParameters(AppVersion appVersion, Workflow workflow) throws VipException {
        Map<String, List<String>> parameters = new HashMap<>();

        // TODO : verify boutiques types
        // TODO : manage results-dir properly

        for (String inputName : workflow.getInputs().keySet()) {
            WorkflowInput workflowInput = workflow.getInputs().get(inputName);
            List<String> data = new ArrayList<>();
            if (workflowInput.isInterval()) {
                List<Double> interval = workflowInput.getInterval();
                if (interval.size() != 3) {
                    throw new VipException("Input interval must have 3 elements");
                }
                for (double d = interval.get(0); d <= interval.get(1); d += interval.get(2)) {
                    data.add(d + "");
                }
            } else if (workflowInput.getType() == BoutiquesInput.InputType.FILE) {
                // TODO : check file exist
                for (String inputValue : workflowInput.getValues()) {
                    data.add(transformParameter(inputName, inputValue));
                }
            } else {
                data.addAll(workflowInput.getValues());
            }
            if ( ! data.isEmpty()) {
                parameters.put(inputName, data);
            }
        }
        return parameters;
    }

    private String transformParameter(String parameterName, String parameterValue)
            throws VipException {

        parameterValue = parameterValue.trim();

        ExternalPlatformBusiness.ParseResult parseResult = externalPlatformBusiness
                .parseParameter(parameterName, parameterValue, getUser());
        if (parseResult.isUri) {
            // The uri has been generated
            return parseResult.result;
        }
        // not an external platform parameter, use legacy format
        String parsedPath = lfcPathsBusiness.parseBaseDir(getUser(), parameterValue);
        if ( ! getUser().isSystemAdministrator()) {
            checkFolderACL(parsedPath);
        }
        return (server.useLocalFilesInInputs() ? "file:" : "lfn:") + parsedPath;
    }

    private void checkFolderACL(String path)
            throws VipException {
        if (path.startsWith(server.getDataManagerUsersHome())) {

            path = path.replace(server.getDataManagerUsersHome() + "/", "");
            if (!path.startsWith(getUser().getFolder())) {

                logger.error("User {} tried to access data from another user: {}", getUser(), path);
                throw new VipException("Access denied to another user's home.");
            }
        } else if (path.startsWith(server.getDataManagerGroupsHome())) {

            path = path.replace(server.getDataManagerGroupsHome() + "/", "");
            if (path.contains("/")) {
                path = path.substring(0, path.indexOf("/"));
            }

            if (!DataManagerUtil.getPaths(getUser().getGroups()).contains(path)) {
                logger.error("User {} tried to access data from a non-autorized group: {}", getUser(), path);
                throw new VipException("Access denied to group '" + path + "'.");
            }
        }
    }
}
