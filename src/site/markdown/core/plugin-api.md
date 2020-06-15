# Imixs-Workflow Plugin API

The **Imixs-Workflow Plugin API** provides a microkernel architecture pattern and is the standard extension mechanism of the Imixs-Workflow Engine. 
The microkernel pattern allows you to add additional features to the Imixs-Workflow engine. The Plugin API is providing extensibility to the processing cycle of a BPMN Event as well as feature separation and isolation.

A Plug-In can be bound to one or many different BPMN models. Each time a BPMN Event is triggered, an associated Plug-In will be executed. 
In this way a Plug-In extends the processing life-cycle of a BPMN Event.
A Plug-In can exchange data or execute platform specific technical functionality. For example a Plug-In can apply a business rule, send an E-Mail notification, or call a Microservice. 

<img src="../images/plugin_api.png"/>  


The Imixs workflow project already offers a number of Plug-Ins that cover many of the standard features of a workflow engine. You will find an overview in the section [Plugin-API](../engine/plugins/index.html). As an alternative extension mechanism see the [Imixs Adapter API](./adapter-api.html) which is also part of the Imixs microkernel architecture pattern 


## Adding a Plug-In to a BPMN Model 

Plug-Ins are bound to the Imixs-Workflow model and can easily be added to a BPMN model using the [Imixs-BPMN modeling tool](../modelling/index.html). 
The section _Workflow_ in a BPMN model defines the Plug-Ins as well as their execution order:

<img src="../images/modelling/bpmn_screen_32.png" style="width:700px"/>

Read more about modeling in the [Imixs-BPMN user guide](../modelling/index.html). 
 
## The Plug-In Architecture
Each Plug-In represents an individual building block inside a Imixs-Workflow application. A Plug-In can be reused in different workflow models for different applications.
A Plug-In, added to a workflow model, is controlled and executed by the [WorkflowKernel](./workflowkernel.html). The _WorkflowKernel_ calls the Plug-In during the processing life-cycle and provides information about the running process instance and the workflow event processed by the workflow engine.

This concept makes it easy for a Plug-In Developer to implement additional business logic and custom functionality. 
A Plug-In can validate a process instance and may also manipulate the business data. A Plug-In can also interrupt the processing phase by throwing a [PluginException](../engine/plugins/exception_handling.html) - for example in case of a validation error.
 
 
###The Two-Phase Commit
The Imixs Plugin-API defines three call-back methods, called by the WorkflowKernel for each Plug-In in one single transaction during the processing life-cycle. This is the concept of a two-phase commit (2PC).
 
 
    public void init(WorkflowContext workflowContext) throws PluginException;
    
    public ItemCollection run(ItemCollection document,ItemCollection event) throws PluginException;
    
    public void close(boolean rollbackTransaction) throws PluginException;
 
After the _WorkflowKernel_ started a new transaction to process a workitem, first the init() method is called for each Plug-In. A Plug-In may setup necessary external resources or can indicate an invalid state by throwing a PluginException. 

In the second phase, the _WorkflowKernel_ executes each Plug-In by calling the run() method. The _WorkflowKernel_ provides the current process instance and also the event as parameters for this method. After a Plug-In has executed its internal business logic, it should return the instance of the processed workitem to indicate that the processing life-cycle can be continued. 
If a Plug-In returns null or throws a PluginException during the run() method, the current transaction will be rolled back by the _WorkflowKernel_. 

In each case, the _WorkflowKernel_ will finish its transaction by calling the call-back method close() for each Plug-In by providing the roll-back-status. In case the transaction will be rolled back, the flag _rollbackTransaction_  will be 'true'. 
The close method allows a Plug-In to tear down external resources or roll-back internal transactions. In a concept of a two-phase commit, the _WorkflowKernal_ takes the role of the _central coordinator_.  
 
### Example
The following example shows the general structure of a Plug-In implementation:
 
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
 

### How to Access the Workflow Model
During the init() phase the _WorkflowKernel_ provides the Plug-In with an instance of the _WorkflowContext_. The _WorkflowContext_ is an abstraction of the workflow environment and provides access to an instance of the [Model interface](./model). The following example illustrates how an instance of a model can be fetched during the initialization of a Plug-In:

    public void init(WorkflowContext ctx) throws Exception {
		if (ctx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			workflowService = (WorkflowService) ctx;
			// get model
			Model model= workflowService.getModelManager().getModel(aModelVersion).
		}
    } 



# Plug-In Dependencies

A Plug-In may optionally implement the interface 'PluginDependency' to indicate dependencies on other Plug-Ins. Plug-In dependencies are validated by the WorkflowKernel during processing a workflow event. If a Plug-In defined by the BPMN model signals dependencies which are not reflected by the current model definition, a warning message is logged. 

The interface _PluginDependency_ only provide one method called 'dependsOn' returning a String array with Plug-Ins need to be called before this Plug-In class can be executed by the WorkflowKernel. 

	org.imixs.workflow.PluginDependency

See the following example:

	public class MyPlugin implements Plugin, PluginDependecy {
    
    public List<String> dependsOn {
    	
    	String[] depends = new String[] { "org.imixs.workflow.engine.plugins.AccessPlugin", "org.imixs.workflow.engine.plugins.RulePlugin" };
		return Arrays.asList(depends);
    } 
  