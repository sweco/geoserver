/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.geoserver.script.app.AppHandler;

/**
 * Base class for script plugins.
 * <p>
 * Instances of this class must be registered in the application context.
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class ScriptPlugin {

    String extension;
    Class<? extends ScriptEngineFactory> scriptEngineFactoryClass;

    /**
     * Constructor.
     * 
     * @param extension The associated extension for the plugin. 
     * @param factoryClass The associated jsr-223 script engine factory class.
     */
    protected ScriptPlugin(String extension, Class<? extends ScriptEngineFactory> factoryClass) {
        this.extension = extension;
        this.scriptEngineFactoryClass = factoryClass;
    }

    /**
     * The associated extension for the script plugin, examples: "py", "js", "rb", etc... 
     */
    public String getExtension() {
        return extension;
    }

    /**
     * The associated script engine factory for the script plugin.
     */
    public Class<? extends ScriptEngineFactory> getScriptEngineFactoryClass() {
        return scriptEngineFactoryClass;
    }

    /**
     * Callback to initialize a new script engine.
     * <p>
     * This method is called whenever a new script engine is created and before any scripts are 
     * created. Plugins may use this method to set up any context they wish to make avialable to 
     * scripts running in the engine. This default implementation does nothing.
     * </p>
     */
    public void initScriptEngine(ScriptEngine engine) {
    }

    /**
     * Creates the handler for "app" requests.
     * <p>
     * This default implementation returns <code>null</code>, subclass should override in order to 
     * implement a custom app handler.
     * </p>
     */
    protected AppHandler createAppHandler() {
        return null;
    }

}
