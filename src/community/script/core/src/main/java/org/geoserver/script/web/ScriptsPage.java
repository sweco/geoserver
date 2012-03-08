/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.io.IOException;
import java.util.Map;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.script.ScriptManager;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.CodeMirrorEditor;

public class ScriptsPage extends GeoServerSecuredPage {

    FileTreePanel<Script> scripts;
    CodeMirrorEditor editor;

    public ScriptsPage() {
         Form form = new Form("form");
         add(form);

         ScriptManager mgr = getScriptManager();
         try {
            form.add(scripts = new FileTreePanel<Script>("scripts",  
                 new FileTreeProvider<Script>(mgr.getAppRoot(),mgr.getWpsRoot())));
        } catch (IOException e) {
            throw new WicketRuntimeException(e);
        }

         form.add(editor = new CodeMirrorEditor("editor", new Model()) {
            @Override
            protected boolean handleOnBlur() {
                 return true;
            }

            @Override
            protected void onBlur(AjaxRequestTarget target) {
                super.onBlur(target);

                Map<String,String[]> map = RequestCycle.get().getRequest().getParameterMap();
                for (String key : map.keySet()) {
                    String[] val = map.get(key);
                    if (val.length == 1 && "".equals(val[0])) {
                        
                    }
                }
            }
         });
         editor.setOutputMarkupId(true);
     }

     protected ScriptManager getScriptManager() {
         return GeoServerExtensions.bean(ScriptManager.class);
     }
}
