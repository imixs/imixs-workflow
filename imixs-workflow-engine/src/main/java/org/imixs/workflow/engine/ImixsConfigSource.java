/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow.engine;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * The ImixsConfigSource is a custom config source based on Microprofile Config
 * API.
 * <p>
 * The config source reads the Imixs-Workflow property file named
 * 'imxis.properties'.
 * <p>
 * With this custom config source the imixs.properties file can be reused
 * without the need to migrate all properties into the file
 * META-INF/microprofile-config.properties. It is recommended to store imixs
 * specific properties into the file imixs.properties
 * <p>
 * As per SPI it is necessary to register the implementation in
 * META-INF/services by adding an entry in a file called
 * 'org.eclipse.microprofile.config.spi.ConfigSource'
 * 
 * @author rsoika
 *
 */

public class ImixsConfigSource implements ConfigSource {

    public static final String NAME = "ImixsConfigSource";
    private Map<String, String> properties = null;
    private static final Logger logger = Logger.getLogger(ImixsConfigSource.class.getName());

    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }

    @Override
    public int getOrdinal() {
        return 900;
    }

    @Override
    public String getValue(String key) {
        if (properties == null) {
            loadProperties();
        }

        String value = properties.get(key);
        // search alterntive / deprecated imixs.property?
        if (value == null || value.isEmpty()) {
            String keyAlternative = getAlternative(key);
            if (keyAlternative != null && !keyAlternative.isEmpty()) {
                value = properties.get(keyAlternative);
                if (value != null && !value.isEmpty()) {
                    logger.log(Level.WARNING, "Deprecated imixs.property ''{0}'' should be replaced by ''{1}''",
                            new Object[]{keyAlternative, key});
                }
            }

        }
        return value;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, String> getProperties() {
        if (properties == null) {
            loadProperties();
        }
        return properties;
    }

    /**
     * This method is used to load a imixs.property file into the property
     * Map<String,String>
     * <p>
     * The imixs.property file is loaded from the current threads classpath.
     * 
     */
    private void loadProperties() {
        properties = new HashMap<String, String>();
        Properties fileProperties = new Properties();
        try {
            fileProperties.load(getFileFromResourceAsStream("imixs.properties"));
            // now we put the values into the property Map.....
            for (Object key : fileProperties.keySet()) {
                String value = fileProperties.getProperty(key.toString());
                if (value != null && !value.isEmpty()) {
                    properties.put(key.toString(), value);
                }
            }

        } catch (Exception e) {
            logger.warning("unable to find imixs.properties in current classpath");
            if (logger.isLoggable(Level.FINE)) {
                e.printStackTrace();
            }
        }

    }

    /**
     * This method provides key alternatives for deprecated imixs.property values
     * 
     * <ul>
     * <li>lucence.fulltextFieldList - index.fields</li>
     * <li>lucence.indexFieldListAnalyze - index.fields.analyze</li>
     * <li>lucence.indexFieldListNoAnalyze - index.fields.noanalyze</li>
     * <li>lucence.indexFieldListStore - index.fields.store</li>
     * 
     * <li>lucence.defaultOperator - index.operator</li>
     * <li>lucence.splitOnWhitespace - index.splitwhitespace</li>
     * </ul>
     * 
     * @param key
     * @return
     */
    private String getAlternative(String key) {

        if ("index.fields".equals(key)) {
            return "lucence.fulltextFieldList";
        }
        if ("index.fields.analyze".equals(key)) {
            return "lucence.indexFieldListAnalyze";
        }
        if ("index.fields.noanalyze".equals(key)) {
            return "lucence.indexFieldListNoAnalyze";
        }
        if ("index.fields.store".equals(key)) {
            return "lucence.indexFieldListStore";
        }

        if ("index.operator".equals(key)) {
            return "lucence.defaultOperator";
        }
        if ("index.splitwhitespace".equals(key)) {
            return "lucence.splitOnWhitespace";
        }

        return null;
    }

    /**
     * Helper method to get a file from the resources folder works everywhere, IDEA,
     * unit test and JAR file.
     * 
     * @see https://mkyong.com/java/java-read-a-file-from-resources-folder/
     * @param fileName
     * @return
     */
    private InputStream getFileFromResourceAsStream(String fileName) {
        // The class loader that loaded the class
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        // the stream holding the file content
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }
    }

}
