# CDI Support 

Imixs-Workflow supports CDI for all service components which makes it easy to tie different services together. 
For example an bean or a Resource can be easily injected into a plugin class by the corresponding annotation. See the following example injecting the `ModelService`:

```java
	public class DemoPlugin extends AbstractPlugin {
		// inject services...
		@Inject
		ModelService modelService;
		...
	}
```

# EJB JNDI Lookup

In some situations where CDI does not work, it can be necessary to fetch a Service EJB by a JNDI lookup.   
The following Example shows a JNDI Lookup for an external Service EJB from the WorkflowService:
 
```java
	MyServiceBean myService;
	try {
		String ejbName="ejb/MyServiceBean";
		InitialContext ic = new InitialContext();
		myService = (MyServiceBean) ic.lookup(ejbName);
	} catch (Exception e) {
		e.printStackTrace();
		workflowService = null;
	}
```

The JNDI Name is defined by the application server. To lookup a EJB or resource by JNDI name, the name need to be configured in ejb-jar.xml. The following example shows the configuration for wildfly:
 

	...
        <session>
			<ejb-name>WorkflowService</ejb-name>
			<ejb-class>org.imixs.workflow.engine.WorkflowService</ejb-class>
			<session-type>Stateless</session-type>
			....			
			<!-- MyServiceBean -->
			<ejb-ref>
                    <ejb-ref-name>ejb/MyServiceBean</ejb-ref-name>
                    <ejb-ref-type>Session</ejb-ref-type>
                    <remote>org.foo.MyServiceBean</remote>
           </ejb-ref>
        	....
		</session>
	</enterprise-beans>
	...




	
	
	
	
 


    
 