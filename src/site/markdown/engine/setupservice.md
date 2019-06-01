# The SetupService 

The Imixs SetupService is used to inizalize the server status.

## DefaultModelData

A default model file can be imported during the setup if the environment variable 'model.default.data' is set with a list of bpmn or xml files:

	model.default.data=my-model.bpmn



## Lucene Index
During the initalization, the Lucen index will be verified and initalized if it does not exist. 


  
## Scheduler Jobs

Optional scheduler Jobs will be startted by the service.
