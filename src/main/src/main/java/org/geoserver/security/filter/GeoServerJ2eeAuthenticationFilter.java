/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

/**
 * J2EE Authentication Filter
 * 
 * @author mcr
 *
 */
public class GeoServerJ2eeAuthenticationFilter extends GeoServerJ2eeBaseAuthenticationFilter {
    @Override
    protected String getPreAuthenticatedPrincipalName(HttpServletRequest request) {
    	String principal = request.getUserPrincipal() == null ? null : request.getUserPrincipal().getName();
    	// Sweco: return principal name only without domain
        return StringUtils.substringAfterLast(principal, "\\");
    }

}
