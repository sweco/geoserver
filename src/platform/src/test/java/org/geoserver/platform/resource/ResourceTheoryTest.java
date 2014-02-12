/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.hamcrest.Matchers.*;
import static org.geoserver.platform.resource.ResourceMatchers.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
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
    
    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    protected abstract Resource getResource(String path) throws Exception;
    
    @Theory
    public void theoryNotNull(String path) throws Exception {
        Resource res = getResource(path);
        
        assertThat(res, notNullValue());
    }

    @Theory
    public void theoryExtantHaveDate(String path) throws Exception {
        Resource res = getResource(path);
        
        assumeThat(res, defined());
        
        long result = res.lastmodified();
        
        assertThat(result, notNullValue());
    }
    
    @Theory
    public void theoryHaveSamePath(String path) throws Exception {
        Resource res = getResource(path);
        
        String result = res.getPath();
        
        assertThat(result, is(equalTo(path)));
    }
    
    @Theory
    public void theoryHaveName(String path) throws Exception {
        Resource res = getResource(path);
        
        String result = res.getPath();
        
        assertThat(result, notNullValue());
    }
    
    @Theory
    public void theoryLeavesHaveIstream(String path) throws Exception {
        Resource res = getResource(path);
        
        assumeThat(res, is(resource()));
        
        InputStream result = res.in();
        
        assertThat(result, notNullValue());
    }
    
    @Theory
    public void theoryLeavesHaveOstream(String path) throws Exception {
        Resource res = getResource(path);
        
        assumeThat(res, is(resource()));
        
        OutputStream result = res.out();
        
        assertThat(result, notNullValue());
    }
    
    @Theory
    public void theoryUndefinedHaveIstream(String path) throws Exception {
        Resource res = getResource(path);
        
        assumeThat(res, is(undefined()));
        
        InputStream result = res.in();
        
        assertThat(result, notNullValue());
    }
    
    @Theory
    public void theoryUndefinedHaveOstream(String path) throws Exception {
        Resource res = getResource(path);
        
        assumeThat(res, is(undefined()));
        
        OutputStream result = res.out();
        
        assertThat(result, notNullValue());
    }
    
    @Theory
    public void theoryNonDirectoriesPersistData(String path) throws Exception {
        Resource res = getResource(path);
        
        assumeThat(res, not(directory()));
        
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
    
    @Theory
    public void theoryDirectoriesHaveNoIstreams(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, is(directory()));
        
        exception.expect(IllegalStateException.class);
        res.in();
    }
    
    @Theory
    public void theoryDirectoriesHaveNoOstream(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, is(directory()));
        
        exception.expect(IllegalStateException.class);
        res.out();
    }
    
    @Theory
    public void theoryLeavesHaveNoListOfChildren(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, is(resource()));
        
        Collection<Resource> result = res.list();
        
        assertThat(result, nullValue());
    }
    
    @Theory
    public void theoryUndefinedHaveNullListOfChildren(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, is(undefined()));
        
        Collection<Resource> result = res.list();
        
        assertThat(result, nullValue());
    }
    
    @Theory
    public void theoryDirectoriesHaveChildren(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, is(directory()));
        
        Collection<Resource> result = res.list();
        
        assertThat(result, notNullValue());
    }
    
    @Theory
    public void theoryChildrenKnowTheirParents(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, is(directory()));
        Collection<Resource> children = res.list();
        assumeThat(children, not(empty())); // Make sure this resource has children
        
        for(Resource child: children) {
            Resource parent = child.getParent();
            assertThat(parent, equalTo(res));
        }
    }
    
    @Theory
    public void theoryParentsKnowTheirChildren(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, is(directory()));
        Resource parent = res.getParent();
        assumeThat(path,parent, notNullValue()); // Make sure this resource has a parent
        
        Collection<Resource> result = parent.list();
        
        assertThat(path,result, hasItem(res)); // this assumed equals was written!
    }
    
    @Theory
    public void theorySamePathGivesEquivalentResource(String path) throws Exception {
        Resource res1 = getResource(path);
        Resource res2 = getResource(path);
        
        assertThat(res2, equalTo(res1));
    }
    
    @Theory
    public void theoryParentIsDirectory(String path) throws Exception {
        Resource res = getResource(path);
        Resource parent = res.getParent();
        assumeThat(path+" not root", parent, notNullValue());
        
        if( res.getType() != Type.UNDEFINED){
            assertThat(path+" directory",parent, is(directory()));
        }
    }
}
