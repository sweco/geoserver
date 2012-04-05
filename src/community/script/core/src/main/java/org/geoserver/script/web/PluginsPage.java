/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import javax.script.ScriptEngineFactory;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.geoserver.script.ScriptPlugin;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;

/**
 * Page providing script plugin info.
 *  
 * @author Justin Deoliveira, OpenGeo
 */
public class PluginsPage extends GeoServerSecuredPage {

    public PluginsPage() {
        GeoServerTablePanel<ScriptPlugin> plugins = 
            new GeoServerTablePanel<ScriptPlugin>("plugins", new PluginProvider()) {
                @Override
                protected Component getComponentForProperty(String id, IModel itemModel, 
                    Property<ScriptPlugin> property) {
                    return new Label(id, property.getModel(itemModel));
                }
            };
        plugins.getTopPager().setVisible(false);
        add(plugins);

        GeoServerTablePanel<ScriptEngineFactory> engines = 
            new GeoServerTablePanel<ScriptEngineFactory>("engines", new ScriptEngineFactoryProvider()) {

                @Override
                protected Component getComponentForProperty(String id,
                        IModel itemModel, Property<ScriptEngineFactory> property) {
                    return new Label(id, property.getModel(itemModel));
                }
            };
        engines.getTopPager().setVisible(false);
        add(engines);

    }
}
