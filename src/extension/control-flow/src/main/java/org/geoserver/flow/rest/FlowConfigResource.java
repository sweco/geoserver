/* Copyright (c) 2001 - 2013 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.flow.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.geoserver.flow.config.DefaultControlFlowConfigurator;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.rest.ReflectiveResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.ReflectiveJSONFormat;
import org.geoserver.security.PropertyFileWatcher;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.vfny.geoserver.global.GeoserverDataDirectory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

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
        return props;
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    protected void handleObjectPut(Object object) throws Exception {
        Properties props = (Properties) object;
        File propsFile = new File(GeoserverDataDirectory.getGeoserverDataDirectory(),
                "controlflow.properties");

        Iterator<Entry<Object, Object>> entries = props.entrySet().iterator();
        while (entries.hasNext()) {
            Entry<Object, Object> entry = entries.next();
            props.put(entry.getKey().toString(), entry.getValue().toString());
        }
        props.store(new FileOutputStream(propsFile), null);

    }

    @Override
    protected void configureXStream(XStream xstream) {
        super.configureXStream(xstream);
        xstream.alias("controlflow", Properties.class);
        xstream.registerConverter(new FlowConfigConverter());
    }

}
