package fr.insalyon.creatis.vip.application.models;

import com.fasterxml.jackson.annotation.JsonView;
import fr.insalyon.creatis.vip.application.models.boutiquesTools.BoutiquesInput;
import fr.insalyon.creatis.vip.core.server.inter.DataViews;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonView(DataViews.User.class)
public class WorkflowInput {

    @NotNull
    private BoutiquesInput.InputType type;
    private List<String> values;
    @Size(max=3)
    private List<Double> interval;

    public WorkflowInput(BoutiquesInput.InputType type, List<String> values) {
        this.type = type;
        this.values = values;
    }

    public WorkflowInput(List<Double> interval) {
        this.type = BoutiquesInput.InputType.NUMBER;
        this.interval = interval;
    }

    public boolean isInterval() {
        return interval != null;
    }

    public BoutiquesInput.InputType getType() {
        return type;
    }

    public void setType(BoutiquesInput.InputType type) {
        this.type = type;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public List<Double> getInterval() {
        return interval;
    }

    public void setInterval(List<Double> interval) {
        this.interval = interval;
    }
}
