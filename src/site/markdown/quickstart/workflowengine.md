# The Imixs Workflow Engine
After you have defined your business model with [Imixs-BPMN](https://www.imixs.org/doc/modelling/index.html) you can instance a new 
workitem and run the Imixs Workflow engine to process the workitem. When a workitem is processed by the workflow engine the workitem becomes a **process instance** of the  current workflow process. From this moment the workflow engine controls the workitem until the business process is completed. 
 
<img src="../images/imixs-engine.png"/> 
 
 
## Create a new Process Instance
To create a new process instance first the workitem must be assigned with a model. Next you can add your business data 
and process the workitem by calling the method processWorkItem() from the WorkflowService. The Imixs-Workflow Engine is based on Java EE, so you can inject the WorklowService into your code. See the following example code:
 
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
  
 
After you have processed a Workitem you can evaluate the workflow data, provided by the workflow engine: 
   
    ....
    // get the assigend workitem unique id...
    String uniqueID=workitem.getUnqiueID();
    // read the current workflow status
    String status=workitem.getItemValueString(WorkflowKernel.WORKFLOWSTATUS);
    ....

With the uniqueID of a workitem you can fetch the process instance later from the WorkflowService:
 
    ....
    // load a process instance by the unique id...
    workitem=workflowService.getWorkItem(uniqueid);
    ....
 
Also you can fetch a list of all workitems assigned to the current user. This list is typical called the 'task list'
  
    ....
    // get all workitems for the current user...
    Collection<ItemCollection> tasklist = workflowService.getWorkList(null, 0,
				-1, 0);
    ....

The Imixs WorkflowServce provides a lot of methods to manage workitems processed by the workflow engine.  See the [workflow engine section](../engine/index.html) for more details.
