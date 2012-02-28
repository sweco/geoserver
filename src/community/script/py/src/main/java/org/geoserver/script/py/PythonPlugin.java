/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.py;

import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.app.AppHandler;
import org.python.jsr223.PyScriptEngineFactory;

/**
 * Python script plugin.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class PythonPlugin extends ScriptPlugin {

    public PythonPlugin() {
        super("py", PyScriptEngineFactory.class);
    }

    @Override
    protected AppHandler createAppHandler() {
        return new PyAppHandler();
    }
}
