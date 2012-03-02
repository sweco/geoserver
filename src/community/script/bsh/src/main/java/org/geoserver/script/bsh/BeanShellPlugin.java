package org.geoserver.script.bsh;

import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.app.AppHandler;
import org.geoserver.script.app.SimpleAppHandler;

import bsh.engine.BshScriptEngineFactory;

/**
 * Script plugin for BeanShell.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class BeanShellPlugin extends ScriptPlugin {

    public BeanShellPlugin() {
        super("bsh", BshScriptEngineFactory.class);
    }

    @Override
    protected AppHandler createAppHandler() {
        return new SimpleAppHandler(this);
    }
}
