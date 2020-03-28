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


## CDI Support 
Imixs-Workflow supports CDI for the plugin API. So an EJB or Resource can be injected into a plugin class by the corresponding annotation. See the following example:

	public class DemoPlugin extends AbstractPlugin {
		// inject services...
		@EJB
		ModelService modelService;
		...
	}



## How to lookup a JDBC Resource from a plugin
A plugin class can also lookup an existing JDBC Resource via a jndi lookup, for example to query a database using SQL statements. The same principle explained in this section is applicable to all resources (such as JMS destinations, JavaMail sessions, Remote EJBs and so on). The resource-ref element in the ejb-jar.xml deployment descriptor file maps the JNDI name of a resource reference to the resource-ref element in the ejb-jar.xml deployment descriptor file. This is similar to a EJB lookup explained before. The resource lookup in the plugin code looks like this:

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
 