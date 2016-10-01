#The Imixs-Workflow Engine
The Imixs-Workflow engine provides a set of Java EE services to access the [workflow model](../quickstart/businessprocess.html) and to create and manage [process instances](../quickstart/workitem.html) of a process definition, called _workitems_. The Imixs-Workflow engine can be embedded into a Java Enterprise application or can be accessed through the [RESTfull service API](../restapi/index.html). 

<img src="../images/engine/imixs-architecture_jee.png"/>


##The Java EE Services 

The Imixs-Workflow Engine is based on the [Imixs-Workflow Core-API](../core/index.html) and implements a set of Java EE service interfaces. These services are divided into the typical building blocks of a workflow management system: 

 * The Database Layer -> [DocumentService](./documentservice.html)
 * The Workflow Kernel -> [WorkflowService](./workflowservice.html)
 * The Model -> [ModelService](./modelservice.html) 
 * The Analysis -> [ReportService](./reportservice.html)
 
### The DocumentService
The _DocumentService_ provides an interface to create, save and load data objects (_workitems_) within a database. A _workitem_ is represented by the [ItemCollection class](../core/itemcollection.html) which presents a generic value object used by all methods of the Imixs-Workflow engine. The _DocumentService_ is independent form the workflow engine and can also be used to store any kind of business data not associated with the workflow engine (e.g configuration data). 

Each data object managed by the _DocumentService_ is assigned to a access control list (ACL). The ACL protects a data object for unauthorized  access. In case the _CallerPrincipal_ has insufficient rights to access or modify a specific data object, the _DocumentService_ throws an _AccessDeniedException_. 

The _DocumentService_ creates a [Lucene Index](https://lucene.apache.org/) over all managed data objects and provides methods to query data objects by a search term. This is a power full feature to navigate easily through the workitems managed by the Imixs-Workflow engine.  

[Read more about the Imixs DocumentService](../engine/documentservice.html).
  
 
### The WorkflowService
The _WorkflowService_ provides the interface to the Imixs-Workflow kernel. This component provides methods to create, process and access workitems. A workitem managed by the _WorkflowService_ must be assigned to a valid workflow model definition managed by the _ModelService_. 
 
[Read more about the Imixs WorkflowService](../engine/workflowservice.html).
 
### The ModelService
The _ModelService_ manages the models and provides methods to store a new model definition. A model can be created with the Eclipse based modeling tool [Imixs-BPMN](../modelling/index.html). The _ModelService_ is used internally by the _WorkflowService_ but can also be used by the business application to navigate through a model definition.

[Read more about the Imixs ModelService](../engine/modelservice.html).
 
### The ReportService
The _ReportService_ component supports methods to create, find and execute business reports created with the Eclipse based [Imixs-Workflow Modeller](../modelling/index.html). A report is used to generate aggregated information from data objects managed by the _DocumentService_.  
  