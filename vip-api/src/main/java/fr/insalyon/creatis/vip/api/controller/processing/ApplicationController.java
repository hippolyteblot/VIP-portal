package fr.insalyon.creatis.vip.api.controller.processing;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.insalyon.creatis.vip.api.controller.ApiController;
import fr.insalyon.creatis.vip.application.models.Application;
import fr.insalyon.creatis.vip.application.server.business.ApplicationBusiness;
import fr.insalyon.creatis.vip.application.server.business.AppVersionBusiness;
import fr.insalyon.creatis.vip.core.client.VipException;

@RestController
@RequestMapping("applications")
public class ApplicationController extends ApiController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ApplicationBusiness applicationBusiness;
    private final AppVersionBusiness appVersionBusiness;

    @Autowired
    protected ApplicationController(ApplicationBusiness applicationBusiness,
                                    AppVersionBusiness appVersionBusiness) {
        this.applicationBusiness = applicationBusiness;
        this.appVersionBusiness = appVersionBusiness;
    }

    @RequestMapping(method = RequestMethod.GET)
    public List<Application> listApplications() throws VipException {
        logMethodInvocation(logger, "listApplications");
        return applicationBusiness.getApplications();
    }

    @RequestMapping(params = "public")
    public List<Application> listPublicApplications() throws VipException {
        logMethodInvocation(logger, "listPublicApplications");
        return appVersionBusiness.getPublicApplications();
    }
}