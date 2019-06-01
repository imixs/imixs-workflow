# The SetupService 

The Imixs SetupService is used to inizalize the server status.

## DefaultModelData

A default model file can be imported during the setup if the imixs.property variable 'model.default.data' is set with a list of bpmn or xml files:

	model.default.data=my-model.bpmn

You can also define the Default Model data by a environment variable. In this case the variable is written in uppercase and with underscores 

	MODEL_DEFAULT_DATA=my-model.bpmn

## Lucene Index
During the initalization, the Lucene index will be verified and initalized if it does not exist. 


  
## Scheduler Jobs

Optional scheduler Jobs will be startted by the service.
