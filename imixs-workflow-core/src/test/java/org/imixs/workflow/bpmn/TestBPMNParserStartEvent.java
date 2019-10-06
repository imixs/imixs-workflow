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
 * Test class test the Imixs BPMNParser
 * 
 * @author rsoika
 */
public class TestBPMNParserStartEvent {

	protected BPMNModel model = null;

	@Before
	public void setUp() throws ParseException, ParserConfigurationException, SAXException, IOException {
		InputStream inputStream = getClass().getResourceAsStream("/bpmn/startevent.bpmn");

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

		// test task 1000
		ItemCollection task = model.getTask(1000);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Simple", task.getItemValueString("txtworkflowgroup"));

		// test activity 1000.10 save
		ItemCollection activity = model.getEvent(1000, 10);
		Assert.assertNotNull(activity);
		Assert.assertEquals("save", activity.getItemValueString("txtname"));
		Assert.assertEquals(1000, activity.getItemValueInteger("numNextProcessID"));

		// test activity for task 1000
		List<ItemCollection> activities = model.findAllEventsByTask(1000);
		Assert.assertNotNull(activities);
		Assert.assertEquals(2, activities.size());

		// test import activity for task 1000.20
		activity = model.getEvent(1000, 20);
		Assert.assertNotNull(activity);
		Assert.assertEquals("import", activity.getItemValueString("txtname"));
		Assert.assertEquals(1000, activity.getItemValueInteger("numNextProcessID"));

	}

	/**
	 * Test the startEvents
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testStartEvents() throws ModelException {
		Assert.assertNotNull(model);

		// test start task....
		List<ItemCollection> startEvents = model.getStartEvents(1000);
		Assert.assertNotNull(startEvents);
		Assert.assertEquals(1, startEvents.size());

		ItemCollection event = startEvents.get(0);
		Assert.assertEquals("import", event.getItemValueString("txtname"));

	}

	
	/**
	 * Test the method initStartEvent
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testInitStartEvent() throws ModelException {
		Assert.assertNotNull(model);

		ItemCollection workitem=new ItemCollection();
		model.initStartEvent(workitem);
		Assert.assertEquals(1000, workitem.getTaskID());
		Assert.assertEquals(20, workitem.getEventID());
		
	}

}
