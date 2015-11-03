#The Imixs-Workflow Model 

The Imixs-Workflow Model separates the process definition from the Workflow implementation. For this purpose, the interface '_org.imixs.workflow.Model_' provides methods to navigate through a  Workflow Model with its 'Process Entities' and 'Activity Entities'. A 'Process Entity' describes the state within the model definition (Task). It is typically represented as a Node inside a graphical workflow model. On the contrary, an 'Activity Entity' contains information to control the process flow (Event). 
An activity entity is typical represented as an edge inside a graphical workflow model. A workflow model always has a unique version which allows to manage different process model inside one Workflow Management System. 

<img src="../images/modelling/bpmn_screen_00.png"/>

##The Process Entity

A 'Process Entity' contains information about a process stage within the process model. It is used to unambiguously define the status of a WorkItem. In BPMN an Process Entity is modeled as a Task element. 

<img src="../images/modelling/bpmn_screen_04.png"/>

A 'Process Entity' is defined by a unique ID. A model cannot contain several 'Process Entities' having the same ID. The following attributes must be provided by each instance of a 'Process Entity': 
  
   * numProcessID  - an integer unique identifier for the 'Process Entity' inside the model   
   * txtName  - The name for the Entity   
   * txtWorkflowGroup - The name of the ProcessGroup the Entity belongs to.

##The Activity Entity
On the contrary, an 'Activity Entity' contains all information required to process a Workitem.  The 'Activity Entity' defines the process flow of a Workitem from one 'Process Entity' to another. In BPMN an Activity Entity is modeled as an Event element.

<img src="../images/modelling/bpmn_screen_05.png"/>

An 'Activity Entity' is assigned to a 'Process Entity'. The ID of each 'Activity Entity' must be unique inside a collection of Activities assigned to the same 'Process Entity'. The 'Activity Entity' must provide the following items:
 
   * numProcessID - an integer ID which associates the 'Activity Entity' to a 'Process Entity' 
   * numActivityID - an integer unique identifier for the 'Activity Entity'
   * numNextID - an Iteger ID which defines the next 'Process Entity' a workitem is assigned to after processing.
   * txtName  - The name for the Entity
 
 
## The Process Flow
When a Workitem is processed by the Imixs-Workflow engine the properties '$ProcessID' and '$ActivityID' are verified against the process model by the WorkflowKernel. Depending on the information of the assigned 'Activity Entity' the WorkflowKernel updates the status of the Workitem ('$ProcessID') after the Workitem was processed.

##The Interface
The Interface '_org.imixs.workflow.Model_' defines the following methods:

|Method              		 | Description 				 |
|----------------------------|---------------------------|
|getProcessEntity(processID,Version)| returns a process entity |
|getActivityEntity(processID,activityID,Version)| returns a activity entity assigned to a process entity| 
|getProcessEntityList(Version)| returns all process entities for a specific model version | 
|getActivityEntityList(processID,Version)| returns all activity entities assigned to a  process entity 


### Navigate the Model
You can use the Model interface to navigate through the process model. See the following example:
 
    Model model;
    //....
    // lookup a ProcessEntiy 
    processEntity=model.getProcessEntity(100,'1.1.0');
    //...
    // receive the processEntityList containing all Process Entities
    Collection<ItemCollection> col1=model.getProcessEntityList();
    // receive a collection of all Activities for a Process Entity
    Collection<ItemCollection> col2=model.getActivityEntityList(processid)


### The Imixs ModelService
The Imixs-Workflow engine provides the ModelService component which implements the Model Interface  on the JEE component stack. This Service provides a various methods to navigate and manage a Workflow model. Find more information about the Imixs-WorkflowService in the section [Model Service](../engine/modelservice.html).    
 

     
 