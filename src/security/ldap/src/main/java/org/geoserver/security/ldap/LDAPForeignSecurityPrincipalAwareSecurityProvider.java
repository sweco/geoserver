package org.geoserver.security.ldap;

import java.io.IOException;

import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;

/**
 * Provides an FSP aware LDAP security provider.
 *
 * @author Martin Kalén {@literal <martin.kalen@sweco.se>}
 */
public class LDAPForeignSecurityPrincipalAwareSecurityProvider extends LDAPSecurityProvider {

    public LDAPForeignSecurityPrincipalAwareSecurityProvider(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    @Override
    public Class<? extends GeoServerRoleService> getRoleServiceClass() {
        return LDAPForeignSecurityPrincipalAwareRoleService.class; 
    }

    @Override
    public GeoServerRoleService createRoleService(SecurityNamedServiceConfig config)
            throws IOException {
        return new LDAPForeignSecurityPrincipalAwareRoleService();
    }

}
