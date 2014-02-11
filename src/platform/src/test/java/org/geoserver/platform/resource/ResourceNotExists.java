/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import org.geoserver.platform.Resource;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class ResourceNotExists extends BaseMatcher<Resource> {
    
    @Override
    public boolean matches(Object item) {
        if(item instanceof Resource) {
            return !((Resource) item).exists();
        }
        return false;
    }
    
    @Override
    public void describeTo(Description description) {
        description.appendText("resource that does not exist");
    }
    
}
