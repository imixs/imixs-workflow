package org.imixs.workflow.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

	MockWorkflowContext workflowEngine;

	@BeforeEach
	public void setup() {
		try {

			workflowEngine = new MockWorkflowContext();
		} catch (PluginException e) {
			fail(e.getMessage());
		}
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
		workflowEngine.loadBPMNModelFromFile("/bpmn/link-event-basic.bpmn");

		// Test Environment
		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0").task(1000).event(20);
		try {
			workItem = workflowEngine.processWorkItem(workItem);
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}
		assertEquals(1, workItem.getItemValueInteger("runs"));
		// Test model & taskID
		assertEquals("1.0.0", workItem.getModelVersion());
		assertEquals(1100, workItem.getTaskID());
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
		workflowEngine.loadBPMNModelFromFile("/bpmn/link-event-followup.bpmn");

		// Test Environment
		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0").task(1000).event(20);
		try {
			workItem = workflowEngine.processWorkItem(workItem);
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}
		assertEquals(2, workItem.getItemValueInteger("runs"));
		// Test model & taskID
		assertEquals("1.0.0", workItem.getModelVersion());
		assertEquals(1100, workItem.getTaskID());
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
		workflowEngine.loadBPMNModelFromFile("/bpmn/link-event-complex.bpmn");
		BPMNModel model = workflowEngine.fetchModel("1.0.0");
		assertNotNull(model);
		try {
			Set<? extends BPMNElementNode> endEvents = model.findProcessByName("Simple")
					.findElementNodes(n -> BPMNTypes.END_EVENT.equals(n.getType()));
			// we expect 2 end events
			assertNotNull(endEvents);
			assertEquals(2, endEvents.size());
		} catch (BPMNModelException e) {
			fail(e.getMessage());
		}

		// Test Environment
		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0").task(1000).event(20);
		try {
			workItem = workflowEngine.processWorkItem(workItem);
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}
		assertEquals(1, workItem.getItemValueInteger("runs"));
		// Test model switch to mode 1.0.0
		assertEquals("1.0.0", workItem.getModelVersion());
		assertEquals(1100, workItem.getTaskID());

		// now we do two events....
		workItem = new ItemCollection();
		workItem.model("1.0.0").task(1000).event(40);
		try {
			workItem = workflowEngine.processWorkItem(workItem);
			assertEquals(1, workItem.getItemValueInteger("runs"));
			assertEquals(1200, workItem.getTaskID());
			workItem.event(20);
			workItem = workflowEngine.processWorkItem(workItem);
			assertEquals(2, workItem.getItemValueInteger("runs"));
			assertEquals(1100, workItem.getTaskID());

		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}

	}

}
