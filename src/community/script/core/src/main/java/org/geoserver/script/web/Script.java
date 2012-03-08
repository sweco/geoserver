/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class Script extends File {

    public Script(File file) {
        super(file.toURI());
    }

    String contents;

    public void update(String contents) {
        this.contents = contents;
    }
    
    public boolean isModified() {
        return contents != null;
    }
    
    public String read() throws IOException {
        return contents != null ? contents : FileUtils.readFileToString(this);
    }
    
    public void save() throws IOException {
        if (contents != null) {
            FileUtils.writeStringToFile(this, contents);
        }
    }
}
