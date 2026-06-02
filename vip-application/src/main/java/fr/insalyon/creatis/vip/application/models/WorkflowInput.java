package fr.insalyon.creatis.vip.application.models;

import com.fasterxml.jackson.annotation.JsonView;
import fr.insalyon.creatis.vip.application.models.boutiquesTools.BoutiquesInput;
import fr.insalyon.creatis.vip.core.server.inter.DataViews;

@JsonView(DataViews.User.class)
public class WorkflowInput {

    private BoutiquesInput.InputType type;
    private String value;

    public WorkflowInput(BoutiquesInput.InputType type, String value) {
        this.type = type;
        this.value = value;
    }

    public BoutiquesInput.InputType getType() {
        return type;
    }

    public void setType(BoutiquesInput.InputType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
