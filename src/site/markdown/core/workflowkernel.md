#The WorkflowKernel

The WorkflowKernel is the core component of the Imixs-Workflow API. The WorkflowKernel controls the flow of a process instance according to the process model definition. The WorkflowKernel is initialized by the [Workflowmanager](./workflowmanager.html) which provides the Model definition and also the plug-ins applied to the process model. To process a single Workitem the _WorkflowKernel_ provides the public method process().
 
    process(ItemCollection workitem) 

## Registration of Workflow Plug-ins
In the processing phase of a WorkItem the _WorkflowKernel_ calls the plug-ins registered by the _WorkflowManager_. To register a plug-in class the WorkflowKernel provides the method :

    registerPlugin(String) 

Plug-ins registered by the _WorkflowManager_ will be called during the processing phase of a workitem. See the section [plug-in api](./plugin-api.html) for further details about the plug-in concept. 
 
The registration of a plug-in can be done ba instance of a plug-in class or by the class name. 

    kernel = new WorkflowKernel(ctx);
    // register plug-in by name
    kernel.registerPlugin(MyPlugin.class.getName());
    // register plug-in by instance
    kernel.registerPlugin(new MyPlugin());
    // process workitem
    workitem=kernel.process(workitem);
    
A plug-in can also be injected by a CDI mechanism. 
Registered plug-ins are always each processing phase of a workitem. To unregister a single workItem the method unregisterPlugin can be called
 
    // unregister plug-in by name
    kernel.unregisterPlugin(MyPlugin.class.getName());


## The Processing Phase
By calling the method process() from the WorkflowKernel the processing phase or a WorkItem can be started. During the processing phase the WorkflowKernel loads all activity entities assigned to the current process status and performs a call-back life-cycle for all registered plug-ins. A plug-in can extend the the processing phase by adding additional activities on the activity stack which is defined by the property '$activityidlist' of the process instance. 
After the processing phase is finished the WorkflowKernel applies the new process status to the WorkItem depending on the process definition.

 

## The WorkflowContext
The Interface _org.imixs.workflow.WorkflowContext_ defines an abstraction of the workflow management system. The WorkflowContext is used by the WorkflowKernel to provide the runtime-environment information to registered Plug-ins.

The WorkflowContext provides the following methods:

|Method              		 | Description 				 |
|----------------------------|---------------------------|
|getSessionContext()| returns a session object (e.g. EJB Context or Web Session) |
|getModelManager()	| returns an instance of the model manager	| 

The session context is platform specific, for example a surrounding EJB Context, a Web Module or a Spring Context. As the WorkflowKernel is part of the Imixs-Workflow core API it is a platform independent building block. Therefor the WorkflowContext builds a bridge between the Workflow System and the processing engine. For details about the concrete implementation see the [WorkflowService implementation](../engine/workflowservice.html). 
 
 
##The Workflow Log

The Imixs WorkflowKernel generates a log entry during each processing phase with information about the current model version, the process entity and the processed workflow event. The log is stored in the property 'txtWorkflowActivityLog'. 
The log entry has the following format:
 
    ISO8601 Time | Model Version | Process.ActivityID |  NewProcessID | comment /optional)
 
Example:

	 2014-08-10T16:44:32.025|1.0.0|100.10|100|
	 2014-08-10T16:45:32.125|1.0.0|100.10|100|
	 2014-08-10T16:47:12.332|1.0.0|100.20|200|userid|comment

The comment is an optional value which can be provided by a [plug-in](./plugin-api.html). A comment is stored in the property 'txtworkflowactivitylogComment' of the current process instance. If no _txtworkflowactivitylogComment_ attribute is provided the comment section will be empty.

