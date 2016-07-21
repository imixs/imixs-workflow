package org.imixs.workflow.jee.ejb;

import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.plugins.TestApplicationPlugin;
import org.junit.Before;

/**
 * Abstract base class for jUnit tests using the WorkflowService.
 * 
 * This test class mocks the classes WorkflowContext, WorkflowService,
 * EntityService and ModelService. The test class generates a test database with
 * process entities and activity entities which can be accessed from a plug-in
 * or the workflowKernel.
 * 
 * A JUnit Test can save, load and process workitems.
 * 
 * JUnit tests can also manipulate the model by changing entities through
 * calling the methods:
 * 
 * getActivityEntity,setActivityEntity,getProcessEntity,setProcessEntity
 * 
 * 
 * @version 2.0
 * @see TestApplicationPlugin
 * @author rsoika
 */
public class AbstractPluginTest extends AbstractWorkflowEnvironment {
	
	@Before
	public void setup() throws PluginException {
		super.setup();
	}
}
