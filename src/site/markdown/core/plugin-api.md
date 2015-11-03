#The Plug-in API

The Imixs Plug-in API is a powerful concept to extend the technical functionality of the Imixs-Workflow engine. A plug-in can provide general business logic as also platform specific technical functionality. Plug-Ins are the building blocks inside a workflow management system implementing different aspects of a business solution. For example a plug-in can send a E-Mail notification, apply business rules or import or export workflow data into a external database. There are a lot of plug-ins provided by the Imixs-Workflow engine which can be used out of the box or used to extend individual functionality on the business layer of a workflow management system. See the section [Engine->Plug-Ins](../engine/plugins/index.html) for further details about existing plug-ins. 
 
##Plug-In Architecture
Each plug-in is a individual building block inside a workflow management system and may be reused in  different phases of a business process.
Plug-ins are bound to the workflow model and controlled by the [Imixs-Workflow Kernel](./workflowkernel.html). The Workflow Kernel calls the plug-in during processing and provides the plug-in with detailed informations about the running process instance. This concept makes it easy to implement certain details of a business process. 
A plugin can analyze and validate the structure of a process instance and may also manipulate the data. Through the information provided by the WorkflowKernel a plugin can determine the context of a process step. A plugin can also navigate through the workflow model to evaluate details about the business process or interrupt the processing phase by throwing a [PluginException](../engine/plugins/exception_handling.html).
 
 
##Transaction and Two-Phase Commit
The Imixs Plug-in API defines three call-back methods which need to be implemented by a plug-in.
 
  * init()
  * run()
  * close()
  
The workflowKernel calls these methods for each registered plug-in inside a single transaction to provide the concept of a two-phase commit (2PC). Before the WorkflowKernel starts the transaction the init() method of each plugin will be called to initialize the transaction. In the second phase the WorkflowKernel executes the plug-in by calling the call-back method run().
A plug-in can terminate this transaction by providing the WorkflowKernel with a specific error code or throwing a PluginException. The WorkflowKernel will finish the transaction by calling the call-back method close() for each plug-in providing a final status code. This allows a plug-in to roll-back internal transactions in case an error code is given, or finalize its execution the internal transaction in case the successful transaction was confirmed by all plug-ins. So in a concept of a two-phase commit the WorkflowKernal takes the role of the central coordinator.  
 

###1) The Matching Phase
During execution of the run() method (matching phase), the WorkflowKernel requests all 
plug-ins to check and prepare their actions. A plugin must finish the run() method with the status  PLUGIN_OK signaling the WorkflowKernel that execution is possible. 
If the plugin returns the status PLUGIN_ERROR, the plugin indicates that the action was not successfully. In this way, the WorkflowKernel gets all votes from the plug-ins and decides how the transaction should be completed. If there was at least one error, the entire transaction will be undone. Otherwise, the WorkflowKernel decides that the transaction can be executed successfully.

###2) The Commit Phase
During execution of the close() method (commit phase), the WorkflowKernel informs the 
plug-ins about its decision. Each plugin must then ensure that the action results in a commit and is actually completed, or execute a roll-back if a PLUGIN_ERROR occurs.
    
The following example shows the general structure of a Imixs Workflow Plugin:
 
    public class MyPlugin implements Plugin {
	    WorkflowContext workflowContext;

	   public void init(WorkflowContext actx) throws Exception {
	 	workflowContext = actx;
    	}

		public int run(ItemCollection workitem,	ItemCollection activity) throws Exception {
			try {
				// you business logic goes here....
			} catch (Exception e) {
				// signal exception....
				return Plugin.PLUGIN_ERROR;
			}
			// signal success 
			return Plugin.PLUGIN_OK;
		}

		public void close(int status) throws Exception {
			try {
				// restore changes?
				if (status == Plugin.PLUGIN_ERROR) {
					// role back....
				}
				if (status == Plugin.PLUGIN_OK) {
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
During the init() phase the WorkflowKernel provides the plug-in with an instance of the WorkflowContext. The WorkflowContext is an abstraction of the workflow environment and provides access to an instance of the [Model interface](./model). The following example illustrates how an instance of a model can be fetched during the initialization of a plugin:

     public void init(WorkflowContext actx) throws Exception {
      ctx=actx;
      Model model=ctx.getModel();
      // your code goes here....
    } 




  