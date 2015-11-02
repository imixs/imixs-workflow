#The Imixs Workflow Engine
After you have defined your business model and created a workitem containing your business data you can run the Imixs Workflow engine to process the workitem. When a workitem is processed by the workflow engine the workitem becomes a process instance of the  current workflow process. From this moment the workflow engine controls the workitem until the business process is completed. 
 
<img src="../images/imixs-engine.png"/> 
 
 
##Run the workflow engine
A workitem can be processed by calling the method processWorkItem() from the WorkflowService. As the Imixs-Workflow Engine is based on Java EE you can inject the WorklowService with the @EJB annotation. This is a simple example:
 
    import org.imixs.workflow.ItemCollection;
    .....
    @EJB
    private org.imixs.workflow.jee.ejb.WorkflowService workflowService;
    ....
    // create a workitem
    ItemCollection workitem=new ItemCollection();
    workitem.replaceItemValue("name","Ralph");
    workitem.replaceItemValue("age",new Integer(40));
    // assigen a workflow model
    workitem.replaceItemValue("$processID",20);
    workitem.replaceItemValue("$activityID",20);
    // process the workitem
    workitem = workflowService.processWorkItem(workitem);
  
 
After you have processed a Workitem you can evaluate the workflow data, provided by the workflow engine: 
   
    ....
    // get the assigend workitem unique id...
    String uniqueID=workitem.getItemValueInteger("$UniqueID");
    // read the current workflow status
    String status=workitem.getItemValueString("txtWorkflowStatus");
    ....

With the uniqueID of a processed workitem you can fetch the process instance from the WorkflowService:
 
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
