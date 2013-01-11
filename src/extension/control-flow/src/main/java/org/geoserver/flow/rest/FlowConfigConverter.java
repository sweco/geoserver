package org.geoserver.flow.rest;

import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class FlowConfigConverter implements Converter {

    @Override
    public boolean canConvert(Class clazz) {
        return clazz.equals(Properties.class);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        Properties properties = (Properties) value;
        Set<Object> keys = properties.keySet();
        for (Object key : keys) {
            writer.startNode(key.toString());
            Object obj = properties.getProperty(key.toString());
            writer.setValue((String) obj);
            writer.endNode();
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
        // TODO Auto-generated method stub
        return null;
    }

}
