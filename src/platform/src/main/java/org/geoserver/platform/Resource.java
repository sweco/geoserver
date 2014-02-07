/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Resource used for configuration storage.
 */
public interface Resource {
  String getPath();
  String name();
  
  InputStream in();
  OutputStream out();
  File file();
  
  long lastmodified();
  
  Resource getParent();
  List<Resource> list();

  boolean exists();
  boolean mkdirs();
  // boolean createNew();
}
