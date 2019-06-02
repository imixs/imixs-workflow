# The SetupService 

The Imixs SetupService initializes the workflow instance by loading the model default data. The service runs during deployment and starts automatically all defined Scheduler Jobs.

## The Default Model Data

A default model file can be imported during the setup if the imixs.property variable 'model.default.data' is set with a list of bpmn or xml files:

	model.default.data=my-model.bpmn

You can also define the Default Model data by a environment variable. In this case the variable is written in upper-case and with underscores 

	MODEL_DEFAULT_DATA=my-model.bpmn

## Lucene Index
During the initialization, the Lucene index will be verified and initialized if it does not exist. 


  
## Scheduler Jobs

Optional scheduler Jobs will be started by the service.


## Workflow Instance Status

With the method 'getModelCount()' a client can check the status of the worklfow instance. The method returns the number of available workflow models. 