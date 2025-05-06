package org.imixs.workflow.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;

/**
 * This test class tests an event that switches the model version.
 * 
 * ===> model-switch-source-1.0.0 ===> model-switch-target-1.0.0
 * 
 * 
 * This test verifies switching by the exact model version as also by regex.
 * 
 * @author rsoika
 */
public class TestBPMNModelSwitchEvent {

	MockWorkflowContext workflowEngine;

	@BeforeEach
	public void setup() {
		try {
			workflowEngine = new MockWorkflowContext();
			workflowEngine.loadBPMNModelFromFile("/bpmn/model-switch-source.bpmn");
			workflowEngine.loadBPMNModelFromFile("/bpmn/model-switch-target.bpmn");

		} catch (PluginException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test a model switch by an exact version, task and event definition.
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testSimpleSwitch() throws ModelException {

		BPMNModel model = workflowEngine.fetchModel("source-1.0.0");
		assertNotNull(model);

		// Test Environment
		ItemCollection workItem = new ItemCollection();
		workItem.model("source-1.0.0").task(1000).event(20);
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}
		assertEquals(2, workItem.getItemValueInteger("runs"));

		// Test model switch to mode target-1.0.0
		assertEquals("target-1.0.0", workItem.getModelVersion());
		assertEquals(1100, workItem.getTaskID());

	}

	/**
	 * Test a model switch by a regex expression for a version, task and event
	 * definition.
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testRegexSwitch() throws ModelException {

		// load test models
		workflowEngine.loadBPMNModelFromFile("/bpmn/link-event-basic.bpmn");
		BPMNModel model = workflowEngine.fetchModel("source-1.0.0");
		assertNotNull(model);

		// Test Environment
		ItemCollection workItem = new ItemCollection();
		workItem.model("source-1.0.0").task(1000).event(21);
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}
		assertEquals(2, workItem.getItemValueInteger("runs"));

		// Test model switch to mode target-1.0.0
		assertEquals("target-1.0.0", workItem.getModelVersion());
		assertEquals(1100, workItem.getTaskID());

	}

}
