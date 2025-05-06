package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Set;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

/**
 * Test class test the Imixs BPMNModel behavior.
 * 
 * @author rsoika
 */
public class TestModelManager {

	BPMNModel model = null;
	ModelManager modelManager = null;
	MockWorkflowContext workflowContext;

	@BeforeEach
	public void setup() {
		try {
			workflowContext = new MockWorkflowContext();
			modelManager = new ModelManager(workflowContext);
			workflowContext.loadBPMNModelFromFile("/bpmn/simple.bpmn");
			model = workflowContext.fetchModel("1.0.0");
			assertNotNull(model);

		} catch (ModelException | PluginException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test the startTasks and startEvents
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testStartTasks() throws ModelException {
		try {
			// find start tasks
			List<ItemCollection> startTasks = modelManager.findStartTasks(model, "Simple");
			assertNotNull(startTasks);
			assertEquals(1, startTasks.size());
			ItemCollection startTask = startTasks.get(0);
			assertNotNull(startTask);
			assertEquals("Task 1", startTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));
		} catch (ModelException e) {
			fail();
		}
	}

	@Test
	public void testStartTasksComplex() throws ModelException {
		try {
			model = BPMNModelFactory.read("/bpmn/simple-startevent.bpmn");
			assertNotNull(model);

			// find start tasks
			List<ItemCollection> startTasks = modelManager.findStartTasks(model, "Simple");
			assertNotNull(startTasks);
			assertEquals(1, startTasks.size());
			ItemCollection startTask = startTasks.get(0);
			assertNotNull(startTask);
			assertEquals("Task 1", startTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));

			List<ItemCollection> events = modelManager.findEventsByTask(model, 1000);
			assertEquals(3, events.size());
		} catch (BPMNModelException e) {
			fail();
		}
	}

	/**
	 * Test the endTasks
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testEndTasks() throws ModelException {
		try {
			// test End task....
			List<ItemCollection> endTasks = modelManager.findEndTasks(model, "Simple");
			assertNotNull(endTasks);
			assertEquals(1, endTasks.size());

			ItemCollection endTask = endTasks.get(0);
			assertEquals("Task 2", endTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));
		} catch (ModelException e) {
			fail();
		}
	}

	/**
	 * Test find events by task
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testFindEvetnsByTasks() throws ModelException {

		// test End task....
		List<ItemCollection> events = modelManager.findEventsByTask(model, 1000);
		assertNotNull(events);
		assertEquals(2, events.size());

	}

	/**
	 * Test the behavior of manipulating event objects.
	 * 
	 * A method can change an attribute of a Event object, but if we reload the
	 * event than the origin value must be returned!
	 * 
	 * @see http://stackoverflow.com/questions/40480/is-java-pass-by-reference-or-pass-by-value#40523
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testModifyEvent() throws ModelException {

		// test event 1000.20
		ItemCollection event = modelManager.findEventByID(model, 1000, 20);

		assertNotNull(event);
		// assertEquals(1100, activity.getItemValueInteger("numNextProcessID"));
		assertEquals("submit", event.getItemValueString("name"));

		// change name of activity....
		event.replaceItemValue("Name", "test");
		assertEquals("test", event.getItemValueString("name"));

		// test activity 1000.10 once again - changes should not have any effect!
		event = modelManager.findEventByID(model, 1000, 20);
		assertNotNull(event);
		assertEquals("submit", event.getItemValueString("name"));

	}

	/**
	 * Test the behavior of manipulating task objects.
	 * 
	 * A method may changes an attribute of a Event object, but if we reload the
	 * event 'model.getEvent()' than the origin value must be returned!
	 * 
	 * @see http://stackoverflow.com/questions/40480/is-java-pass-by-reference-or-pass-by-value#40523
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testModifyTask() throws ModelException {

		// test task 1000
		ItemCollection task = modelManager.findTaskByID(model, 1000);
		assertNotNull(task);
		assertEquals("Task 1", task.getItemValueString("name"));
		assertEquals("Some documentation...", task.getItemValueString("documentation"));
		// change some attributes of task....
		task.replaceItemValue("txtworkflowgroup", "test");
		assertEquals("test", task.getItemValueString("txtworkflowgroup"));
		// test task 1000 once again
		task = modelManager.findTaskByID(model, 1000);
		assertNotNull(task);
		// changes should not have taken effect.
		assertEquals("", task.getItemValueString("txtworkflowgroup"));

	}

	/**
	 * Test the behavior of manipulating the definition.
	 * 
	 * A method may changes an attribute of a Event object, but if we reload the
	 * event 'model.getEvent()' than the origin value must be returned!
	 * 
	 * @see http://stackoverflow.com/questions/40480/is-java-pass-by-reference-or-pass-by-value#40523
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testModifyDefinition() throws ModelException {
		ItemCollection workitem = new ItemCollection();
		workitem.model("1.0.0");

		// test definition
		ItemCollection definition = modelManager.loadDefinition(model);
		assertNotNull(definition);
		assertEquals("1.0.0", definition.getItemValueString("$ModelVersion"));
		// change name of definition....
		definition.replaceItemValue("$ModelVersion", "test");
		assertEquals("test", definition.getItemValueString("$ModelVersion"));
		// test definition once again
		definition = modelManager.loadDefinition(model);
		assertNotNull(definition);
		assertEquals("1.0.0", definition.getItemValueString("$ModelVersion"));
	}

	/**
	 * Test the behavior of manipulating group objects.
	 * 
	 * A method may changes an attribute of a Event object, but if we reload the
	 * event 'model.getEvent()' than the origin value must be returned!
	 * 
	 * @see http://stackoverflow.com/questions/40480/is-java-pass-by-reference-or-pass-by-value#40523
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testModifyGroups() throws ModelException {
		Set<String> groups = modelManager.findAllGroupsByModel(model);
		assertNotNull(groups);
		assertEquals(1, groups.size());
		// add a new group....
		try {
			groups.add("test-group1");
			fail();
		} catch (UnsupportedOperationException e) {
			// we expect a UnsupportedOperationException
			// test groups once again
			groups = modelManager.findAllGroupsByModel(model);
			assertNotNull(groups);
			assertEquals(1, groups.size());
		}
	}

}
