
#EJB Remote Lookup
The following Example shows a remote Lookup for the Imixs EntityService Bean in a java client application
 
	WorkflowServiceRemote	workflowService;
	try {
		String ejbName="ejb/MyImixsWorkflowServiceBean";
		InitialContext ic = new InitialContext();
		workflowService = (WorkflowServiceRemote) ic.lookup(ejbName);
	} catch (Exception e) {
		e.printStackTrace();
		workflowService = null;
	}

the Remote JNDI Name is defined by the remote application. This can be different depending on the JEE application server platform the following example shows the configuration for glassfish in a glassfish-ejb-jar.xml deployment descriptor.
 
	...
	<enterprise-beans>
			<ejb>
				<ejb-name>WorkflowServiceBean</ejb-name>
				<jndi-name> ejb/MyImixsEntityServiceBean</jndi-name>
			</ejb>
	</enterprise-beans>
	...




	
	
	
	
 


    
 