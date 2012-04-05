package org.geoserver.script.app;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.geoserver.script.ScriptIntTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class AppTest extends ScriptIntTestSupport {

    File app;
    String ext;
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
    
        app = getScriptManager().findOrCreateAppDir("foo");
        ext = getExtension();
    }

    protected String getExtension() {
        return "js";
    }

    public void testSimple() throws Exception {
        FileUtils.copyURLToFile(
            getClass().getResource("index-helloWorld."+ext), new File(app, "index."+ext));
    
        MockHttpServletResponse resp = getAsServletResponse("/script/apps/foo/index."+ext);
        assertEquals(200, resp.getStatusCode());
        assertEquals("Hello World!", resp.getOutputStreamContent());
    
    }
}
