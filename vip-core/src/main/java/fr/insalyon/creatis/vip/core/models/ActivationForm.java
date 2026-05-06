package fr.insalyon.creatis.vip.core.models;

import jakarta.validation.constraints.NotBlank;

public class ActivationForm {

    @NotBlank
    private String code;

    public ActivationForm() {
    }

    public ActivationForm(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
