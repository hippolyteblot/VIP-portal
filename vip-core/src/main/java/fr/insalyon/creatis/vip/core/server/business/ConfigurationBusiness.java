package fr.insalyon.creatis.vip.core.server.business;

import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.server.business.base.CommonBusiness;
import fr.insalyon.creatis.vip.core.server.business.proxy.ProxyClient;

@Service
@Transactional
public class ConfigurationBusiness extends CommonBusiness {

    private final Server server;
    private final ProxyClient proxyClient;

    @Autowired
    public ConfigurationBusiness(ProxyClient proxyClient, Server server) {
        this.server = server;
        this.proxyClient = proxyClient;
    }

    public void configure() throws VipException {
        if (server.getMyProxyEnabled()) {
            try {
                logger.debug("Configuring VIP server proxy.");
                proxyClient.checkProxy();
    
            } catch (Exception ex) {
                logger.error("Error configuring myproxy : {}", ex.getMessage());
                throw new VipException(ex);
            }
        } else {
            logger.info("Proxy not needed and not validated !");
        }
    }
}
