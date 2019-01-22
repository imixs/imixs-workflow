# The PropertyService 

The Imixs Workflow Engine provides a property service to manage application specific properties  in a common way. The properties can be stored into a file named 'imixs.properties'.  The singleton ejb 'PropertyService' provides a service to access the imxis.property file. The property file can be packaged together with an application in any ejb module.  This in an example how to acces the imixs.properties:
  
    ...
    @EJB 	
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

The properties are cached for each application using this service. If the properties have changed during runtime an application can reset the cached properties.
 
	// change some properties...
	.....
	// reset property configuration
	propertyService.reset();
	// read new properties
	configurationProperties =propertyService.getProperties();

  