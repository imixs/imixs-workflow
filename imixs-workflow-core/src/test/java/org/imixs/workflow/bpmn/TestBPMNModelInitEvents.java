package org.imixs.workflow.bpmn;

import java.util.ArrayList;
import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

/**
 * Test class tests complex situations of a Task with several init events.
 * 
 * @author rsoika
 */
public class TestBPMNModelInitEvents {

	BPMNModel model = null;
	ModelManager openBPMNModelManager = null;

	@Before
	public void setup() {
		openBPMNModelManager = new ModelManager();
		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/startevent_followup.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			Assert.assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			Assert.fail();
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
			openBPMNModelManager.findStartTasks(model, "Simple");
			List<ItemCollection> startEvents = openBPMNModelManager.findEventsByTask(model, 1000);
			Assert.assertNotNull(startEvents);
			Assert.assertEquals(3, startEvents.size());

			// we expect event 20 and 10 but not event 30 as a possible Start events
			List<String> names = new ArrayList<String>();
			for (ItemCollection event : startEvents) {
				names.add(event.getItemValueString(BPMNUtil.EVENT_ITEM_NAME));
			}
			Assert.assertTrue(names.contains("import"));
			Assert.assertTrue(names.contains("init"));
			Assert.assertTrue(names.contains("save"));
		} catch (BPMNModelException e) {
			Assert.fail(e.getMessage());
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
			startTasks = openBPMNModelManager.findStartTasks(model, "Simple");
			Assert.assertNotNull(startTasks);
			Assert.assertEquals(1, startTasks.size());
			ItemCollection startTask = startTasks.get(0);
			Assert.assertEquals("Task 1", startTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));
		} catch (BPMNModelException e) {
			Assert.fail(e.getMessage());
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
			endTasks = openBPMNModelManager.findEndTasks(model, "Simple");
			Assert.assertNotNull(endTasks);
			Assert.assertEquals(1, endTasks.size());
			ItemCollection endTask = endTasks.get(0);
			Assert.assertEquals("Task 1", endTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));
		} catch (BPMNModelException e) {
			Assert.fail(e.getMessage());
		}
	}
}
