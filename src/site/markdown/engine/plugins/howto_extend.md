# How to Extend The Plugin-API

The **Imixs Plugin-API** is the extension concept of the Imixs-Workflow Engine. You can implement your own plugins to add additional functionality into your workflow application.

## The Abstract Plugin Class

Most plugins provided by the Imixs-Workflow engine are extending the AbstractPlugin class. This class provides a set of convenient methods to access the Imixs-Workflow environment and processing logic. For custom implementation this is a good starting point to subclass from the class:


	public class MyPlugin extends AbstractPlugin {
	
	  @Override
	  public void init(WorkflowContext actx) throws PluginException {
	    // setup ...
	  }
	  
	  @Override
	  public ItemCollection run(ItemCollection workitem, ItemCollection event) throws PluginException {
	    // your code goes here...
	  }
	  
	  @Override
	  public void close(boolean rollbackTransaction) throws PluginException {
	    // tear down...
	  }
	}

Read more about the Imixs Plugin-API in the section [core concepts](../../core/plugin-api.html).

## Helper Methods
   
This abstract plugin class provides a set of helper methods:

 * getWorkflowService - returns an instance of the Workflow Engine
 * replaceDynamicValues - replacing string values
 * formatItemValues - formats a string object depending of an attribute type
 * getCtx - returns the current workflow context

## How to access the WorkflowService EJB from a plugin
Implementing a Imixs-Workflow plugin is an easy way to extend the behavior of the Imixs-Workflow engine. A plugin is a simple Java Class (POJO) which makes it easy to implement. The WorkflowKernel provides a plugin with informations about the environment the plugin runs in. Each plugin class gets an instance of the interface WorkflowContext during the initialization of the plugin. This interface represents the context the plugin currently runs in.  The context depends on the concrete WorkflowManager implementation which is calling a plugin to process a workitem. 
 
If a plugin runs in the Imixs-Workflow Engine, the WorkflowContext is an instance of the  WorkflowService EJB. This behavior makes it easy to access the functionality provided by the WorkflowEngine  directly through a plugin. The following example shows how to access the WorkflowService and ModelService EJBs to determine the latest model version used in the WorkflowInstance: 

	public void init(WorkflowContext actx) throws Exception {
		super.init(actx);
		// check for an instance of WorkflowService
		if (actx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			WorkflowService ws=(WorkflowService)actx;
			// get the model service....
			ModelService modelService=ws.getModelService();
			List<String> versions = modelService.getVersions();
			.....
		}
	}


## CDI Support 
Imixs-Workflow supports CDI for the plugin API. So an EJB or Resource can be injected into a plugin class by the corresponding annotation. See the following example:

	public class DemoPlugin extends AbstractPlugin {
		// inject services...
		@EJB
		ModelService modelService;
		...
	}

## How to lookup a EJB from a plugin
An alternative way to get a reference to an existing EJB in a plugin Class, is to use a JNDI Lookup. The JNDI
Lookup fetches the EJB from the EJB Container provided by the application server. So this way it is possible to get an EJB Instance inside a POJO Class without CDI support. The following example shows how to lookup an EJB during the init() method of a plugin:

	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);

		try {
			InitialContext ictx = new InitialContext();
			Context ctx = (Context) ictx.lookup("java:comp/env");
			myService = (MyService) ctx.lookup("ejb/MyServiceBean");
		} catch (NamingException e) {
			throw new PluginException(this.getClass().getName(), "JNDI_LOOKUP_ERROR", e.getMessage());
		}
	}

In this case a reference to the MyService Interface was created by JNDI Lookup. The Lookup fetches an EJB Reference with the name "ejb/MyServiceBean". To get this Interface returned from the JNDI Context it is necessary to add this reference to the WorkflowService EJB which is calling the plugin. This can be done in the ejb-jar.xml file: 

		<session>
			<ejb-name>WorkflowService</ejb-name>
			<ejb-class>org.imixs.workflow.engine.WorkflowService</ejb-class>
			<session-type>Stateless</session-type>
			<ejb-ref>
			    <ejb-ref-name>ejb/MyServiceBean</ejb-ref-name>
			    <ejb-ref-type>Session</ejb-ref-type>
			    <remote>org.foo.ejb.MyService</remote>
			</ejb-ref>
		</session>


##How to lookup a JDBC Resource from a plugin
Similar to a EJB JNDI Lookup a plugin class can also lookup an existing JDBC Resource  to query a database using SQL statements. The same principle explained in this section is applicable to all resources (such as JMS destinations, JavaMail sessions, and so on). The resource-ref element in the sun-ejb-jar.xml deployment descriptor file maps the JNDI name of a resource reference to the resource-ref element in the ejb-jar.xml J2EE deployment descriptor file. This is similar to a EJB lookup explained before. The resource lookup in the plugin code looks like this:

		public void init(WorkflowContext actx) throws Exception {
			super.init(actx);
			InitialContext ic = new InitialContext();
			String dsName = "java:comp/env/jdbc/HelloDbDs";
			DataSource ds = (javax.sql.DataSource)ic.lookup(dsName);
			Connection connection = ds.getConnection();
			....
			.....
		}

The resource being queried is listed in the res-ref-name element of the ejb-jar.xml file as follows:

	   ....
		<session>
			<ejb-name>WorkflowService</ejb-name>
			<ejb-class>org.imixs.workflow.engine.WorkflowService</ejb-class>
			<session-type>Stateless</session-type>
			
			<!-- JDBC ressource -->
			<resource-ref>
			  <res-ref-name>jdbc/HelloDbDs</res-ref-name>
			  <res-type>javax.sql.DataSource</res-type>
			  <res-auth>Container</res-auth>
			</resource-ref>
	
		</session>
		....

The resource-ref section in a Sun Java System specific deployment descriptor, for example sun-ejb-jar.xml, maps the res-ref-name (the name being queried in the application code) to the JNDI name of the JDBC resource. The JNDI name is the same as the name of the JDBC resource  as defined in the resource file when the resource is created.

	   ....
		<ejb>
			<ejb-name>WorkflowService</ejb-name>
			<jndi-name>ejb/WorkflowService</jndi-name>
			<!-- JDBC ressource -->
			<resource-ref>
			         <res-ref-name>jdbc/HelloDbDs</res-ref-name>
					<jndi-name>jdbc/HelloDbDs</jndi-name>
			</resource-ref>
		</ejb>
		.....
 
The JNDI name in the Sun Java System specific deployment descriptor must match the JNDI name  you assigned to the resource when you created and configured it.
 
 

## What's Next...

Read more about:

 * [The Imixs Plugin-API](../../core/plugin-api.html) 
 * [Exception Handling](exception_handling.html) 
 