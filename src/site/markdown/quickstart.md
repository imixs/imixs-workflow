# Quickstart

In the following seciton you can see how Imixs-Workflow works and why you should use it for your next business application.


Imixs-Workflow is an **open source workflow engine** for a human-centric business process management (BPM). Human-centric BPM means to support human skills and activities in a task oriented and event driven way. To start with Imixs-Workflow you first need a Model. A model describes how your business application works.
Imixs-Workflow is based on the [BPMN 2.0 modeling standard](http://www.bpmn.org/) and a model can be created with the modeling tool [Imixs-BPMN](./modelling/). 
 
Let's look at a simple business process that models an ordering process:


<img src="./images/modelling/order-01.png" />


This ordering process has several tasks and events to get from one state to another. You can download the model from [Github](https://github.com/imixs/imixs-workflow/tree/master/src/site/resources/bpmn). 

Now lets see how you can use this model within your own application. 

## How to use it

The Imixs-Workflow engine can be run in different ways. You can run it as a [Imixs-Microservice project](https://github.com/imixs/imixs-microservice) and interact with the engine via the Imixs-Rest API. You can build a web application like the [MVC Example](https://github.com/imixs/imixs-mvc-example). Or you can use the engine in the embedded mode within your Jakarta EE application. In the following we use the last variant to keep the code examples small.

To start a new order process with the embedded mode all you have to do is to create a business object and assign it to the model:


	import org.imixs.workflow.ItemCollection;
	.....
	@EJB
	private org.imixs.workflow.engine.WorkflowService workflowService;
	....
	// create a new workitem assigned to a workflow model
	ItemCollection workitem=new ItemCollection().model("1.0.0").task(100).event(10);
	// assign business data
	workitem.replaceItemValue("_customer","M. Melman");
	workitem.replaceItemValue("_ordernumber",20051234);
	// process the workitem
	workitem = workflowService.processWorkItem(workitem);


From now on the order process instance is fully under the controll of your business model. 

## Why should we use it?

So the first question so far is - why should you use a workflow engine? 
Surely you are also able to implement an order management with a database and the corresponding CRUD operations. But the reason why you should use a workflow engine comes from a different direction. The workflow engine generates a lot of useful business information behind the scene. 

The process instance returned by our method call 'processWorkItem()' contains several usefull new items. For example to get the order status based on our workflow model we call:

	String status=workitem.getItemValueString(WorkflowKernel.WORKFLOWSTATUS);

also the so called workflow group, in our example 'Order' can be asked: 
	

	String group=workitem.getItemValueString(WorkflowKernel.WORKFLOWGROUP);
	
Beside this general status information we also get information about how has crated the process instance and how was the last editor: 


	String creator=workitem.getItemValueString(WorkflowKernel.CREATOR);
	String editor=workitem.getItemValueString(WorkflowKernel.EDITOR);

And of course you can also check the time points: 

	Date created==workitem.getItemValueDate(WorkflowKernel.CREATED);
	Date modified==workitem.getItemValueDate(WorkflowKernel.MODIFIED);
	
But the more interesting part is who is responsible for the ordering process? You can ask this by checking the owner list of the process instance:

	List<String> owners=workitem.getItemValue("namowner");

The Imixs-Workflow engine in addtion adds a custom [Access Controll List (ACL)](https://www.imixs.org/doc/engine/acl.html) to each process instance. This ensures that only authorized persons can change the process instance. 	


	List<String> authors=workitem.getItemValue(WorkflowKernel.WRITEACCESS);
	List<String> readers=workitem.getItemValue(WorkflowKernel.READACCESS);

A shortcut to ask if the current workitem is editable by the current user is:

	boolean editable=workitem.getItemValueBoolean(WorkflowKernel.ISAUTHOR);	

The owners and ACL settings are part of the modeling process and can be set individually for each task or event:

<img src="./images/bpmn-example02.png" width="500px" />


## Model Driven Business Logic

In addition to all these informations, we can also ask which events are allowed in a specific state of our ordering process. 
This allows us to make sure that our order process takes the course previously defined in the model. 

   
	List<ItemCollection> events=workflowService.getEvents(workitem);

The list of events also holds information about the model (e.g. the name or the description of an event). 
If we have an event, we can process the workitem and transfer the order into a new state:

	// process the workitem with a new event
	workitem = workflowService.processWorkItem(workitem, event);




## The processing log 

During the processing the Imixs-Workflow engine not only add business logic. The Engine also logs all processing steps so that a contiguous log is created. This log can be accessed by process instance and can be displayed in an application:

<img src="./images/modelling/order-02.png" width="500px" />



Additional information about how to use the Imixs-Workflow engine can be found here:

 * [How to manage business data](./quickstart/workitem.html)
 * [The Business Process](./quickstart/businessprocess.html)
 * [The Imixs Workflow Engine](./quickstart/workflowengine.html)
 * [The Imixs-BPMN Modeler - User Guide](./modelling/index.html)
 
 