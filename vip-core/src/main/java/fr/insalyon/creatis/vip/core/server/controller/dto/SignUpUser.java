package fr.insalyon.creatis.vip.core.server.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.client.view.user.UserLevel;
import fr.insalyon.creatis.vip.core.client.view.util.CountryCode;
import fr.insalyon.creatis.vip.core.server.inter.DataViews;

/**
 * DTO used only for signup requests so password handling is isolated from the general User payload.
 */
@JsonView(DataViews.User.class)
public class SignUpUser extends User {

    private String signUpPassword;

    public SignUpUser() {
        super();
    }

    public SignUpUser(String id, String firstName, String lastName, String email, String institution,
            UserLevel level, CountryCode countryCode) {
        super(id, firstName, lastName, email, institution, level, countryCode);
    }

    @Override
    @JsonView(DataViews.User.class)
    @JsonProperty("password")
    public String getPassword() {
        return signUpPassword;
    }

    @Override
    @JsonView(DataViews.User.class)
    @JsonProperty("password")
    public void setPassword(String password) {
        this.signUpPassword = password;
    }
}
