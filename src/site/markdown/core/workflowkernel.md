#The Workflow Kernel

The class _org.imxis.workflow.WorkflowKernel_ is the core component of the Imixs-Workflow API. The _WorkflowKernel_ controls the processing life cycle of a process instance (Workitem) according to an Imixs-BPMN process model. 
The processing life cycle is defined by a BPMN _Event_ describing the transition between two BPMN _Task_ elements.

<img src="../images/modelling/example_01.png"/>

The _WorkflowKernel_ is initialized by the _[Workflowmanager](./workflowmanager.html)_ which is providing the _WorkflowContext_ and the _BPMN 2.0 Model definition_. 
To process a single Workitem the _WorkflowKernel_ provides the method _process()_:
 
    process(ItemCollection workitem) 


The _WorkflowKernel_ controls the following attributes of a process instance:

|Attribute      	| Description 				 						|
|-------------------|---------------------------------------------------|
|$UniqueID    	    | A unique key to access the process instance    	|
|$WorkitemID        | A unique process instance id of this workitem     |
|$ModelVersion      | The Version of the model the workitem belongs to  |
|$ProcessID         | The current BPMN Task ID of the workItem          |
|$lastEvent         | The last processed BPMN Event element             |
|$lastTask          | The last assigned BPMN Task element   			|


## The Processing Phase
By calling the method _process()_ from the _WorkflowKernel_ the processing phase for a WorkItem is started. During the processing phase the _WorkflowKernel_ loads the assigned BPMN event and triggers all registered plugins to be executed.
During the processing phase additional BPMN Events can be triggered according to the model definition. See the section ['How to model'](../modelling/howto.html) for further details. 
When the processing phase is completed the _WorkflowKernel_ applies the new BPMN Task element to the workitem as defined by the process model.
 
### Conditional Events

A conditional event is used by the _WorkflowKernel_ to evaluate the output of an event during the processing life-cycle. 
A conditional-event can be placed before an _ExclusiveGateway_ where each output of the event defines a boolean expression.

<img src="../images/modelling/example_08.png"/>
 
The expressions are evaluated by the _WorkflowKernel_ to compute the output of a BPMN  Gateway element. 
See the section ['How to model'](../modelling/howto.html#Conditional_Events) for further details about modeling Conditional Events.  



### Split Events

In Imixs-Workflow a BPMN _Event_ followed by a _Parallel Gateway_ is called a _split-event_ and forces the _WorkflowKernel_ to create a new version of the current process instance.

<img src="../images/modelling/example_11.png"/>

The _WorklfowKernel_ evaluates the conditions assigned to the outcome of the _Parallel Gateway_. The conditions are either evaluated to the boolean value _true_ or _false_. 
If the condition evaluates to '_true_', this outcome is followed by the current process instance (Source Workitem).
If the condition evaluates to '_false_', then a new version of the Source Workitem is created.  

**Note:** The _WorklfowKernel_ expects that each outcome evaluated to '_false_' is followed by an Imixs-Event element. 
This event will be processed by the new created version. If an outcome evaluated to '_false_' is not followed by an Imixs-Event, a _ModelException_ is thrown. See the section ['How to model'](../modelling/howto.html#Split_Events) for details about modeling Split Events.  
 
  
| Condition 	| Type              | Description                               						|
|:-------------:|:-----------------:|-------------------------------------------------------------------|
|true           | Source            | describes the outcome for the current process instance.			|
|false          | Version           | triggers the creation of a new version. 							|
 

The current process instance is called the _Source Workitem_. The _Source Workitem_ and all versions have the same _'$workitemID'_. In addition, the  _'$UniqueID'_ of a new version is stored into the attribute _'$uniqueidVersions'_ of the current process instance. A version holds a reference to the _Source Workitem_ in the attribute _'$uniqueIdSource'_.

|Attribute      	| Source | Version | Description 				 										|
|-------------------|:------:|:-------:|--------------------------------------------------------------------|
|$workitemId    	| x      | x       |A unique shared key across all versions and the source workitem.	|
|$uniqueIdSource	|        | x       |A reference to the $UniqueID of the Source workitem.				|  	 	
|$uniqueIdVersions	| x      |         |A list of $UniqueIDs of all created versions.	|
|$isVersion			| 		 | x	   |This temporary attribute indicates that the current instance is a version. | 
 
The temporary attribute _'$isVersion'_ flags the version during the processing phase and can be used by Plugins to handle these workitems. See also the section: [Workflow Data](../quickstart/workitem.html#Temporary_Attributes).

The _WorkflowKernel_ returns new created versions of the current process instance by the method _getSplitWorkitems()_. This method is used by the Workflow Engine to store the versions into a database.   


## Registration of Workflow Plugins
In the processing phase of a WorkItem the _WorkflowKernel_ calls the plug-ins registered by the _WorkflowManager_. To register a plug-in  the _WorkflowKernel_ provides the method :

    registerPlugin(String) 

Plugins registered by the _WorkflowManager_ will be executed during the processing phase of a workitem. See the section [Plugin API](./plugin-api.html) for further details about the plugin concept. 
 
The registration of a plugin can be done by an instance of a plugin class or by the class name. Plugins are also supporting the the injection mechanism of CDI. 

    kernel = new WorkflowKernel(ctx);
    // register plugin by name
    kernel.registerPlugin(MyPlugin.class.getName());
    // register plugin by instance
    kernel.registerPlugin(new MyPlugin());
    // process workitem
    workitem=kernel.process(workitem);
    
To unregister a single plugin, the method _unregisterPlugin()_ can be called:
 
    // unregister plugin by name
    kernel.unregisterPlugin(MyPlugin.class.getName());



## The WorkflowContext
The Interface _org.imixs.workflow.WorkflowContext_ defines an abstraction of the workflow management system. The WorkflowContext is used by the _WorkflowKernel_ to provide the runtime-environment information to registered plugins.

The WorkflowContext provides the following methods:

|Method              		 | Description 				 |
|----------------------------|---------------------------|
|getSessionContext()| returns a session object (e.g. EJB Context or Web Session) |
|getModelManager()	| returns an instance of the model manager	| 

The session context is platform specific, for example a surrounding EJB Context, a Web Module or a Spring Context. As the _WorkflowKernel_ is part of the Imixs-Workflow core API it is a platform independent building block. Therefor the WorkflowContext builds a bridge between the Workflow System and the processing engine. For details about the concrete implementation see the [WorkflowService implementation](../engine/workflowservice.html). 
 
 
##The Workflow Log

The Imixs _WorkflowKernel_ generates a log entry during each processing phase with information about the current model version, the process entity and the processed workflow event. The log is stored in the property '$EventLog'. 
The log entry has the following format:
 
    ISO8601 Time | Model Version | Process.ActivityID |  NewProcessID | comment /optional)
 
Example:

	 2014-08-10T16:44:32.025|1.0.0|100.10|100|
	 2014-08-10T16:45:32.125|1.0.0|100.10|100|
	 2014-08-10T16:47:12.332|1.0.0|100.20|200|userid|comment

The comment is an optional value which can be provided by a [plugin](./plugin-api.html). A comment is stored in the property 'txtworkflowactivitylogComment' of the current process instance. If no _txtworkflowactivitylogComment_ attribute is provided the comment section will be empty.

