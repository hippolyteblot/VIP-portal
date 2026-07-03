package fr.insalyon.creatis.vip.datamanager.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.server.business.UserBusiness;

@RestController
@RequestMapping("/apikey")
public class ApiKeyController {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyController.class);

    private final UserBusiness userBusiness;

    @Autowired
    public ApiKeyController(UserBusiness userBusiness) {
        this.userBusiness = userBusiness;
    }

    @GetMapping
    public String getApiKey(@RequestParam(value = "new", defaultValue = "false") boolean generateNew) throws VipException {
        if (generateNew) {
            logger.info("Generating new API key for user {}", userBusiness.getUserEmail());
            return userBusiness.generateNewUserApikey(userBusiness.getUserEmail());
        }
        return userBusiness.getUserApikey(userBusiness.getUserEmail());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteApiKey() throws VipException {
        logger.info("Deleting API key for user {}", userBusiness.getUserEmail());
        userBusiness.deleteUserApikey(userBusiness.getUserEmail());
    }
}
