/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import org.apache.commons.io.FileUtils;
import org.geotools.util.logging.Logging;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

/**
 * A script file or directory.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class Script extends File {

    static Logger LOGGER = Logging.getLogger(Script.class);

    String contents;
    long fileChecksum;

    public Script(File file) {
        super(file.toURI());
        fileChecksum = checksum();
    }

    long checksum() {
        try {
            return Files.getChecksum(this, new CRC32());
        } catch (IOException e) {
            LOGGER.log(Level.FINER, "unable to compute checksum for " + getPath(), e);
        }
        return -1;
    }

    public void update(String contents) {
        this.contents = contents;
    }
    
    public boolean isModified() {
        if (contents == null) {
            return false;
        }

        //compute checksum and compare to file checksum
        long checksum = -1; 
        try {
            checksum = ByteStreams.getChecksum(
                ByteStreams.newInputStreamSupplier(contents.getBytes()), new CRC32());
        } catch (IOException e) {
            LOGGER.log(Level.FINER, "unable to compute checksum for contents " + contents, e);
            return true;
        }

        return checksum != fileChecksum;
    }
    
    public String read() throws IOException {
        return contents != null ? contents : FileUtils.readFileToString(this);
    }
    
    public void save() throws IOException {
        if (contents != null) {
            FileUtils.writeStringToFile(this, contents);
            contents = null;
            fileChecksum = checksum(); 
        }
    }
}
