# The Imixs ModelManager 

The Imixs `ModelManager` separates the BPMN process definition from the Workflow implementation. The interface  provides methods to manager BPMN Model instances and navigate through a  Workflow Model with its different elements. A workflow model has a unique version ID which allows to uniquely identify a BPMN model inside one Workflow Management System.  The Model Version is provided by a workflow instance in the item `$modelVersion`. 

An Imixs BPMN Model can be defined using the [Imixs-BPMN modeling tool](../modelling/index.html).

<img src="../images/modelling/bpmn_screen_00.png"/>



## The ModelManager Interface 
The interface `org.imixs.workflow.ModelManager` manage instances of a `BPMNModel` and is used by the [WorkflowKernel](workflowkernel.html) to access model information during the processing life cycle .

The Interface defines the following methods:

|Method              		 | Description 				 |
|----------------------------|---------------------------|
|getModel(version)           | Returns a Model by version. The method throws a ModelException in case  the model version did not exits.|
|addModel(BPMNModel)         | Adds a new Model to the ModelManager.|
|removeModel(version)        | Removes a Model from the ModelManager.|
|loadDefinition(workitem)    | Returns the BPMN Definition entity associated with a given workitem.|
|loadProcess(workitem)       | Returns the BPMN Process entity associated with a given workitem. |
|loadTask(workitem)          | Returns the BPMN Task entity associated with a given workitem.|
|loadEvent(workitem)         | Returns the BPMN Event entity associated with a given workitem.|
|nextModelElement(event, workitem) | Returns the next BPMN Flow Entity followed by a given Event Entity.|



## The Task Element

A 'Task Element' describes the unambiguously status of a running process instance within the process. 

<img src="../images/modelling/bpmn_screen_04.png"/>

A 'Task' can be identified by a unique ID in each model. A model cannot contain several Task elements having the same ID. The Imixs Task entity returned by the `ModelManager` is represendted as an [ItemCollection](itemcollection.md) and provides at least the following attributes: 
  
|Item                | Description 			 	                                                      |
|--------------------|-----------------------------------------------------------------------------|
|id                  | ID attribute of the corresponding BPMN element in the BPMN model. |
|type                | Type of the corresponding BPMN element: 'task'
|name                | Name of the Task Element 
|taskID              | Unique integer ID to identify the Task Entity in a BPMNModel
|documentation       | Optional Task description |
|workflow.summary    | Removes a Model from the ModelManager.|
|workflow.abstract   | Returns the BPMN Definition entity associated with a given workitem.|
|application.type    | Type attribute for a workflow instance assigned to the Task |
|application.editor  | Optional editor information for a workflow instance assigned to the Task.|
|application.icon    | Optional Icon to display the workflow status.|
|acl.owner.list      | Owner information for ACL settings.|
|acl.readaccess.list | Read-Access information for ACL settings.|
|acl.writeaccess.list| Write-Access information for ACL settings.|



## The Event Element

The 'Event Element' defines all information required to process a Workitem.  The 'Event Element' defines the process flow of a Workitem from one 'Task' to another. 

<img src="../images/modelling/bpmn_screen_05.png"/>

An 'Event Element' is assigned to a 'Task'. The ID of each 'Event' must be unique inside a collection of events assigned to the same 'Task'. 
The Event entity returned by the `ModelManager` is represendted as an [ItemCollection](itemcollection.md) and provides at least the following attributes:


 |Item                | Description 			 	                                                      |
|--------------------|-----------------------------------------------------------------------------|
|id                  | ID attribute of the corresponding BPMN element in the BPMN model. |
|type                | Type of the corresponding BPMN element: 'intermediateCatchEvent'
|name                | Name of the Event Element 
|eventID             | Integer ID to identify the Event assigned to a Task Entity in a BPMNModel
|documentation       | Optional Event description |
|workflow.public     | Optional indicates if the Event is shown in a Applicaton UI  a Model from the ModelManager.|
|workflow.public.actors   | Optional list of actors with access to this event |
|acl.owner.list      | Owner information for ACL settings.|
|acl.readaccess.list | Read-Access information for ACL settings.|
|acl.writeaccess.list| Write-Access information for ACL settings.|
 


## The BPMNModel

The  Imixs `ModelManager` operates directly on BPMNModel instances from the [Open-BPMN Meta model](https://github.com/imixs/open-bpmn/tree/master/open-bpmn.metamodel).
The `BPMNModel` instance allows full access to a BPMN 2.0 model and provides a lot of convenient methods to navigate through a BPMN model. 

Find further information at the [Open-BPMN Meta Project on Github](https://github.com/imixs/open-bpmn/tree/master/open-bpmn.metamodel).

## The OpenBPMNModelManager

The `OpenBPMNModelManager` implements the interface `org.imixs.workflow.ModelManager` and can be used in any custom Java implementation of Imixs-Workflow. The `OpenBPMNModelManager` provides additional methods to manage BPMNModel instances and helper methods to navigate throug a BPMN model as also methods to  evaluate specific model situations based on the [Imixs RuleEngine](ruleengine.md).  



### The Imixs ModelService

The Imixs-Workflow engine provides the EJB [ModelService](../engine/modelservice.html) implementing the `ModelManager` and used by the Imixs WorkflowService.
 

     
 