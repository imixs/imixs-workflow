# The Imixs-Workflow Engine

The Imixs-Workflow engine is the core service inside the Imixs-Workflow technology. The Imixs-Workflow engine is an transactional robust service component to process business data objects based on a BPMN 2.0 model. 

After you have defined your business model with [Imixs-BPMN](https://www.imixs.org/doc/modelling/index.html) you can create a new data object - called workitem - to process it with the Imixs-Workflow engine.
After processing a workitem by the workflow engine, the workitem becomes a **process instance** of the current workflow.
From this moment the workflow engine controls the workitem until the business process is completed. 
 
<img src="../images/imixs-engine.png"/> 
 
 
## Create a new Process Instance

To start a new process instance, you must first create a new workitem and assign it to an existing model.
Next you can add any business data to this data object and process it by calling the method _processWorkItem()_. The Imixs-Workflow Engine is an EJB service component which you can inject into your code. See the following example code:
 
    import org.imixs.workflow.ItemCollection;
    .....
    @EJB
    private org.imixs.workflow.engine.WorkflowService workflowService;
    ....
    // create a new workitem assigned to a workflow model
    ItemCollection workitem=new ItemCollection().model("1.0.0").task(100).event(10);
    // assign business data
    workitem.setItemValue("_customer","M. Melman");
    workitem.setItemValue("_ordernumber",20051234);
    // process the workitem
    workitem = workflowService.processWorkItem(workitem);
  
 
After you have processed a Workitem you can evaluate the [workflow data](workitem.html), provided by the workflow engine: 
   
    ....
    // get the assigend workitem unique id...
    String uniqueID=workitem.getUnqiueID();
    // read the current workflow status
    String status=workitem.getItemValueString(WorkflowKernel.WORKFLOWSTATUS);
    ....

With the uniqueID which is automatically assigend to a workitem, you can fetch the process instance later from by WorkflowService:
 
    ....
    // load a process instance by the unique id...
    workitem=workflowService.getWorkItem(uniqueid);
    ....
 
You can also request different lists of workitems by calling the corresponding get method: 
  
    ....
    // get all workitems for the current user...
    List<ItemCollection> tasklist = workflowService.getWorkListByOwner(null, null, 30, 0, null, false);
    ....


The Imixs WorkflowServce provides various methods to manage workitems processed by the workflow engine.  See the [workflow engine section](../engine/index.html) for more details.
