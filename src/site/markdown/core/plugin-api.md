#The Plug-in API

The Imixs Plug-in API introduces an extensibility mechanism that allows to extend the standard functionality of the Imixs-Worklfow engine. A plug-in can provide business logic to influence the processing life-cycle of a process instance, as also platform specific technical functionality, to exchange data with its surrounding environment. 

For example a plug-in can apply a business rule, send an E-Mail notification, or import business data from an external datasource. The Imixs-Workflow project already provides a set of plug-ins which covers a lot of standard functionality of a workflow engine. See the section [Engine->Plug-Ins](../engine/plugins/index.html) for further details about existing plug-ins. 

Plug-ins are bound to the Imixs-Workflow model and can be easily added to an existing BPMN model using the [Imixs-BPMN modeling tool](../modelling/index.html).

<img src="../images/modelling/bpmn_screen_32.png" style="width:700px"/>

Read more about modelling in the [Imixs-BPMN user guide](../modelling/index.html). 
 
##Plug-In Architecture
Each plug-in is a individual building block inside a workflow management system and can be reused for different workflow models.
A plug-in added to a workflow model is controlled and executed by the [Imixs-Workflow Kernel](./workflowkernel.html). The _WorkflowKernel_ calls the plug-in during the processing life-cylce of a process instance and provides information about the running process instance and the event processed by the workflow engine.

This concept makes it easy to implement plug-in providing additional business logic and custom functionality. 
A plug-in can validate a process instance and may also manipulate the business data. A plug-in can also interrupt the processing phase by throwing a [PluginException](../engine/plugins/exception_handling.html) - for example in case of validation error.
 
 
###The Two-Phase Commit
The Imixs Plug-in API defines three call-back methods called by the WorkflowKernel for each plug-in in one single transaction of the processing life-cycle. This is the concept of a two-phase commit (2PC).
 
 
    public void init(WorkflowContext workflowContext) throws PluginException;
    
    public ItemCollection run(ItemCollection document,ItemCollection event) throws PluginException;
    
    public void close(boolean rollbackTransaction) throws PluginException;
 
After the _WorkflowKernel_ started a new transaction to process a workitem, first the init() method is called for each plug-in. A plug-in may setup necessary external resources or can indicate an invalid state by throwing a PluginException. 

In the second phase, the _WorkflowKernel_ executes each plug-in by calling the run() method. The _WorkflowKernel_ provides the current process instance and also the event as parameters for this method. After a plug-in has executed its internal business logic, it should return the instance of the processed workitem. 
If a plug-in returns null or throws a PluginException during the run() method, the current transaction will be rolled back by the _WorkflowKernel_. 

In each case, the _WorkflowKernel_ will finish its transaction by calling the call-back method close() for each plug-in by providing the roll-back-status. In case the transaction will be rolled back, the flag 'rollbackTransaction==true' will be provided. 
This allows a plug-in to tear down external resources or roll-back internal transactions. In a concept of a two-phase commit the WorkflowKernal takes the role of the _central coordinator_.  
 
### Example
The following example shows the general structure of a Imixs Workflow Plugin:
 
    public class MyPlugin implements Plugin {
	    WorkflowContext workflowContext;

	   public void init(WorkflowContext actx) throws Exception {
	 	workflowContext = actx;
    	}

		public ItemCollection run(ItemCollection workitem, ItemCollection event) throws Exception {
			try {
				// you business logic goes here....
			} catch (Exception e) {
				// signal exception....
				throw new PluginExeption(e);
			}
			// signal success 
			return workitem;
		}

		public void close(boolean rollbackTransaction) throws Exception {
			try {
				// restore changes?
				if (rollbackTransaction) {
					// role back....
				} else {
					// commit changes....
				}
			} catch (Exception e) {
				 
			}
			finally {
				// close resources 
			}
		}
    }
 

###How to Access the Workflow Model
During the init() phase the WorkflowKernel provides the plug-in with an instance of the WorkflowContext. The WorkflowContext is an abstraction of the workflow environment and provides access to an instance of the [Model interface](./model). The following example illustrates how an instance of a model can be fetched during the initialization of a plug-in:

     public void init(WorkflowContext ctx) throws Exception {
		if (ctx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			workflowService = (WorkflowService) ctx;
			// get model
			Model model= workflowService.getModelManager().getModel(aModelVersion).
		}
    } 




  