package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import junit.framework.Assert;

/**
 * Test class test the Imixs BPMNParser with special start event case including
 * a follow up
 * 
 * @author rsoika
 */
public class TestBPMNParserStartEventFollowUp {

	protected BPMNModel model = null;
	
	@Before
	public void setUp() throws ParseException, ParserConfigurationException, SAXException, IOException {
		InputStream inputStream = getClass().getResourceAsStream("/bpmn/startevent_followup.bpmn");

		try {
			model = BPMNParser.parseModel(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void testSimple()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		String VERSION = "1.0.0";

	
		Assert.assertNotNull(model);

		// Test Environment
		ItemCollection profile = model.getDefinition();
		Assert.assertNotNull(profile);
		Assert.assertEquals("environment.profile", profile.getItemValueString("txtname"));
		Assert.assertEquals("WorkflowEnvironmentEntity", profile.getItemValueString("type"));
		Assert.assertEquals(VERSION, profile.getItemValueString("$ModelVersion"));

		Assert.assertTrue(model.getGroups().contains("Simple"));

		// test count of elements
		Assert.assertEquals(1, model.findAllTasks().size());

		// test import activity for task 1000.20 (import)
		ItemCollection activity = model.getEvent(1000, 20);
		Assert.assertNotNull(activity);
		Assert.assertEquals("import", activity.getItemValueString("txtname"));
		Assert.assertTrue("1".equals(activity.getItemValueString("keyFollowUp")));
		Assert.assertEquals(30, activity.getItemValueInteger("numNextActivityID"));
		Assert.assertEquals(1000, activity.getItemValueInteger("numprocessid"));

		// test 1000.30 ([followup])
		activity = model.getEvent(1000, 30);
		Assert.assertNotNull(activity);
		Assert.assertEquals("[follow up]", activity.getItemValueString("txtname"));
		Assert.assertFalse("1".equals(activity.getItemValueString("keyFollowUp")));
		Assert.assertEquals(1000, activity.getItemValueInteger("numprocessID"));
		Assert.assertEquals(1000, activity.getItemValueInteger("numprocessid"));

		// test 1000.10 (save)
		activity = model.getEvent(1000, 10);
		Assert.assertNotNull(activity);
		Assert.assertEquals("Save", activity.getItemValueString("txtname"));
		Assert.assertFalse("1".equals(activity.getItemValueString("keyFollowUp")));
		Assert.assertEquals(1000, activity.getItemValueInteger("numprocessID"));
		Assert.assertEquals(1000, activity.getItemValueInteger("numprocessid"));

		// Now we test the task 1000
		ItemCollection task = model.getTask(1000);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Simple", task.getItemValueString("txtworkflowgroup"));
		List<ItemCollection> events = model.findAllEventsByTask(1000);
		Assert.assertEquals(3, events.size());

	}

	

	/**
	 * Test the startEvents
	 * @throws ModelException
	 */
	@Test
	public void testStartEvents() throws ModelException {
		Assert.assertNotNull(model);
		
		// test start task....
		List<ItemCollection> startEvents = model.getStartEvents();
		Assert.assertNotNull(startEvents);
		Assert.assertEquals(1, startEvents.size());
		
		ItemCollection startEvent=startEvents.get(0);
		Assert.assertEquals("import", startEvent.getItemValueString("txtname"));
		
		
		
		
	}
	

	/**
	 * Test the startTasks 
	 * @throws ModelException
	 */
	@Test
	public void testStartTasks() throws ModelException {
		Assert.assertNotNull(model);
		
		// test start task....
		List<ItemCollection> startTasks = model.getStartTasks();
		Assert.assertNotNull(startTasks);
		Assert.assertEquals(1, startTasks.size());
		
		ItemCollection startTask=startTasks.get(0);
		Assert.assertEquals("Task 1", startTask.getItemValueString("txtname"));
		
	}
	
	/**
	 * Test the endTasks 
	 * @throws ModelException
	 */
	@Test
	public void testEndTasks() throws ModelException {
		Assert.assertNotNull(model);
		
		// test start task....
		List<ItemCollection> endTasks = model.getEndTasks();
		Assert.assertNotNull(endTasks);
		Assert.assertEquals(1, endTasks.size());
		
		ItemCollection endTask=endTasks.get(0);
		Assert.assertEquals("Task 1", endTask.getItemValueString("txtname"));
		
	}
}
