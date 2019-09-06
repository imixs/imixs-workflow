# The WorkflowService 
The WorkflowService is the Java EE implementation of the [WorkflowManager interface](../core/workflowmanager.html) from the core API. This service component allows to process, update and lookup workItems in the Imixs-Workflow engine. 

## How to Process a Workitem
Before a workitem can be processed by the WorkflowService, a process model need to be defined and deployed together with the workflow engine. See the section [Imixs-BPMN Modeler](../modelling/index.html) for details about  how to create a model and upload it into the workflow server. The following example shows how a workitem can be processed using the WorkflowService component. A workitem must provide at least the following properties:

   * $ModelVersion
   * $TaskID 
   * $EventID 
   
These properties are defining the workflow activity which should be processed by the workflowManager.

	  @EJB
	  org.imixs.workflow.jee.ejb.WorkflowService workflowService;
	  //...
	  // create an empty workitem assigend to a model
	  ItemCollection workitem=new ItemCollection().model("1.0.0").task(100).event(10);
	  // assign business data
	  workitem.setItemValue("_name", "M. Alex");
	  workitem.setItemValue("_Titel", "My first workflow example");
			
	  // process the workitem
	  workitem=workflowService.processWorkItem(workitem);



The model version can also be specified as a regex. 

	  // take best match for model version 1.x
	  ItemCollection workitem=new ItemCollection().model("(^1.)").task(100).event(10);

Another alternative to assign a new workitem with a model version is by specifying the $workflowgroup. 


	  // create an empty workitem assigend to a workflow group
	  ItemCollection workitem=new ItemCollection().task(100).event(10);
	  // assign group
	  workitem.setItemValue(WorkflowKernel.WORKFLOWGROUP, "Invoice");
	  // assign the workitem to the latest version matching the workfow group 'invoice'
	  workitem=workflowService.processWorkItem(workitem);


After a new workitem is process the first time, it is under the control of the _WorkflowService_.


## Worklist Methods

To get the current list of all workitems, the _WorkflowService_ provides a set of methods. These methods provide different ways to read a worklist by categories. The _WorkflowService_ returns only workitems in a result set if the user has read access. If a workitem is not accessible for the user, this workitem will not be included in the result-set.  All result-sets can be ordered by modified or creation date. 


### getWorkListByCreator

The method getWorkListByCreator can be called to read the list of all workitems created by a specific user:
  
	  @EJB
	  org.imixs.workflow.jee.ejb.WorkflowService workflowService;
	  // get the first 10 workitem for the current user
	  Collection<ItemCollection> worklist=workflowService.getWorkListByCreator(null,10,0);
	  // get list for  a named user
	  Collection<ItemCollection> worklist=workflowService.getWorkListByCreator('manfred',10,0);
  
The WorkflowManager provides a paging mechanism to browse through long result-sets. The following example
shows how to get 5 workitems from the tenth page
  
    Collection<ItemCollection> worklist=workflowService.getWorkListByCreator(null,5,10);



### getWorkList
The method returns a collection of workitems for the current user. A Workitem belongs to a user or role if the  user has at least read write access to this workitem. 

    Collection<ItemCollection> list=workflowService.getWorkList();
    //...

You can specify the type, the start position, the count and sort order of workitems returned 
by this method. The type of a workitem is defined by the workitem property 'type' which can be set before a workitem is processed.

	  String type="workitem";
	  Collection<ItemCollection> list=workflowService.getWorkList(0,-1,
	     type,WorkflowService.SORT_ORDER_CREATED_DESC);
	  //...

### getWorkListByAuthor

The method returns a collection of workitems belonging to a specified user. This filter can be set to 
 a username or a user role defined by the application. A Workitem belgons to a user or role if the  user has write access to this workitem. So the method returns workitems which can be 
 processed by the user.  

	  String type="workitem";
	  String user="manfred"
	  Collection<ItemCollection> list=workflowService.getWorkList(user,0,-1,
	     type,WorkflowService.SORT_ORDER_CREATED_DESC);
	  //...


### getWorkListByGroup
The method returns a collection of workitems belonging to a specified workflow group.  The workflow group is defined by the workflow model and includes all process entities defined by 
 a business process 

	  String type="workitem";
	  String group="Ticketservice";
	  Collection<ItemCollection> list=workflowService.getWorkListByGroup(group,0,-1,
	     type,WorkflowService.SORT_ORDER_CREATED_DESC);
	  //...


### getWorkListByProcessID

The method returns a collection of workitems belonging to a specified $processID defined by the workflow model.

	  String type="workitem";
	  Collection<ItemCollection> list=workflowService.getWorkListByProcessID(2100,0,-1,
	     type,WorkflowService.SORT_ORDER_CREATED_DESC);
	  //...


### getWorkListByOwner
The method returns a collection of workitems containing a '$owner' item belonging to a specified username.  The '$owner' item is typical controlled by the OwnerPlugin using the Imixs Workflow Modeler

	  String type="workitem";
	  String user="Manfred"
	  Collection<ItemCollection> list=workflowService.getWorkListByOwner(user,0,-1,
	     type,WorkflowService.SORT_ORDER_CREATED_DESC);
	  //...
  
###getWorkListByWriteAccess
The method returns a collection of workitems where the current user has at least writeAccess. This means the either the  username or one of the user roles is contained in the $writeaccess property of each workitem returned by the method.
 
	  String type="workitem";
	  Collection<ItemCollection> list=workflowService.getWorkListByWriteAccess(0,-1,
	     type,WorkflowService.SORT_ORDER_CREATED_DESC);
	  //...
  
###getWorkListByRef
The method returns a collection of workitems belonging to a specified workitem identified by the attribute $UniqueIDRef. 

	  String type="workitem";
	  Collection<ItemCollection> list=workflowService.getWorkListByRef(refID,0,-1,
	     type,WorkflowService.SORT_ORDER_CREATED_DESC);
	  //...
  
## Model Version Management 
Each time a running process instance is updated, the WorkflowService compares  the internal model version with the model versions provided by the model repository.  In case the current model version is no longer available the WorkflowService  automatically upgrades an active process instance to the latest version in the   repository. Therefore the engine verifies the task ID (numprocessid) and the process name   (txtworkflowgroup) with the corresponding models. This mechanism allows to upgrade  process instances at run time to a newer version. 
  
It is also possible to handle different versions of a model at the same time.   In this case each process instance is processed by the model version from which  it was started.
  
 
   
## CDI Events

The WorkflowService EJB provides an Observer Pattern based on CDI Events. The events are fired when a workitem is processed.
The Event is defined by the class:

    org.imixs.workflow.engine.ProcessingEvent

The class _ProcessingEvent_ defines the following event types:

 * **BEFORE\_PROCESS** - is send immediately before a workitem will be processed 
 * **AFTER\_PROCESS** - is send immediately after a workitem was processed but before the document is [saved](https://imixs.org/doc/engine/documentservice.html#The_CDI_DocumentEvent).

This event can be consumed by another Session Bean or managed bean implementing the @Observes annotation: 

	@Stateless
	public class WorkflowServiceListener {
	    public void onEvent(@Observes ProcessingEvent processingEvent){
	        ItemCollection workitem=processingEvent.getDocument();
	        System.out.println("Received ProcessingEvent Type = " + processingEvent.getType());
    	}
	}
 
## Evaluate the Next Task Element

The _WorkflowService_ provides the method 'evalNextTask' to evaluate the next BPMN task element based on a Event element. This method can be called by Plugins to get the outcome of the current processing step. If the event did not point to a new task, the current task will be returned.
The method supports 'conditional-events' as well as 'split-events'.  A conditional-event contains the attribute 'keyExclusiveConditions' defining conditional targets (tasks) or adds conditional follow up events
 A split-event contains the attribute 'keySplitConditions' defining the target for the current master version (condition evaluates to 'true'). See also the section [How to Model...](../modelling/howto.html)
 
 
	// get next process entity
	nextTask = workflowService.evalNextTask(adocumentContext, adocumentActivity);
