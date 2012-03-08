/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * Toolbar panel.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class Toolbar extends Panel {
    
    ListView<Tool> tools;

    public Toolbar(String id, List<Tool> tools) {
        super(id);

        add(this.tools = new ListView<Tool>("tools", tools) {
            @Override
            protected void populateItem(ListItem<Tool> item) {
                Tool t = item.getModelObject();
                
                item.setOutputMarkupId(true);
                item.add(new AjaxLink<Tool>("link", item.getModel()) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        getModelObject().onClick(target);
                    }
                    protected void disableLink(org.apache.wicket.markup.ComponentTag tag) {
                        super.disableLink(tag);
                        tag.setName("a");
                    };
                }.setEnabled(t.isEnabled()));
                item.add(new SimpleAttributeModifier("class", t.css()));
            }
        });
    }

    public void toggleToolEnabled(boolean enabled, String toolName, AjaxRequestTarget target) {
        for (Iterator it = tools.iterator(); it.hasNext();) {
            ListItem<Tool> item = (ListItem<Tool>) it.next();
            Tool t = item.getModelObject();
            t.setEnabled(enabled);

            if (t.getName().equals(toolName)) {
                item.get("link").setEnabled(enabled);
                item.add(new SimpleAttributeModifier("class", t.css()));
                if (target != null) {
                    target.addComponent(item);
                }
            }
        }
    }

    public static class Tool implements Serializable {

        String name;
        boolean enabled;

        public Tool(String name) {
            this(name, true);
        }

        public Tool(String name, boolean enabled) {
            this.name = name;
            this.enabled = enabled;
        }

        public String getName() {
            return name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void onClick(AjaxRequestTarget target) {
        }

        String css() {
            String css = "icon button-" + name;
            if (!enabled) {
                css += " disabled";
            }
            return css;
        }
    }
}
