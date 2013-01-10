/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.flow.rest;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.geoserver.rest.ReflectiveResource;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class FlowConfigResource extends ReflectiveResource {

    public FlowConfigResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        Properties props = new Properties();
        props.load(new FileInputStream(new File(GeoserverDataDirectory.getGeoserverDataDirectory(),
                "controlflow.properties")));
        Integer global = (props.getProperty("ows.global") != null) ? Integer.parseInt(props
                .getProperty("ows.global")) : null;
        Integer user = (props.getProperty("user") != null) ? Integer.parseInt(props
                .getProperty("user")) : null;
        Integer ip = (props.getProperty("ip") != null) ? Integer.parseInt(props.getProperty("ip"))
                : null;
        Integer timeout = (props.getProperty("timeout") != null) ? Integer.parseInt(props
                .getProperty("timeout")) : null;

        Integer gwc = (props.getProperty("ows.gwc") != null) ? Integer.parseInt(props
                .getProperty("ows.gwc")) : null;

        ConfigProps configProps = new ConfigProps(global, user, ip, timeout, gwc);
        return configProps;
    }

}
