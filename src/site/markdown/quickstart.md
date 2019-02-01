# Quickstart

To start with **Imixs-Workflow** you first create a workflow-model. The model describes your business logic. You can change the logic later without changing one line of code.
Imixs-Workflow is based on the [BPMN 2.0 modeling standard](http://www.bpmn.org/). BPMN is useful for visualizing both - the business process and the responsibilities. 

You create your workflow model with the Eclipse based modeling tool [Imixs-BPMN](./modelling/). Let's take look at a simple example:

<img src="./images/modelling/order-01.png" />

The blue boxes symbolize a **Task**, while the yellow symbols describe an **Event** that changes the state within the process.
An event is typically triggered by a process participant within your application. The example model can be download from [Github](https://github.com/imixs/imixs-workflow/tree/master/src/site/resources/bpmn). 

Next lets see how you can integrate Imixs-Workflow in your own application. 

## How to Integrate Imixs-Workflow

The Imixs-Workflow engine can be integrated in different ways. You can run Imixs-Worklfow as a [Microservice](https://github.com/imixs/imixs-microservice) and interact with the engine through the [Imixs-Rest API](/restapi/index.html).
Or you can build a Java application and embed the Workflow engine as a library. You can find an example on [Github](https://github.com/imixs/imixs-jsf-example).

Let's see what the last variant looks like in your Java code:

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

From now on the newly created process instance is under the control of your business model. 
After you have created a new process instance you can use the _UniqueID_ to access the instance later: 
   
    String uniqueID=workitem.getUnqiueID();
    ....
    // load the instance
    ItemCollection workitem=workflowService.getWorkItem(unqiueID);
    ....



# What's next?

Additional information about how to use the Imixs-Workflow engine can be found here:

 * [How to Model with Imixs-BPMN](./modelling/howto.html)
 * [How to Manage Business Data](./quickstart/workitem.html)
 * [Why Should I Use Imixs-Workflow?](./quickstart/why.html)
 * [The Imixs-BPMN Modeler - User Guide](./modelling/index.html)
 * [The Plugin API](./engine/plugins/index.html)
 * [The Rest API](./restapi/index.html)
 
If you have any questions about how Imixs-Worklfow works and how you can use it in your own project, ask your question on the [GitHub Issue Tracker](https://github.com/imixs/imixs-workflow/issues). If you have general questions and your are not sure where to put it, use the [discussion forum on gitter](https://gitter.im/imixs/imixs-workflow). 