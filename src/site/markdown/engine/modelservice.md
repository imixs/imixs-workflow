# The ModelService
The Model Service provides a service layer to manage workflow models. The model service persists BPMN models in the internal model repository. Each model is identified by a unique version assigned to a model.  This service component is used by the Imixs Workflow Engine internally. The service is based on the core interface [Model](../core/model.html).

To create and manage workflow models the Eclipse tool  [Imixs-BPMN Modeller](../modelling/index.html) can be used.
 
##Methods 
The ModelService EJB extends the interface '_org.imixs.workflow.ModelManager_' and provides the following methods:


|Method              		 | Description 				 |
|----------------------------|---------------------------|
|getModel(version)           | Returns a Model by version. The method throws a ModelException in case  the model version did not exits.|
|addModel(model  )           | Adds a new Model to the ModelManager.|
|removeModel(version)        | Removes a Model from the ModelManager.|
|getModelByWorkitem(workitem)| Returns a Model matching a given workitem. The method throws a ModelException in case the model version did not exits..|
|getVersions()        |Returns a sorted String list of all stored model versions.|
|findVersionsByGroup(group)        | Returns a sorted list of model versions containing the workflow group. The result is sorted in reverse order, so the highest version number is the first in the result list.|
|saveModel(model)        | Saves a BPMNModel as an Entity and adds the model into the ModelManager.|
|deleteModel(version)        | Deletes an existing Model Entities from the database and removes the model form the internal ModelStore..|
|loadModelEntity(version)        | Loads an existing Model Entities from the database.|


 