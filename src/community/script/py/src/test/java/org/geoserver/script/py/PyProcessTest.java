package org.geoserver.script.py;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.geoserver.script.ScriptManager;
import org.geoserver.script.wps.ScriptProcessTest;

public class PyProcessTest extends ScriptProcessTest {

    @Override
    protected File setUpInternal(ScriptManager scriptMgr) throws Exception {
        File wps = scriptMgr.getWpsRoot();
        File script = new File(wps, "buffer.py");

        FileUtils.copyURLToFile(getClass().getResource(script.getName()), script);
        return script;
    }
}
