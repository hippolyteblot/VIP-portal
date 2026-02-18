package fr.insalyon.creatis.vip.application.server.business;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.insalyon.creatis.vip.application.models.AppVersion;
import fr.insalyon.creatis.vip.application.models.Application;
import fr.insalyon.creatis.vip.application.models.Resource;
import fr.insalyon.creatis.vip.application.models.Tag;
import fr.insalyon.creatis.vip.application.server.dao.ApplicationDAO;
import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.models.Group;
import fr.insalyon.creatis.vip.core.models.GroupType;
import fr.insalyon.creatis.vip.core.server.business.GroupBusiness;
import fr.insalyon.creatis.vip.core.server.business.base.CommonBusiness;
import fr.insalyon.creatis.vip.core.server.dao.DAOException;
import fr.insalyon.creatis.vip.core.server.inter.annotations.VIPExternalSafe;
import fr.insalyon.creatis.vip.core.server.model.PrecisePage;

@Service
@Transactional
public class AppVersionBusiness extends CommonBusiness {

    private final TagBusiness tagBusiness;
    private final ResourceBusiness resourceBusiness;
    private final ApplicationBusiness applicationBusiness;
    private final ApplicationDAO applicationDAO;
    private final GroupBusiness groupBusiness;

    @Autowired
    public AppVersionBusiness(TagBusiness tagBusiness, ResourceBusiness resourceBusiness, ApplicationDAO applicationDAO, ApplicationBusiness applicationBusiness, GroupBusiness groupBusiness) {
        this.tagBusiness = tagBusiness;
        this.resourceBusiness = resourceBusiness;
        this.applicationBusiness = applicationBusiness;
        this.applicationDAO = applicationDAO;
        this.groupBusiness = groupBusiness;
    }

    @VIPExternalSafe
    public void add(AppVersion version) throws VipException {
        permissions.shouldExist(applicationBusiness.get(version.getApplicationName()));
        permissions.filter((chain) -> chain
            .admin()
            .developer(() -> {
                List<Resource> userResources = resourceBusiness.getUserContextResources();

                for (Resource wantedResource : version.getResources()) {
                    permissions.checkItemInList(wantedResource, userResources);
                }
        }));
        try {
            applicationDAO.addVersion(version);

            for (Tag tag : version.getTags()) {
                tag.setApplication(version.getApplicationName());
                tag.setVersion(version.getVersion());
                tagBusiness.addOrUpdate(tag);
            }
            for (Resource resource : version.getResources()) {
                resourceBusiness.associate(resource, version);
            }
        } catch (DAOException e) {
            throw new VipException(e);
        }
    }

    @VIPExternalSafe
    public void update(AppVersion version) throws VipException {
        AppVersion exisitingVersion = permissions.shouldExist(get(version.getApplicationName(), version.getVersion()));

        permissions.filter((chain) -> chain
            .admin()
            .developer(() -> {
                // developer can only associate resources at CREATION
                permissions.checkUnchanged(version.getResources(), exisitingVersion.getResources());
        }));
        try {
            List<String> beforeResourceNames = exisitingVersion.getResourcesNames();
            List<Tag> editedTags = exisitingVersion.getTags();
            editedTags.removeAll(version.getTags());

            applicationDAO.updateVersion(version);
            for (Resource resource : version.getResources()) {
                if ( ! beforeResourceNames.removeIf((s) -> s.equals(resource.getName()))) {
                    resourceBusiness.associate(resource, version);
                }
            }
            for (Tag tag : editedTags) {
                tagBusiness.remove(tag);
            }
            for (Tag tag : version.getTags()) {
                tagBusiness.addOrUpdate(tag);
            }
            for (String resource : beforeResourceNames) {
                resourceBusiness.dissociate(new Resource(resource), version);
            }

        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    @VIPExternalSafe
    public void remove(String applicationName, String version) throws VipException {
        Application app = permissions.shouldExist(applicationBusiness.getApplication(applicationName));
        AppVersion appVersion = get(applicationName, version);

        if (appVersion == null) return;
        permissions.filter((chain) -> chain
            .admin()
            .developer(() -> {
                // same rule than for Application
                permissions.checkItemInList(app, applicationBusiness.getUserContextApplications());
                permissions.checkOnlyUserPrivateGroups(app.getGroups());
        }));
        try {
            applicationDAO.removeVersion(applicationName, version);
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public void updateDoiForVersion(String doi, String applicationName, String version) throws VipException {
        try {
            applicationDAO.updateDoiForVersion(doi, applicationName, version);
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public List<AppVersion> getVersions(String applicationName) throws VipException {
        try {
            List<AppVersion> versions = applicationDAO.getVersions(applicationName);

            for (AppVersion version : versions) {
                version.setResources(resourceBusiness.getByAppVersion(version));
                version.setTags(tagBusiness.getTags(version));
            }
            return versions;
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public List<Application> getPublicApplications() throws VipException {
        List<Group> publicAppGroups = groupBusiness.getPublic()
            .stream()
            .filter((g) -> g.getType().equals(GroupType.APPLICATION))
            .collect(Collectors.toList());
        List<Application> apps = new ArrayList<>();

        for (Group group : publicAppGroups) {
            for (Application app : applicationBusiness.getApplications(group)) {
                // keep application if at least a Version is visible
                if (getVersions(app.getName()).stream().anyMatch(AppVersion::isVisible)) {
                    apps.add(app);
                }
            }
        }

        // remove doublons + sort
        return apps.stream().collect(Collectors.toMap(Application::getName, a -> a, (a1, a2) -> a1)).values()
                .stream().sorted(Comparator.comparing(Application::getName)).collect(Collectors.toList());
    }

    public AppVersion getVersion(String applicationName, String applicationVersion) throws VipException {
        try {
            AppVersion version = applicationDAO.getVersion(applicationName, applicationVersion);

            if (version != null) {
                version.setResources(resourceBusiness.getByAppVersion(version));
                version.setTags(tagBusiness.getTags(version));
            }

            return version;
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    @VIPExternalSafe
    public AppVersion get(String application, String version) throws VipException {
        try {
            Application app = applicationBusiness.get(application);
            AppVersion appVersion = applicationDAO.getVersion(app.getName(), version);

            if (appVersion != null) {
                // to avoid permissions leaks
                appVersion.setResources(permissions.filterOnlySame(
                        resourceBusiness.getByAppVersion(appVersion),
                        resourceBusiness.getUserContextResources()));
                appVersion.setTags(tagBusiness.getTags(appVersion));
            }

            return appVersion;
        } catch (DAOException e) {
            throw new VipException(e);
        }
    }

    @VIPExternalSafe
    public PrecisePage<AppVersion> get(int offset, int quantity, String application) throws VipException {
        try {
            Application app = applicationBusiness.get(application);
            List<AppVersion> versions = applicationDAO.getVersions(app.getName());
            List<Resource> userResources = resourceBusiness.getUserContextResources();

            for (AppVersion v : versions) {
                // to avoid permissions leaks
                v.setResources(permissions.filterOnlySame(
                        resourceBusiness.getByAppVersion(v),
                        userResources));
                v.setTags(tagBusiness.getTags(v));
            }

            return pageBuilder.doPrecise(offset, quantity, versions);
        } catch (DAOException e) {
            throw new VipException(e);
        }
    }
}


