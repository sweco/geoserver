/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wps;

import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.geoserver.script.ScriptHandler;
import org.geoserver.script.ScriptPlugin;
import org.geotools.data.Parameter;

/**
 * Handles wps / process requests.
 * <p>
 * This class is a bridge between the GeoTools/GeoServer process api and the api for process 
 * scripts.
 * </p>
 * <p>
 * All the methods on this interface take a {@link ScriptEngine} instance that is already "loaded"
 * with the current version of the process script.
 * </p>
 * <p>
 * Instances of this class must be thread safe.
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class WpsHandler extends ScriptHandler {

    public WpsHandler(ScriptPlugin plugin) {
        super(plugin);
    }

    /**
     * The title of the process.
     */
    public abstract String getTitle(ScriptEngine engine) throws ScriptException;

    /**
     * The description of the process.
     * <p>
     * Subclasses should override this method, the default behavior is simply to return 
     * {@link #getTitle(ScriptEngine)}.
     * </p>
     */
    public String getDescription(ScriptEngine engine) throws ScriptException{
        return getTitle(engine);
    }

    /**
     * The version of the process.
     * <p>
     * Subclasses should override this method, the default behavour is simply to return "1.0.0". 
     * </p>
     */
    public String getVersion(ScriptEngine engine) throws ScriptException{
        return "1.0.0";
    }

    /**
     * The process inputs.
     */
    public abstract Map<String, Parameter<?>> getInputs(ScriptEngine engine) throws ScriptException;

    /**
     * The process outputs.
     */
    public abstract Map<String, Parameter<?>> getOutputs(ScriptEngine engine) throws ScriptException;

    /**
     * Executes the process.
     */
    public abstract Map<String, Object> run(Map<String, Object> input, ScriptEngine engine) 
        throws ScriptException;
}
