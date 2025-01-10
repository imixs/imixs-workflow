package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

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
public class TestBPMNModelBasicMultipleTasktypes {

	BPMNModel model = null;
	ModelManager openBPMNModelManager = null;

	@BeforeEach
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new ModelManager();
		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/simple-multiple-tasktypes.bpmn"));
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
			assertEquals("Task-9", endTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));
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
	 * Test the usage of multiple task types
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testMultipleTaskTypes() throws ModelException {
		try {

			// find start tasks
			List<ItemCollection> allTasks = openBPMNModelManager.findTasks(model, "Simple");
			assertNotNull(allTasks);
			assertEquals(8, allTasks.size());

		} catch (ModelException e) {
			fail();
		}
	}
}
