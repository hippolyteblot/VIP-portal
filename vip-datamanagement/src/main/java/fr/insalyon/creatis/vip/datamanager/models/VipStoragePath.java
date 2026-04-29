package fr.insalyon.creatis.vip.datamanager.models;

import fr.insalyon.creatis.vip.core.client.VipException;
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

    private enum VipPathType {
        VIP_ROOT,
        USERS_HOME,
        GROUP,
        USERS_FOLDER,
        VO_ROOT
    }

    public VipStoragePath(User user, Path vipPath, String usersHome, String groupsHome, String voRoot) throws VipException {
        this.user = user;
        this.usersHome = usersHome;
        this.groupsHome = groupsHome;
        this.voRoot = voRoot;
        this.vipPath = vipPath.normalize().toAbsolutePath();

        if (!this.vipPath.startsWith(DataManagerConstants.ROOT)) {
            throw new VipException(DataManagementError.STORAGE_VALIDATION_ERROR, "Vip path should start with /vip");
        }
        checkSecondFolder();
        checkCharacters(this.vipPath);
        // convert to realPath
        realPath = convertToRealPath(this.vipPath);
    }

    public void checkSecondFolder() throws VipException {
        if (vipPath.getNameCount() < 1) {
            throw new VipException(DataManagementError.STORAGE_VALIDATION_ERROR, "Vip path is invalid");
        }
        VipPathType type = detectSecondFolderType(vipPath);
        if (type == VipPathType.VIP_ROOT ||
                type == VipPathType.USERS_HOME || type == VipPathType.GROUP ||
                type == VipPathType.VO_ROOT || type == VipPathType.USERS_FOLDER) {
            // valid prefixes; permission checks are enforced elsewhere
            return;
        }
        throw new VipException(DataManagementError.STORAGE_VALIDATION_ERROR, "Vip path should start with /vip/Home or /vip/Groups or /vip/VoRoot or /vip/Users");
    }

    public void checkCharacters(Path vipPath) throws VipException {
        // Allow alphanumeric, dot, dash, underscore, space and parentheses in path parts
        for (Path part : vipPath) {
            String partStr = part.toString();
            if (!partStr.matches(DataManagerConstants.VALID_PATH_CHARS)) {
                throw new VipException(DataManagementError.STORAGE_VALIDATION_ERROR, "Vip path should only contain alphanumeric characters, dots, dashes, underscores, spaces and parentheses");
            }
        }
    }

    public Path convertToRealPath(Path vipPath) throws VipException {
        Path rootPath;
        VipPathType type = detectSecondFolderType(vipPath);
        switch (type) {
            case VIP_ROOT -> rootPath = Path.of(DataManagerConstants.ROOT);
            case USERS_HOME -> rootPath = Path.of(usersHome).resolve(resolveUserFolder());
            case GROUP -> {
                String secondFolder = vipPath.getName(1).toString();
                String groupName = secondFolder.substring(0,
                        secondFolder.length() - DataManagerConstants.GROUP_APPEND.length());
                rootPath = Path.of(groupsHome).resolve(groupName.replace(" ", "_"));
            }
            case USERS_FOLDER -> rootPath = Path.of(usersHome);
            case VO_ROOT -> rootPath = Path.of(voRoot);
            default -> throw new IllegalArgumentException("Unsupported vip path prefix: " + vipPath.getName(1).toString());
        }

        Path result = rootPath;
        for (int i = 2; i < vipPath.getNameCount(); i++) {
            result = result.resolve(vipPath.getName(i).toString());
        }

        return result.normalize();
    }

    private VipPathType detectSecondFolderType(Path vipPath) throws VipException {
        if (vipPath.getNameCount() < 1) {
            throw new VipException(DataManagementError.STORAGE_VALIDATION_ERROR, "Vip path is invalid");
        }
        if (vipPath.getNameCount() == 1) {
            return VipPathType.VIP_ROOT;
        }
        String secondFolder = vipPath.getName(1).toString();
        if (secondFolder.equals(DataManagerConstants.USERS_HOME)) {
            return VipPathType.USERS_HOME;
        }
        if (secondFolder.endsWith(DataManagerConstants.GROUP_APPEND)) {
            return VipPathType.GROUP;
        }
        if (secondFolder.equals(DataManagerConstants.USERS_FOLDER)) {
            return VipPathType.USERS_FOLDER;
        }
        if (secondFolder.equals(DataManagerConstants.VO_ROOT_FOLDER)) {
            return VipPathType.VO_ROOT;
        }
        return VipPathType.VIP_ROOT;
    }

    private String resolveUserFolder() throws VipException {
        if (user == null || user.getFolder() == null || user.getFolder().isBlank()) {
            throw new VipException(DataManagementError.STORAGE_VALIDATION_ERROR, "User folder is not defined");
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
