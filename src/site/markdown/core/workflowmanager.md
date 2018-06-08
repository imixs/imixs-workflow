#The WorkflowManager
The WorkflowManager is the central interface for each implementation of an Imixs-Workflow Engine. This interface provides the general functionality to create, process and search Workitems. 
The WorkflowManager instantiates the [WorkflowKernel](./workflowkernel.html) to process a WorkItem and provides the implementation of a Model. The WorkflowManager can be used by an application to process and search Workitems and provide the environment for the business logic implemented by the plugins.
The [WorkflowManager Service](../engine/workflowservice.html) implements the WorkflowManager based on the JEE architecture.
 
## The Plugin-API
The general workflow functions and also application specific business logic is provided by [Plugin API](./plugin-api.html). The WorkflowManager can register a specific set of plugins to be called by the WorkflowKernel when processing a Workitem.  A typical code example is structured as follows:

    ....
    // init WorkflowKernel 
    WorkflowKernel workflowKernel =new org.imixs.workflow.WorkflowKernel(workflowContext);
    // register plugins.....
    workflowKernel.registerPlugin("org.imixs.workflow.plugins.AccessPlugin");
    workflowKernel.registerPlugin("org.imixs.workflow.plugins.HistoryPlugin");
    // processing workitem...
    myWorkitem=workflowKernel.process(myWorkitem) ;
    ....

 
 
##Processing a Workitem
The method 'processWorkItem()' is used to create or update a Workitem. The WorkflowManager implementation is responsible to load the Model, call the WorkflowKernel and persist the Workitem so it can be retrieved later by the application. 
 
    public void processWorkItem(ItemCollection aWorkItem)throws Exception {
       // ...instantiate workflowKernel...
       
       // ...register plugins to workflowKernel...
       
       aWorkItem=workflowKernel.process(itemCol);
       
       // ...save the workitem...
    ....
    }

The WorkflowKernel can throw a ProcessingErrorException if the Workitem did not match the model status.  Therefore the WorkflowManager implementation have to verify if the Workitem provides valid values for the properties '$taskID' and '$eventID'.  
 
## Find Workitems

The property "$uniqueid" identifies the workitem controlled by the workflowManager. This ID can be used to load a workitem from the WorkflowManager:
  
    workitem=wfm.getWorkItem(uniqueid);

It is also possible to receive a list of workitems for the current user by calling the method  getWorkList(). 
  
    List<ItemCollection> worklist=wfm.getWorkList();
  
The method returns a list of all workItems assigned to the current user.  A workitem is typically managed by a WorkflowManger for the complete life cycle.  To remove a workitem from the WorkflowManager underlying database you can call the removeWorkitem method:
  
    wfm.removeWorkItem(workitem);
 
##The Imixs WorkflowService
 
The Imixs-Workflow engine provides the WorkflowService component which implements the WorkflowManager  on the JEE component stack.   Find more information about the Imixs-WorkflowService in the section [WorkflowService](../engine/workflowservice.html).    

