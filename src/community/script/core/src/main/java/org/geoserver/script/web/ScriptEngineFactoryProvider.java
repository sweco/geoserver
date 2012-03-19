/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.util.Arrays;
import java.util.List;

import javax.script.ScriptEngineFactory;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.script.ScriptManager;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * Provider for available script engine factories.
 *  
 * @author Justin Deoliveira, OpenGeo
 */
public class ScriptEngineFactoryProvider extends GeoServerDataProvider<ScriptEngineFactory> {

    public static Property<ScriptEngineFactory> NAME = new BeanProperty("name", "engineName");

    public static Property<ScriptEngineFactory> VERSION = new BeanProperty("version", "engineVersion");

    public static Property<ScriptEngineFactory> LANG = new BeanProperty("language", "languageName");
    
    public static Property<ScriptEngineFactory> LANG_VERSION = new BeanProperty("languageVersion", "languageVersion");
    
    @Override
    protected List<GeoServerDataProvider.Property<ScriptEngineFactory>> getProperties() {
        return Arrays.asList(NAME, VERSION, LANG, LANG_VERSION);
    }

    @Override
    protected List<ScriptEngineFactory> getItems() {
        return getScriptManager().getEngineManager().getEngineFactories();
    }

    @Override
    protected IModel newModel(Object object) {
        return new ScriptEngineFactoryModel((ScriptEngineFactory)object);
    }

    ScriptManager getScriptManager() {
        return GeoServerApplication.get().getBeanOfType(ScriptManager.class);
    }

    class ScriptEngineFactoryModel extends LoadableDetachableModel<ScriptEngineFactory> {

        String name;

        ScriptEngineFactoryModel(ScriptEngineFactory factory) {
            name = factory.getEngineName();
        }

        @Override
        protected ScriptEngineFactory load() {
            for (ScriptEngineFactory factory : 
                getScriptManager().getEngineManager().getEngineFactories()) {
                if (factory.getEngineName().equals(name)) {
                    return factory;
                }
            }
            return null;
        }
    
    }
}
