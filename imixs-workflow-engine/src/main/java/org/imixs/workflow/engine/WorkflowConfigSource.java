package org.imixs.workflow.engine;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * This is a custom config source based on Microprofile Config API.
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

public class WorkflowConfigSource implements ConfigSource {

	public static final String NAME = "WorkflowConfigSource";
	private Properties properties = null;
	private static Logger logger = Logger.getLogger(WorkflowConfigSource.class.getName());

	@Override
	public int getOrdinal() {
		return 900;
	}

	@Override
	public String getValue(String key) {
		if (properties == null) {
			loadProperties();
		}
		return properties.getProperty(key);
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
		return new PropertyAdapter(properties);
	}

	/**
	 * loads a imixs.property file
	 * 
	 * (located at current threads classpath)
	 * 
	 */
	private void loadProperties() {
		properties = new Properties();
		try {
			properties
					.load(Thread.currentThread().getContextClassLoader().getResource("imixs.properties").openStream());
		} catch (Exception e) {
			logger.warning("unable to find imixs.properties in current classpath");
			if (logger.isLoggable(Level.FINE)) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * This class helps to adapt the java.util.Properties to the Microprofile
	 * Properties Map.
	 * 
	 * @author rsoika
	 */
	class PropertyAdapter implements Map<String, String> {
		Properties properties;

		public PropertyAdapter() {
			// no op
		}

		public PropertyAdapter(Properties properties) {
			this.properties = properties;
		}

		@Override
		public int size() {
			return properties.size();
		}

		@Override
		public boolean isEmpty() {
			return properties.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return properties.containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return properties.containsValue(value);
		}

		@Override
		public String get(Object key) {
			return properties.getProperty((String) key);
		}

		@Override
		public String put(String key, String value) {
			return (String) properties.put(key, value);
		}

		@Override
		public String remove(Object key) {
			return (String) properties.remove(key);
		}

		@Override
		public void putAll(Map<? extends String, ? extends String> m) {
			properties.putAll(m);

		}

		@Override
		public void clear() {
			properties.clear();

		}

		@Override
		public Set<String> keySet() {
			Set<Object> objSet = properties.keySet();
			Set<String> confSet = new HashSet<>();
			for (Object entry : objSet) {
				confSet.add(entry.toString());
			}

			return confSet;
		}

		@Override
		public Collection<String> values() {
			Collection<String> result = new Vector<String>();
			for (Object obj : properties.values()) {
				result.add(obj.toString());
			}
			return result;
		}

		@Override
		public Set<Entry<String, String>> entrySet() {

			Set<Entry<Object, Object>> propertiesEntrySet = properties.entrySet();
			Set<Entry<String, String>> result = new HashSet<Map.Entry<String, String>>();
			for (Entry<Object, Object> propertyEntrySet : propertiesEntrySet) {
				Entry<String, String> e = new Entry<String, String>() {
					@Override
					public String getKey() {
						return propertyEntrySet.getKey().toString();
					}

					@Override
					public String getValue() {
						return this.getValue().toString();
					}

					@Override
					public String setValue(String value) {
						return this.setValue(value);
					}

				};

				result.add(e);
			}
			return result;
		}

	}

}