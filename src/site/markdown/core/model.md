#The Imixs-Workflow Model 

The Imixs-Workflow Model separates the process definition from the Workflow implementation. For this purpose, the interface '_org.imixs.workflow.Model_' provides methods to navigate through a  Workflow Model with its different elements. A workflow model has a unique version ID which allows to manage different workflow models inside one Workflow Management System. 

A Workflow Model can be defined using the [Imixs-BPMN modeling tool](../modelling/index.html).

<img src="../images/modelling/bpmn_screen_00.png"/>

## The Task Element

A 'Task Element' describes the unambiguously status of a running process instance within the process. 

<img src="../images/modelling/bpmn_screen_04.png"/>

A 'Task' can be identified by a unique ID in each model. A model cannot contain several Task elements having the same ID. The following attributes must be at least provided by an instance of a 'Task Element': 
  
   * numProcessID  - an integer unique identifier for the 'Task' inside the model   
   * txtName  - The name for the Entity   
   * txtWorkflowGroup - The name of the WorkflowGroup the Task belongs to.

## The Event Element
On the contrary, the 'Task Element', the 'Event Element' defines all information required to process a Workitem.  The 'Event Element' defines the process flow of a Workitem from one 'Task' to another. 

<img src="../images/modelling/bpmn_screen_05.png"/>

An 'Event Element' is assigned to a 'Task'. The ID of each 'Event' must be unique inside a collection of events assigned to the same 'Task'. The following attributes must be at least provided by an instance of a 'Event Element': 
 
   * numProcessID - an integer ID which associates the 'Event' to a 'Task Element' 
   * numActivityID - an integer unique identifier for the 'Event'
   * numNextID - an Integer ID which defines the next 'Task' a workitem is assigned to after processing.
   * txtName  - The name for the Entity
 
 
### The Process Flow
When a Workitem is processed by the Imixs-Workflow engine, the properties '$modelVersion', '$TaskID' and '$EventID' are verified against the current workflow model. Depending on the information of the assigned 'Event' the _WorkflowKernel_ updates the status of the Workitem ('$taskID') after a Workitem was processed successful.

##The Model Interface
The Interface '_org.imixs.workflow.Model_' defines the following methods:

|Method              		 | Description 				 |
|----------------------------|---------------------------|
|getVersion()| returns the workflow model version |
|getDefinition()| returns a ItemCollection holding general model information (e.g. the plugin list) |
|getTask(taskId)| returns a task by its id |
|getEvent(taskId,eventId)| returns an event by its id |
|getGroups()| returns a list of all workflow groups defined in the model |
|findAllTasks()| returns a list of all task elements defined in the model |
|findAllEventsByTask(taskId)| returns a list of all event elements assigned to a task|
|findAllTasksByGroup(workflowgroup)| returns a list of all task elements assigned to a workflow group|


## The ModelManager 
The interface ModelManager stores instances of a Model. A Model instance is uniquely identified by the ModelVersion. The IModelManager is used by the [WorkflowKernel](workflowkernel.html) to manage the process-model of a workitem.

The Interface '_org.imixs.workflow.ModelManager_' defines the following methods:

|Method              		 | Description 				 |
|----------------------------|---------------------------|
|getModel(version)           | Returns a Model by version. The method throws a ModelException in case  the model version did not exits.|
|addModel(model  )           | Adds a new Model to the ModelManager.|
|removeModel(version)        | Removes a Model from the ModelManager.|
|getModelByWorkitem(workitem)| Returns a Model matching a given workitem. The method throws a ModelException in case the model version did not exits..|


### The Imixs ModelService
The Imixs-Workflow engine provides the [ModelService](../engine/modelservice.html) which provides additional methods to managed different models in one application.    
 

     
 