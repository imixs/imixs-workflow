package org.imixs.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The Test class TestTemplate can be used as a template to write junit tests.
 * The class uses the {@link WorkflowMockEnvironment} to simulate a workflow
 * environment with a in-memory database.
 * 
 * The {@link WorkflowMockEnvironment} can be used to simulate any scenario with
 * real BPMN model files including the execution of Plugin code.
 * 
 * 
 * @author rsoika
 */
public class TestTemplate {

	protected WorkflowMockEnvironment workflowEnvironment;

	/**
	 * Setup the Mock environment
	 * 
	 * @throws PluginException
	 * @throws ModelException
	 */
	@BeforeEach
	public void setUp() throws PluginException, ModelException {
		workflowEnvironment = new WorkflowMockEnvironment();
		workflowEnvironment.setUp();
		workflowEnvironment.loadBPMNModel("/bpmn/TestWorkflowService.bpmn");
	}

	/**
	 * This test simulates a workflowService process call.
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * @throws ModelException
	 */
	@Test
	public void testProcessSimple() {
		try {
			// load a test workitem
			ItemCollection workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
			workitem.model("1.0.0").task(100).event(10);
			workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
			// expected new task is 200
			assertEquals(200, workitem.getTaskID());
		} catch (AccessDeniedException | ProcessingErrorException | PluginException | ModelException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testConditionalEvent1()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		workflowEnvironment.loadBPMNModel("/bpmn/conditional_event1.bpmn");

		// load test workitem
		ItemCollection workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		workitem.replaceItemValue("_budget", 99);

		workitem.model("1.0.0").task(1000).event(10);
		// test _budget<100 => 1200
		workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
		assertEquals(1200, workitem.getTaskID());

		// test _budget>100 => 1100
		workitem.replaceItemValue("_budget", 9999);
		workitem.model("1.0.0").task(1000).event(10);
		workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
		assertEquals(1100, workitem.getTaskID());

	}

}
