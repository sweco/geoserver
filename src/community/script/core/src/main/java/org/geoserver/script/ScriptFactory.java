package org.geoserver.script;

import org.geoserver.platform.GeoServerExtensions;

/**
 * Base class for classes implementing factory extension points.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ScriptFactory {

    /** script manager, lazily loaded */
    private ScriptManager scriptMgr;

    protected ScriptFactory() {
        this(null);
    }

    protected ScriptFactory(ScriptManager scriptMgr) {
        this.scriptMgr = scriptMgr;
    }

    /*
     * method to lookup script manager lazily, we do this because this factory is created as part
     * of the SPI plugin process, which happens before spring context creation
     */
    protected ScriptManager scriptMgr() {
        if (scriptMgr == null) {
            scriptMgr = GeoServerExtensions.bean(ScriptManager.class);
        }
        return scriptMgr;
    }
}
