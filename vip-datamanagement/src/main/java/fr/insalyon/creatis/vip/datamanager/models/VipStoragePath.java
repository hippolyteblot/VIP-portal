package fr.insalyon.creatis.vip.datamanager.models;

import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.datamanager.client.DataManagerConstants;

import java.nio.file.Path;

public class VipStoragePath {

    private final User user;

    private final String usersHome;

    private final String groupsHome;

    private final String voRoot;

    private Path vipPath;

    private Path realPath;

    public VipStoragePath(User user, Path vipPath, String usersHome, String groupsHome, String voRoot) {
        this.user = user;
        this.usersHome = usersHome;
        this.groupsHome = groupsHome;
        this.voRoot = voRoot;
        this.vipPath = vipPath.normalize().toAbsolutePath();

        if (!this.vipPath.startsWith(DataManagerConstants.ROOT)) {
            throw new IllegalArgumentException("Vip path should start with /vip");
        }
        checkSecondFolder(this.vipPath);
        checkCharacters(this.vipPath);
        // convert to realPath
        realPath = convertToRealPath(this.vipPath);
    }

    public void checkSecondFolder(Path vipPath) {
        if (vipPath.getNameCount() < 2) {
            return;
        }
        String secondFolder = vipPath.getName(1).toString();
        if (secondFolder.equals(DataManagerConstants.USERS_HOME)) {
            return;
        }
        if (secondFolder.endsWith(DataManagerConstants.GROUP_APPEND)) {
            return;
        }
        if (user != null && user.isSystemAdministrator() &&
            (secondFolder.equals(DataManagerConstants.VO_ROOT_FOLDER) || 
            secondFolder.equals(DataManagerConstants.USERS_FOLDER))) {
            return;
        }
        throw new IllegalArgumentException("Vip path should start with /vip/Home or /vip/Groups or /vip/VoRoot or /vip/Users");
    }

    public void checkCharacters(Path vipPath) {
        // only alphanumeric, dash, underscore and slash are allowed in path
        for (Path part : vipPath) {
            String partStr = part.toString();
            if (!partStr.matches("[a-zA-Z0-9._\\-]+")) {
                throw new IllegalArgumentException("Vip path should only contain alphanumeric characters, dots, dashes, underscores and slashes");
            }
        }
    }

    public Path convertToRealPath(Path vipPath) {
        if (vipPath.getNameCount() < 2) {
            return Path.of("/");
        }

        String secondFolder = vipPath.getName(1).toString();
        Path rootPath;

        if (secondFolder.equals(DataManagerConstants.USERS_HOME)) {
            rootPath = Path.of(usersHome).resolve(resolveUserFolder());
        } else if (secondFolder.endsWith(DataManagerConstants.GROUP_APPEND)) {
            String groupName = secondFolder.substring(0,
                    secondFolder.length() - DataManagerConstants.GROUP_APPEND.length());
            rootPath = Path.of(groupsHome).resolve(groupName.replace(" ", "_"));
        } else if (secondFolder.equals(DataManagerConstants.USERS_FOLDER)) {
            rootPath = Path.of(usersHome);
        } else if (secondFolder.equals(DataManagerConstants.VO_ROOT_FOLDER)) {
            rootPath = Path.of(voRoot);
        } else {
            throw new IllegalArgumentException("Unsupported vip path prefix: " + secondFolder);
        }

        Path result = rootPath;
        for (int i = 2; i < vipPath.getNameCount(); i++) {
            result = result.resolve(vipPath.getName(i).toString());
        }

        return result.normalize();
    }

    private String resolveUserFolder() {
        if (user == null || user.getFolder() == null || user.getFolder().isBlank()) {
            throw new IllegalArgumentException("User folder is required to resolve /vip/Home paths");
        }
        return user.getFolder();
    }

    public String getVipPath() {
        return vipPath.toString();
    }

    public String getRealPath() {
        return realPath.toString();
    }
}
