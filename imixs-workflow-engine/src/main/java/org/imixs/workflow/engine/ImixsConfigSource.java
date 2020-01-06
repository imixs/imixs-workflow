/*******************************************************************************
 * <pre>
 *  Imixs Workflow 
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - initial API and implementation
 *      Ralph Soika - Software Developer
 * </pre>
 *******************************************************************************/

package org.imixs.workflow.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * The ImixsConfigSource is a custom config source based on Microprofile Config API.
 * <p>
 * The config source reads the Imixs-Workflow property file named 'imxis.properties'.
 * <p>
 * With this custom config source the imixs.properties file can be reused without the need to
 * migrate all properties into the file META-INF/microprofile-config.properties. It is recommended
 * to store imixs specific properties into the file imixs.properties
 * <p>
 * As per SPI it is necessary to register the implementation in META-INF/services by adding an entry
 * in a file called 'org.eclipse.microprofile.config.spi.ConfigSource'
 * 
 * @author rsoika
 *
 */

public class ImixsConfigSource implements ConfigSource {

  public static final String NAME = "ImixsConfigSource";
  private Map<String, String> properties = null;
  private static Logger logger = Logger.getLogger(ImixsConfigSource.class.getName());

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
          logger.warning("Deprecated imixs.property '" + keyAlternative
              + "' should be replaced by '" + key + "'");
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
   * This method is used to load a imixs.property file into the property Map<String,String>
   * <p>
   * The imixs.property file is loaded from the current threads classpath.
   * 
   */
  private void loadProperties() {
    properties = new HashMap<String, String>();
    Properties fileProperties = new Properties();
    try {
      fileProperties.load(Thread.currentThread().getContextClassLoader()
          .getResource("imixs.properties").openStream());

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
}
