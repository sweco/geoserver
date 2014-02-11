/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.hamcrest.CoreMatchers.*;
import static org.geoserver.platform.resource.ResourceMatchers.*;

import java.io.InputStream;
import java.io.OutputStream;

import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

/**
 * JUnit Theory test class for Resource invariants. Subclasses should provide representative 
 * DataPoints to test.
 * 
 * @author Kevin Smith, Boundless
 *
 */
@RunWith(Theories.class)
public abstract class ResourceTheoryTest {
    
    protected abstract Resource getResource(String path) throws Exception;
    
    @Theory
    public void theoryNotNull(String path) throws Exception {
        Resource res = getResource(path);
        
        assertThat(res, notNullValue());
    }

    @Theory
    public void theoryExtantHaveDate(String path) throws Exception {
        Resource res = getResource(path);
        
        assumeThat(res, exists());
        
        assertThat(res.lastmodified(), notNullValue());
    }
    
    @Theory
    public void theoryHaveSamePath(String path) throws Exception {
        Resource res = getResource(path);
        
        assertThat(res.getPath(), is(equalTo(path)));
    }
    
    @Theory
    public void theoryHaveName(String path) throws Exception {
        Resource res = getResource(path);
        
        assertThat(res.getPath(), notNullValue());
    }
    
    @Theory
    public void theoryLeavesHaveIstream(String path) throws Exception {
        Resource res = getResource(path);
        
        assumeThat(res, is(leaf()));
        
        assertThat(res.in(), notNullValue());
    }
    
    @Theory
    public void theoryLeavesHaveOstream(String path) throws Exception {
        Resource res = getResource(path);
        
        assumeThat(res, is(leaf()));
        
        assertThat(res.out(), notNullValue());
    }
    
    @Theory
    public void theoryLeavesPersistData(String path) throws Exception {
        Resource res = getResource(path);
        
        assumeThat(res, is(leaf()));
        
        byte[] test = {42, 29, 32, 120, 69, 0, 1};
        
        OutputStream ostream = res.out();
        try {
            ostream.write(test);
        } finally {
            ostream.close();
        }
        
        byte[] result=new byte[test.length];
        
        InputStream istream = res.in();
        try {
            istream.read(result);
            assertThat(istream.read(), is(-1));
        } finally {
            istream.close();
        }
        assertThat(result, equalTo(test));
    }
}
