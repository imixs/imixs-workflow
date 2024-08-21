package org.imixs.workflow.kernel;

import static org.junit.Assert.assertTrue;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowEngine;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbpmn.bpmn.BPMNModel;

/**
 * Test class test the Imixs BPMNParser with special start event case including
 * a follow up
 * 
 * @author rsoika
 */
public class TestBPMNStartEventWithFollowUp {

	private BPMNModel model;
	private MockWorkflowEngine workflowEngine;

	@Before
	public void setup() throws PluginException {
		workflowEngine = new MockWorkflowEngine();
		workflowEngine.loadBPMNModel("/bpmn/startevent_followup.bpmn");
		try {
			model = workflowEngine.getModelManager().getModel("1.0.0");
			Assert.assertNotNull(model);
		} catch (ModelException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * This test calls a start event with a follow up event
	 */
	@Test
	public void testFollowUpStartEvent() {

		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0")
				.task(1000)
				.event(20);
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			Assert.assertEquals(2, workItem.getItemValueInteger("runs"));
			Assert.assertEquals(1000, workItem.getTaskID());

		} catch (ModelException | ProcessingErrorException | PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/**
	 * This test calls a init event with a follow up event
	 */
	@Test
	public void testFollowUpInit() {

		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0")
				.task(1000)
				.event(40);
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			Assert.assertEquals(2, workItem.getItemValueInteger("runs"));
			Assert.assertEquals(1000, workItem.getTaskID());

		} catch (ModelException | ProcessingErrorException | PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/**
	 * This test calls invalid follow up event from a start event
	 * 
	 */
	@Test
	public void testInvalidEventCall() {

		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0")
				.task(1000)
				.event(30);
		try {
			// it should not be possible to call event 30!
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			Assert.fail();
		} catch (ProcessingErrorException | PluginException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (ModelException e) {
			e.printStackTrace();
			// expected!
			assertTrue(e.getMessage().contains("1000.30 is not a callable in model"));
		}
	}

}
