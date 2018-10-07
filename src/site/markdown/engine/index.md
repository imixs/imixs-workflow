#The Imixs-Workflow Engine
The Imixs-Workflow engine provides a set of Java EE services to access the [workflow model](../quickstart/businessprocess.html) and to create and manage [process instances](../quickstart/workitem.html) of a process definition, called _workitems_. The Imixs-Workflow engine can be embedded into a Java Enterprise application or can be accessed through the [RESTfull service API](../restapi/index.html). 

<img src="../images/engine/imixs-architecture_jee.png"/>


##The Imixs-Workflow Services 

The Imixs-Workflow Engine is based on the [Imixs-Workflow Core-API](../core/index.html) and implements a set of Java enterprise service interfaces. These services are divided into the typical building blocks of a workflow management system: 

 * The Database Layer -> [DocumentService](./documentservice.html)
 * The Workflow Kernel -> [WorkflowService](./workflowservice.html)
 * The Model -> [ModelService](./modelservice.html) 
 * The Analysis -> [ReportService](./reportservice.html)
 
### The DocumentService
The _DocumentService_ is the general persistence layer of the Imixs-Workflow engine and provides an interface to store, load and query data objects (_Documents_) within a database. 
The _DocumentService_ is independent from the workflow engine and can not only be used to persist a process instance (_workitem_), but also any other kind of business data, not necessarily associated with the workflow engine (e.g configuration data). Each document managed by the _DocumentService_ is assigned to a access control list (ACL). The ACL protects the document from unauthorized  access. 

The _DocumentService_ creates a [Lucene Search Index](https://lucene.apache.org/) over all documents and provides methods to query documents by a search term. This is a power full feature to navigate easily through the workitems managed by the Imixs-Workflow engine.  

[Read more about the Imixs DocumentService](../engine/documentservice.html).
  
 
### The WorkflowService
The _WorkflowService_ provides the interface to the Imixs-Workflow kernel. This component provides methods to create, process and access workitems. A workitem managed by the _WorkflowService_ must be assigned to a valid workflow model definition managed by the _ModelService_. 
 
[Read more about the Imixs WorkflowService](../engine/workflowservice.html).
 
### The ModelService
The _ModelService_ manages the models and provides methods to store a new model definition. A model can be created with the Eclipse based modeling tool [Imixs-BPMN](../modelling/index.html). The _ModelService_ is used internally by the _WorkflowService_ but can also be used by the business application to navigate through a model definition.

[Read more about the Imixs ModelService](../engine/modelservice.html).
 
### The ReportService
The _ReportService_ component supports methods to create, find and execute business reports created with the Eclipse based [Imixs-Workflow Modeller](../modelling/index.html). A report is used to generate aggregated information from data objects managed by the _DocumentService_.  
  