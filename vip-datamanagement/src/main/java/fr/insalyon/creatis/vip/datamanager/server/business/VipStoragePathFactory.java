package fr.insalyon.creatis.vip.datamanager.server.business;

import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.server.business.Server;
import fr.insalyon.creatis.vip.datamanager.models.VipStoragePath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class VipStoragePathFactory {

    private final Server server;

    @Autowired
    public VipStoragePathFactory(Server server) {
        this.server = server;
    }

    public VipStoragePath create(User user, String first, String... more) throws VipException {
        return create(user, Path.of(first, more));
    }

    public VipStoragePath create(User user, Path vipPath) throws VipException {
        return new VipStoragePath(
                user,
                vipPath,
                server.getDataManagerUsersHome(),
                server.getDataManagerGroupsHome(),
                server.getVoRoot()
        );
    }
}
