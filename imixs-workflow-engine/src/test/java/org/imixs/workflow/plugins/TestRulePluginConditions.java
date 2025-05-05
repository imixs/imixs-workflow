package org.imixs.workflow.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.MockWorkflowEnvironment;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for WorkflowService
 * 
 * This test verifies conditional events modified by a business rule.
 * 
 * 
 * @author rsoika
 */
public class TestRulePluginConditions {
	public static final String DEFAULT_MODEL_VERSION = "1.0.0";

	ItemCollection event;
	ItemCollection workitem;
	protected MockWorkflowEnvironment workflowEngine;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {
		workflowEngine = new MockWorkflowEnvironment();
		workflowEngine.setUp();
		workflowEngine.loadBPMNModelFromFile("/bpmn/TestRulePluginConditions.bpmn");
		workitem = workflowEngine.getDocumentService().load("W0000-00001");
		workitem.model("1.0.0").task(100);
	}

	/**
	 * This test simulates a workflowService process call.
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * @throws ModelException
	 */
	@Test
	public void testProcessTypeBusinessSimple()
			throws PluginException, ModelException, AccessDeniedException, ProcessingErrorException {

		// test _budget > 100
		ItemCollection workitem = new ItemCollection();
		workitem.model(DEFAULT_MODEL_VERSION).task(1000).event(10);
		workitem.replaceItemValue("_budget", 0);
		workitem = workflowEngine.getWorkflowService().processWorkItem(workitem);

		assertNotNull(workitem);
		// test budget
		assertEquals(0, workitem.getItemValueInteger("_budget"));
		// test conditional event
		assertEquals(1200, workitem.getTaskID());
	}

	/**
	 * This test simulates a workflowService process call.
	 * 
	 * The test validates the update of the _budget item by a business rule in
	 * combination with conditional events.
	 * 
	 * @throws PluginException
	 * @throws ModelException
	 */
	@Test
	public void testProcessTypeBusinessRule()
			throws PluginException, ModelException {

		// test _budget > 100
		ItemCollection workitem = new ItemCollection();
		workitem.model(DEFAULT_MODEL_VERSION).task(1000).event(20);
		workitem.replaceItemValue("_budget", 0);
		workitem = workflowEngine.getWorkflowService().processWorkItem(workitem);

		assertNotNull(workitem);
		// test budget
		assertEquals(500, workitem.getItemValueInteger("_budget"));
		// test conditional event
		assertEquals(1300, workitem.getTaskID());
	}

}
