/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import net.opengis.wfs.FeatureCollectionType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.asn1.x509.Attribute;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.MapLayerInfo;
import org.geotools.data.ows.Layer;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReTypingFeatureCollection;
import org.geotools.data.wms.WebMapServer;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.WMSLayer;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.util.logging.Logging;
import org.geotools.wfs.v1_0.WFSConfiguration;
import org.geotools.xml.Parser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.SAXException;

/**
 * Layer identifier specialized in WMS cascading layers
 * 
 * @author Andrea Aime - GeoSolutions 
 */
public class WMSLayerIdentifier implements LayerIdentifier {
    
    static final Logger LOGGER = Logging.getLogger(WMSLayerIdentifier.class);

    private EntityResolverProvider resolverProvider;

    public WMSLayerIdentifier(EntityResolverProvider resolverProvider) {
        this.resolverProvider = resolverProvider;
    }

    public List<FeatureCollection> identify(FeatureInfoRequestParameters params, int maxFeatures) throws IOException {
        final int x = params.getX();
        final int y = params.getY();
        WMSLayerInfo info = (WMSLayerInfo) params.getLayer().getResource();
        WebMapServer wms = info.getStore().getWebMapServer(null);
        Layer layer = info.getWMSLayer(null);

        CoordinateReferenceSystem crs = params.getRequestedCRS();
        if (crs == null) {
            // use the native one
            crs = info.getCRS();
        }
        ReferencedEnvelope bbox = params.getRequestedBounds();
        int width = params.getWidth();
        int height = params.getHeight();

        // we can cascade GetFeatureInfo on queryable layers and if the GML or ESRI mime type is supported
        if (!layer.isQueryable()) {
            return null;
        }

        final List<String> infoFormats = wms.getCapabilities().getRequest().getGetFeatureInfo().getFormats();
        final boolean supportsGml2 = infoFormats.contains("application/vnd.ogc.gml");
        final boolean supportsEsri = infoFormats.contains("application/vnd.esri.wms_featureinfo_xml");
        if (!(supportsGml2 || supportsEsri)) {
            return null;
        }

        // the wms layer does request in a CRS that's compatible with the WMS server srs
        // list,
        // we may need to transform
        WMSLayer ml = new WMSLayer(wms, layer);
        // delegate to the web map layer as there's quite a bit of reprojection magic
        // code
        // that we want to be consistently reproduced for GetFeatureInfo as well
        List<FeatureCollection> results = null;
        InputStream is = null;
        try {
            if (supportsGml2) {
                is = ml.getFeatureInfo(bbox, width, height, x, y, "application/vnd.ogc.gml", maxFeatures);
                results = getGmlInfoResults(info, is);
            } else {
                is = ml.getFeatureInfo(bbox, width, height, x, y, "application/vnd.esri.wms_featureinfo_xml", maxFeatures);
                results = getEsriInfoResults(info, is);
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Tried to parse " + (supportsGml2 ? "GML2" : "ESRI") + " response, but failed", t);
        } finally {
            is.close();
        }
        if (results == null) {
            return Collections.emptyList();
        }
        return results;
    }

    private List<FeatureCollection> getGmlInfoResults(final WMSLayerInfo info, final InputStream is) throws IOException, SAXException, ParserConfigurationException {
        final List<FeatureCollection> results = new ArrayList<FeatureCollection>();
        final Parser parser = new Parser(new WFSConfiguration());
        parser.setStrict(false);
        parser.setEntityResolver(resolverProvider.getEntityResolver());
        final Object result = parser.parse(is);
        if (result instanceof FeatureCollectionType) {
            FeatureCollectionType fcList = (FeatureCollectionType) result;
            final List<SimpleFeatureCollection> rawResults = fcList.getFeature();

            // retyping feature collections to replace name and namespace
            // from cascading server with our local WMSLayerInfo
            for (final SimpleFeatureCollection fc : rawResults) {
                final SimpleFeatureType ft = fc.getSchema();

                final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
                builder.init(ft);

                builder.setName(info.getName());
                builder.setNamespaceURI(info.getNamespace().getURI());

                final SimpleFeatureType targetFeatureType = builder.buildFeatureType();
                FeatureCollection rfc = new ReTypingFeatureCollection(fc, targetFeatureType);

                results.add(rfc);
            }
        }
        return results;
    }

    private List<FeatureCollection> getEsriInfoResults(final WMSLayerInfo info, final InputStream is) throws IOException, SAXException {
        final List<FeatureCollection> results = new ArrayList<FeatureCollection>();
        // TODO: Use proper XML handling, for now count on known "field observations" + our own ESRI format
        BufferedReader reader = null;
        try {
            final Locale locale = new Locale("sv", "SE"); // TODO: locale
            final NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
            final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            final String charset = "UTF-8"; // TODO: dynamic when moving to proper XML-parsing
            reader = new BufferedReader(new InputStreamReader(is, charset));
            String line;
            DefaultFeatureCollection collection = null;
            String layerName = null;
            String attributeName = null;
            Map<String, Object> attributes = null;
            boolean inResponse = false;
            boolean inCollection = false;
            boolean inFeature = false;
            boolean inField = false;
            while ((line = reader.readLine()) != null) {
                line = StringUtils.trimToEmpty(line);
                if (line.equals("")) {
                    continue;
                }
                if (inField) {
                    if (line.contains("<FieldName>")) {
                        attributeName = StringUtils.trimToEmpty(line.replace("<FieldName>", "").replace("</FieldName>", ""));
                    } else if (line.contains("<FieldValue>")) {
                        final String string = StringUtils.trimToNull(line.replace("<FieldValue>", "").replace("</FieldValue>", ""));
                        boolean stringType = true;
                        if (string != null) {
                            final ParsePosition pos = new ParsePosition(0);
                            final Date date = dateFormat.parse(string, pos);
                            if (pos.getIndex() == string.length()) {
                                stringType = false;
                                attributes.put(attributeName, date);
                            }
                            if (stringType) {
                                pos.setIndex(0);
                                final Number number = numberFormat.parse(string, pos);
                                if (pos.getIndex() == string.length()) {
                                    stringType = false;
                                    attributes.put(attributeName, number);
                                }
                            }
                        }
                        if (stringType) {
                            attributes.put(attributeName, string);
                        }
                        attributeName = null;
                    } else if (line.equals("</Field>")) {
                        inField = false;
                    }
                } else if (inFeature) {
                    if (line.equals("<Field>")) {
                        inField = true;
                    } else if (line.equals("</FeatureInfo>")) {
                        final SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
                        //featureTypeBuilder.setName(layerName);
                        featureTypeBuilder.setName(info.getName());
                        featureTypeBuilder.setNamespaceURI(info.getNamespace().getURI());
                        for (final String featureAttributeName : attributes.keySet()) {
                            final Object value = attributes.get(featureAttributeName);
                            if (value instanceof Date) {
                                featureTypeBuilder.add(featureAttributeName, Date.class);
                            } else if (value instanceof Double) {
                                featureTypeBuilder.add(featureAttributeName, Double.class);
                            } else if (value instanceof Float) {
                                featureTypeBuilder.add(featureAttributeName, Float.class);
                            } else if (value instanceof Long) {
                                if (((Long) value).longValue() == ((Long) value).intValue()) {
                                    featureTypeBuilder.add(featureAttributeName, Integer.class);
                                } else {
                                    featureTypeBuilder.add(featureAttributeName, String.class); // GeoTools has no long, treat as text
                                }
                            } else if (value instanceof Integer) {
                                featureTypeBuilder.add(featureAttributeName, Integer.class);
                            } else {
                                featureTypeBuilder.add(featureAttributeName, String.class);
                            }
                        }
                        final SimpleFeatureType featureType = featureTypeBuilder.buildFeatureType();
                        final SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
                        final String id = "fid-" + UUID.randomUUID().toString(); // TODO: id?
                        final SimpleFeature feature = featureBuilder.buildFeature(id);
                        for (final String featureAttributeName : attributes.keySet()) {
                            feature.setAttribute(featureAttributeName, attributes.get(featureAttributeName));
                        }
                        collection.add(feature);
                        attributes = null;
                        inFeature = false;
                    } else {
                        throw new SAXException("Ill-formatted ESRI XML, found unexpected content in FeatureInfo: " + line);
                    }
                } else if (inCollection) {
                    if (line.equals("<FeatureInfo>")) {
                        attributes = new HashMap<String, Object>();
                        inFeature = true;
                    } else if (line.equals("</FeatureInfoCollection>")) {
                        results.add(collection);
                        collection = null;
                        layerName = null;
                        inCollection = false;
                    } else {
                        throw new SAXException("Ill-formatted ESRI XML, found unexpected content in FeatureInfoCollection: " + line);
                    }
                } else if (inResponse) {
                    if (line.startsWith("<FeatureInfoCollection")) {
                        layerName = StringUtils.trimToEmpty(line.replace("<FeatureInfoCollection layername=\"", "").replace("\">", ""));
                        collection = new DefaultFeatureCollection(layerName);
                        inCollection = true;
                    } else if (line.equals("</FeatureInfoResponse>")) {
                        inResponse = false;
                    } else {
                        throw new SAXException("Ill-formatted ESRI XML, found unexpected content in FeatureInfoResponse: " + line);
                    }
                } else if (line.startsWith("<FeatureInfoResponse")) {
                    inResponse = true;
                } else if (line.startsWith("<?xml")) {
                    // OK
                } else {
                    throw new SAXException("Ill-formatted ESRI XML, found unexpected content: " + line);
                }
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }
        return results;
    }

    public boolean canHandle(MapLayerInfo layer) {
        return layer.getType() == MapLayerInfo.TYPE_WMS;
    }

}
