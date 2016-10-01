# The WorkflowService 
The WorkflowService is the Java EE implementation of the [WorkflowManager interface](../core/workflowmanager.html) of the core API. This service component allows to process, update and lookup workItems in the Imixs-Workflow engine. 

Before a workitem can be processed by the WorkflowService a process model need to be defined and deployed into the workflow engine. See the section [Imixs-BPMN Modeler](../modelling/index.html) for details about  how to create a model and upload it into the workflow server. The following example shows how a workitem can be processed using the WorkflowService component. A workitem must provide at least the following properties:

   * $ModelVersion
   * $ProcessID 
   * $ActivityID 
   
These properties are defining the workflow activity which should be processed by the workflowManager.

	  @EJB
	  org.imixs.workflow.jee.ejb.WorkflowService workflowService;
	  //...
	  // create an empty workitem
	  ItemCollection workitem=new ItemCollection();
	  workitem.replaceItemValue("type", "workitem");
	  workitem.replaceItemValue("name", "Anna");
	  workitem.replaceItemValue("txtTitel", "My first workflow example");
			
	  // set workflow status based on a supported model
	  workitem.replaceItemValue("$modelVersion", "1.0.0");
	  workitem.replaceItemValue("$processID", 10);
	  workitem.replaceItemValue("$ActivityID", 10);
	  // process the workitem
	  workitem=workflowService.processWorkItem(workitem);

After a new workitem is process the first time, it is under the control of the WorkflowManager. To get the current list of all workitems created by the current user, the  method() getWorkListByCreator can be called: 
  
	  @EJB
	  org.imixs.workflow.jee.ejb.WorkflowService workflowService;
	  //...
	  Collection<ItemCollection> worklist=workflowService.getWorkListByCreator(null,0,-1);
	  //...

  
To read the list of all workitems created by a specific user, the parameter username need to be specified:
  
    Collection<ItemCollection> worklist=workflowService.getWorkListByCreator('manfred',0,-1);
  
The WorkflowManager provides a paging mechanism to browse through long result-sets. The following example
shows how to get 5 workitems starting at the tenth record
  
    Collection<ItemCollection> worklist=workflowService.getWorkListByCreator(null,10,5);

##Worklist methods
The following methods provide different ways to read a worklist by categories. The workflowService returns only workitems in a worklist if the user has read access. If a workitem is not access able for the user this workitem will not be included in the result-set.  All result-sets can be ordered by modified or creation date. 

###getWorkList
Returns a collection of workitems for the current user. A Workitem belongs to a user or role if the  user has at least read write access to this workitem. 

    Collection<ItemCollection> list=workflowService.getWorkList();
    //...

You can specify the type, the start position, the count and sort order of workitems returned 
by this method. The type of a workitem is defined by the workitem property 'type' which can be set before a workitem is processed.

	  String type="workitem";
	  Collection<ItemCollection> list=workflowService.getWorkList(0,-1,
	     type,WorkflowService.SORT_ORDER_CREATED_DESC);
	  //...

###getWorkListByAuthor

Returns a collection of workitems belonging to a specified user. This filter can be set to 
 a username or a user role defined by the application. A Workitem belgons to a user or role if the  user has write access to this workitem. So the method returns workitems which can be 
 processed by the user.  

	  String type="workitem";
	  String user="manfred"
	  Collection<ItemCollection> list=workflowService.getWorkList(user,0,-1,
	     type,WorkflowService.SORT_ORDER_CREATED_DESC);
	  //...


###getWorkListByGroup
Returns a collection of workitems belonging to a specified workflow group.  The workflow group is defined by the workflow model and includes all process entities defined by 
 a business process 

	  String type="workitem";
	  String group="Ticketservice";
	  Collection<ItemCollection> list=workflowService.getWorkListByGroup(group,0,-1,
	     type,WorkflowService.SORT_ORDER_CREATED_DESC);
	  //...


###getWorkListByProcessID

Returns a collection of workitems belonging to a specified $processID defined by the workflow model.

	  String type="workitem";
	  Collection<ItemCollection> list=workflowService.getWorkListByProcessID(2100,0,-1,
	     type,WorkflowService.SORT_ORDER_CREATED_DESC);
	  //...


###getWorkListByOwner
Returns a collection of workitems containing a namOwner property belonging to a specified username.  The namOwner property is typical controlled by the OwnerPlugin using the Imixs Workflow Modeler

	  String type="workitem";
	  String user="Manfred"
	  Collection<ItemCollection> list=workflowService.getWorkListByOwner(user,0,-1,
	     type,WorkflowService.SORT_ORDER_CREATED_DESC);
	  //...
  
###getWorkListByWriteAccess
Returns a collection of workitems where the current user has at least writeAccess. This means the either the  username or one of the user roles is contained in the $writeaccess property of each workitem returned by the method.
 
	  String type="workitem";
	  Collection<ItemCollection> list=workflowService.getWorkListByWriteAccess(0,-1,
	     type,WorkflowService.SORT_ORDER_CREATED_DESC);
	  //...
  
###getWorkListByRef
 Returns a collection of workitems belonging to a specified workitem identified by the attribute $UniqueIDRef. 

	  String type="workitem";
	  Collection<ItemCollection> list=workflowService.getWorkListByRef(refID,0,-1,
	     type,WorkflowService.SORT_ORDER_CREATED_DESC);
	  //...
  
##Model Version Management 
Each time a running process instance is updated, the WorkflowService compares  the internal model version with the model versions provided by the model repository.  In case the current model version is no longer available the WorkflowService  automatically upgrades an active process instance to the latest version in the   repository. Therefore the engine verifies the task ID (numprocessid) and the process name   (txtworkflowgroup) with the corresponding models. This mechanism allows to upgrade  process instances at run time to a newer version. 
  
It is also possible to handle different versions of a model at the same time.   In this case each process instance is processed by the model version from which  it was started.
  
 