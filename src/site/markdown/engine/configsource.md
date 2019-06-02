# The Imixs Config Source 

The Imixs-Workflow ConfigSource provides a custom config source based on [Microprofile Config API](https://microprofile.io/project/eclipse/microprofile-config).

In this way an Imixs-Workflow instance can be configured based on a running environment. It is possible to modify configuration data from outside so that the workflow engine itself does not need to be redeployed. In general the configuration data can come from different locations and in different formats (e.g. system properties, system environment variables, .properties, .xml, datasource). 

The Imixs Config Source reads the Imixs-Workflow property file named 'imxis.properties'.


## How to Get a Configuraiton Property

Config variables  can be stored in the following 4 ConfigSources:

* System.getProperties() - (ordinal=400)
* System.getenv() - (ordinal=300)
* File META-INF/microprofile-config.properties
* File imixs.properties (ordinal=900)

If the same property is defined in multiple ConfigSources, based on the ordinal policy one of the values will effectively be used.

### Obtain Config via Injection

Each individual property can be injected directly. The injected value is static and the value is fixed on application starting.

	@Inject @ConfigProperty(name="lucence.indexDir") 
	String lucenePath;

### Config Object Injection

The config object can also be injected. With this object you can use the getValue() method to retrieve an individual property at runtime.

	@Inject Config config;
	...
	String lucenePath =  config.getValue("lucence.indexDir", String.class);
	....


## Deployment:

As per SPI it is necessary to register the implementation in META-INF/services by adding an entry in a file called
 
	org.eclipse.microprofile.config.spi.ConfigSource
	
This file is already provided by the Imixs-Workflow engine.


