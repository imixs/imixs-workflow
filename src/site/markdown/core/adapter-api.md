# Imixs-Workflow Adapter API

The **Imixs-Workflow Adapter API** is an extension mechanism of the Imixs-Workflow engine. An adapter can provide business logic to adapt or extend a process instance. For example, an adapter can exchange data with an external Microservice API. 

In different to the [Plugin API](./plugin-api.html) the Adapter API is bound to a single Event within a BPMN 2.0 model. This allows a more fine grained configuration. 

Adapters are defined as BPMN Signal-Events. The signal definition contains the adapter class name to be called by the Imixs Workflow engine:

<img src="../images/modelling/bpmn_screen_37.png" style="width:700px"/>


 
## The Adapter Interface

The Imixs Adapter-API defines an Interface with the call-back method '_execute_'. This method is called by the WorkflowKernel for each matching signal event.
     
    public ItemCollection execute(ItemCollection document,ItemCollection event) throws AdapterException;
   
    
## CDI Support

The Imixs-Workflow Adapter API also supports CDI. In this way an EJB or Resource can be injected into an adapter class by the corresponding CDI annotation. See the following example:


	public class DemoAdapter implements org.imixs.workflow.Adapter {
	    // inject services...
	    @EJB
	    ModelService modelService;
	    ...
	    @Override
		public ItemCollection execute(ItemCollection document, ItemCollection event) throws AdapterException {
			List<String> versions = modelService.getVersions();
			....
		}
	}

In this example the adapter injects the Imixs ModelService to ask for available model versions. 
 
## Exception Handling
    
An adapter can also extend the processing phase by throwing an _AdapterException_. For example in case of a communication error.

See the following example handling a jax-rs client communication:

		public ItemCollection execute(ItemCollection workitem, ItemCollection event) throws AdapterException {
			...
			// call external Rest API....
			try {
				Response response = client.target(uri).request(MediaType.APPLICATION_XML)
					.post(Entity.entity(data, MediaType.APPLICATION_XML));
			} catch (ResponseProcessingException e) {
				throw new AdapterException(
						MyAdapter.class.getSimpleName(),ERROR_API_COMMUNICATION,"Failed to call rest api!");
			}
			.....
		} 

In this example an Adapter throws an _AdapterException_ when the rest api call failed. The Exception contains the  Adapter name, an Error Code, and a Error Message. The processing life-cycle will not be interrupted by an AdapterException. But the Exception inforration will be added into the current process instance in the following items:


* adapter.error_code - the exception code
* adapter.error_message - the exception message
* adapter.error_cause - the exception cause
* adapter.error_params - optional exception params provided by the AdapterException

These data can be used to control the processing flow. For example a conditional event can evaluate the adapter.error_code to control the outcome of the event. 

Of course, a Plugin can investigate the Adapter Exception data and interrupt the processing life-cycle by throwing a PluginException. In this case a running transaction will be automatically rolled back. 

	...
	public ItemCollection run(ItemCollection documentContext, ItemCollection adocumentActivity)
		throws PluginException {
		....
		if (documentContext.hasItem("adapter.error_code") {
			throw new PluginException(documentContext.getItemValueString("adapter.error_context"),
		 	 documentContext.getItemValueString("adapter.error_code"),
			 documentContext.getItemValueString("adapter.error_message")
			);
		}
	}


If you want to interrupt the processing imideately your Adapter Implementaion can throw a ProcessingErrorException

		public ItemCollection execute(ItemCollection workitem, ItemCollection event) throws AdapterException {
			...
			// call external Rest API....
			try {
				Response response = client.target(uri).request(MediaType.APPLICATION_XML)
					.post(Entity.entity(data, MediaType.APPLICATION_XML));
			} catch (ResponseProcessingException e) {
				// interrupt current transaction
				throw new ProcessingErrorException(
						MyAdapter.class.getSimpleName(),ERROR_API_COMMUNICATION,"Failed to call rest api!");
			}
			.....
		}

In case of a ProcessingErrorException a running transaction will be automatically rolled back because it is a Runtime Exception. 


## StaticAdapter 

The Adapter Interface _org.imixs.workflow.StaticAdapter_ extends the Adapter Interface. A static Adapter is executed independent from the BPMN model. A StaticAdapter should not be associated with a BPMN Signal Event.

StaticAdapters can be uses to execute general business logic.  A StaticAdapter can be
 a CDI implementation.	