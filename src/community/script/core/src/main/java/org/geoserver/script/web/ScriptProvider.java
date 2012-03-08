package org.geoserver.script.web;

import java.io.File;
import java.io.IOException;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.script.ScriptManager;

public class ScriptProvider extends FileTreeProvider<Script> {

    public ScriptProvider() throws IOException {
        super(getScriptManager().getAppRoot(),getScriptManager().getWpsRoot());
    }

    @Override
    protected Script file(File file) {
        return new Script(file);
    }

    @Override
    protected boolean isModified(Script file) {
        return file.isModified();
    }

    @Override
    protected void save(Script file) throws IOException {
        file.save();
    }

    public static ScriptManager getScriptManager() {
        return GeoServerExtensions.bean(ScriptManager.class);
    }
}
