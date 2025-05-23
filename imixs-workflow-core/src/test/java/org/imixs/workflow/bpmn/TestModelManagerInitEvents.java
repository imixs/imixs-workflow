package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;

/**
 * Test class tests complex situations of a Task with several init events.
 * 
 * @author rsoika
 */
public class TestModelManagerInitEvents {
	BPMNModel model = null;
	ModelManager modelManager = null;
	MockWorkflowContext workflowContext;

	@BeforeEach
	public void setup() {
		try {
			workflowContext = new MockWorkflowContext();
			modelManager = new ModelManager(workflowContext);
			workflowContext.loadBPMNModelFromFile("/bpmn/startevent_followup.bpmn");
			model = workflowContext.fetchModel("1.0.0");
			assertNotNull(model);

		} catch (ModelException | PluginException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test the startEvents. We expect 3 Events for Task 1000 : 'init', 'import',
	 * 'save'
	 * 
	 */
	@Test
	public void testStartEvents() {
		try {
			// test start task....
			modelManager.findStartTasks(model, "Simple");
			List<ItemCollection> startEvents = modelManager.findEventsByTask(model, 1000);
			assertNotNull(startEvents);
			assertEquals(3, startEvents.size());

			// we expect event 20 and 10 but not event 30 as a possible Start events
			List<String> names = new ArrayList<String>();
			for (ItemCollection event : startEvents) {
				names.add(event.getItemValueString(BPMNUtil.EVENT_ITEM_NAME));
			}
			assertTrue(names.contains("import"));
			assertTrue(names.contains("init"));
			assertTrue(names.contains("save"));
		} catch (ModelException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test the startTasks
	 * 
	 */
	@Test
	public void testStartTasks() {
		// test start task....
		List<ItemCollection> startTasks;
		try {
			startTasks = modelManager.findStartTasks(model, "Simple");
			assertNotNull(startTasks);
			assertEquals(1, startTasks.size());
			ItemCollection startTask = startTasks.get(0);
			assertEquals("Task 1", startTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));
		} catch (ModelException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test the endTasks
	 * 
	 */
	@Test
	public void testEndTasks() {
		// test start task....
		try {
			List<ItemCollection> endTasks;
			endTasks = modelManager.findEndTasks(model, "Simple");
			assertNotNull(endTasks);
			assertEquals(1, endTasks.size());
			ItemCollection endTask = endTasks.get(0);
			assertEquals("Task 1", endTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));
		} catch (ModelException e) {
			fail(e.getMessage());
		}
	}
}
