package fr.insalyon.creatis.vip.application.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.gwt.user.client.rpc.IsSerializable;
import fr.insalyon.creatis.vip.application.client.view.monitor.WorkflowStatus;
import fr.insalyon.creatis.vip.core.server.inter.DataViews;
import jakarta.validation.constraints.NotBlank;

import java.util.Date;
import java.util.Map;

/**
 * This is almost the same as the workflow class from workflowsdb
 *
 */
@JsonView(DataViews.User.class)
public class Workflow implements IsSerializable {

    private String id;
    @NotBlank
    private String applicationName;
    @NotBlank
    private String applicationVersion;
    @NotBlank
    private String workflowName;
    private WorkflowStatus status;
    private Map<String,WorkflowInput> inputs;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Map<String,String> outputs;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String userId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date startDate;

    @JsonIgnore
    private String engineName;
    @JsonIgnore
    private String tags;

    public Workflow() {
    }

    public Workflow(String application, String applicationVersion,
                    String id, String userId, Date startDate,
                    String workflowName, String status, String engineName, String tags) {

        this.applicationName = application;
        this.applicationVersion = applicationVersion;
        this.id = id;
        this.userId = userId;
        this.startDate = startDate;
        this.workflowName = workflowName;
        this.status = WorkflowStatus.valueOf(status);
        this.engineName = engineName;
        this.tags = tags;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public Date getStartDate() {
        return startDate;
    }

    public String getUserId() {
        return userId;
    }

    public String getID() {
        return id;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }
    
    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public String getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return applicationName + "\n" + id + "\n" + userId + "\n" + startDate;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public Map<String, WorkflowInput> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, WorkflowInput> inputs) {
        this.inputs = inputs;
    }

    public Map<String, String> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, String> outputs) {
        this.outputs = outputs;
    }
}
