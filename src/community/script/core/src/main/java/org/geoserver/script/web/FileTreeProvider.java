/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import wickettree.ITreeProvider;

public class FileTreeProvider<T extends File> implements ITreeProvider<T> {

    List<T> roots;

    public FileTreeProvider(File... roots) {
        this(Arrays.asList(roots));
    }

    public FileTreeProvider(List<File> roots) {
        this.roots = new ArrayList();
        for (File f : roots) {
            this.roots.add(file(f));
        }
    }

    @Override
    public Iterator<T> getRoots() {
        return roots.iterator();
    }

    public boolean isRoot(T file) {
        return roots.contains(file);
    }

    @Override
    public boolean hasChildren(T object) {
        return object.isDirectory();
    }

    @Override
    public Iterator<? extends T> getChildren(T object) {
        final Iterator<File> it = Arrays.asList(object.listFiles()).iterator();
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public T next() {
                return file(it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }
        };
    }

    protected T file(File file) {
        return (T) file;
    }

    @Override
    public IModel<T> model(T object) {
        return new Model<T>(object);
    }

    @Override
    public void detach() {
    }
}
