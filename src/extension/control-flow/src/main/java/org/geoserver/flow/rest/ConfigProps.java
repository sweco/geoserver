/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.rest;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class ConfigProps {

    private final Integer global;

    private final Integer user;

    private final Integer ip;

    private final Integer timeout;

    private final Integer gwc;

    public ConfigProps(Integer global, Integer user, Integer ip, Integer timeout, Integer gwc) {
        this.global = global;
        this.user = user;
        this.ip = ip;
        this.timeout = timeout;
        this.gwc = gwc;
    }

    public Integer getGlobal() {
        return global;
    }

    public Integer getUser() {
        return user;
    }

    public Integer getIp() {
        return ip;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public Integer getGwc() {
        return gwc;
    }

}
