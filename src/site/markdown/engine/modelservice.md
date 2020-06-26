# The ModelService
The Model Service provides a service layer to manage workflow models. The model service persists BPMN models in the internal model repository. Each model is identified by a unique version assigned to a model.  This service component is used by the Imixs Workflow Engine internally. The service is based on the core interface [Model](../core/model.html).

To create and manage workflow models the Eclipse tool  [Imixs-BPMN Modeller](../modelling/index.html) can be used.
 
## Methods 
The ModelService EJB extends the interface '_org.imixs.workflow.ModelManager_' and provides the following methods:


|Method              		 | Description 				 |
|----------------------------|---------------------------|
|getModel(version)           | Returns a Model by version. The method throws a ModelException in case  the model version did not exits.|
|addModel(model  )           | Adds a new Model to the ModelManager.|
|removeModel(version)        | Removes a Model from the ModelManager.|
|getModelByWorkitem(workitem)| Returns a Model matching a given workitem. The method throws a ModelException in case the model version did not exits..|
|getVersions()        |Returns a sorted String list of all stored model versions.|
|findVersionsByGroup(group)        | Returns a sorted list of model versions containing the workflow group. The result is sorted in reverse order, so the highest version number is the first in the result list.|
|saveModel(model,filename)   | Saves a BPMNModel into a Document and adds the model into the ModelManager. The model can be accessed by its filename|
|saveModel(model)        | Saves a BPMNModel into a Document and adds the model into the ModelManager.|
|deleteModel(version)        | Deletes an existing Model Entities from the database and removes the model form the internal ModelStore..|
|loadModelEntity(version)        | Loads an existing Model Entities from the database.|


## Auto Detect ModelVersion

With the method *getModelByWorkitem(workitem)* the ModelService provides an algorithm to find a matching model version either by the $workflowGroup or by a regex. For example

	modelversion = "(^1.0)" 
	
will find a model version 1.0.7  

If no modelversion is provided by a workitem but the $workflowgroup matches a model the model version will be taken from the latest version of the corresponding group. 



## How to Add a Model

The ModelService provides a method to add a BPMNModel object. This object can be created from a file. See the following example:

	@EJB
	ModelService modelService;
	InputStream inputStream = new FileInputStream(new File("ticket.bpmn"));
	ticketModel = BPMNParser.parseModel(inputStream, "UTF-8");
	modelService.addModel(model);

You can also persist the model into the data storage

	modelService.save(ticketModel,"ticket.bpmn");

A persisted model will be automatically loaded when the workflow service starts. 	
