package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

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
public class TestBPMNModelSimple {

	protected BPMNModel model = null;

	@Before
	public void setUp() throws ParseException, ParserConfigurationException, SAXException, IOException {

		try {
			model = BPMNModelFactory.read("/bpmn/simple.bpmn");
		} catch (BPMNModelException e) {
			e.printStackTrace();
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
		Assert.assertNotNull(model);

		// test start task....
		List<ItemCollection> startTasks = model.getStartTasks();
		Assert.assertNotNull(startTasks);
		Assert.assertEquals(1, startTasks.size());

		ItemCollection startTask = startTasks.get(0);
		Assert.assertEquals("Task 1", startTask.getItemValueString("txtname"));

		// get start events
		List<ItemCollection> startEvents = model.getStartEvents(startTask.getItemValueInteger("numProcessID"));
		Assert.assertEquals(2, startEvents.size());

	}

	/**
	 * Test the endTasks
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testEndTasks() throws ModelException {
		Assert.assertNotNull(model);

		// test start task....
		List<ItemCollection> endTasks = model.getEndTasks();
		Assert.assertNotNull(endTasks);
		Assert.assertEquals(1, endTasks.size());

		ItemCollection endTask = endTasks.get(0);
		Assert.assertEquals("Task 2", endTask.getItemValueString("txtname"));

	}

	/**
	 * Test the behavior of manipulating event objects.
	 * 
	 * A method may changes an attribute of a Event object, but if we reload the
	 * event than the origin value must be returned!
	 * 
	 * @see http://stackoverflow.com/questions/40480/is-java-pass-by-reference-or-pass-by-value#40523
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testModifyEvent() throws ModelException {
		// test activity 1000.10 submit
		ItemCollection activity = OpenBPMNUtil.findEventByID(model, 1000, 20);

		Assert.assertNotNull(activity);
		// Assert.assertEquals(1100, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("submit", activity.getItemValueString("name"));

		// change name of activity....
		activity.replaceItemValue("Name", "test");
		Assert.assertEquals("test", activity.getItemValueString("name"));

		// test activity 1000.10 once again - changes should not have any effect!
		activity = OpenBPMNUtil.findEventByID(model, 1000, 20);
		Assert.assertNotNull(activity);
		Assert.assertEquals("submit", activity.getItemValueString("name"));

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

		Assert.assertNotNull(model);

		// test task 1000
		ItemCollection task = OpenBPMNUtil.findTaskByID(model, 1000);
		Assert.assertNotNull(task);
		Assert.assertEquals("Task 1", task.getItemValueString("name"));
		Assert.assertEquals("Some documentation...", task.getItemValueString("documentation"));

		// change some attributes of task....
		task.replaceItemValue("txtworkflowgroup", "test");
		Assert.assertEquals("test", task.getItemValueString("txtworkflowgroup"));

		// test task 1000 once again
		task = OpenBPMNUtil.findTaskByID(model, 1000);
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

		Assert.assertNotNull(model);

		// test definition
		ItemCollection definition = OpenBPMNUtil.findDefinition(model);
		Assert.assertNotNull(definition);
		Assert.assertEquals("1.0.0", definition.getItemValueString("$ModelVersion"));

		// change name of definition....
		definition.replaceItemValue("$ModelVersion", "test");
		Assert.assertEquals("test", definition.getItemValueString("$ModelVersion"));

		// test definition once again
		definition = OpenBPMNUtil.findDefinition(model);
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

		Assert.assertNotNull(model);

		// test groups
		List<String> groups = model.getGroups();
		Assert.assertNotNull(groups);
		Assert.assertEquals(1, groups.size());

		// add a new group....
		groups.add("test-group1");
		Assert.assertEquals(2, groups.size());

		// test groups once again
		groups = model.getGroups();
		Assert.assertNotNull(groups);
		Assert.assertEquals(1, groups.size());

	}

	@Test
	public void testModifyFindAllEventsByTask() throws ModelException {
		Assert.assertNotNull(model);
		// test tasks
		List<ItemCollection> events = model.findAllEventsByTask(1000);

		// test activity 1000.10 submit
		ItemCollection activity = events.get(0);
		Assert.assertNotNull(activity);
		Assert.assertEquals(1000, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("update", activity.getItemValueString("txtname"));

		// change name of activity....
		activity.replaceItemValue("txtName", "test");
		Assert.assertEquals("test", activity.getItemValueString("txtname"));

		// test activity 1000.10 once again
		events = model.findAllEventsByTask(1000);
		activity = events.get(0);
		Assert.assertNotNull(activity);
		Assert.assertEquals(1000, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("update", activity.getItemValueString("txtname"));

	}

	// @Test
	public void testModifyFindTasksByGroup() throws ModelException {

		Assert.assertNotNull(model);

		// test task 1000
		List<ItemCollection> tasks = model.findTasksByGroup("Simple");
		ItemCollection task = tasks.get(0);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Simple", task.getItemValueString("txtworkflowgroup"));

		// change name of task....
		task.replaceItemValue("txtworkflowgroup", "test");
		Assert.assertEquals("test", task.getItemValueString("txtworkflowgroup"));
		tasks.set(0, task);

		// test task 1000 once again
		tasks = model.findTasksByGroup("Simple");
		task = tasks.get(0);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Simple", task.getItemValueString("txtworkflowgroup"));

	}

	// @Test
	// public void testModifyFindAllTasks() throws ModelException {

	// Assert.assertNotNull(model);

	// // test task 1000
	// List<ItemCollection> tasks = model.findAllTasks();
	// ItemCollection task = tasks.get(0);
	// Assert.assertNotNull(task);
	// Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
	// Assert.assertEquals("Simple", task.getItemValueString("txtworkflowgroup"));

	// // change name of task....
	// task.replaceItemValue("txtworkflowgroup", "test");
	// Assert.assertEquals("test", task.getItemValueString("txtworkflowgroup"));

	// // test task 1000 once again
	// tasks = model.findAllTasks();
	// task = tasks.get(0);
	// Assert.assertNotNull(task);
	// Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
	// Assert.assertEquals("Simple", task.getItemValueString("txtworkflowgroup"));

	// }

}
