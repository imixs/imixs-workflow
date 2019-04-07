# Exception Handling 
The Imixs Workflow API prvides a set of Exception classes to signal unexpected
 program situations. For the Plugin API there exists the specific Exception type 'PluginException'. This exception can be used throw an exception in a plugin which can be handled by  a workflow application.See the following example:

	 public class MyPlugin extends AbstractPlugin {
	    ......
	 	@Override
		public ItemCollection run(ItemCollection workitem, ItemCollection event) throws PluginException {
			// some code going wrong.....
			Object[] params={workitem.getItemValueString("_attachments")};
			throw new PluginException(
						MyPlugin.class.getSimpleName(),
						ERROR_ATTACHMENTS_MISSING,
						"Please enter a valid file name",params);
			.....
		} 
		....
	}

In this example a Plugin throws a _PluginException_. The Exception contains the  Plugin name, an Error Code, a Error Message and an optional array of params.  The optional params can be used by an application to provide additional information to the user. 
 
## Handling PluginExceptions in JSF
This is an example how an application can handle a Plugin Exception and create a JSF Faces Message based on the information of the PluginException:
 
	
	@Named("workflowController")
	@SessionScoped
	public class WorkflowController extends
			org.imixs.workflow.jee.faces.workitem.WorkflowController implements
			Serializable {
		 .....
		 /**
		 * The action method processes the current workItem and handles PluginExceptions.
		 */
		@Override
		public String process() throws AccessDeniedException,
				ProcessingErrorException {
			// process workItem and catch exceptions
			try {
				actionResult = super.process();
			} catch (PluginException pe) {
	
				String message = pe.getErrorCode();
				// try to find the message text in resource bundle...
				try {
					// try to translage the error message code into a readable message text
					ResourceBundle rb = ResourceBundle.getBundle("bundle.app");
					message = rb.getString(pe.getErrorCode());
				} catch (MissingResourceException mre) {
					logger.warning("WorkflowController: " + mre.getMessage());
				}
				// add global message
				FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, message, null));
			} 
	 ....
	}
 
This example catches a PluginExcpetion and add a global FacesMessage. The message can be displayed in a jsf page using the messages tag:
 
    <h:messages globalOnly="true" />
 	
The attribute 'globalOnly' indicates to display global messages not bound to a specific JSF input element.	