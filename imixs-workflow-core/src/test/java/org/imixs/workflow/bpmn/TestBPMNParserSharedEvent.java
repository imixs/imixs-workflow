package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

/**
 * Test class to test the behavior of the ModelManager.
 * 
 * Test class test shared events (one event used
 * by two different task elements)
 * 
 * @author rsoika
 */
public class TestBPMNParserSharedEvent {

	BPMNModel model = null;
	ModelManager modelManager = null;
	MockWorkflowContext workflowContext;

	@BeforeEach
	public void setup() {
		try {
			workflowContext = new MockWorkflowContext();
			modelManager = new ModelManager(workflowContext);
		} catch (PluginException e) {
			fail(e.getMessage());
		}

	}

	/**
	 * Simple shared event
	 * 
	 */
	@Test
	public void testSharedEvent() {

		try {
			model = BPMNModelFactory.read("/bpmn/shared_event1.bpmn");
			assertNotNull(model);
		} catch (BPMNModelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// test task 1000
		ItemCollection task = modelManager.findTaskByID(model, 1000);
		assertNotNull(task);

		// test activity for task 1000
		List<ItemCollection> events = modelManager.findEventsByTask(model, 1000);
		assertNotNull(events);
		assertEquals(2, events.size());

		// test activity for task 1100
		events = modelManager.findEventsByTask(model, 1100);
		assertNotNull(events);
		assertEquals(1, events.size());

	}

	/**
	 * Shared event with an follow up event
	 * 
	 */
	@Test
	public void testSharedEventWithFollowUp() {
		try {
			model = BPMNModelFactory.read("/bpmn/shared_event2.bpmn");
			assertNotNull(model);
		} catch (BPMNModelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// test task 2000
		ItemCollection task = modelManager.findTaskByID(model, 2000);
		assertNotNull(task);
		List<ItemCollection> events = modelManager.findEventsByTask(model, 2000);
		assertNotNull(events);
		assertEquals(2, events.size());

		// test activity for task 2100
		events = modelManager.findEventsByTask(model, 2100);
		assertNotNull(events);
		assertEquals(1, events.size());

	}

	/**
	 * Shared event with an follow up event
	 * 
	 */
	@Test
	public void testSharedLinkedEventWithFollowUp() {

		try {
			model = BPMNModelFactory.read("/bpmn/shared_event3.bpmn");
			assertNotNull(model);
		} catch (BPMNModelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		// test task 3000
		ItemCollection task = modelManager.findTaskByID(model, 3000);
		assertNotNull(task);
		List<ItemCollection> events = modelManager.findEventsByTask(model, 3000);
		assertNotNull(events);
		assertEquals(2, events.size());

		// test task 3100
		task = modelManager.findTaskByID(model, 3100);
		assertNotNull(task);
		events = modelManager.findEventsByTask(model, 3100);
		assertNotNull(events);
		assertEquals(1, events.size());

		// test task 3200
		task = modelManager.findTaskByID(model, 3200);
		assertNotNull(task);
		events = modelManager.findEventsByTask(model, 3200);
		assertNotNull(events);
		assertEquals(0, events.size());

	}

	/**
	 * Shared event with a direct follow up event
	 * 
	 */
	@Test
	public void testSharedEvent_Case4() {

		try {
			model = BPMNModelFactory.read("/bpmn/shared_event4.bpmn");
			assertNotNull(model);
		} catch (BPMNModelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// test task 2000
		ItemCollection task = modelManager.findTaskByID(model, 2000);
		assertNotNull(task);
		List<ItemCollection> events = modelManager.findEventsByTask(model, 2000);
		assertNotNull(events);
		// there are 3 events....
		assertEquals(2, events.size());
		assertNotNull(modelManager.findEventByID(model, 2000, 10)); // save
		assertNotNull(modelManager.findEventByID(model, 2000, 90)); // follow up

		// test task 2100
		task = modelManager.findTaskByID(model, 2100);
		assertNotNull(task);
		events = modelManager.findEventsByTask(model, 2100);
		assertNotNull(events);
		// there are no events....
		assertEquals(0, events.size());

	}

	/**
	 * Shared event with a direct follow up event
	 * 
	 */
	@Test
	public void testSharedEvent_Case5() {

		try {
			model = BPMNModelFactory.read("/bpmn/shared_event5.bpmn");
			assertNotNull(model);
		} catch (BPMNModelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// test task 2000
		ItemCollection task = modelManager.findTaskByID(model, 2000);
		assertNotNull(task);
		List<ItemCollection> events = modelManager.findEventsByTask(model, 2000);
		assertNotNull(events);
		// there are 3 events....
		assertEquals(2, events.size());
		assertNotNull(modelManager.findEventByID(model, 2000, 10)); // save
		assertNotNull(modelManager.findEventByID(model, 2000, 90)); // follow up

		// test task 2100
		task = modelManager.findTaskByID(model, 2100);
		assertNotNull(task);
		events = modelManager.findEventsByTask(model, 2100);
		assertNotNull(events);
		// there are 2 events....
		assertEquals(1, events.size());
		assertNotNull(modelManager.findEventByID(model, 2100, 80)); // archive

	}

}
