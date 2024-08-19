package org.imixs.workflow.kernel;

import java.util.Set;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowEngine;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNModelException;

/**
 * Test class to test different follow up events
 * 
 * @author rsoika
 */
public class TestBPMNLinkEvent {

	private MockWorkflowEngine workflowEngine;

	@Before
	public void setup() throws PluginException {
		workflowEngine = new MockWorkflowEngine();
	}

	/**
	 * This simple link event
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testLinkEventBasic()
			throws ModelException {

		// load test models
		workflowEngine.loadBPMNModel("/bpmn/link-event-basic.bpmn");
		BPMNModel model = workflowEngine.getModelManager().getModel("1.0.0");
		Assert.assertNotNull(model);

		// Test Environment
		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0").task(1000).event(20);
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertEquals(1, workItem.getItemValueInteger("runs"));
		// Test model & taskID
		Assert.assertEquals("1.0.0", workItem.getModelVersion());
		Assert.assertEquals(1100, workItem.getTaskID());
	}

	/**
	 * This link event with an followup event
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testLinkEventFollowup()
			throws ModelException {

		// load test models
		workflowEngine.loadBPMNModel("/bpmn/link-event-followup.bpmn");
		BPMNModel model = workflowEngine.getModelManager().getModel("1.0.0");
		Assert.assertNotNull(model);

		// Test Environment
		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0").task(1000).event(20);
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertEquals(2, workItem.getItemValueInteger("runs"));
		// Test model & taskID
		Assert.assertEquals("1.0.0", workItem.getModelVersion());
		Assert.assertEquals(1100, workItem.getTaskID());
	}

	/**
	 * This test test intermediate link events with a more complex follow up event
	 * situation
	 * 
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testLinkEventComplex() throws ModelException {

		// load test models
		workflowEngine.loadBPMNModel("/bpmn/link-event-complex.bpmn");
		BPMNModel model = workflowEngine.getModelManager().getModel("1.0.0");
		Assert.assertNotNull(model);
		try {
			Set<? extends BPMNElementNode> endEvents = model.findProcessByName("Simple")
					.findElementNodes(n -> BPMNTypes.END_EVENT.equals(n.getType()));
			// we expect 2 end events
			Assert.assertNotNull(endEvents);
			Assert.assertEquals(2, endEvents.size());
		} catch (BPMNModelException e) {
			Assert.fail(e.getMessage());
		}

		// Test Environment
		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0").task(1000).event(20);
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertEquals(1, workItem.getItemValueInteger("runs"));
		// Test model switch to mode 1.0.0
		Assert.assertEquals("1.0.0", workItem.getModelVersion());
		Assert.assertEquals(1100, workItem.getTaskID());

		// now we do two events....
		workItem = new ItemCollection();
		workItem.model("1.0.0").task(1000).event(40);
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			Assert.assertEquals(1, workItem.getItemValueInteger("runs"));
			Assert.assertEquals(1200, workItem.getTaskID());
			workItem.event(20);
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			Assert.assertEquals(2, workItem.getItemValueInteger("runs"));
			Assert.assertEquals(1100, workItem.getTaskID());

		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

	}

}
