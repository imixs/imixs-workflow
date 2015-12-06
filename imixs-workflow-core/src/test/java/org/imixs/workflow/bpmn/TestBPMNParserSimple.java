package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Test class test the Imixs BPMNParser
 * 
 * @author rsoika
 */
public class TestBPMNParserSimple {

	@Before
	public void setup() {

	}

	@After
	public void teardown() {

	}

	@Test
	public void testSimple() throws ParseException, ParserConfigurationException, SAXException, IOException {

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

		// Test Environment
		ItemCollection profile = model.getProfile();
		Assert.assertNotNull(profile);
		Assert.assertEquals("environment.profile", profile.getItemValueString("txtname"));
		Assert.assertEquals("WorkflowEnvironmentEntity", profile.getItemValueString("type"));
		Assert.assertEquals(VERSION, profile.getItemValueString("$ModelVersion"));

		Assert.assertTrue(model.workflowGroups.contains("Simple"));

		// test count of elements
		Assert.assertEquals(2, model.getProcessEntityList(VERSION).size());

		// test task 1000
		ItemCollection task = model.getProcessEntity(1000, VERSION);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Simple", task.getItemValueString("txtworkflowgroup"));

		// test activity for task 1000
		List<ItemCollection> activities = model.getActivityEntityList(1000, VERSION);
		Assert.assertNotNull(activities);
		Assert.assertEquals(2, activities.size());

		// test activity 1000.10 submit
		ItemCollection activity = model.getActivityEntity(1000, 10, VERSION);
		Assert.assertNotNull(activity);
		Assert.assertEquals(1100,
				activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("submit",
				activity.getItemValueString("txtname"));
		

		// test activity 1000.20 submit
		activity = model.getActivityEntity(1000, 20, VERSION);
		Assert.assertNotNull(activity);
		Assert.assertEquals(1000,
				activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("update",
				activity.getItemValueString("txtname"));

	}
	
	
	
	
	@Test
	public void testSimpleLoop() throws ParseException, ParserConfigurationException, SAXException, IOException {

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
		ItemCollection profile = model.getProfile();
		Assert.assertNotNull(profile);
		Assert.assertEquals("environment.profile", profile.getItemValueString("txtname"));
		Assert.assertEquals("WorkflowEnvironmentEntity", profile.getItemValueString("type"));
		Assert.assertEquals(VERSION, profile.getItemValueString("$ModelVersion"));

		Assert.assertTrue(model.workflowGroups.contains("Simple"));

		// test count of elements
		Assert.assertEquals(2, model.getProcessEntityList(VERSION).size());

		// test task 1000
		ItemCollection task = model.getProcessEntity(1000, VERSION);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Simple", task.getItemValueString("txtworkflowgroup"));

		// test activity for task 1000
		List<ItemCollection> activities = model.getActivityEntityList(1000, VERSION);
		Assert.assertNotNull(activities);
		Assert.assertEquals(2, activities.size());

		// test activity 1000.10 submit
		ItemCollection activity = model.getActivityEntity(1000, 10, VERSION);
		Assert.assertNotNull(activity);
		Assert.assertEquals(1100,
				activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("submit",
				activity.getItemValueString("txtname"));
		

		// test activity 1000.20 update
		activity = model.getActivityEntity(1000, 20, VERSION);
		Assert.assertNotNull(activity);
		Assert.assertEquals(1000,
				activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("update",
				activity.getItemValueString("txtname"));

	}

	/**
	 * Simple follow up test
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	@Test
	public void testFollowUp() throws ParseException, ParserConfigurationException, SAXException, IOException {

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
		ItemCollection task = model.getProcessEntity(1000, VERSION);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Simple", task.getItemValueString("txtworkflowgroup"));

		// test activity for task 1000
		List<ItemCollection> activities = model.getActivityEntityList(1000, VERSION);
		Assert.assertNotNull(activities);
		Assert.assertEquals(2, activities.size());

		
		// test activity 1000.10 submit
		ItemCollection activity = model.getActivityEntity(1000, 10, VERSION);
		Assert.assertNotNull(activity);
		Assert.assertEquals("1", activity.getItemValueString("keyFollowUp"));
		Assert.assertEquals(20, activity.getItemValueInteger("numNextActivityID"));
		Assert.assertEquals("submit",
				activity.getItemValueString("txtname"));
		Assert.assertEquals(VERSION,activity.getModelVersion());
		Assert.assertEquals(1000,activity.getItemValueInteger("numprocessid"));

		Assert.assertEquals("ActivityEntity",activity.getType());
		
		
		// test activity 1000.20 followup
		activity = model.getActivityEntity(1000, 20, VERSION);
		Assert.assertNotNull(activity);
		Assert.assertFalse("1".equals(activity.getItemValueString("keyFollowUp")));
		Assert.assertEquals(1100, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("followup",
				activity.getItemValueString("txtname"));
		Assert.assertEquals(VERSION,activity.getModelVersion());
		Assert.assertEquals(1000,activity.getItemValueInteger("numprocessid"));
		Assert.assertEquals("ActivityEntity",activity.getType());
		
		// test activity for task 1100
		activities = model.getActivityEntityList(1100, VERSION);
		Assert.assertNotNull(activities);
		Assert.assertEquals(0, activities.size());
	}

}
