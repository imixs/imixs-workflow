# Quickstart

To start with **Imixs-Workflow** you first create a workflow-model. The model describes your business logic. You can change the logic later without changing one line of code.
Imixs-Workflow is based on the [BPMN 2.0 modeling standard](http://www.bpmn.org/). BPMN is useful for visualizing both - the business process and the responsibilities. 

You create your workflow model with the Eclipse based modeling tool [Imixs-BPMN](./modelling/). Let's take look at a simple example:

<img src="./images/modelling/order-01.png" />

The blue boxes symbolize **Task** elements, while the yellow symbols describe **Event** elements. The later can change the state within the process.
An event is typically triggered by a process participant within your application. The example model can be download from [Github](https://github.com/imixs/imixs-workflow/tree/master/src/site/resources/bpmn). 

Next lets see how you can integrate Imixs-Workflow in your own Java application. 

## How to Integrate Imixs-Workflow

The Imixs-Workflow engine is based on Jakarta EE and so it can be integrated easily into a business application by injection.

Let's see what this looks like in your Java code:

	@EJB
	private org.imixs.workflow.engine.WorkflowService workflowService;

	ItemCollection workitem=new ItemCollection().model("1.0.0").task(1000).event(10);
	// assign some business data...
	workitem.setItemValue("_customer","M. Melman");
	workitem.setItemValue("_ordernumber",20051234);
	// process the workitem
	workitem = workflowService.processWorkItem(workitem);


1. You inject the Workflow Engine with the annotation @EJB. 
2. Next you create a new business object and assign it to your model. 
3. You also can add your own business data. 
4. Finally you 'process' your object. 

From now on the newly created **Process Instance** is under the control of your business model. 
After you have created a new process instance you can use the _UniqueID_ to access the instance later: 
   
    String uniqueID=workitem.getUnqiueID();
    ....
    // load the instance
    ItemCollection workitem=workflowService.getWorkItem(unqiueID);
    ....

Depending on the design of your workflow model a process instance can be assigned to a team or a single process participant. E.g. the method _getWorkListByOwner_ can be used to select all process instances belonging to
a specified participant:

	List<ItemCollection> result=workflowService.getWorkListByOwner("melman", "workitem", 30, 0,null,false);  

See the documentation of the [WorkflowService](engine/workflowservice.html) for more details. 


## What's Next...

Continue reading more about:

 * [How to Model with Imixs-BPMN](./modelling/howto.html)
 * [How to Manage your Business Data](./quickstart/workitem.html)
 * [Why You Should Use Imixs-Workflow](./quickstart/why.html)
 * [What Means Human Centric Workflow?](./quickstart/human.html)
 * [Imixs-BPMN - The Modeler User Guide](./modelling/index.html)
 * [The Imixs-Worklfow Plugin API](./engine/plugins/index.html)
 * [The Imixs-Worklfow Rest API](./restapi/index.html)
 
