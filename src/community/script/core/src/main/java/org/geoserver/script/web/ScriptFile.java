package org.geoserver.script.web;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.io.FileUtils;

public class ScriptFile implements Serializable {

    File file;
    String contents;
    
    ScriptFile(File file) {
        this.file = file;
    }
    
    public File getFile() {
        return file;
    }
    
    public void update(String contents) {
        this.contents = contents;
    }
    
    public boolean isModified() {
        return contents != null;
    }
    
    public String read() throws IOException {
        return contents != null ? contents : FileUtils.readFileToString(file);
    }
    
    public void save() throws IOException {
        if (contents != null) {
            FileUtils.writeStringToFile(file, contents);
        }
    }
}
