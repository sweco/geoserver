package org.geoserver.flow.rest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class FlowConfigConverter implements Converter {

    @Override
    public boolean canConvert(Class clazz) {
        return clazz.equals(HashMap.class);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        Map<String, String> propsMap = (Map<String, String>) value;
        Set<Entry<String, String>> entrySet = propsMap.entrySet();
        Iterator<Entry<String, String>> entryIterator = entrySet.iterator();
        while (entryIterator.hasNext()) {
            Entry<String, String> entry = entryIterator.next();
            writer.startNode(entry.getKey());
            writer.setValue(entry.getValue());
            writer.endNode();
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
        // TODO Auto-generated method stub
        return null;
    }

}
