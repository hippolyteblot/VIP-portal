package fr.insalyon.creatis.vip.application.server.controller;

import java.util.List;

import fr.insalyon.creatis.vip.application.models.Task;
import fr.insalyon.creatis.vip.application.models.Workflow;
import fr.insalyon.creatis.vip.application.server.business.WorkflowLogBusiness;
import fr.insalyon.creatis.vip.core.client.DefaultError;
import fr.insalyon.creatis.vip.core.client.VipException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflows")
public class WorkflowLogController {

    private final WorkflowLogBusiness workflowLogBusiness;

    @Autowired
    public WorkflowLogController(WorkflowLogBusiness workflowLogBusiness) {
        this.workflowLogBusiness = workflowLogBusiness;
    }

    @GetMapping("{wid}/stdout")
    public String readStdout(@PathVariable String wid) throws VipException {
        Workflow w = workflowLogBusiness.getWorkflow(wid);
        if (w == null) {
            throw new VipException(DefaultError.NOT_FOUND, Workflow.class.getSimpleName(), wid);
        }
        Task task = workflowLogBusiness.getFirstTask(wid);
        return workflowLogBusiness.readTaskStdout(task, wid);
    }

    @GetMapping("{wid}/stderr")
    public String readStderr(@PathVariable String wid) throws VipException {
        Workflow w = workflowLogBusiness.getWorkflow(wid);
        if (w == null) {
            throw new VipException(DefaultError.NOT_FOUND, Workflow.class.getSimpleName(), wid);
        }
        Task task = workflowLogBusiness.getFirstTask(wid);
        return workflowLogBusiness.readTaskStderr(task, wid);
    }

    @GetMapping("{wid}/jobs")
    public List<Task> listJobs(@PathVariable String wid) throws VipException {
        Workflow w = workflowLogBusiness.getWorkflow(wid);
        if (w == null) {
            throw new VipException(DefaultError.NOT_FOUND, Workflow.class.getSimpleName(), wid);
        }
        return workflowLogBusiness.listJobs(wid);
    }

    @GetMapping("{wid}/jobs/{invocationId}/stdout")
    public String readJobStdout(
            @PathVariable String wid,
            @PathVariable int invocationId) throws VipException {
        Workflow w = workflowLogBusiness.getWorkflow(wid);
        if (w == null) {
            throw new VipException(DefaultError.NOT_FOUND, Workflow.class.getSimpleName(), wid);
        }
        Task task = workflowLogBusiness.getTaskByInvocationId(wid, invocationId);
        return workflowLogBusiness.readTaskStdout(task, wid);
    }

    @GetMapping("{wid}/jobs/{invocationId}/stderr")
    public String readJobStderr(
            @PathVariable String wid,
            @PathVariable int invocationId) throws VipException {
        Workflow w = workflowLogBusiness.getWorkflow(wid);
        if (w == null) {
            throw new VipException(DefaultError.NOT_FOUND, Workflow.class.getSimpleName(), wid);
        }
        Task task = workflowLogBusiness.getTaskByInvocationId(wid, invocationId);
        return workflowLogBusiness.readTaskStderr(task, wid);
    }
}
