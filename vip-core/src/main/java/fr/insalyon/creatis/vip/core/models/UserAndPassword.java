package fr.insalyon.creatis.vip.core.models;

import com.fasterxml.jackson.annotation.JsonView;

import fr.insalyon.creatis.vip.core.server.inter.DataViews;

@JsonView(DataViews.User.class)
public class UserAndPassword {

    public User user;
    public String password;
    public String comment;
}
