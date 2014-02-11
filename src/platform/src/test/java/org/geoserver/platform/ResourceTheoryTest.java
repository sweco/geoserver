/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.hamcrest.CoreMatchers.*;
import static org.geoserver.platform.ResourceMatchers.*;

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
    
    @Theory
    public void theoryExtantHaveDate(Resource res) throws Exception {
        assumeThat(res, exists());
        
        assertThat(res.lastmodified(), notNullValue());
    }
    
    @Theory
    public void theoryHavePath(Resource res) throws Exception {
        assertThat(res.getPath(), notNullValue());
    }
    
    @Theory
    public void theoryHaveName(Resource res) throws Exception {
        assertThat(res.getPath(), notNullValue());
    }
    
    @Theory
    public void theoryLeavesHaveIstream(Resource res) throws Exception {
        assumeThat(res, is(leaf()));
        
        assertThat(res.in(), notNullValue());
    }
    
    @Theory
    public void theoryLeavesHaveOstream(Resource res) throws Exception {
        assumeThat(res, is(leaf()));
        
        assertThat(res.out(), notNullValue());
    }
    
    @Theory
    public void theoryLeavesPersistData(Resource res) throws Exception {
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
