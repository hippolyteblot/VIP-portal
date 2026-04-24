package fr.insalyon.creatis.vip.datamanager.models;

import fr.insalyon.creatis.vip.core.models.User;

import java.nio.file.Path;

public class VipStoragePath {

    private User user;

    private Path vipPath;

    private Path realPath;

    private VipStoragePath(User user, Path vipPath) {
        this.user = user;
        this.vipPath = vipPath.normalize().toAbsolutePath();
        // TODO
        // verify starts with /vip
        // verify second folder format in /vip/***
        // verify only allowed-characters in path
        // convert to realPath
    }

    public static VipStoragePath of(User user, String first, String... more) {
        return new VipStoragePath(user, Path.of(first, more));
    }

    public String getVipPath() {
        return vipPath.toString();
    }


}
