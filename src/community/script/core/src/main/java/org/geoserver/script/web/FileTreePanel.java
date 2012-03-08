/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.script.web.Toolbar.Tool;

import com.google.common.io.Files;

public class FileTreePanel<T extends File> extends Panel {

    Toolbar toolbar;
    FileTree<T> tree;

    public FileTreePanel(String id, FileTreeProvider<T> provider) {
        super(id);

        List<Tool> tools = new ArrayList<Tool>();
        tools.add(new Tool("page-new", false) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                T file = tree.getSelected();
                try {
                    Files.touch(newUntitledFile(file));
                    tree.updateBranch(file, target);
                } catch (IOException e) {
                    error(e);
                }
            }
        });
        tools.add(new Tool("folder-new", false) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                T file = tree.getSelected();
                newUntitledFile(file).mkdir();
                tree.updateBranch(file, target);
            }
        });
        tools.add(new Tool("delete", false) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                T file = tree.getSelected();
                T parent = tree.getProvider().file(file.getParentFile());

                file.delete();
                tree.updateBranch(parent, target);
            }
        });
        add(toolbar = new Toolbar("toolbar", tools));
        
        add(tree = new FileTree<T>("tree", provider) {
            protected void onClick(T file, AjaxRequestTarget target) {
                toolbar.toggleToolEnabled(file.isDirectory(), "page-new", target);
                toolbar.toggleToolEnabled(file.isDirectory(), "folder-new", target);

                toolbar.toggleToolEnabled(!tree.getProvider().isRoot(file), "delete", target);
            };
        });
    }

    File newUntitledFile(T file) {
        String name = "untitled";
        File newFile = new File(file, name);
        int i = 1;
        while(newFile.exists()) {
            newFile = new File(file, name + String.valueOf(i));
        }
        return newFile;
    }

}
