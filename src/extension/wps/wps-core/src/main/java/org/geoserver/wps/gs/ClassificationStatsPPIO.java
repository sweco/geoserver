package org.geoserver.wps.gs;

import java.io.InputStream;

import javax.xml.namespace.QName;

import org.geoserver.wps.ppio.XMLPPIO;
import org.geotools.process.classify.ClassificationStats;
import org.jaitools.numeric.Range;
import org.jaitools.numeric.Statistic;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

public class ClassificationStatsPPIO extends XMLPPIO {

    protected ClassificationStatsPPIO() {
        super(ClassificationStats.class, ClassificationStats.class, new QName("Results"));
    }

    @Override
    public void encode(Object object, ContentHandler h) throws Exception {
        ClassificationStats results = (ClassificationStats) object;

        h.startDocument();
        h.startElement(null, null, "Results", null);

        for (int i = 0; i < results.size(); i++) {
            Range range = results.range(i);

            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, null, "lowerBound", null, range.getMin().toString());
            atts.addAttribute(null, null, "upperBound", null, range.getMax().toString());
            atts.addAttribute(null, null, "count", null, results.count(i).toString());
            
            h.startElement(null, null, "Class", atts);
            for (Statistic stat : results.getStats()) {
                h.startElement(null, null, stat.name(), null);
                
                String value = String.valueOf(results.value(i, stat));
                h.characters(value.toCharArray(), 0, value.length());
                
                h.endElement(null, null, stat.name());
            }
            h.endElement(null, null, "Class");
        }

        h.endElement(null, null, "Results");
        h.endDocument();
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        return null;
    }

}
