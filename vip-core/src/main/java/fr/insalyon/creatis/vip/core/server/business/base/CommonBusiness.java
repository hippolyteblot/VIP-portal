package fr.insalyon.creatis.vip.core.server.business.base;

import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;

import fr.insalyon.creatis.vip.core.client.view.user.UserLevel;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.server.business.PageBuilder;

public abstract class CommonBusiness {

    protected CorePermissions permissions;
    protected Supplier<User> userSupplier;
    protected PageBuilder pageBuilder;

    @Autowired
    public void setUserSupplier(Supplier<User> userSupplier) {
        this.userSupplier = userSupplier;
    }

    @Autowired
    public void setCorePermissions(CorePermissions corePermissions) {
        this.permissions = corePermissions;
    }

    @Autowired
    public void setPageBuilder(PageBuilder pageBuilder) {
        this.pageBuilder = pageBuilder;
    }

    public User getUser() {
        return userSupplier.get();
    }

    public UserLevel getUserLevel() {
        return getUser().getLevel();
    }
}
