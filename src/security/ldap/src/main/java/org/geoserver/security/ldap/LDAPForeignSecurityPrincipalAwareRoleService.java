package org.geoserver.security.ldap;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;

import javax.naming.directory.DirContext;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.commons.lang.StringUtils;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.ldap.core.AuthenticatedLdapEntryContextCallback;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapEntryIdentification;
import org.springframework.ldap.core.support.DefaultTlsDirContextAuthenticationStrategy;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.ldap.authentication.SpringSecurityAuthenticationSource;

/**
 * Foreign Security Principal (FSP)-aware Sweco extension of the standard
 * GeoServer LDAPRoleService. Used for cross-forest AD trust with mixed
 * individuals as group members (FSP/foreign- mixed with domain-local users).
 *
 * FSP resolving counts on the domain name beeing present in the authenticated
 * user, e.g. from J2EE or header authentication signed in as "DOMAIN\\user".
 *
 * @author Martin Kalén {@literal <martin.kalen@sweco.se>}
 */
public class LDAPForeignSecurityPrincipalAwareRoleService extends LDAPRoleService {

    LdapContextSource foreignLdapContext;
    SpringSecurityLdapTemplate foreignTemplate;

    String foreignUser, foreignPassword;
    String foreignDomainPrefix;
    String foreignUserFilter;

    @Override
    public void initializeFromConfig(final SecurityNamedServiceConfig config)
            throws IOException {
        super.initializeFromConfig(config);

        final LDAPForeignSecurityPrincipalAwareRoleServiceConfig ldapConfig = (LDAPForeignSecurityPrincipalAwareRoleServiceConfig) config;
        if (ldapConfig.getForeignServerURL() != null) {
            foreignLdapContext = createForeignLdapContext(ldapConfig);
            foreignDomainPrefix = ldapConfig.getForeignDomainPrefix();
            foreignUserFilter = ldapConfig.getForeignUserFilter();
    
            if (ldapConfig.isForeignBind()) {
                // authenticate before LDAP searches
                foreignUser = ldapConfig.getForeignUser();
                foreignPassword = ldapConfig.getForeignPassword();
                foreignTemplate = new BindingLdapTemplate(foreignLdapContext);
            } else {
                foreignTemplate = new SpringSecurityLdapTemplate(foreignLdapContext);
            }
        }
    }

    private LdapContextSource createForeignLdapContext(final LDAPForeignSecurityPrincipalAwareRoleServiceConfig ldapConfig) {
        final LdapContextSource ldapContext = new DefaultSpringSecurityContextSource(ldapConfig.getForeignServerURL());
        ldapContext.setCacheEnvironmentProperties(false);
        ldapContext.setAuthenticationSource(new SpringSecurityAuthenticationSource());

        if (ldapConfig.isForeignUseTLS()) {
            // TLS does not play nicely with pooled connections
            ldapContext.setPooled(false);

            DefaultTlsDirContextAuthenticationStrategy tls = new DefaultTlsDirContextAuthenticationStrategy();
            tls.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            ldapContext.setAuthenticationStrategy(tls);
        }
        return ldapContext;
    }

    @Override
    public SortedSet<GeoServerRole> getRolesForUser(final String username) throws IOException {
        if (username == null) {
            return Collections.emptySortedSet();
        }
        final String unqualifiedUser;
        if (StringUtils.contains(username, "\\")) {
            unqualifiedUser = StringUtils.substringAfter(username, "\\");
        } else {
            unqualifiedUser = username;
        }
        final SortedSet<GeoServerRole> roles;
        if (foreignDomainPrefix != null && username.startsWith(foreignDomainPrefix)) {
            final SortedSet<GeoServerRole> rolesForFsp = new TreeSet<GeoServerRole>();
            final List<String> userDnAndSid = new Vector<String>();
            userDnAndSid.add(unqualifiedUser);
            userDnAndSid.add(unqualifiedUser);
            authenticateIfNeeded(new AuthenticatedLdapEntryContextCallback() {
                @Override
                public void executeWithContext(DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                    try {
                        DirContextOperations result = LDAPUtils
                                .getLdapTemplateInContext(ctx, foreignTemplate)
                                .searchForSingleEntry("", foreignUserFilter,
                                        new String[] { unqualifiedUser });
                        final String dn = result.getDn().toString();
                        final String sid = result.getObjectAttribute("objectSid").toString();
                        userDnAndSid.clear();
                        userDnAndSid.add(dn);
                        userDnAndSid.add(sid);
                    } catch (Exception e) {
                        LOGGER.log(Level.FINE, "User " + username + " not found during FSP lookup", e);
                    }
                }
            });
            authenticateIfNeeded(new AuthenticatedLdapEntryContextCallback() {
                @Override
                public void executeWithContext(DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                    final Iterator<String> iter = userDnAndSid.iterator();
                    fillRolesForUser(ctx, unqualifiedUser, iter.next(), iter.next(), rolesForFsp);
                }
            });
            roles = Collections.unmodifiableSortedSet(rolesForFsp);
        } else {
            roles = super.getRolesForUser(unqualifiedUser);
        }
        return roles;
    }

    /**
     * Execute authentication, if configured to do so, and then
     * call the given callback on authenticated context, or simply
     * call the given callback if no authentication is needed.
     *
     * @param callback
     */
    private void authenticateIfNeeded(AuthenticatedLdapEntryContextCallback callback) {
        if (foreignUser != null && foreignPassword != null) {
            foreignTemplate.authenticate(DistinguishedName.EMPTY_PATH, foreignUser, foreignPassword, callback);
        } else {
            callback.executeWithContext(null, null);
        }
    }

    private void fillRolesForUser(DirContext ctx, String username, String userDn, String sid, SortedSet<GeoServerRole> roles) {
        Set<String> roleNames = LDAPUtils.getLdapTemplateInContext(ctx, template)
                .searchForSingleAttributeValues(groupSearchBase, groupSearchFilter,
                        new String[] { username, userDn, sid }, groupRoleAttribute);
        addRolesToSet(roles, roleNames);
    }

}
