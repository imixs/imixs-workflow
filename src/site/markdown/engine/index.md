#The Imixs-Workflow Engine
The Imixs-Workflow Engine provides JEE services to process and control business data based on  a workflow model. This simplifies the way developing business applications in a very fast and flexible way. There is no need to reinvent the wheel for managing the state of a business object or to control the flow of business data between several actors. The Imixs-Workflow Engine fulfills the requirements to a scalable, transactional, robust and simple deployable workflow management component.
 
<img src="../images/engine/imixs-architecture_jee.png"/>


##The Components 

The Imixs-Workflow Engine is based on the [Imixs-Workflow Core-API](../../core/index.html) and provides a set of service components which can be used to implement enterprise business applications and workflow management solutions.  The components are divided by the different building blocks of a workflow management system. 
 
 
###The EntityService
The EntityService component is used to save and load a process instance (Workitem) into a  Database. A Workitem is represented by the  {{{/api/itemcollection.html}Imixs temCollection}} which is a   value object containing all information about a process instance. For example all workitems processed by the Imixs-Workflow Engine are stored by the EntityService into the database. But also Model entities or other business objects can be managed by the EntityService.   The EnityService provides an access control list (ACL) for each entity to avoid unauthorized  access to the values of an ItemCollection. When the CallerPrincipal is not allowed to save or read a specific ItemCollection from the database the EntityService throws an AccessDeniedException.  The EntityService can be used to save business objects into a database with individual read- or   writeAccess restrictions. The EntityService is the core service component used by the Imixs JEE Workflow.
  
  {{{./services/entityservice.html}Read more about the Imixs EntityService}}.
  
 
### The WorkflowService
The WorkflowService is the JEE implementation of the Imixs-Workflow Manager. This component supports general methods to create, process and access workitems based on a workflow model. The component is easy to use and all the business logic can be controlled fully by a workflow model. The WorkflowService component provides also a set of methods to find and fetch collections of workitems processed by the WorkflowManager. The WorkflowService is the main component used by an application to manage business logic.  
  
 {{{./services/workflowservice.html}Read more about the Imixs WorkflowService}}.
 
###The ModelService
The ModelService component is the JEE Implementation of the Model interface from the Imixs-Workflow API  used by the WorkflowService.
 The Service extends the standard interface with useful methods to load, store and navigate through  workflow models. The  ModelService component can be used by an application to provide the user with information about the workflow model for a specific business process. Models can be managed by the {{{http://www.imixs.org/modeler/}Eclipse based Imixs-Workflow Modeller}}. 
 
 {{{./services/modelservice.html}Read more about the Imixs ModelService}}.
 
###The ReportService
The ReportService component supports methods to create, find and execute Imixs-Workflow Reports. A report can be used to generate aggregated information from workitems and processing data managed by the WorkflowService. Reports are a very flexible technology and provide an easy to use interface based on the {{{http://www.imixs.org/xml/}Imixs REST Service API}}.  Reports can be managed with the {{{http://www.imixs.org/modeler/}Eclipse based Imixs-Workflow Modeller}}.
  