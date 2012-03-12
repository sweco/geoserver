package org.geoserver.script;

import java.util.concurrent.atomic.AtomicLong;

import javax.script.ScriptEngine;

public class ScriptSession {

    static AtomicLong IDGEN = new AtomicLong();

    long id;
    String extension;
    ScriptEngine engine;

    ScriptSession(ScriptEngine engine, String extension) {
        this.engine = engine;
        this.extension = extension;
        this.id = IDGEN.getAndIncrement();
    }

    public long getId() {
        return id;
    }

    public String getExtension() {
        return extension;
    }

    public String getEngineName() {
        return engine.getFactory().getEngineName();
    }

    public ScriptEngine getEngine() {
        return engine;
    }

}
