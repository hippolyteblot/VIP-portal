package fr.insalyon.creatis.vip.core.server.controller.dto;

import com.fasterxml.jackson.annotation.JsonView;

import fr.insalyon.creatis.vip.core.server.inter.DataViews;

@JsonView(DataViews.User.class)
public class SignUpForm {

    public SignUpUser user;
    public String comment;
}
