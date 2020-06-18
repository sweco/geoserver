/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import javax.naming.directory.DirContext;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.support.AbstractContextSource;
import org.springframework.ldap.core.support.DefaultTlsDirContextAuthenticationStrategy;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.ldap.authentication.SpringSecurityAuthenticationSource;

/**
 * LDAP utility class.
 * Here are the LDAP access functionalities common to all LDAP security services.
 * 
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 *
 */
public class LDAPUtils {

    private static final int HEX = 16;

    /**
     * Creates an LdapContext from a configuration object.
     * 
     * @param ldapConfig
     * @return
     */
    public static LdapContextSource createLdapContext(
            LDAPBaseSecurityServiceConfig ldapConfig) {
        LdapContextSource ldapContext = new DefaultSpringSecurityContextSource(
                ldapConfig.getServerURL());
        ldapContext.setCacheEnvironmentProperties(false);
        ldapContext
                .setAuthenticationSource(new SpringSecurityAuthenticationSource());
    
        if (ldapConfig.isUseTLS()) {
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
    
    /**
     * Returns an LDAP template bounded to the given context, if not null.
     * 
     * @param ctx
     * @param template
     * @return
     */
    public static SpringSecurityLdapTemplate getLdapTemplateInContext(
            final DirContext ctx,final SpringSecurityLdapTemplate template) {
        SpringSecurityLdapTemplate authTemplate;
        if (ctx == null) {            
            authTemplate = template;
            ((AbstractContextSource)authTemplate.getContextSource()).setAnonymousReadOnly(true);
        } else {
            // if we have the authenticated context we build a new LdapTemplate
            // using it
            authTemplate = new SpringSecurityLdapTemplate(new ContextSource() {

                @Override
                public DirContext getReadOnlyContext() throws NamingException {
                    return ctx;
                }

                @Override
                public DirContext getReadWriteContext() throws NamingException {
                    return ctx;
                }

                @Override
                public DirContext getContext(String principal,
                        String credentials) throws NamingException {
                    return ctx;
                }

            });
        }
        return authTemplate;
    }

    /**
     * Returns decoded SID string from SID binary attribute value.
     * The String value is: S-Revision-Authority-SubAuthority[n]...
     *
     * Based on code from here - https://ldapwiki.com/wiki/ObjectSID
     * (in turn based on http://forums.oracle.com/forums/thread.jspa?threadID=1155740&tstart=0)
     * and here - https://github.com/spring-projects/spring-ldap/blob/master/core/src/main/java/org/springframework/ldap/support/LdapUtils.java
     */
    public static String decodeSID(byte[] sid) {
        final StringBuilder strSid = new StringBuilder("S-");

        // get byte(0) - revision level
        final int revision = sid[0];
        strSid.append(Integer.toString(revision));

        //next byte byte(1) - count of sub-authorities
        final int countSubAuths = sid[1] & 0xFF;

        //byte(2-7) - 48 bit authority ([Big-Endian])
        long authority = 0;
        //String rid = "";
        for(int i = 2; i <= 7; i++) {
            authority |= ((long)sid[i]) << (8 * (5 - (i - 2)));
        }
        strSid.append("-");
        strSid.append(Long.toHexString(authority));

        //iterate all the sub-auths and then countSubAuths x 32 bit sub authorities ([Little-Endian])
        int offset = 8;
        int size = 4; //4 bytes for each sub auth
        for(int j = 0; j < countSubAuths; j++) {
            long subAuthority = 0;
            for(int k = 0; k < size; k++) {
                subAuthority |= (long)(sid[offset + k] & 0xFF) << (8 * k);
            }
            // format it
            strSid.append("-");
            strSid.append(subAuthority);
            offset += size;
        }
        return strSid.toString();
    }

}
