#The Imixs-Workflow Engine
The Imixs-Workflow Engine provides different Java EE services to access the model information and to control and process the data of a running workflow instance (_workitem_). The service EJBs can be used directly form a Java Enterprise application or can be accessed through the [RESTfull service API](../restapi/index.html). 

<img src="../images/engine/imixs-architecture_jee.png"/>


##The Java EE Services 

The Imixs-Workflow Engine is based on the [Imixs-Workflow Core-API](../core/index.html) and provides a set of services which can be used to access the Imixs-Workflow engine. The components are divided by the different building blocks of a workflow management system. 
 

 * The Database Layer -> EntityService
 * The Worklfow Engine -> WorkflowService
 * The Model -> ModelService 
 * The Analysis -> ReportService
 
### The EntityService
The _EntityService_ component provides an interface to create, save and load data objects (_workitems_) which a  database. A _workitem_ is represented by the [ItemCollection](../core/itemcollection.html) which is a value object class. An _ItemCollection_ can contain any kind of information used by the workflow engine and the business application. The _EntityService_ is independent form the workflow engine and can also be used to store data objects which are not associated with a workflow model (e.g configuration data). 

Each data object managed by the _EntityService_ is assigned to a access control list (ACL). The ACL protects a data object for unauthorized  access. In case the _CallerPrincipal_ has insufficient rights to save or read a specific data object, the _EntityService_ throws an _AccessDeniedException_. 

[Read more about the Imixs EntityService](../engine/entityservice.html).
  
 
### The WorkflowService
The _WorkflowService_ component provides the interface to the Imixs-Workflow kernel. This component supports methods to create, process and access workitems. A workitem managed by the _WorkflowService_ need to be assigned to a valid workflow model definition. 
 
[Read more about the Imixs WorkflowService](../engine/workflowservice.html).
 
### The ModelService
The _ModelService_ component provides methods to write and read model information created with the Eclipse based modeling tool [Imixs-BPMN](../modelling/index.html). The _ModelService_ can also be used by the business application to read the model information.

[Read more about the Imixs ModelService](../engine/modelservice.html).
 
### The ReportService
The _ReportService_ component supports methods to create, find and execute business reports created with the Eclipse based [Imixs-Workflow Modeller](../modelling/index.html). A report is used to generate aggregated information from workitems and processing data managed by the _WorkflowService_. Reports can also be executed by the Imixs-Workflow engine. 
  