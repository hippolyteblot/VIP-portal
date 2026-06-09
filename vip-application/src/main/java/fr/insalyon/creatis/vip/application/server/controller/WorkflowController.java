package fr.insalyon.creatis.vip.application.server.controller;

import fr.insalyon.creatis.vip.application.models.Workflow;
import fr.insalyon.creatis.vip.application.server.business.WorkflowBusiness;
import fr.insalyon.creatis.vip.application.server.business.util.NewWorkflowBusiness;
import fr.insalyon.creatis.vip.core.client.DefaultError;
import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.server.model.PrecisePage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/workflows")
public class WorkflowController {

    private final NewWorkflowBusiness workflowBusiness;

    @Autowired
    public WorkflowController(NewWorkflowBusiness workflowBusiness) {
        this.workflowBusiness = workflowBusiness;
    }

    // TODO : add filters : start date, end date
    @GetMapping
    public PrecisePage<Workflow> list(
            @RequestParam(defaultValue = "0") @PositiveOrZero int offset,
            @RequestParam(defaultValue = "10") @Positive @Max(value = 50) int quantity) throws VipException {
        return null;
    }

    @GetMapping(value = "{wid}")
    public Workflow get(@PathVariable String wid) throws VipException {
        Workflow w = null;
        //Workflow w = WorkflowBusiness.get(wid);

        if (w == null) {
            throw new VipException(DefaultError.NOT_FOUND, Workflow.class.getSimpleName(), wid);
        } else {
            return w;
        }
    }

    /**
     * used to update status, to kill or clean
     */
    @PutMapping(value = "{wid}")
    public Workflow updateStatus(@PathVariable String wid, @RequestBody @Valid Workflow workflow)
            throws VipException {
        if ( ! wid.equals(workflow.getID())) {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, wid, "Workflow id do not match!");
        } else {
            return null;
            //return workflowBusiness.updateStatus(workflow);
        }
    }

    @PostMapping
    public Workflow launch(@RequestBody @Valid Workflow workflow) throws VipException {
        return workflowBusiness.launch(workflow);
    }
}
