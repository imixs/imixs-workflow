package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;
import org.xml.sax.SAXException;

/**
 * Test class test the Imixs BPMNModel behavior.
 * 
 * @author rsoika
 */
public class TestBPMNModelBasic {

	BPMNModel model = null;
	ModelManager openBPMNModelManager = null;

	@BeforeEach
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new ModelManager();
		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/simple.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
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
			List<ItemCollection> startTasks = openBPMNModelManager.findStartTasks(model, "Simple");
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

			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/simple-startevent.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			assertNotNull(model);

			// find start tasks
			List<ItemCollection> startTasks = openBPMNModelManager.findStartTasks(model, "Simple");
			assertNotNull(startTasks);
			assertEquals(1, startTasks.size());
			ItemCollection startTask = startTasks.get(0);
			assertNotNull(startTask);
			assertEquals("Task 1", startTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));

			List<ItemCollection> events = openBPMNModelManager.findEventsByTask(model, 1000);
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
			List<ItemCollection> endTasks = openBPMNModelManager.findEndTasks(model, "Simple");
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
		List<ItemCollection> events = openBPMNModelManager.findEventsByTask(model, 1000);
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
		ItemCollection event = openBPMNModelManager.findEventByID(model, 1000, 20);

		assertNotNull(event);
		// assertEquals(1100, activity.getItemValueInteger("numNextProcessID"));
		assertEquals("submit", event.getItemValueString("name"));

		// change name of activity....
		event.replaceItemValue("Name", "test");
		assertEquals("test", event.getItemValueString("name"));

		// test activity 1000.10 once again - changes should not have any effect!
		event = openBPMNModelManager.findEventByID(model, 1000, 20);
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
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 1000);
		assertNotNull(task);
		assertEquals("Task 1", task.getItemValueString("name"));
		assertEquals("Some documentation...", task.getItemValueString("documentation"));
		// change some attributes of task....
		task.replaceItemValue("txtworkflowgroup", "test");
		assertEquals("test", task.getItemValueString("txtworkflowgroup"));
		// test task 1000 once again
		task = openBPMNModelManager.findTaskByID(model, 1000);
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
		ItemCollection definition = openBPMNModelManager.loadDefinition(model);
		assertNotNull(definition);
		assertEquals("1.0.0", definition.getItemValueString("$ModelVersion"));
		// change name of definition....
		definition.replaceItemValue("$ModelVersion", "test");
		assertEquals("test", definition.getItemValueString("$ModelVersion"));
		// test definition once again
		definition = openBPMNModelManager.loadDefinition(model);
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
		Set<String> groups = openBPMNModelManager.findAllGroupsByModel(model);
		assertNotNull(groups);
		assertEquals(1, groups.size());
		// add a new group....
		try {
			groups.add("test-group1");
			fail();
		} catch (UnsupportedOperationException e) {
			// we expect a UnsupportedOperationException
			// test groups once again
			groups = openBPMNModelManager.findAllGroupsByModel(model);
			assertNotNull(groups);
			assertEquals(1, groups.size());
		}
	}

	/**
	 * Test case testing a kind of corrupted model file
	 * 
	 * Model Manager is unable to load event 20
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testRecursiveBug() throws ModelException {
		try {

			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/recursive_issue1.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			assertNotNull(model);

			ItemCollection workitem = new ItemCollection().model("1.0.0").task(100).event(10);
			assertEquals(3, model.findAllEvents().size());
			ItemCollection event = openBPMNModelManager.loadEvent(workitem);
			assertNotNull(event);

		} catch (BPMNModelException e) {
			fail();
		}
	}

}
