# The Imixs-Workflow Engine

The Imixs-Workflow engine consists of different service components. Each component maps a specific functionality into your workflow application: 

 * The [WorkflowService](./workflowservice.html) → the core component to create and update a process instance
 * The [ModelService](./modelservice.html) → the management component for BPMN models. 
 * The [DocumentService](./documentservice.html) → the data access layer to store workflow related data
 * The [ReportService](./reportservice.html) → a service component to create data reports

All services can either be injected into a Java Enterprise application or can be accessed through its [RESTfull service API](../restapi/index.html). 

Further more, all services are subject to the [Imixs-Workflow Security Model](./acl.html). In this way only an authenticated access to these service components is allowed. This concept ensures the protection of your business data. 

 
### The WorkflowService
The _WorkflowService_ is the core service to create, update and read a process instance. To create a process instance a workitem is assigned to a BPMN 2.0  model definition managed by the _ModelService_. 

	@EJB
	WorkflowService workflowService;
	ItemCollection workitem=new ItemCollection().model("1.0.0").task(100).event(10);
	workitem=workflowService.processWorkItem(workitem);

 
Read more about in the section [Imixs WorkflowService](../engine/workflowservice.html).
 
### The DocumentService
The _DocumentService_ is the general persistence layer of the Imixs-Workflow engine and provides an interface to store, load and query data objects (_Documents_) within a database. 
The _DocumentService_ is independent from the workflow engine and can not only be used to persist a process instance (_workitem_), but also any other kind of business data, not necessarily associated with the workflow engine (e.g configuration data). 

	@EJB
	DocumentService documentService;
	ItemCollection myDocument=new ItemCollection;
	myDocument.setItemValue("type","product");
	myDocument.setItemValue("name","coffee");
	myDocument=documentService.save(myDocument);
	  

The _DocumentService_ provides also a [Full-Text-Search](./luceneservice.html). In this way documents can be accessed through a search query:

	List<ItemCollection> result=documentService.find("(type:'workitem')(imixs*)");


Read more about in the section [DocumentService](../engine/documentservice.html).
  

 
### The ModelService
The _ModelService_ provides methods to manage BPMN model definitions. A model can be created with the Eclipse based modeling tool [Imixs-BPMN](../modelling/index.html). 

	@EJB
	ModelService modelService;
	InputStream inputStream = new FileInputStream(new File("ticket.bpmn"));
	ticketModel = BPMNParser.parseModel(inputStream, "UTF-8");
	modelService.addModel(model);
	

The _ModelService_ is used internally by the _WorkflowService_ but can also be used by your application to navigate through a model definition.

	@EJB
	ModelService modelService;
	Model ticketModel = modelService.getModel("ticket-1.0.0");
	List<ItemCollection> tasks = modelService.findAllTasks();

Read more about in the section [ModelService](../engine/modelservice.html).
 
### The ReportService
The _ReportService_ component supports methods to create, find and execute business reports created with the Eclipse based [Imixs-Workflow Modeller](../modelling/index.html). A report is used to generate aggregated information from data objects managed by the _DocumentService_.  

