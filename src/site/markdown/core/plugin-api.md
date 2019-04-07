# Imixs-Workflow Plugin API

The [Imixs-Workflow Plugin API](../engine/plugins/index.html) introduce an extensibility mechanism to extend the standard functionality of the Imixs-Workflow engine. A plugin can provide business logic to control the processing life-cycle of a process instance, as also platform specific technical functionality, to exchange data with its surrounding environment. 

For example a plugin can apply a business rule, send an E-Mail notification, or import business data from an external datasource. The Imixs-Workflow project already provides a set of plug-ins which covers a lot of standard functionality of a workflow engine. See the section [Plugin-API](../engine/plugins/index.html) for further information. 

Plugins are bound to the Imixs-Workflow model and can be easily added to an existing BPMN model using the [Imixs-BPMN modeling tool](../modelling/index.html).

<img src="../images/modelling/bpmn_screen_32.png" style="width:700px"/>

Read more about modelling in the [Imixs-BPMN user guide](../modelling/index.html). 
 
## The Plugin Architecture
Each plugin represents an individual building block inside a Imixs-Workflow application. A plugin can be reused in different workflow models for different applications.
A plugin, added to a workflow model, is controlled and executed by the [WorkflowKernel](./workflowkernel.html). The _WorkflowKernel_ calls the plugin during the processing life-cycle and provides information about the running process instance and the workflow event processed by the workflow engine.

This concept makes it easy for a Plugin Developer to implement additional business logic and custom functionality. 
A plugin can validate a process instance and may also manipulate the business data. A plugin can also interrupt the processing phase by throwing a [PluginException](../engine/plugins/exception_handling.html) - for example in case of a validation error.
 
 
###The Two-Phase Commit
The Imixs Plugin-API defines three call-back methods, called by the WorkflowKernel for each plugin in one single transaction during the processing life-cycle. This is the concept of a two-phase commit (2PC).
 
 
    public void init(WorkflowContext workflowContext) throws PluginException;
    
    public ItemCollection run(ItemCollection document,ItemCollection event) throws PluginException;
    
    public void close(boolean rollbackTransaction) throws PluginException;
 
After the _WorkflowKernel_ started a new transaction to process a workitem, first the init() method is called for each plugin. A plugin may setup necessary external resources or can indicate an invalid state by throwing a PluginException. 

In the second phase, the _WorkflowKernel_ executes each plugin by calling the run() method. The _WorkflowKernel_ provides the current process instance and also the event as parameters for this method. After a plugin has executed its internal business logic, it should return the instance of the processed workitem to indicate that the processing life-cycle can be continued. 
If a plugin returns null or throws a PluginException during the run() method, the current transaction will be rolled back by the _WorkflowKernel_. 

In each case, the _WorkflowKernel_ will finish its transaction by calling the call-back method close() for each plugin by providing the roll-back-status. In case the transaction will be rolled back, the flag _rollbackTransaction_  will be 'true'. 
The close method allows a plugin to tear down external resources or roll-back internal transactions. In a concept of a two-phase commit, the _WorkflowKernal_ takes the role of the _central coordinator_.  
 
### Example
The following example shows the general structure of a plugin implementation:
 
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
During the init() phase the _WorkflowKernel_ provides the plugin with an instance of the _WorkflowContext_. The _WorkflowContext_ is an abstraction of the workflow environment and provides access to an instance of the [Model interface](./model). The following example illustrates how an instance of a model can be fetched during the initialization of a plugin:

    public void init(WorkflowContext ctx) throws Exception {
		if (ctx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			workflowService = (WorkflowService) ctx;
			// get model
			Model model= workflowService.getModelManager().getModel(aModelVersion).
		}
    } 



# Plugin Dependencies

A plugin may optionally implement the interface 'PluginDependency' to indicate dependencies on other plugins. Plugin dependencies are validated by the WorkflowKernel during processing a workflow event. If a plugin defined by the BPMN model signals dependencies which are not reflected by the current model definition, a warning message is logged. 

The interface _PluginDependency_ only provide one method called 'dependsOn' returning a String array with plugins need to be called before this plugin class can be executed by the WorkflowKernel. 

	org.imixs.workflow.PluginDependency

See the following example:

	public class MyPlugin implements Plugin, PluginDependecy {
    
    public List<String> dependsOn {
    	
    	String[] depends = new String[] { "org.imixs.workflow.engine.plugins.AccessPlugin", "org.imixs.workflow.engine.plugins.RulePlugin" };
		return Arrays.asList(depends);
    } 
  