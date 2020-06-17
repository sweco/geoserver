package org.geoserver.security.ldap;

import org.geoserver.security.config.SecurityRoleServiceConfig;

/**
 * Configuration class for the LDAPForeignSecurityPrincipalAwareRoleService.
 *
 * @author Martin Kalén {@literal <martin.kalen@sweco.se>}
 */
public class LDAPForeignSecurityPrincipalAwareRoleServiceConfig extends LDAPRoleServiceConfig implements SecurityRoleServiceConfig {

    private static final long serialVersionUID = 1L;

    private String foreignServerURL;
    private Boolean foreignUseTLS;

    //private String groupSearchBase;
    //private String groupSearchFilter;

    // bind to the server before extracting groups
    // some LDAP server require this (e.g. ActiveDirectory)
    //Boolean bindBeforeGroupSearch;

    // LDAP user name and password for authenticated resolving of
    // foreign domain objectSid
    private String foreignUser;
    private String foreignPassword;
    private Boolean foreignBind;
    private String foreignDomainPrefix;
    private String foreignUserFilter;

    public LDAPForeignSecurityPrincipalAwareRoleServiceConfig() {
    }

    public LDAPForeignSecurityPrincipalAwareRoleServiceConfig(LDAPForeignSecurityPrincipalAwareRoleServiceConfig other) {
        super(other);
        foreignServerURL = other.getForeignServerURL();
        foreignUseTLS = other.isForeignUseTLS();
        foreignUser = other.getForeignUser();
        foreignPassword = other.getForeignPassword();
        foreignBind = other.isForeignBind();
    }

    public String getForeignServerURL() {
        return foreignServerURL;
    }
    public void setForeignServerURL(String serverURL) {
        this.foreignServerURL = serverURL;
    }
    public void setForeignUseTLS(Boolean useTLS) {
        this.foreignUseTLS = useTLS;
    }
    public Boolean isForeignUseTLS() {
        return foreignUseTLS;
    }
    public String getForeignUser() {
        return foreignUser;
    }
    public void setForeignUser(String userDn) {
        this.foreignUser = userDn;
    }
    public String getForeignPassword() {
        return foreignPassword;
    }
    public void setForeignPassword(String password) {
        this.foreignPassword = password;
    }
    public String getForeignDomainPrefix() {
        return foreignDomainPrefix;
    }
    public void setForeignDomainPrefix(String domainPrefix) {
        this.foreignDomainPrefix = domainPrefix;
    }
    public String getForeignUserFilter() {
        return foreignUserFilter;
    }
    public void setForeignUserFilter(String userFilter) {
        this.foreignUserFilter = userFilter;
    }
    public Boolean isForeignBind() {
        return foreignBind;
    }
    public void setForeignBind(Boolean bind) {
        this.foreignBind = bind;
    }

}
