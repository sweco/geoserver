package org.geoserver.flow.rest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
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
        Properties props = (Properties) value;
        //writer.startNode(arg0)
        //writer.setValue(props.toString());
        Set<Entry<Object, Object>> entrySet = props.entrySet();
        Iterator<Entry<Object, Object>> it = entrySet.iterator();
        while (it.hasNext()) {
            Entry<Object, Object> entry = it.next();
            writer.startNode(entry.getKey().toString());
            writer.setValue(entry.getValue().toString());
            writer.endNode();
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
        // TODO Auto-generated method stub
        return null;
    }

}
