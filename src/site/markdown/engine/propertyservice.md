# The PropertyService 
 
<p style="border:solid 1px red;padding:5px;">
<strong>Note:</strong><span>This service is deprecated and is replaced since version 5.0.0 by the Microprofile Config API! <br />See <a href="configsource.html">Imixs Config Source</a>.</span> 
</p>

The Imixs Workflow Engine provides a property service to manage application specific properties  in a common way. The properties can be stored into a file named `imixs.properties`.  The class `PropertyService` provides a service to access the `imxis.property` file. The property file can be packaged together with an application in any ejb module.  This in an example how to access the imixs.properties:
  
```java  
    @Inject 	
    PropertyService propertyService;
	@PostConstruct
	void init() {
		// load configuration
		configurationProperties =propertyService.getProperties();
		// skip if no configuration available
		if (configurationProperties != null) {
			String myProperty=configurationProperties.getProperty("myProperty");
			....
		}
		.....
	}
```

The properties are cached for each application using this service. If the properties have changed during runtime an application can reset the cached properties.
 
```java
	// change some properties...
	.....
	// reset property configuration
	propertyService.reset();
	// read new properties
	configurationProperties =propertyService.getProperties();
```
  