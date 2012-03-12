package org.geoserver.script.bsh;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.script.ScriptManager;
import org.geoserver.test.GeoServerTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class BshAppTest extends GeoServerTestSupport {

    File app;
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
    
        app = getScriptManager().findOrCreateAppDir("foo");
    }

    protected ScriptManager getScriptManager() {
        return GeoServerExtensions.bean(ScriptManager.class);
    }

    public void testSimple() throws Exception {
        FileUtils.copyURLToFile(
            getClass().getResource("index-helloWorld.bsh"), new File(app, "index.bsh"));

        MockHttpServletResponse resp = getAsServletResponse("/script/apps/foo/index.bsh");
        assertEquals(200, resp.getStatusCode());
        assertEquals("Hello World!", resp.getOutputStreamContent());

    }
}
