/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

/**
 * Base class for hooks.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ScriptHook {

    protected ScriptPlugin plugin;

    public ScriptHook(ScriptPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * The script plugin for the hook. 
     */
    public ScriptPlugin getPlugin() {
        return plugin;
    }

}
