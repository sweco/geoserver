package org.geoserver.wms.featureinfo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.xml.XMLUtils;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;

import com.vividsolutions.jts.geom.Geometry;

import net.opengis.wfs.FeatureCollectionType;

/**
 * A GetFeatureInfo response handler specialized in producing ESRI ArcGIS Server data for a GetFeatureInfo request.
 * <p>
 * This class is mainly implemented to let GeoServer use the cascading WMS feature of an ArcGIS Server remote WMS
 * and still be able to query the layer.
 * 
 * TODO: Use proper XML streaming instead of hand-made strings...
 *
 * @author Sweco Position AB
 */
public class ESRIFeatureInfoOutputFormat extends GetFeatureInfoOutputFormat {

    private WMS wms;

    public ESRIFeatureInfoOutputFormat(final WMS wms) {
        super("application/vnd.esri.wms_featureinfo_xml");
        this.wms = wms;
    }

    /**
     * Writes the feature information to the client in ESRI format.
     * 
     * @see GetFeatureInfoOutputFormat#write
     */
    public void write(FeatureCollectionType results, GetFeatureInfoRequest request, OutputStream out) throws ServiceException, IOException {
        final Charset charSet = wms.getCharSet();
        OutputStreamWriter osw = new OutputStreamWriter(out, charSet);
        PrintWriter writer = null;
        try {
        	writer = new PrintWriter(osw);
        	writer.print("<?xml version=\"1.0\" encoding=\""); writer.print(charSet.name()); writer.println("\"?>");
        	writer.print("<FeatureInfoResponse version=\""); writer.print(wms.getVersion()); writer.println("\" xmlns=\"http://www.esri.com/wms\">");
        	
            int featuresPrinted = 0;
            final int maxfeatures = request.getFeatureCount();
            final List collections = results.getFeature();
            final int size = collections.size();
            for (int i = 0; i < size; i++)
            {
                final FeatureCollection fr = (FeatureCollection) collections.get(i);
                boolean layerResults = false;
                FeatureIterator reader = null;
                try {
	                reader = fr.features();
	                boolean startFeat = true;
	                while (reader.hasNext()) {
	                    final Feature feature = reader.next();
	                    if (startFeat) {
	                    	final Name schemaName = fr.getSchema().getName();
	                    	writer.println(" <FeatureInfoCollection layername=\"" + ResponseUtils.encodeXML(schemaName.getLocalPart()) + "\">");
	                    	layerResults = true;
	                    	startFeat = false;
	                    }
	                	if (featuresPrinted < maxfeatures) {
	                		if (feature instanceof SimpleFeature) {
	                			writer.println("  <FeatureInfo>");
		                        final SimpleFeature f = (SimpleFeature) feature;
		                        final SimpleFeatureType schema = (SimpleFeatureType) f.getType();
		                        final List<AttributeDescriptor> types = schema.getAttributeDescriptors();
	                            for (final AttributeDescriptor descriptor : types) {
	                                final Name name = descriptor.getName();
	                                if (Geometry.class.isAssignableFrom(descriptor.getType().getBinding())) {
	                                	// TODO: SEMARA: Research if ESRI FeatureInfo can include geometry
	                                } else {
	                                	final String localName = name.getLocalPart();
	                                	final Object value = f.getAttribute(name);
	                                	writer.println("   <Field>");
	                                	writer.print("    <FieldName>"); writer.print(ResponseUtils.encodeXML(localName)); writer.print("</FieldName>");
	                                	writer.print("    <FieldValue>");
	                                	if (value != null) {
	                                		writer.print(ResponseUtils.encodeXML(value.toString()));
	                                	}
	                                	writer.println("    </FieldValue>");
	                                	writer.println("   </Field>");
	                                }
	                            }
	                            writer.println("  </FeatureInfo>");
	                            featuresPrinted++;
	                		} else {
	                			LOGGER.warning("No implementation for feature of type " + feature.getClass().getName() + " - ignoring");
	                		}
		                }
	                }
	                if (layerResults) {
	                	writer.println(" </FeatureInfoCollection>");
	                }
                } finally {
                	IOUtils.closeQuietly(reader);
                }
            }
            writer.println("</FeatureInfoResponse>");
        } finally {
        	IOUtils.closeQuietly(writer);
        }
    }        
    
    @Override
    public String getCharset(){ 
        return wms.getGeoServer().getSettings().getCharset();
    }

}
