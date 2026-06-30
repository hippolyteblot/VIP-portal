package fr.insalyon.creatis.vip.datamanager.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.datamanager.server.business.ApiKeyBusiness;

@RestController
@RequestMapping("/apikey")
public class ApiKeyController {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyController.class);

    private final ApiKeyBusiness apiKeyBusiness;

    @Autowired
    public ApiKeyController(ApiKeyBusiness apiKeyBusiness) {
        this.apiKeyBusiness = apiKeyBusiness;
    }

    @GetMapping
    public String getApiKey(@RequestParam(value = "new", defaultValue = "false") boolean generateNew) throws VipException {
        if (generateNew) {
            logger.info("Generating new API key for current user");
            return apiKeyBusiness.generateNewVipApiKey();
        }
        return apiKeyBusiness.getVipApiKey();
    }
}
