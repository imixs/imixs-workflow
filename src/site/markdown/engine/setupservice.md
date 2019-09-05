# The SetupService 

The Imixs SetupService initializes the workflow instance by loading the model default data. The service runs during deployment and starts automatically all defined Scheduler Jobs.

## The Default Model Data

A default model file can be imported during the setup if the imixs.property variable 'model.default.data' is set with a list of bpmn or xml files:

	model.default.data=my-model.bpmn

You can also define the Default Model data by a environment variable. In this case the variable is written in upper-case and with underscores. 

	MODEL_DEFAULT_DATA=my-model.bpmn

**Note:** An entry in the imixs.properties will overwrite the Environment variable in any case!

See also the section [Imixs Config Source ](./configsource.html)

## Default Instance Data

The SetupService can also be used to import any xml entity data stream. This mode can be used to import data like configuration data.

The data file need to be a valid XML file. The configuration is controlled by the imixs.property variable 'model.default.data'


	model.default.data=my-model.bpmn, my-data.xml

## Lucene Index
During the initialization, the Lucene index will be verified and initialized if it does not exist. 


  
## Scheduler Jobs

Optional scheduler Jobs will be started by the service.


## Workflow Instance Status

With the method 'getModelCount()' a client can check the status of the worklfow instance. The method returns the number of available workflow models. 



## The CDI SetupEvent

The SetupService EJB provides an Observer Pattern based on CDI Events. The events are fired before the setup service finished.
The Event is defined by the class:

    org.imixs.workflow.engine.SetupEvent

This _SetupEvent_ can be consumed by another Session Bean or managed bean implementing the @Observes annotation: 

	@Stateless
	public class MySetupHandler {
	    public void onEvent(@Observes SetuptEvent setupEvent){
	        ... extend the setup .....
    	}
	}
