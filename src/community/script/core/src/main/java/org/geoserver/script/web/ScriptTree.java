package org.geoserver.script.web;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.script.ScriptManager;

import wickettree.ITreeProvider;
import wickettree.NestedTree;
import wickettree.content.Folder;
import wickettree.theme.WindowsTheme;

public class ScriptTree extends NestedTree<ScriptFile> {

    IModel<ScriptFile> selected;

    public ScriptTree(String id) {
        super(id, new ScriptProvider());
        add(new AbstractBehavior() {
            @Override
            public void renderHead(IHeaderResponse response) {
                response.renderCSSReference(new WindowsTheme());
            }
        });
    }

    public IModel<ScriptFile> getSelected() {
        return selected;
    }

    protected void onScriptClick(IModel<ScriptFile> script, IModel<ScriptFile> last, AjaxRequestTarget target) {
    }

    @Override
    protected Component newContentComponent(String id, IModel<ScriptFile> model) {
        return new Folder<ScriptFile>(id, this, model) {
            @Override
            protected Component newLabelComponent(String id, IModel<ScriptFile> model) {
                ScriptFile f = model.getObject();
                String label = f.getFile().getName();
                if (f.isModified()) {
                    label = "<i>" + label + "</i>";
                }
                return new Label(id, label).setEscapeModelStrings(false);
            }

            @Override
            protected IModel<?> newLabelModel(IModel<ScriptFile> model) {
                return new PropertyModel(model, "file.name");
            }

            @Override
            protected boolean isSelected() {
                return selected != null && selected.equals(getModel());
            }

            @Override
            protected boolean isClickable() {
                return true;
            }

            @Override
            protected void onClick(AjaxRequestTarget target) {
                ScriptFile f = getModelObject();
                if (!f.getFile().isDirectory()) {
                    onScriptClick(getModel(), selected, target);
                    if (selected != null) {
                        updateNode(selected.getObject(), target);
                    }
                    selected = getModel();
                    updateNode(f, target);
                }
                else {
                    super.onClick(target);
                }
            
            }

        };
    }

    static class ScriptProvider implements ITreeProvider<ScriptFile> {

        Map<File,ScriptFile> files = new HashMap();

        @Override
        public Iterator<? extends ScriptFile> getRoots() {
            try {
                ScriptManager scriptMgr = GeoServerExtensions.bean(ScriptManager.class);
                return Arrays.asList(wrap(scriptMgr.getAppRoot()), wrap(scriptMgr.getWpsRoot()))
                    .iterator();
            } catch (IOException e) {
                throw new WicketRuntimeException(e);
            }
        }

        @Override
        public boolean hasChildren(ScriptFile object) {
            return object.getFile().isDirectory();
        }

        @Override
        public Iterator<? extends ScriptFile> getChildren(ScriptFile object) {
            final Iterator<File> it = Arrays.asList(object.getFile().listFiles()).iterator();
            return new Iterator<ScriptFile>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public ScriptFile next() {
                    return wrap(it.next());
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public IModel<ScriptFile> model(ScriptFile object) {
            return new Model(object);
        }

        @Override
        public void detach() {
        }

        ScriptFile wrap(File file) {
            ScriptFile scriptFile = files.get(file);
            if (scriptFile == null) {
                scriptFile = new ScriptFile(file);
                files.put(file, scriptFile);
            }
            return scriptFile;
        }
    }
}
