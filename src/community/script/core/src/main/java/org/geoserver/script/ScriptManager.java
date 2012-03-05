/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.script.app.AppHandler;
import org.geoserver.script.wps.WpsHandler;

/**
 * Facade for the scripting subsystem, providing methods for obtaining script context and managing
 * scripts. 
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ScriptManager {

    GeoServerDataDirectory dataDir;
    ScriptEngineManager engineMgr;

    volatile List<ScriptPlugin> plugins;

    public ScriptManager(GeoServerDataDirectory dataDir) {
        this.dataDir = dataDir;
        engineMgr = new ScriptEngineManager();
    }

    /**
     * Returns the underlying engine manager used to create and manage script engines.
     */
    public ScriptEngineManager getEngineManager() {
        return engineMgr;
    }

    /**
     * The root "scripts" directory, located directly under the root of the data directory. 
     */
    public File getScriptRoot() throws IOException {
        return dataDir.findOrCreateDir("scripts");
    }

    /**
     * Finds a script directory located at the specified path, returning <code>null</code> if no 
     * such directory exists.
     * 
     */
    public File findScriptDir(String path) throws IOException {
        File f = new File(getScriptRoot(), path);
        if (f.exists() && f.isDirectory()) {
            return f;
        }
        return null;
    }

    /**
     * Finds a script directory located at the specified path, creating the directory if it does not
     * already exist.
     * 
     */
    public File findOrCreateScriptDir(String path) throws IOException {
        File f = findScriptDir(path);
        if (f != null) {
            return f;
        }

        f = new File(getScriptRoot(), path);
        if (!f.mkdirs()) {
            throw new IOException("Unable to create directory " + f.getPath());
        }

        return f;
    }

    /**
     * Finds a script file with the specified filename located  in the specified directory path, 
     * returning <code>null</code> if the file does not exist.
     */
    public File findScriptFile(String dirPath, String filename) throws IOException {
        return findScriptFile(dirPath + File.separator + filename);
    }

    /**
     * Finds a script file at the specified path, returning <code>null</code> if the file does not 
     * exist.
     */
    public File findScriptFile(String path) throws IOException {
        File f = new File(getScriptRoot(), path);
        return f.exists() ? f : null;
    }

    /**
     * The root "apps" directory, located directly under {@link #getScriptRoot()}.
     */
    public File getAppRoot() throws IOException {
        return dataDir.findOrCreateDir("scripts", "apps");
    }

    /**
     * Finds a named app dir, returning <code>null</code> if the directory does not exist.
     */
    public File findAppDir(String app) throws IOException {
        return findScriptDir("apps" + File.separator + app);
    }

    /**
     * Finds a named app dir, creating if it does not already exist.
     */
    public File findOrCreateAppDir(String app) throws IOException {
        return findOrCreateScriptDir("apps" + File.separator + app);
    }

    /**
     * The root "wps" directory, located directly under {@link #getScriptRoot()} 
     */
    public File getWpsRoot() throws IOException {
        return dataDir.findOrCreateDir("scripts", "wps");
    }

    /**
     * Creates a new script engine for the specified script file.
     */
    public ScriptEngine createNewEngine(File script) {
        return createNewEngine(ext(script));
    }

    /**
     * Creates a new script engine for the specified file extension.
     */
    public ScriptEngine createNewEngine(String ext) {
        return initEngine(engineMgr.getEngineByExtension(ext));
    }

    /*
     * Initializes a new script engine by looking up the plugin matching the engines factory. 
     */
    ScriptEngine initEngine(ScriptEngine engine) {
        if (engine == null) {
            return null;
        }

        for (ScriptPlugin plugin : plugins()) {
            if (plugin.getScriptEngineFactoryClass().isInstance(engine.getFactory())) {
                plugin.initScriptEngine(engine);
                break;
            }
        }

        return engine;
    }

    
    /**
     * Looks up the app handler for the specified script returning <code>null</code> if no such 
     * handler can be found.
     * <p>
     * This method works by looking up all {@link ScriptPlugin} instances and delegating to 
     * {@link ScriptPlugin#createAppHandler()}.
     * </p>
     */
    public AppHandler lookupAppHandler(File script) {
        ScriptPlugin p = plugin(script);
        return p != null ? p.createAppHandler() : null;
    }

    /**
     * Looks up the wps handler for the speified script returning <code>null</code> if no such 
     * handler can be found.
     * <p>
     * This method works by looking up all {@link ScriptPlugin} instances and delegating to 
     * {@link ScriptPlugin#createWpsHandler()}.
     * </p>
     */
    public WpsHandler lookupWpsHandler(File script) {
        ScriptPlugin p = plugin(script);
        return p != null ? p.createWpsHandler() : null;
    }

    public String lookupPluginId(File script) {
        ScriptPlugin p = plugin(script);
        return p != null ? p.getId() : null;
    }

    public String lookupPluginDisplayName(File script) {
        ScriptPlugin p = plugin(script);
        return p != null ? p.getDisplayName() : null;
    }

    public String lookupPluginEditorMode(File script) {
        ScriptPlugin p = plugin(script);
        return p != null ? p.getEditorMode() : null;
    }

    /*
     * Looks up all {@link ScriptPlugin} instances in the application context.
     */
    List<ScriptPlugin> plugins() {
        if (plugins == null) {
            synchronized (this) {
                if (plugins == null) {
                    plugins = GeoServerExtensions.extensions(ScriptPlugin.class);
                }
            }
        }
        return plugins;
    }

    /*
     * Looks up the plugin for the specified script.
     */
    ScriptPlugin plugin(File script) {
        String ext = ext(script);

        for (ScriptPlugin plugin : plugins()) {
            if (ext.equalsIgnoreCase(plugin.getExtension())) {
                return plugin;
            }
        }

        return null;
    }

    /*
     * Helper method for extracting extension from filename throwing exception if the file has no
     * extension. 
     */
    String ext(File script) throws IllegalArgumentException {
        String ext = FilenameUtils.getExtension(script.getName());
        if (ext == null) {
            throw new IllegalArgumentException(script.getName() + " has no extension");
        }
        return ext;
    }
}
