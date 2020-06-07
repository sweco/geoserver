/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.geoserver.security.config.RequestHeaderAuthenticationFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;

/**
 * J2EE Authentication Filter
 *
 * @author mcr
 *
 */
public class GeoServerRequestHeaderAuthenticationFilter extends GeoServerPreAuthenticatedUserNameFilter {

    private String principalHeaderAttribute;


    public String getPrincipalHeaderAttribute() {
        return principalHeaderAttribute;
    }

    public void setPrincipalHeaderAttribute(String principalHeaderAttribute) {
        this.principalHeaderAttribute = principalHeaderAttribute;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        RequestHeaderAuthenticationFilterConfig authConfig =
                (RequestHeaderAuthenticationFilterConfig) config;
        setPrincipalHeaderAttribute(authConfig.getPrincipalHeaderAttribute());
    }

    @Override
    protected String getPreAuthenticatedPrincipalName(HttpServletRequest request) {
        // Sweco: return principal name only without domain
        String principal = request.getHeader(getPrincipalHeaderAttribute());
        return StringUtils.substringAfterLast(principal, "\\");
    }
}
