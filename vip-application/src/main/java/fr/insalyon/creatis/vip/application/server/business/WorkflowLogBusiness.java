package fr.insalyon.creatis.vip.application.server.business;

import java.util.List;

import fr.insalyon.creatis.vip.application.models.Task;
import fr.insalyon.creatis.vip.application.models.Workflow;
import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.server.business.base.CommonBusiness;
import fr.insalyon.creatis.vip.core.server.business.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkflowLogBusiness extends CommonBusiness {

    private final ListWorkflowsBusiness listWorkflowsBusiness;
    private final SimulationBusiness simulationBusiness;

    @Autowired
    public WorkflowLogBusiness(ListWorkflowsBusiness listWorkflowsBusiness,
                               SimulationBusiness simulationBusiness) {
        this.listWorkflowsBusiness = listWorkflowsBusiness;
        this.simulationBusiness = simulationBusiness;
    }

    public Workflow getWorkflow(String wid) throws VipException {
        return listWorkflowsBusiness.getNotRefreshedWorkflow(wid);
    }

    public List<Task> listJobs(String wid) throws VipException {
        return simulationBusiness.getJobsList(wid);
    }

    public Task getFirstTask(String wid) throws VipException {
        List<Task> tasks = listJobs(wid);
        if (tasks == null || tasks.isEmpty()) {
            throw new VipException("No jobs found for workflow " + wid);
        }
        return tasks.get(0);
    }

    public Task getTaskByInvocationId(String wid, int invocationId) throws VipException {
        List<Task> tasks = listJobs(wid);
        for (Task task : tasks) {
            if (task.getInvocationID() == invocationId) {
                return task;
            }
        }
        throw new VipException("Job with invocationId " + invocationId + " not found for workflow " + wid);
    }

    public String readTaskStdout(Task task, String wid) throws VipException {
        return simulationBusiness.readFile(wid, "out", task.getFileName(), ".sh.app.out");
    }

    public String readTaskStderr(Task task, String wid) throws VipException {
        return simulationBusiness.readFile(wid, "err", task.getFileName(), ".sh.app.err");
    }
}
