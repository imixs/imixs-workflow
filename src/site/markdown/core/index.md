# The Imixs-Workflow Core-API 
 
The following section gives you a brief overview about the **Imixs-Workflow Core-API**. The API is independent from a specific platform or technology and can be used in all kinds of Java applications. For example this API is the foundation of the [Imixs-Workflow engine](../engine/index.html) which is build on the Java Enterprise Stack. 
But the API can also be used to implement a workflow engine on a different technology stack (e.g. Spring Boot). 

The following section describes the basic concepts of the _Imixs-Workflow Core-API_:
 
	
 
### The ItemCollection

All data objects in the Imixs-Workflow API are encapsulated in a generic value object _org.imixs.workflow.ItemCollection_. 
The ItemCollection can be treated as a kind of document-based data structure. A ItemCollection consists of an Item Set (attributes). Each Item is defined by a key (item name) and a value list (item value).  

See the section [ItemCollection](./itemcollection.html) for further information and examples.
  
  
### The Model

The interface _org.imixs.workflow.Model_ provides methods to access and navigate through the model. 
The Model is used by the WorkflowKernel to compute the next Task or Event elements within the processing life-cycle. The Imixs-Workflow engine implements this interface based on the BPMN 2.0 standard. 


### The WorkflowKernel

The WorkflowKernel implements the core functionality of the Imixs-Workflow API. The WorkflowKernel can start a new process instance and process a running instance based on the definition of the process model. The WorkflowKernel validates the processing phase and coordinates the interaction between the model and the plugins. 

See the section [WorkflowKernel](./workflowkernel.html) for more information.
 

### The Plugin API

The Imixs Plugin-API is the extension concept of the Imixs-Workflow Engine. The business logic of an application as also technical interfaces can be implemented by plugins which can easily be activated through the workflow model. 

See the section [Plugin-API](./plugin-api.html) for more information. The Imixs-Workflow Engine defines a set of [standard plugins](../engine/plugins/index.html) to be used in a workflow model. 



### The Workflow Manager

The WorkflowManager interface defines the uppermost layer of each workflow system based on the _Imixs-Workflow API_. The interface _org.imixs.workflow.WorkflowManager_ provides basic methods to create and control a process instance.

If you develop your own workflow engine you need to implement this interface. The Imixs WorkflowKernel expects an instance of WorkflowManager when processing a workflow instance. 	