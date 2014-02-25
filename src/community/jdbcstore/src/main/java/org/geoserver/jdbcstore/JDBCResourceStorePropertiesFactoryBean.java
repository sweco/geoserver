package org.geoserver.jdbcstore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.util.IOUtils;
import org.geoserver.jdbcconfig.JDBCGeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.data.DataUtilities;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.web.context.ServletContextAware;

public class JDBCResourceStorePropertiesFactoryBean extends PropertiesFactoryBean implements ServletContextAware {

    private static final Logger LOGGER = Logging.getLogger(JDBCGeoServerLoader.class);
    
    static final String CONFIG_FILE = "jdbcstore.properties";
    
    static final String CONFIG_SYSPROP = "jdbcstore.properties";
    
    static final String JDBCURL_SYSPROP = "jdbcstore.jdbcurl";
    
    static final String INITDB_SYSPROP = "jdbcstore.initdb";
    
    static final String IMPORT_SYSPROP = "jdbcstore.import";
    
    ServletContext servletContext;
    
    /**
     * DDL scripts copied to <data dir>/jdbcstore/scripts/ on first startup
     */
    private static final String[] SCRIPTS = { "dropdb.h2.sql", "dropdb.postgres.sql", 
        "initdb.h2.sql", "initdb.postgres.sql" };

    private static final String[] SAMPLE_CONFIGS = { "jdbcstore.properties.h2",
            "jdbcstore.properties.postgres" };

    GeoServerDataDirectory dataDir;
    
    public JDBCResourceStorePropertiesFactoryBean(GeoServerDataDirectory dataDir) {
        this.dataDir = dataDir;
    }

    @Override
    protected Properties createProperties() throws IOException {
        JDBCResourceStoreProperties config = loadConfig();
        if (!config.isEnabled()) {
            LOGGER.info("jdbc resource store is disabled");
            return config;
        }
        return config;
    }

    private JDBCResourceStoreProperties loadDefaultConfig() throws IOException {
        JDBCResourceStoreProperties config = new JDBCResourceStoreProperties(this);
        config.load(getClass().getResourceAsStream("/" + CONFIG_FILE));
        return config;
    }

    private JDBCResourceStoreProperties loadConfig() throws IOException {
        //copy over sample scripts 
        JDBCResourceStoreProperties config = loadDefaultConfig();

        /*
         * Find configuration, lookup heuristic is as follows.
         * 1. check system property "jdbcstore.properties" for path/url to properties file
         * 2. check system properties jdbcstore.jdbcurl, jdbconfig.initdb, jdbcimport.import
         * 3. look for <GEOSERVER_DATA_DIR>/jdbcstore/jdbcstore.properties
         * 4. use built in defaults 
         */
        if (loadConfigFromURL(config)) {
            return config;
        }

        if (loadConfigFromSysProps(config)) {
            return config;
        }

        if (loadConfigFromDataDir(config)) {
            return config;
        }

        LOGGER.info("Configuring jdbc resource store from defaults");

        //copy over default config to data dir
        saveConfig(config, "Default GeoServer JDBC resource store driver and connection pool options." + 
            " Edit as appropriate.");
        copySampleConfigsToDataDir();
        copyScriptsToDataDir();
        
        return config;
    }

    private boolean loadConfigFromSysProps(JDBCResourceStoreProperties config) throws IOException {
        String jdbcUrl = System.getProperty(JDBCURL_SYSPROP);
        if (jdbcUrl != null) {
            config.setJdbcUrl(jdbcUrl);

            config.setInitDb(Boolean.getBoolean(INITDB_SYSPROP));
            config.setImport(Boolean.getBoolean(IMPORT_SYSPROP));
            
            if (LOGGER.isLoggable(Level.INFO)) {
                StringBuilder msg = 
                    new StringBuilder("Configuring jdbc resource store from system properties:\n");
                msg.append("  ").append(JDBCURL_SYSPROP).append("=").append(jdbcUrl).append("\n");
                msg.append("  ").append(INITDB_SYSPROP).append("=").append(config.isInitDb()).append("\n");
                msg.append("  ").append(IMPORT_SYSPROP).append("=").append(config.isImport()).append("\n");
                LOGGER.info(msg.toString());
            }
            return true;
        }
        return false;
    }

    private boolean loadConfigFromURL(JDBCResourceStoreProperties config) throws IOException {
        String propUrl = System.getProperty(CONFIG_SYSPROP);
        if (propUrl == null) {
            return false;
        }

        URL url = null;
        try {
            //try to parse directly as url
            try {
                url = new URL(propUrl);
            }
            catch(MalformedURLException e) {
                //failed, try as a file path
                File f = new File(propUrl);
                if (f.canRead() && f.exists()) {
                    url = DataUtilities.fileToURL(f);
                }
            }
        }
        catch(Exception e) {
            LOGGER.log(Level.WARNING, "Error trying to read " + propUrl, e);
        }

        if (url != null) {
            LOGGER.info("Configuring jdbc resource store from " + url.toString());
            InputStream in = url.openStream();
            try {
                config.load(in);
            }
            finally {
                in.close();
            }
            return true;
        }
        
        LOGGER.severe("System property " + CONFIG_SYSPROP + " specified " + propUrl + " but could not be read, ignoring.");
        return false;
    }

    private boolean loadConfigFromDataDir(JDBCResourceStoreProperties config) throws IOException {
        File propFile = new File(getBaseDir(), CONFIG_FILE);
        if (propFile.exists()) {
            LOGGER.info("Loading jdbc resource store properties from " + propFile.getAbsolutePath());
            FileInputStream stream = new FileInputStream(propFile);
            try {
                config.load(stream);
                return true;
            } finally {
                stream.close();
            }
        }
        return false;
    }

    void saveConfig(JDBCResourceStoreProperties config) throws IOException {
        saveConfig(config, "");
    }

    private void saveConfig(JDBCResourceStoreProperties config, String comment) throws IOException {
        File propFile = new File(getBaseDir(), CONFIG_FILE);
        
        try {
            propFile.createNewFile();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Can't create file " + propFile.getAbsolutePath(), e);
            return;
        }

        try {
            OutputStream out = new FileOutputStream(propFile);
            try {
                config.store(out, comment);
            } finally {
                out.close();
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error saving jdbc resource store properties to file "
                + propFile.getAbsolutePath(), e);
            propFile.delete();
        }
    }

    private void copyScriptsToDataDir() throws IOException {
        final File scriptsDir = getScriptDir();
        Class<?> scope = JDBCGeoServerLoader.class;
        for (String scriptName : SCRIPTS) {
            File target = new File(scriptsDir, scriptName);
            if (!target.exists()) {
                copyFromClassPath(scriptName, scope, target);
            }
        }
    }
    
    private void copySampleConfigsToDataDir() throws IOException {
        final File baseDirectory = getBaseDir();
        for (String sampleConfig : SAMPLE_CONFIGS) {
            File target = new File(baseDirectory, sampleConfig);
            if (!target.exists()) {
                copyFromClassPath(sampleConfig, getClass(), target);
            }
        }
    }

    void copyFromClassPath(String name, Class<?> scope, File target) throws IOException {
        InputStream templateStream = scope.getResourceAsStream(name);
        try {
            IOUtils.copy(templateStream, target);
        } finally {
            templateStream.close();
        }
    }

    File getDataDir() {
        return new File(GeoServerResourceLoader.lookupGeoServerDataDirectory(servletContext));
    }

    File getBaseDir() throws IOException {
        return new File(getDataDir(), "jdbcstore");
    }

    File getScriptDir() throws IOException {
        return new File(getBaseDir(), "scripts");
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext=servletContext;
    }
}
