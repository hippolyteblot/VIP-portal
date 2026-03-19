package fr.insalyon.creatis.vip.core.models;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.gwt.user.client.rpc.IsSerializable;

import fr.insalyon.creatis.vip.core.client.view.CoreConstants.GROUP_ROLE;
import fr.insalyon.creatis.vip.core.client.view.user.UserLevel;
import fr.insalyon.creatis.vip.core.client.view.util.CountryCode;
import fr.insalyon.creatis.vip.core.server.inter.DataViews;

public class User implements IsSerializable {

    @JsonView(DataViews.Admin.class) private boolean confirmed;
    @JsonView(DataViews.Admin.class) private boolean accountLocked;

    @JsonView(DataViews.User.class) private String id;
    @JsonView(DataViews.User.class) private String firstName;
    @JsonView(DataViews.User.class) private String lastName;
    @JsonView(DataViews.User.class) private String email;
    @JsonView(DataViews.User.class) private String institution;
    @JsonView(DataViews.User.class) private Timestamp registration;
    @JsonView(DataViews.User.class) private Timestamp lastLogin;
    @JsonView(DataViews.User.class) private UserLevel level;
    @JsonView(DataViews.User.class) private int maxRunningSimulations;
    @JsonView(DataViews.User.class) private CountryCode countryCode;
    @JsonView(DataViews.User.class) private Timestamp termsOfUse;
    @JsonView(DataViews.User.class) private Timestamp lastUpdatePublications;
    @JsonView(DataViews.User.class) private Set<Group> groups;
    @JsonView(DataViews.User.class) private String apiKey;
    
    private Map<Group, GROUP_ROLE> groupsMap;
    private String nextEmail;
    private String code;
    private String folder;
    private String session;
    private String password;
    private int failedAuthentications;

    public User() { }

    public User(String id, String firstName, String lastName, String email, String institution, UserLevel level, CountryCode countryCode) {
        this(id, firstName, lastName, email, null, institution, "", false, "", "",
                "", null, null, level, countryCode, 1,null,null,0,false, null);
    }

    public User(String id, String firstName, String lastName, String email, String institution, String password, UserLevel level, CountryCode countryCode) {
        this(id, firstName, lastName, email, null, institution, password, false, "", "",
                "", null, null, level, countryCode, 1,null,null,0,false, null);

    }

    public User(String id, String firstName, String lastName, String email, String institution,
            String password, CountryCode countryCode,Timestamp lastUpdatePublications) {

        this(id, firstName, lastName, email, null, institution, password, false,
                "", "", "", new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()), null, countryCode, 1,null,lastUpdatePublications,0,false, null);
    }

    public User(
            String id,
            String firstName,
            String lastName,
            String email,
            String nextEmail,
            String institution,
            String password,
            boolean confirmed,
            String code,
            String folder,
            String session,
            Timestamp registration,
            Timestamp lastLogin,
            UserLevel level,
            CountryCode countryCode, 
            int maxRunningSimulations,
            Timestamp termsOfUse,
            Timestamp lastUpdatePublications,
            int failedAuthentications,
            boolean locked,
            String apiKey
    ) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.nextEmail = nextEmail;
        this.institution = institution;
        this.password = password;
        this.confirmed = confirmed;
        this.code = code;
        this.folder = folder;
        this.session = session;
        this.registration = registration;
        this.lastLogin = lastLogin;
        this.level = level;
        this.maxRunningSimulations = maxRunningSimulations;
        this.countryCode = countryCode;
        this.termsOfUse=termsOfUse;
        this.lastUpdatePublications=lastUpdatePublications;
        this.failedAuthentications = failedAuthentications;
        this.accountLocked = locked;
        this.groupsMap = new HashMap<>();
        this.groups = new HashSet<>();
        this.apiKey = apiKey;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getCode() {
        return code;
    }

    public String getEmail() {
        return email;
    }

    public String getNextEmail() {
        return nextEmail;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getFolder() {
        return folder;
    }

    public String getInstitution() {
        return institution;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isSystemAdministrator() {
        return level == UserLevel.Administrator;
    }

    public boolean isDeveloper() {
        return level == UserLevel.Developer;
    }

    public String getSession() {
        return session;
    }

    public Timestamp getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }

    public UserLevel getLevel() {
        return level;
    }

    public void setLevel(UserLevel level) {
        this.level = level;
    }

    public int getMaxRunningSimulations() {
        return maxRunningSimulations;
    }

    public void setMaxRunningSimulations(int maxRunningSimulations) {
        this.maxRunningSimulations = maxRunningSimulations;
    }

    public CountryCode getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(CountryCode countryCode) {
        this.countryCode = countryCode;
    }

    public Timestamp getRegistration() {
        return registration;
    }

    public void setRegistration(Timestamp registration) {
        this.registration = registration;
    }

    @JsonIgnore // this field can't be jsonified (need a string as key of map)
    public void setGroups(Map<Group, GROUP_ROLE> groups) {
        this.groups = groups.keySet();
        this.groupsMap = groups;
        filterGroups();
    }

    public Timestamp getTermsOfUse() {
        return termsOfUse;
    }

    public void setTermsOfUse(Timestamp termsOfUse) {
        this.termsOfUse = termsOfUse;
    }

    public Timestamp getLastUpdatePublications() {
        return lastUpdatePublications;
    }

    public void setLastUpdatePublications(Timestamp lastUpdatePublications) {
        this.lastUpdatePublications = lastUpdatePublications;
    }

    @JsonIgnore // this field can't be jsonified (need a string as key of map)
    public boolean hasGroupAccess(String groupName) {
        for (Group group : groupsMap.keySet()) {
            if (group.getName().equals(groupName)) {
                return true;
            }
        }
        return false;
    }

    public Set<Group> getGroups() {
        return groups;
    }

    @JsonIgnore // this field can't be jsonified (need a string as key of map)
    private void filterGroups() {
        Iterator<Group> it = groupsMap.keySet().iterator();
        while (it.hasNext()) {
            Group group = it.next();
            if (groupsMap.get(group) == GROUP_ROLE.None) {
                it.remove();
            }
        }
    }

    @JsonIgnore // this field can't be jsonified (need a string as key of map)
    public boolean isGroupAdmin() {
        for (GROUP_ROLE role : groupsMap.values()) {
            if (role == GROUP_ROLE.Admin) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore // this field can't be jsonified (need a string as key of map)
    public boolean isGroupAdmin(String groupName) {
        for (Group group : groupsMap.keySet()) {
            if (group.getName().equals(groupName)
                    && groupsMap.get(group) == GROUP_ROLE.Admin) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAcceptTermsOfUse(){
        return getTermsOfUse()!=null;
       }

    public boolean hasGroups(){
        return this.groups.isEmpty();
    }

    public int getFailedAuthentications() {
        return this.failedAuthentications;
    }

    public boolean isAccountLocked() {
        return this.accountLocked;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String toString() {
        return email;
    }
}
