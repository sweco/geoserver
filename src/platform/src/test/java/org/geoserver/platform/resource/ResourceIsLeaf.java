/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import org.geoserver.platform.Resource;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class ResourceIsLeaf extends BaseMatcher<Resource> {
    
    @Override
    public boolean matches(Object item) {
        if(item instanceof Resource) {
            Resource res = (Resource)item;
            return res.exists() && res.list()==null;
        }
        return false;
    }
    
    @Override
    public void describeTo(Description description) {
        description.appendText("leaf resource");
    }
    
}
