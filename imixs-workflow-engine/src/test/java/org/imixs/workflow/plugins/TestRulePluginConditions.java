package org.imixs.workflow.plugins;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.ModelPluginMock;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.ResultPlugin;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class for WorkflowService
 * 
 * This test verifies conditional events modified by a business rule.
 * 
 * 
 * @author rsoika
 */
public class TestRulePluginConditions {
	ResultPlugin resultPlugin = null;
	public static final String DEFAULT_MODEL_VERSION = "1.0.0";
	
	WorkflowMockEnvironment workflowMockEnvironment;

	@Before
	public void setup() throws PluginException, ModelException {

		workflowMockEnvironment = new WorkflowMockEnvironment();
		workflowMockEnvironment.setModelPath("/bpmn/TestRulePluginConditions.bpmn");

		workflowMockEnvironment.setup();

		resultPlugin = new ResultPlugin();
		try {
			resultPlugin.init(workflowMockEnvironment.getWorkflowService());
		} catch (PluginException e) {

			e.printStackTrace();
		}
		try {
			workflowMockEnvironment.getModelService().addModel(new ModelPluginMock(workflowMockEnvironment.getModel(),
					"org.imixs.workflow.engine.plugins.ApplicationPlugin","org.imixs.workflow.engine.plugins.RulePlugin"));
		} catch (ModelException e) {
			e.printStackTrace();
		}
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

		workitem = workflowMockEnvironment.processWorkItem(workitem);

		Assert.assertNotNull(workitem);
		// test budget
		Assert.assertEquals(0, workitem.getItemValueInteger("_budget"));
		// test conditional event
		Assert.assertEquals(1200, workitem.getTaskID());
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

		workitem = workflowMockEnvironment.processWorkItem(workitem);

		Assert.assertNotNull(workitem);
		// test budget
		Assert.assertEquals(500, workitem.getItemValueInteger("_budget"));
		// test conditional event
		Assert.assertEquals(1300, workitem.getTaskID());
	}

}
