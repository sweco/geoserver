/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.io.IOException;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.script.ScriptManager;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.CodeMirrorEditor;

/**
 * Page providing a tree browser and an editor for scripts.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ScriptsPage extends GeoServerSecuredPage {

    FileTreePanel<Script> scripts;
    CodeMirrorEditor editor;

    public ScriptsPage() {
         Form form = new Form("form");
         add(form);

         try {
            form.add(scripts = new FileTreePanel<Script>("scripts", new ScriptProvider()) {
                @Override
                protected void onClick(Script file, AjaxRequestTarget target) {
                    try {
                        //upadte the editor contents
                        editor.getModel().setObject(file.read());

                        //set the mode depending on the file
                        String mode = getScriptManager().lookupPluginEditorMode(file);
                        if (mode != null) {
                            editor.setMode(mode);
                        }
                        target.addComponent(editor);
                    } catch (IOException e) {
                        error(e);
                    }
                }
                @Override
                protected void onDelete(Script file, AjaxRequestTarget target) {
                    editor.getModel().setObject("");
                    target.addComponent(editor);
                }
            });
        } catch (IOException e) {
            throw new WicketRuntimeException(e);
        }

         form.add(editor = new CodeMirrorEditor("editor", new Model()) {
            @Override
            protected boolean handleOnBlur() {
                 return true;
            }

            @Override
            protected void onBlur(String contents, AjaxRequestTarget target) {
                Script script = scripts.getSelected();
                if (script == null) {
                    return;
                }

                script.update(contents);
                scripts.changed(script, target);
            }
         });
         editor.setOutputMarkupId(true);
     }

     protected ScriptManager getScriptManager() {
         return GeoServerExtensions.bean(ScriptManager.class);
     }
}
