package fr.insalyon.creatis.vip.core.models;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;

public class ActivationForm {

    @JsonProperty("code")
    private String code;

    public ActivationForm() {
    }

    @JsonCreator
    public ActivationForm(@JsonProperty("code") String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
