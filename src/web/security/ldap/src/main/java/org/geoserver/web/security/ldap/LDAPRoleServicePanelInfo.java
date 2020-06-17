/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.ldap;

import org.geoserver.security.ldap.LDAPForeignSecurityPrincipalAwareRoleService;
import org.geoserver.security.ldap.LDAPForeignSecurityPrincipalAwareRoleServiceConfig;
import org.geoserver.security.web.role.RoleServicePanelInfo;

public class LDAPRoleServicePanelInfo extends RoleServicePanelInfo<LDAPForeignSecurityPrincipalAwareRoleServiceConfig, LDAPRoleServicePanel> {

    private static final long serialVersionUID = 1L;

    public LDAPRoleServicePanelInfo() {
        setComponentClass(LDAPRoleServicePanel.class);
        setServiceClass(LDAPForeignSecurityPrincipalAwareRoleService.class);
        setServiceConfigClass(LDAPForeignSecurityPrincipalAwareRoleServiceConfig.class);
    }

}
