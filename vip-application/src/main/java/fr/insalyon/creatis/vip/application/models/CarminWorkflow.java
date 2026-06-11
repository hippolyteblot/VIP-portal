package fr.insalyon.creatis.vip.application.models;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Used for the CARMIN API where inputs can be a list of input maps
 */
public class CarminWorkflow extends Workflow {

    private List<Map<String,WorkflowInput>> inputsMapsList;

    public CarminWorkflow(String id, String workflowName, String applicationName, String applicationVersion,
                          String userId, String status, Date startDate, Date endDate,
                          String engineName, String tags) {
        super(id, workflowName, applicationName, applicationVersion, userId, status, startDate, endDate, engineName, tags);
    }

    public List<Map<String, WorkflowInput>> getInputsMapsList() {
        return inputsMapsList;
    }

    public void setInputsMapsList(List<Map<String, WorkflowInput>> inputsMapsList) {
        this.inputsMapsList = inputsMapsList;
    }
}
