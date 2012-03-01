/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

/**
 * Base class for handlers.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ScriptHandler {

    protected ScriptPlugin plugin;

    public ScriptHandler(ScriptPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * The script plugin for the handler. 
     */
    public ScriptPlugin getPlugin() {
        return plugin;
    }

}
