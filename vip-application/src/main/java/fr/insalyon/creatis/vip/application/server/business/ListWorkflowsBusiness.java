package fr.insalyon.creatis.vip.application.server.business;

import fr.insalyon.creatis.moteur.plugins.workflowsdb.dao.WorkflowDAO;
import fr.insalyon.creatis.moteur.plugins.workflowsdb.dao.WorkflowsDBDAOException;
import fr.insalyon.creatis.vip.application.client.view.monitor.WorkflowStatus;
import fr.insalyon.creatis.vip.application.models.Workflow;
import fr.insalyon.creatis.vip.core.client.DefaultError;
import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.server.business.UserBusiness;
import fr.insalyon.creatis.vip.core.server.business.base.CommonBusiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ListWorkflowsBusiness extends CommonBusiness {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final WorkflowDAO workflowDAO;
    private final WorkflowExecutionBusiness workflowExecutionBusiness;
    private final UserBusiness userBusiness;

    @Autowired
    public ListWorkflowsBusiness(WorkflowDAO workflowDAO, WorkflowExecutionBusiness workflowExecutionBusiness, UserBusiness userBusiness) {
        this.workflowDAO = workflowDAO;
        this.workflowExecutionBusiness = workflowExecutionBusiness;
        this.userBusiness = userBusiness;
    }

    public Workflow getRefreshedSimulation(String workflowId) throws VipException {
        return getSimulation(workflowId, true);
    }

    public Workflow getNotRefreshedSimulation(String workflowId) throws VipException {
        return getSimulation(workflowId, false);
    }

    private Workflow getSimulation(String workflowId, boolean refresh) throws VipException {
        Workflow workflow;

        try {
            fr.insalyon.creatis.moteur.plugins.workflowsdb.bean.Workflow dbWorkflow = workflowDAO.get(workflowId);
            if (dbWorkflow == null) {
                logger.error("Cannot find execution with id {}", workflowId);
                throw new VipException("Cannot find execution with id " + workflowId);
            }
            workflow = parseDbWorkflow(dbWorkflow);

            if ( ! getUser().isSystemAdministrator() && !Objects.equals(workflow.getUserId(), getUser().getId())) {
                logger.error("unauthorized access to workflow {} by {}", workflowId, getUser().getEmail());
                throw new VipException(DefaultError.ACCESS_DENIED);
            }

            if (refresh) {
                refreshRunningSimulations(List.of(workflow));
            }

        } catch (WorkflowsDBDAOException ex) {
            logger.error("Error getting simulation {}", workflowId, ex);
            throw new VipException(ex);
        }

        return workflow;
    }

    public List<Workflow> getAllWorkflows(Date lastDate) throws VipException {
        if ( ! getUser().isSystemAdministrator() ) {
            logger.error("unauthorized access to all workflows by {}", getUser().getEmail());
            throw new VipException(DefaultError.ACCESS_DENIED);
        }
        return getWorkflows(null, lastDate);
    }

    public List<Workflow> getCurrentUserWorflows(Date lastDate) throws VipException {
        return getWorkflows(getUser(), lastDate);
    }

    private List<Workflow> getWorkflows(User user, Date lastDate) throws VipException {
        try {
            return parseDbWorkflows(workflowDAO.get(user != null ? user.getFullName() : null, lastDate));
        } catch (WorkflowsDBDAOException ex) {
            logger.error("Error getting simulations for {} since {}", user, lastDate, ex);
            throw new VipException(ex);
        }
    }

    public List<Workflow> searchAndRefreshWorkflows(String userName, String application, String status,
                                         Date startDate, Date endDate) throws VipException {

        return searchAndRefreshWorkflows(userName, application, status, startDate, endDate, null);
    }

    public List<Workflow> searchAndRefreshWorkflows(String userName, String application, String status, Date startDate, Date endDate, String tag) throws VipException {
        fr.insalyon.creatis.moteur.plugins.workflowsdb.bean.WorkflowStatus wStatus = (status != null) ? fr.insalyon.creatis.moteur.plugins.workflowsdb.bean.WorkflowStatus.valueOf(status) : null;
        List<String> users = (userName != null) ? Collections.singletonList(userName) : Collections.emptyList();
        List<String> applications = (application != null) ? Collections.singletonList(application) : new ArrayList<>();
        List<Workflow> workflows;

        // TODO : check rights on users and applications

        try {
            if (endDate != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(endDate);
                calendar.add(Calendar.DATE, 1);
                endDate = calendar.getTime();
            }

            workflows = parseDbWorkflows(workflowDAO.get(users, applications, wStatus, null, startDate, endDate, tag));
            refreshRunningSimulations(workflows);

            return workflows;

        } catch (WorkflowsDBDAOException ex) {
            logger.error("Error searching simulations for users {}", users, ex);
            throw new VipException(ex);
        }
    }

    private List<Workflow> parseDbWorkflows(List<fr.insalyon.creatis.moteur.plugins.workflowsdb.bean.Workflow> dbWorkflowList) throws VipException {
        List<String> usernameList = dbWorkflowList.stream().map(dbworkflow -> dbworkflow.getUsername()).toList();
        List<User> userList = userBusiness.getByFullnames(usernameList);
        // create map of unique names found
        // in case of duplicate, it is removed
        Map<String, User> usersByFullname = userList.stream().collect(Collectors.toMap(
                u -> u.getFullName(),
                u -> u,
                (existing, replacement) -> null));
        List<Workflow> workflows = new ArrayList<>();
        for (fr.insalyon.creatis.moteur.plugins.workflowsdb.bean.Workflow dbWorkflow : dbWorkflowList) {
            workflows.add(parseDbWorkflow(dbWorkflow, usersByFullname));
        }
        return workflows;
    }


    private Workflow parseDbWorkflow(fr.insalyon.creatis.moteur.plugins.workflowsdb.bean.Workflow dbWorkflow) throws VipException {
        return parseDbWorkflow(dbWorkflow, null);
    }

    private Workflow parseDbWorkflow(fr.insalyon.creatis.moteur.plugins.workflowsdb.bean.Workflow dbWorkflow, Map<String, User> usersByFullname) throws VipException {
        Optional<User> workflowUser;
        if (usersByFullname == null) {
            workflowUser = userBusiness.getByFullname(dbWorkflow.getUsername());
        } else {
            workflowUser = Optional.ofNullable(usersByFullname.get(dbWorkflow.getUsername()));
        }
        if (workflowUser.isEmpty()) {
            return new Workflow(
                    dbWorkflow.getId(),
                    dbWorkflow.getDescription(),
                    dbWorkflow.getApplication(),
                    dbWorkflow.getApplicationVersion(),
                    dbWorkflow.getUsername(),
                    dbWorkflow.getStatus().name(),
                    dbWorkflow.getStartedTime(),
                    dbWorkflow.getFinishedTime(),
                    dbWorkflow.getEngine(),
                    dbWorkflow.getTags());
        } else {
            return new Workflow(
                    dbWorkflow.getId(),
                    dbWorkflow.getDescription(),
                    dbWorkflow.getApplication(),
                    dbWorkflow.getApplicationVersion(),
                    workflowUser.get(),
                    dbWorkflow.getStatus().name(),
                    dbWorkflow.getStartedTime(),
                    dbWorkflow.getFinishedTime(),
                    dbWorkflow.getEngine(),
                    dbWorkflow.getTags());
        }
    }

    private void refreshRunningSimulations(List<Workflow> workflows) throws VipException, WorkflowsDBDAOException {
        for (Workflow workflow : workflows) {

            if (workflow.getStatus() == WorkflowStatus.Running
                    || workflow.getStatus() == WorkflowStatus.Unknown) {
                WorkflowStatus workflowStatus = workflowExecutionBusiness.getStatus(workflow.getEngineName(), workflow.getID());
                logger.debug("Simulation {} : old status : {}, new status : {} ",
                        workflow.getID(), workflow.getStatus(), workflowStatus);

                if (workflowStatus != workflow.getStatus()) {
                    workflow.setStatus(workflowStatus);
                    fr.insalyon.creatis.moteur.plugins.workflowsdb.bean.Workflow dbWorkflow = workflowDAO.get(workflow.getID());
                    dbWorkflow.setStatus(fr.insalyon.creatis.moteur.plugins.workflowsdb.bean.WorkflowStatus.valueOf(workflowStatus.name()));
                    workflowDAO.update(dbWorkflow);
                }
            }
        }
    }
}
