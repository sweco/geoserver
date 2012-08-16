/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

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

    /**
     * Helper method to look up an object in a script engine, verifying its type and optionally 
     * throwing an exception if it doesn't exist. 
     */
    protected <T> T lookup(ScriptEngine engine, String name, Class<T> type, boolean mandatory) 
        throws ScriptException{
        Object obj = engine.get(name);
        if (obj == null) {
            if (mandatory) {
                throw new ScriptException("No such object: " + name);
            }
            else {
                return null;
            }
        }

        if (!type.isInstance(obj)) {
            throw new IllegalArgumentException("Object " + obj + " is not of type " + type.getName());
        }

        return type.cast(obj);
    }

    /**
     * Helper method to invoke a function through a script engine. 
     */
    protected Object invoke(ScriptEngine engine, String name, Object... args) throws ScriptException {
        if (engine instanceof Invocable) {
            try {
                return ((Invocable)engine).invokeFunction(name, args);
            } catch (NoSuchMethodException e) {
                throw new ScriptException(e);
            }
        }
        else {
            throw new ScriptException("Engine does not implement Invocable, plugin must implement"
                + " custom script hook");
        }

    }
}
