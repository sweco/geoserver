/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.py;

import java.util.HashMap;

import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.app.AppHandler;
import org.geoserver.script.wps.WpsHandler;
import org.python.core.Py;
import org.python.core.PyBoolean;
import org.python.core.PyException;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.jsr223.PyScriptEngineFactory;

/**
 * Python script plugin.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class PythonPlugin extends ScriptPlugin {

    public PythonPlugin() {
        super("py", PyScriptEngineFactory.class);
    }

    @Override
    protected AppHandler createAppHandler() {
        return new PyAppHandler(this);
    }

    @Override
    protected WpsHandler createWpsHandler() {
        return new PyWpsHandler(this);
    }

    static HashMap<Class<? extends PyObject>, Class> pyToJava = new HashMap();
    static {
        pyToJava.put(PyString.class, String.class);
        pyToJava.put(PyInteger.class, Integer.class);
        pyToJava.put(PyLong.class, Long.class);
        pyToJava.put(PyFloat.class, Double.class);
        pyToJava.put(PyBoolean.class, Boolean.class);
        //pyToJava.put(PyFile.class, File.class);
    }
    
    public static Class toJavaClass(PyType type) {
        Class clazz = null;
        try {
            Object o = Py.tojava(type, Object.class);
            if (o != null && o instanceof Class) {
                clazz = (Class) o;
            }
        }
        catch(PyException e) {}
        
        if (clazz != null && PyObject.class.isAssignableFrom(clazz)) {
            try {
                PyObject pyobj = (PyObject) clazz.newInstance();
                Object obj = pyobj.__tojava__(Object.class);
                if (obj != null) {
                    clazz = obj.getClass();
                }
            }
            catch(Exception e) {}
        }
        
        if (clazz != null && PyObject.class.isAssignableFrom(clazz)) {
            Class jclass = pyToJava.get(clazz);
            if (jclass != null) {
                clazz = jclass;
            }
        }
        
        if (clazz != null && clazz.getName().startsWith("org.python.proxies")) {
            //get base type
            PyType base = (PyType) type.getBase();
            Class c = toJavaClass(base);
            if (c != null) {
                clazz = c;
            }
        }
         return clazz;
    }
}
