package org.imixs.workflow.engine;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * This is a custom config source based on Microprofile Config API.
 * <p>
 * The config source reads the optional property file name 'imxis.properties'.
 * 
 * @author rsoika
 *
 */
public class WorkflowConfigSource implements ConfigSource {
    
    public static final String NAME = "MemoryConfigSource";
    private static final Map<String,String> PROPERTIES = new HashMap<>();

    @Override
    public int getOrdinal() {
        return 900;
    }

    @Override
    public Map<String, String> getProperties() {
        return PROPERTIES;
    }

    @Override
    public String getValue(String key) {
        if(PROPERTIES.containsKey(key)){
            return PROPERTIES.get(key);
        }
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }
}