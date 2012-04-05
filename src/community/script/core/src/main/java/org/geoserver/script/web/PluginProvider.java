/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.util.Arrays;
import java.util.List;

import org.geoserver.script.ScriptManager;
import org.geoserver.script.ScriptPlugin;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * Data provider for script plugins.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class PluginProvider extends GeoServerDataProvider<ScriptPlugin> {

    public static Property<ScriptPlugin> NAME = new BeanProperty("name", "displayName");

    public static Property<ScriptPlugin> ENGINE 
        = new BeanProperty("engine", "scriptEngineFactoryClass.name");

    @Override
    protected List<Property<ScriptPlugin>> getProperties() {
        return Arrays.asList(NAME, ENGINE);
    }

    @Override
    protected List<ScriptPlugin> getItems() {
        return getApplication().getBeanOfType(ScriptManager.class).getPlugins();
    }
}
