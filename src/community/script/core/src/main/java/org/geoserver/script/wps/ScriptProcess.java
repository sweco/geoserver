/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.geoserver.platform.FileWatcher;
import org.geoserver.script.ScriptManager;
import org.geotools.data.Parameter;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

/**
 * Implementation of {@link Process} backed by a script.
 * <p>
 * This class does its work by delegating all methods to the {@link WpsHandler} interface. This 
 * class maintains a link to the backing script {@link File} and uses a {@link FileWatcher} to 
 * detect changes to the underlying script. When changed a new {@link ScriptEngine} is created and 
 * the underlying script is reloaded. 
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ScriptProcess implements Process {

    /** process name*/
    Name name;
    
    /** watcher for file changes */
    FileWatcher<ScriptEngine> fw;
    
    /** script manager */
    ScriptManager scriptMgr;

    /** the handler for interacting with the script */
    WpsHandler handler;

    ScriptProcess(Name name, File script, ScriptManager scriptMgr) {
        this.name = name;
        this.scriptMgr = scriptMgr;

        handler = scriptMgr.lookupWpsHandler(script);
        fw = new FileWatcher<ScriptEngine>(script) {
            @Override
            protected ScriptEngine parseFileContents(InputStream in) throws IOException {
                ScriptEngine engine = ScriptProcess.this.scriptMgr.createNewEngine(getFile());
                try {
                    engine.eval(new InputStreamReader(in));
                } catch (ScriptException e) {
                    throw new IOException(e);
                }
                return engine;
            }
        };
    }

    String getTitle() throws ScriptException, IOException {
        return handler.getTitle(fw.read());
    }

    String getVersion() throws ScriptException, IOException {
        return handler.getVersion(fw.read());
    }

    public String getDescription() throws ScriptException, IOException {
        return handler.getDescription(fw.read());
    }

    public Map<String, Parameter<?>> getInputs() throws ScriptException, IOException {
        return handler.getInputs(fw.read());
    }

    public Map<String, Parameter<?>> getOutputs() throws ScriptException, IOException {
        return handler.getOutputs(fw.read());
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input,
            ProgressListener monitor) throws ProcessException {

        try {
            return handler.run(input, fw.read());
        } catch (Exception e) {
            throw new ProcessException(e);
        }
    }

}
