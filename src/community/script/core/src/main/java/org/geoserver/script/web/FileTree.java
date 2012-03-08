/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.io.File;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;

import wickettree.NestedTree;
import wickettree.content.Folder;
import wickettree.theme.WindowsTheme;

/**
 * File based tree.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class FileTree<T extends File> extends NestedTree<T> {

    T selected;

    public FileTree(String id, FileTreeProvider<T> provider) {
        super(id, provider);
        add(new AbstractBehavior() {
            @Override
            public void renderHead(IHeaderResponse response) {
                response.renderCSSReference(new WindowsTheme());
            }
        });
    }

    public T getSelected() {
        return selected;
    }

    public void select(T file) {
        this.selected = file;
    }

    @Override
    public FileTreeProvider<T> getProvider() {
        return (FileTreeProvider<T>) super.getProvider();
    }

    protected void onClick(T file, AjaxRequestTarget target) {
    }

    @Override
    protected Component newContentComponent(String id, IModel<T> model) {
        return new Folder<T>(id, this, model) {
            @Override
            protected Component newLabelComponent(String id, IModel<T> model) {
                T file = model.getObject();

                if (getProvider().isRoot(file)) {
                    return super.newLabelComponent(id, model).setEscapeModelStrings(false); 
                }
                
                return new AjaxEditableLabel(id, newLabelModel(model)) {
                    @Override
                    protected WebComponent newLabel(MarkupContainer parent,
                            String componentId, IModel model) {
                        WebComponent c = super.newLabel(parent, componentId, model);
                        c.setEscapeModelStrings(false);
                        return c;
                    }
                    
                    protected String getLabelAjaxEvent() {
                        return "ondblclick";
                    }

                    protected void onSubmit(AjaxRequestTarget target) {
                        super.onSubmit(target);

                        T file = getModelObject();
                        select(file);
                        updateNode(file, target);
                    };
                    
                };
            }

            @Override
            protected IModel<?> newLabelModel(IModel<T> model) {
                return new FileLabelModel(model);
            }

            @Override
            protected boolean isClickable() {
                return true;
            }

            @Override
            protected boolean isSelected() {
                return selected != null && selected.equals(getModelObject()) ;
            }

            @Override
            protected void onClick(AjaxRequestTarget target) {
                T file = getModelObject();
                
                if (selected != null) {
                    updateNode(selected, target);
                }

                selected = file;
                updateNode(selected, target);
                
                FileTree.this.onClick(file, target);
            }
        };
    }

    class FileLabelModel implements IModel<String> {

        IModel<T> model;

        FileLabelModel(IModel<T> model) {
            this.model = model;
        }
       
        @Override
        public String getObject() {
            T file = model.getObject();
            String label = file.getName();
            
            if (getProvider().isModified(file)) {
                label = "*<i>" + label + "</i>";
            }
            return label;
        }

        @Override
        public void setObject(String object) {
            if (object == null || object.trim().isEmpty()) {
                return;
            }

            T file = model.getObject();
            if (file.getName().equals(object)) {
                return;
            }

            //rename the file
            File newFile = new File(file.getParentFile(), object);
            if (file.renameTo(newFile)) {
                model.setObject(getProvider().file(newFile));
            }
            else {
                error("Could not rename file " + file.getPath() + " to " + object);
            }
        }

        @Override
        public void detach() {
            model.detach();
        }
    }
}
