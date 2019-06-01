package org.imixs.workflow.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
		return properties.get(key);
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
			fileProperties
					.load(Thread.currentThread().getContextClassLoader().getResource("imixs.properties").openStream());

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

}