package org.imixs.workflow.bpmn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Test;
import org.xml.sax.SAXException;

import org.junit.Assert;

/**
 * Test class test the Imixs BPMNParser
 * 
 * @author rsoika
 */
public class TestBPMNParserSimple {


	@Test
	public void testSimple()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		String VERSION = "1.0.0";

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/simple.bpmn");

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

		// ZWISCHENTEST

		byte[] rawData = model.getRawData();
		InputStream bpmnInputStream = new ByteArrayInputStream(rawData);

		try {
			model = BPMNParser.parseModel(bpmnInputStream, "UTF-8");
			Assert.assertNotNull(model);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// ZWISCHENTEST

		// test version
		Assert.assertEquals(VERSION, model.getVersion());

		// Test Environment
		ItemCollection profile = model.getDefinition();
		Assert.assertNotNull(profile);
		Assert.assertEquals("environment.profile", profile.getItemValueString("txtname"));
		Assert.assertEquals("WorkflowEnvironmentEntity", profile.getItemValueString("type"));
		Assert.assertEquals(VERSION, profile.getItemValueString("$ModelVersion"));

		Assert.assertTrue(model.getGroups().contains("Simple"));

		// test count of elements
		Assert.assertEquals(2, model.findAllTasks().size());

		// test task 1000
		ItemCollection task = model.getTask(1000);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Simple", task.getItemValueString("txtworkflowgroup"));

		// test activity for task 1000
		List<ItemCollection> activities = model.findAllEventsByTask(1000);
		Assert.assertNotNull(activities);
		Assert.assertEquals(2, activities.size());

		// test activity 1000.10 submit
		ItemCollection activity = model.getEvent(1000, 10);
		Assert.assertNotNull(activity);
		Assert.assertEquals(1000, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("update", activity.getItemValueString("txtname"));
		
		// test activity 1000.20 submit
		activity = model.getEvent(1000, 20);
		Assert.assertNotNull(activity);
		Assert.assertEquals(1100, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("submit", activity.getItemValueString("txtname"));

	}

	@Test
	public void testSimpleLoop()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		String VERSION = "1.0.0";

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/simple_loop.bpmn");

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
		Assert.assertEquals("environment.profile", profile.getItemValueString("txtname"));
		Assert.assertEquals("WorkflowEnvironmentEntity", profile.getItemValueString("type"));
		Assert.assertEquals(VERSION, profile.getItemValueString("$ModelVersion"));

		Assert.assertTrue(model.getGroups().contains("Simple"));

		// test count of elements
		Assert.assertEquals(2, model.findAllTasks().size());

		// test task 1000
		ItemCollection task = model.getTask(1000);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Simple", task.getItemValueString("txtworkflowgroup"));

		// test activity for task 1000
		List<ItemCollection> activities = model.findAllEventsByTask(1000);
		Assert.assertNotNull(activities);
		Assert.assertEquals(2, activities.size());

		// test activity 1000.10 submit
		ItemCollection activity = model.getEvent(1000, 10);
		Assert.assertNotNull(activity);
		Assert.assertEquals(1100, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("submit", activity.getItemValueString("txtname"));

		// test activity 1000.20 update
		activity = model.getEvent(1000, 20);
		Assert.assertNotNull(activity);
		Assert.assertEquals(1000, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("update", activity.getItemValueString("txtname"));

	}

	/**
	 * Simple follow up test
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException
	 */
	@Test
	public void testFollowUp()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		String VERSION = "1.0.0";

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/followup.bpmn");

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

		// test task 1000
		ItemCollection task = model.getTask(1000);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Simple", task.getItemValueString("txtworkflowgroup"));

		// test activity for task 1000
		List<ItemCollection> activities = model.findAllEventsByTask(1000);
		Assert.assertNotNull(activities);
		Assert.assertEquals(2, activities.size());

		// test activity 1000.10 submit
		ItemCollection activity = model.getEvent(1000, 10);
		Assert.assertNotNull(activity);
		Assert.assertEquals("1", activity.getItemValueString("keyFollowUp"));
		Assert.assertEquals(20, activity.getItemValueInteger("numNextActivityID"));
		Assert.assertEquals("submit", activity.getItemValueString("txtname"));
		Assert.assertEquals(VERSION, activity.getModelVersion());
		Assert.assertEquals(1000, activity.getItemValueInteger("numprocessid"));

		Assert.assertEquals("ActivityEntity", activity.getType());

		// test activity 1000.20 followup
		activity = model.getEvent(1000, 20);
		Assert.assertNotNull(activity);
		Assert.assertFalse("1".equals(activity.getItemValueString("keyFollowUp")));
		Assert.assertEquals(1100, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("followup", activity.getItemValueString("txtname"));
		Assert.assertEquals(VERSION, activity.getModelVersion());
		Assert.assertEquals(1000, activity.getItemValueInteger("numprocessid"));
		Assert.assertEquals("ActivityEntity", activity.getType());

		// test activity for task 1100
		activities = model.findAllEventsByTask(1100);
		Assert.assertNotNull(activities);
		Assert.assertEquals(0, activities.size());
		
		
		// test start event
		List<ItemCollection> startEvents = model.getStartEvents(1000);
		Assert.assertNotNull(startEvents);
		Assert.assertEquals(1, startEvents.size());
		ItemCollection startEvent=startEvents.get(0);
		Assert.assertEquals("submit", startEvent.getItemValueString("txtname"));
	}

	
	/**
	 * This test test a more complex start scenario where a start event has
	 * possible more than one target tasks
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException
	 */
	@Test
	public void testFollowUpComplex()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		String VERSION = "1.0.0";

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/followup_complex.bpmn");

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
		Assert.assertEquals("environment.profile", profile.getItemValueString("txtname"));
		Assert.assertEquals("WorkflowEnvironmentEntity", profile.getItemValueString("type"));
		Assert.assertEquals(VERSION, profile.getItemValueString("$ModelVersion"));

		Assert.assertTrue(model.getGroups().contains("Simple"));

		// test count of elements
		Assert.assertEquals(2, model.findAllTasks().size());

		// test task 1100
		ItemCollection task = model.getTask(1100);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Simple", task.getItemValueString("txtworkflowgroup"));

		// test import activity for task 1000.20 (followup)
		ItemCollection activity = model.getEvent(1100, 20);
		Assert.assertNotNull(activity);
		Assert.assertEquals("import", activity.getItemValueString("txtname"));

		Assert.assertFalse("1".equals(activity.getItemValueString("keyFollowUp")));
		Assert.assertEquals(1100, activity.getItemValueInteger("numprocessID"));
		Assert.assertEquals(1100, activity.getItemValueInteger("numprocessid"));

		// test import activity for task 1000.30 (followup)
		activity = model.getEvent(1100, 30);
		Assert.assertNotNull(activity);
		Assert.assertEquals("[follow up-1]", activity.getItemValueString("txtname"));
		Assert.assertFalse("1".equals(activity.getItemValueString("keyFollowUp")));
		Assert.assertEquals(1100, activity.getItemValueInteger("numprocessid"));

	}
	
	
	/**
	 * This test verifies if the stream data can be stored in a byte array to be
	 * parsed again. rawData is used by the ModelService to store the file
	 * content into a model Entity.
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException
	 */
	@Test
	public void testRawData()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {
		InputStream inputStream = getClass().getResourceAsStream("/bpmn/simple.bpmn");

		BPMNModel model1 = null;
		BPMNModel model2 = null;
		try {
			model1 = BPMNParser.parseModel(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertNotNull(model1);

		// get the rawdata object
		byte[] rawData = model1.getRawData();
		Assert.assertNotNull(rawData);
		// Test again to convert this data into a stream and parse again
		InputStream bpmnInputStream = new ByteArrayInputStream(rawData);

		try {
			model2 = BPMNParser.parseModel(bpmnInputStream, "UTF-8");
			Assert.assertNotNull(model1);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// compare models
		Assert.assertEquals(model1.findAllTasks(), model2.findAllTasks());
	}

}
