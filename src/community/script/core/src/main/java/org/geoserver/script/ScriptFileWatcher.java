/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.geoserver.platform.FileWatcher;

/**
 * Special watcher that watches an underlying script and when changed creates a new 
 * {@link ScriptEngine} instance evaluated with the contents of the modified script.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ScriptFileWatcher extends FileWatcher<ScriptEngine> {

    ScriptManager scriptMgr;

    public ScriptFileWatcher(File file, ScriptManager scriptMgr) {
        super(file);
        this.scriptMgr = scriptMgr;
    }

    @Override
    protected ScriptEngine parseFileContents(InputStream in) throws IOException {
        ScriptEngine engine = scriptMgr.createNewEngine(getFile());
        try {
            engine.eval(new InputStreamReader(in));
        } catch (ScriptException e) {
            throw new IOException(e);
        }
        return engine;
    }
}
