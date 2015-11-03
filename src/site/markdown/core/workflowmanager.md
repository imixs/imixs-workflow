#The WorkflowManager

The WorkflowManager is the central interface to implement a Workflow Engine. 
This interface provides the general functionality to create, process and search Workitems. 
 
The WorkflowManager serves as the central interface between the business application and the 
 {{{./workflowkernel.html}WorkflowKernel}}.  For this purpose, the WorkflowManager provides the functionality to process and  search {{{./quickstart_workitem.html}Workitems}}, retrieve the {{{./model.html}Workflow Model}}, and manage {{{./plugin-api.html}plug-ins}} for custom implementations.

The {{{http://www.imixs.org/jee/}Imixs-Workflow Engine}} is a typical implementation of the WorkflowManager and extends the functionality in a platform specific way. 
 
##Workflow Plug-Ins
The general workflow functions and also application specific business logic is provided by {{{./plugin-api.html}plug-ins}}. The WorkflowManager can register a specific set of plug-ins to be called by the WorkflowKernel when processing a Workitem.  A typical code example is structured as follows:

    ....
    // init WorkflowKernel 
    WorkflowKernel workflowKernel =new org.imixs.workflow.WorkflowKernel(workflowContext);
    // register Plug-ins.....
    workflowKernel.registerPlugin("org.imixs.workflow.plugins.AccessPlugin");
    workflowKernel.registerPlugin("org.imixs.workflow.plugins.HistoryPlugin");
    // processing workitem...
    myWorkitem=workflowKernel.process(myWorkitem) ;
    ....

 
 
##Processing a Workitem

The method 'processWorkItem' is to create or update a process instance for a Workitem.
The WorkflowManager implementation is responsible to load the Model, call the WorkflowKernel 
and persist the Workitem so it can be retrieved later by the application. 
 
    public void processWorkItem(ItemCollection aWorkItem)throws Exception {
       // ...instantiate workflowKernel...
       
       // ...register plug-ins to workflowKernel...
       
       aWorkItem=workflowKernel.process(itemCol);
       
       // ...save the workitem...
    ....
    }

The WorkflowKernel can throw a ProcessingErrorException if the Workitem did not match the model status.  There for the WorkflowManager implementation have to verify if the Workitem provides valid values for the properties '$processID' and '$ActivityID'.  For further information see the section {{{http://www.imixs.org/jee/}Imixs-Workflow Engine}} which is a concrete implementation based on the JEE platform. 
 
##Find Workitems

The property "$uniqueid" identifies the workitem controlled by the workflowManager. This ID can be used to load a workitem from the WorkflowManager:
  
    workitem=wfm.getWorkItem(uniqueid);

You can also receive a list of workitems for the current user by calling the method  getWorkList(). 
  
    List<ItemCollection> worklist=wfm.getWorkList();
  
The method returns a list of all workItems assigned to the current user.  A workitem is typically managed by a WorkflowManger for the complete life cycle.  To remove a workitem from the WorkflowManager underlying database you can call the removeWorkitem method:
  
    wfm.removeWorkItem(workitem);
 
This method removes an instance of a workitem form the WorkflowManger.  
 
 
##The Imixs WorkflowService
 
The Imixs-Workflow engine provides the WorkflowService component which implements the WorkflowManager  on the JEE component stack. This Service provides a full features WorkflowManager.  Find more information about the Imixs-WorkflowService in the section {{{http://www.imixs.org/jee/services/overview.html}Imixs-Workflow engine Services}}.    

