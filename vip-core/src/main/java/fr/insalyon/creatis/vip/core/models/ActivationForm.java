package fr.insalyon.creatis.vip.core.models;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.gwt.user.client.rpc.IsSerializable;
import fr.insalyon.creatis.vip.core.server.inter.DataViews;

import com.fasterxml.jackson.annotation.JsonCreator;

@JsonView(DataViews.User.class)
public class ActivationForm implements IsSerializable {

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
