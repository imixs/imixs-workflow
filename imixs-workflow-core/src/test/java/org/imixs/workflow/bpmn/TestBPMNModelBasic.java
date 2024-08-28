package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
	OpenBPMNModelManager openBPMNModelManager = null;

	@Before
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new OpenBPMNModelManager();
		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/simple.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			Assert.assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			Assert.fail();
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
			Assert.assertNotNull(startTasks);
			Assert.assertEquals(1, startTasks.size());
			ItemCollection startTask = startTasks.get(0);
			Assert.assertNotNull(startTask);
			Assert.assertEquals("Task 1", startTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));
		} catch (BPMNModelException e) {
			Assert.fail();
		}
	}

	@Test
	public void testStartTasksComplex() throws ModelException {
		try {

			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/simple-startevent.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			Assert.assertNotNull(model);

			// find start tasks
			List<ItemCollection> startTasks = openBPMNModelManager.findStartTasks(model, "Simple");
			Assert.assertNotNull(startTasks);
			Assert.assertEquals(1, startTasks.size());
			ItemCollection startTask = startTasks.get(0);
			Assert.assertNotNull(startTask);
			Assert.assertEquals("Task 1", startTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));

			List<ItemCollection> events = openBPMNModelManager.findEventsByTask(model, 1000);
			Assert.assertEquals(3, events.size());
		} catch (BPMNModelException e) {
			Assert.fail();
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
			Assert.assertNotNull(endTasks);
			Assert.assertEquals(1, endTasks.size());

			ItemCollection endTask = endTasks.get(0);
			Assert.assertEquals("Task 2", endTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));
		} catch (BPMNModelException e) {
			Assert.fail();
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
		Assert.assertNotNull(events);
		Assert.assertEquals(2, events.size());

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

		Assert.assertNotNull(event);
		// Assert.assertEquals(1100, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("submit", event.getItemValueString("name"));

		// change name of activity....
		event.replaceItemValue("Name", "test");
		Assert.assertEquals("test", event.getItemValueString("name"));

		// test activity 1000.10 once again - changes should not have any effect!
		event = openBPMNModelManager.findEventByID(model, 1000, 20);
		Assert.assertNotNull(event);
		Assert.assertEquals("submit", event.getItemValueString("name"));

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

		Assert.assertNotNull(task);
		Assert.assertEquals("Task 1", task.getItemValueString("name"));
		Assert.assertEquals("Some documentation...", task.getItemValueString("documentation"));

		// change some attributes of task....
		task.replaceItemValue("txtworkflowgroup", "test");
		Assert.assertEquals("test", task.getItemValueString("txtworkflowgroup"));

		// test task 1000 once again
		task = openBPMNModelManager.findTaskByID(model, 1000);
		Assert.assertNotNull(task);
		// changes should not have taken effect.
		Assert.assertEquals("", task.getItemValueString("txtworkflowgroup"));

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

		Assert.assertNotNull(definition);
		Assert.assertEquals("1.0.0", definition.getItemValueString("$ModelVersion"));

		// change name of definition....
		definition.replaceItemValue("$ModelVersion", "test");
		Assert.assertEquals("test", definition.getItemValueString("$ModelVersion"));

		// test definition once again
		definition = openBPMNModelManager.loadDefinition(model);
		Assert.assertNotNull(definition);
		Assert.assertEquals("1.0.0", definition.getItemValueString("$ModelVersion"));

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

		Set<String> groups = openBPMNModelManager.findAllGroups(model);
		Assert.assertNotNull(groups);
		Assert.assertEquals(1, groups.size());
		// add a new group....
		groups.add("test-group1");
		Assert.assertEquals(2, groups.size());
		// test groups once again
		groups = openBPMNModelManager.findAllGroups(model);
		Assert.assertNotNull(groups);
		Assert.assertEquals(1, groups.size());

	}

}