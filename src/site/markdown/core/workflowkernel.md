#The WorkflowKernel

The Workflowkernel is the core component of this Imixs Workflow API to control the processing of a workitem. A Workflowmanager implementation (like the Imixs JEE Workflow) loads an instance of the Workflowkernel, hand over a Model Instance and register a list of workflow plugins. To process a single Workitem you typical call the method 'process' of the WorkflowManager  interface.
 
    process(ItemCollection workitem) 

##Registration of a Workflow Plugin
The WorkflowManager over hands the workitem to the WorkflowKernel to process the workitem by a predefined set of workflow plugins. Therefore the WorkflowKernel provides the method :

    registerPlugin(String) 

This method enables the WorkflowManager to register different PluginClasses to be loaded by 
the Kernel during the Process method.
 
The internal method processActivity() process an activity instance by loading and running all  registered plugins. Then the method computes the next processId which will be stored in the  property $ProcessID. The method processActivity will run as long as the property $ActivtyList holds additional ActivityIds to be processed. So a WorkflowManger can also run through a process queue on a single Workitem.
  
##The Workflow Log

The Imixs WorkflowKernel automatically generates a log entry for each transition 
with information about the modelversion, process and activity id.
The log is stored in the property 'txtWorkflowActivityLog'. 
 
The Log entries have the following format:
 
    ISO8601 Time | Model Version | Process.ActivityID |  NewProcessID | comment /optional)
 
Example:

	 2014-08-10T16:44:32.025|1.0.0|100.10|100|
	 2014-08-10T16:45:32.125|1.0.0|100.10|100|
	 2014-08-10T16:47:12.332|1.0.0|100.20|200|userid|comment

The comment is an optional value which can be provided by a plugin. The comment should be stored into the property 'txtworkflowactivitylogComment' of the current document context. If no txtworkflowactivitylogComment is provided the comment section is empty.
    