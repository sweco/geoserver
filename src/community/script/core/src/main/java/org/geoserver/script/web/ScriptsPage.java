package org.geoserver.script.web;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.script.ScriptManager;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.CodeMirrorEditor;

public class ScriptsPage extends GeoServerSecuredPage {

    ScriptTree tree;
    CodeMirrorEditor editor;

     public ScriptsPage() {
         Form form = new Form("form");
         add(form);

         form.add(tree = new ScriptTree("scripts") {
             @Override
            protected void onScriptClick(IModel<ScriptFile> model, IModel<ScriptFile> last, AjaxRequestTarget target) {
                 ScriptFile script = model.getObject();

                 //update the editor contents
                 try {
                    editor.setModelObject(script.read());
                 } catch (IOException e) {
                    throw new WicketRuntimeException(e);
                 }

                 //update syntax highlighting
                 String mode = getScriptManager().lookupPluginEditorMode(script.getFile());
                 if(mode != null) {
                     editor.getOptions().put("mode", mode);
                 }

                 target.addComponent(editor);
            }
         });

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
                        ScriptFile file = tree.getSelected().getObject();
                        file.update(key);
                        tree.updateNode(file, target);
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
