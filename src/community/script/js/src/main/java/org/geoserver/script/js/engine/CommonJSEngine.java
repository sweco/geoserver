package org.geoserver.script.js.engine;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;

public class CommonJSEngine extends AbstractScriptEngine implements Invocable {

    private CommonJSEngineFactory factory;
    private Scriptable scope;
    
    public CommonJSEngine() {
        this(new CommonJSEngineFactory(null));
    }

    public CommonJSEngine(CommonJSEngineFactory factory) {
        this.factory = factory;
        Global global = getGlobal();
        Context cx = enterContext();
        try {
            scope = cx.newObject(global);
            scope.setPrototype(global);
            scope.put("exports", scope, cx.newObject(global));
        } finally {
            Context.exit();
        }
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }
    
    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        if (script == null) {
            throw new NullPointerException("Null script");
        }
        return eval(new StringReader(script) , context);
    }
    
    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        String filename = null;
        if (context != null && bindings != null) {
            filename = (String) bindings.get(ScriptEngine.FILENAME);
        }
        if (filename == null) {
            filename = (String) get(ScriptEngine.FILENAME);
        }
        
        filename = filename == null ? "<Unknown source>" : filename;
        Object result;
        Context cx = enterContext();
        try {
            result = cx.evaluateReader(scope, reader, filename, 1, null);
        } catch (IOException e) {
            throw new ScriptException(e);
        } finally {
            Context.exit();
        }
        return result;
    }
    
    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }
    
    private Global getGlobal() {
        return factory.getGlobal();
    }

    @Override
    public <T> T getInterface(Class<T> cls) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T getInterface(Object object, Class<T> cls) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object invokeFunction(String name, Object... args)
            throws ScriptException, NoSuchMethodException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object invokeMethod(Object object, String method, Object... args)
            throws ScriptException, NoSuchMethodException {
        // TODO Auto-generated method stub
        return null;
    }
    
    /**
     * Associate a context with the current thread.  This calls Context.enter()
     * and sets the language version to 1.8.
     * @return a Context associated with the thread
     */
    public static Context enterContext() {
        Context cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_1_8);
        return cx;
    }


}
