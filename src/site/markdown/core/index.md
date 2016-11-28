#The Imixs-Workflow Core-API 
 
The Imixs-Workflow Core-API provides a platform independent open source workflow API. 
This API is the basic library for all other components provided by the Imixs-Workflow project. The Imixs-Workflow Core-API defines a set of java classes and interfaces to develop a workflow management solution. The Core-API is used by the Imixs-Workflow Engine and also the Imixs-Workflow REST API. The following section gives a brief overview about the architecture and general concepts.
 
##The Architecture

The Imixs-Workflow Core-API consists of a set of interfaces and classes providing the general functionality of a workflow management solution. The following section describes these components and their basic concepts. The Imixs-Workflow Core-API is independent from a specific platform or technology and can be used in all kinds of java applications. 
 
<img src="../images/api-architecture.gif"/>

### The Workflow Manager
The WorkflowManager is the uppermost layer of each workflow system based on the Imixs-Workflow API. The interface _org.imixs.workflow.WorkflowManager_ provides basic methods to create and control a process instance.
 
 
### The Model
A process model describes the process flow of the business process. The interface _org.imixs.workflow.Model_ provides methods to access a process model and navigate through the model. Each model consists of a set of process entities (Tasks) and activity entities (Events). A process entity defines the status of a process instance. In a graphical workflow model a process entity is typical represented by a node. An activity entity defines an action  which can be performed on a workitem and also the transition to the new status. Each process entity and activity entity stores process information as also processing introductions.  There are different ways a Model can be implemented by a Workflow Management System. The [Imixs-BPMN Modeler](./modelling/index.html) provides an eclipse based graphical editor to define workflow models based on the BPMN 2.0 specification.  


### The Plug-ins
The Imixs-Workflow Core API provides a Plug-In concept to provide components implementing a concrete behavior inside a workflow management system. A Plug-in is called by the WorkflowManager when a process instance is processed. Plug-ins can implement application specific functionality and business logic. Plug-ins are reusable components in the Workflow Management System and typical used by the application developer to design the  behavior of a business application. See the section [Plug-Ins](./plugin-api.html) for more information.

### The WorkflowKernel
The WorkflowKernel implements the core functionality of the Imixs-Workflow API. The WorkflowKernel can start a new process instance and process a running instance based on the definition of the process model. The WorkflowKernel validates the processing phase and coordinates the interaction between the model and the plug-ins. See the section [WorkflowKernel](./workflowkernel.html) for more information.
 
### The ItemCollection
All data objects in the Imixs-Workflow API are encapsulated in a generic value object called 'ItemCollection'.  The ItemColleciton is a very flexible kind of document oriented data structure which can consist of a variable number of attributes (Items). Each Item in an ItemCollection is defined by the Item-Name and a Item-Value. The value of an Item can be any java based data object which is serializeable. See the section [ItemCollection](./itemcollection.html) for further information and examples.
  