package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import junit.framework.Assert;

/**
 * Test class test the Imixs BPMNParser.
 * 
 * Special case: Conditional-Events
 * 
 * @see issue #299
 * @author rsoika
 */
public class TestBPMNParserConditionalEvents {

	@Before
	public void setup() {

	}

	@After
	public void teardown() {

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSimple()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/conditional_event1.bpmn");

		BPMNModel model = null;
		try {
			model = BPMNParser.parseModel(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertNotNull(model);

		// Test Environment
		ItemCollection profile = model.getDefinition();
		Assert.assertNotNull(profile);

		// test count of elements
		Assert.assertEquals(3, model.findAllTasks().size());

		// test task 1000
		ItemCollection task = model.getTask(1000);
		Assert.assertNotNull(task);

		// test events for task 1000
		List<ItemCollection> events = model.findAllEventsByTask(1000);
		Assert.assertNotNull(events);
		Assert.assertEquals(1, events.size());

		// test activity 1000.10 submit
		ItemCollection activity = model.getEvent(1000, 10);
		Assert.assertNotNull(activity);
		Assert.assertEquals("conditional event", activity.getItemValueString("txtname"));

		Assert.assertEquals(1000, activity.getItemValueInteger("numNextProcessID"));

		// Now we need to evaluate if the Event is marked as an conditional Event with
		// the condition list copied from the gateway.
		Assert.assertTrue(activity.hasItem("keyExclusiveConditions"));
		Map<String, String> conditions = (Map<String, String>) activity.getItemValue("keyExclusiveConditions").get(0);
		Assert.assertNotNull(conditions);
		Assert.assertEquals("(workitem._budget && workitem._budget[0]>100)", conditions.get("task=1100"));
		Assert.assertEquals("(workitem._budget && workitem._budget[0]<=100)", conditions.get("task=1200"));
	}

	@SuppressWarnings({ "unchecked" })
	@Test
	// @Ignore
	public void testFollowUp()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/conditional_event2.bpmn");

		BPMNModel model = null;
		try {
			model = BPMNParser.parseModel(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertNotNull(model);

		// Test Environment
		ItemCollection profile = model.getDefinition();
		Assert.assertNotNull(profile);

		// test count of elements
		Assert.assertEquals(3, model.findAllTasks().size());

		// test task 1000
		ItemCollection task = model.getTask(1000);
		Assert.assertNotNull(task);

		// test events for task 1000
		List<ItemCollection> events = model.findAllEventsByTask(1000);
		Assert.assertNotNull(events);
		Assert.assertEquals(2, events.size());

		// test activity 1000.10 submit
		ItemCollection activity = model.getEvent(1000, 10);
		Assert.assertNotNull(activity);
		Assert.assertEquals("conditional event", activity.getItemValueString("txtname"));

		Assert.assertEquals(1000, activity.getItemValueInteger("numNextProcessID"));

		// Now we need to evaluate if the Event is marked as an conditional Event with
		// the condition list copied from the gateway.
		Assert.assertTrue(activity.hasItem("keyExclusiveConditions"));
		Map<String, String> conditions = (Map<String, String>) activity.getItemValue("keyExclusiveConditions").get(0);
		Assert.assertNotNull(conditions);
		Assert.assertEquals("(workitem._budget && workitem._budget[0]>100)", conditions.get("task=1100"));
		Assert.assertEquals("(workitem._budget && workitem._budget[0]<=100)", conditions.get("event=20"));

	}

	/**
	 * This test combines a conditional event with a split event.
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testConditionalSplitEvent()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/conditional_split_event.bpmn");

		BPMNModel model = null;
		try {
			model = BPMNParser.parseModel(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertNotNull(model);

		// Test Environment
		ItemCollection profile = model.getDefinition();
		Assert.assertNotNull(profile);

		// test count of elements
		Assert.assertEquals(4, model.findAllTasks().size());

		// test task 1000
		ItemCollection task = model.getTask(1000);
		Assert.assertNotNull(task);

		// test events for task 1000
		List<ItemCollection> events = model.findAllEventsByTask(1000);
		Assert.assertNotNull(events);
		Assert.assertEquals(3, events.size());

		// test activity 1000.10 submit
		ItemCollection activity = model.getEvent(1000, 10);
		Assert.assertNotNull(activity);
		Assert.assertEquals("conditional event", activity.getItemValueString("txtname"));

		Assert.assertEquals(1000, activity.getItemValueInteger("numNextProcessID"));

		// Now we need to evaluate if the Event is marked as an conditional Event with
		// the condition list copied from the gateway.
		Assert.assertTrue(activity.hasItem("keyExclusiveConditions"));
		Map<String, String> conditions = (Map<String, String>) activity.getItemValue("keyExclusiveConditions").get(0);
		Assert.assertNotNull(conditions);
		Assert.assertEquals("(workitem._budget && workitem._budget[0]<=100)", conditions.get("task=1300"));
		Assert.assertEquals("(workitem._budget && workitem._budget[0]>100)", conditions.get("event=20"));

		// test split...
		activity = model.getEvent(1000, 20);
		Assert.assertNotNull(activity);
		Assert.assertEquals("split event", activity.getItemValueString("txtname"));

		Assert.assertEquals(1000, activity.getItemValueInteger("numNextProcessID"));

		// Now we need to evaluate if the Event is marked as an conditional Event with
		// the condition list copied from the gateway.
		Assert.assertTrue(activity.hasItem("keySplitConditions"));
		conditions = (Map<String, String>) activity.getItemValue("keySplitConditions").get(0);
		Assert.assertNotNull(conditions);
		Assert.assertEquals("true", conditions.get("task=1100"));
		Assert.assertEquals("false", conditions.get("event=30"));
	}

}