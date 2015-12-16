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
 * Test class test the Imixs BPMNParser.
 * 
 * Special case: a event with no direct next task (none task)
 * 
 * 
 * 
 * @author rsoika
 */
public class TestBPMNParserRuleEvents {

	@Before
	public void setup() {

	}

	@After
	public void teardown() {

	}

	@Test
	public void testSimple() throws ParseException, ParserConfigurationException, SAXException, IOException {

		String VERSION = "1.0.0";

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/event_rules.bpmn");

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

		// test count of elements
		Assert.assertEquals(9, model.getProcessEntityList(VERSION).size());

		// test task 1000
		ItemCollection task = model.getProcessEntity(1000, VERSION);
		Assert.assertNotNull(task);

		// test activity for task 1000
		List<ItemCollection> activities = model.getActivityEntityList(1000, VERSION);
		Assert.assertNotNull(activities);
		Assert.assertEquals(1, activities.size());

		// test activity 1000.10 submit
		ItemCollection activity = model.getActivityEntity(1000, 10, VERSION);
		Assert.assertNotNull(activity);
		Assert.assertEquals(1000, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("submit", activity.getItemValueString("txtname"));

	}
	
	
	
	

	@Test
	public void testFollowUp() throws ParseException, ParserConfigurationException, SAXException, IOException {

		String VERSION = "1.0.0";

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/event_rules.bpmn");

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

	
		/* Test 2000 Task */

		ItemCollection task = model.getProcessEntity(2000, VERSION);
		Assert.assertNotNull(task);

		// test activity for task 2000
		List<ItemCollection> activities = model.getActivityEntityList(2000, VERSION);
		Assert.assertNotNull(activities);
		Assert.assertEquals(2, activities.size());

		// test activity 2000.10 submit
		ItemCollection activity = model.getActivityEntity(2000, 10, VERSION);
		Assert.assertNotNull(activity);
		Assert.assertEquals(2000, activity.getItemValueInteger("numProcessID"));
		Assert.assertEquals(2000, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("submit", activity.getItemValueString("txtname"));
		Assert.assertFalse("1".equals( activity.getItemValueString("keyFollowUp")));
		Assert.assertEquals(0, activity.getItemValueInteger("numNextActivityID"));
		
		
		
		// test activity 2000.20 followup
		activity = model.getActivityEntity(2000, 20, VERSION);
		Assert.assertNotNull(activity);
		Assert.assertEquals(2000, activity.getItemValueInteger("numProcessID"));
		Assert.assertEquals(2100, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("followup", activity.getItemValueString("txtname"));
		Assert.assertFalse("1".equals( activity.getItemValueString("keyFollowUp")));
		Assert.assertEquals(0, activity.getItemValueInteger("numNextActivityID"));

	}
	
	
	
	
	
	
	
	@Test
	public void testSimpleNoGateway() throws ParseException, ParserConfigurationException, SAXException, IOException {

		String VERSION = "1.0.0";

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/event_rules.bpmn");

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

		// test count of elements
		Assert.assertEquals(9, model.getProcessEntityList(VERSION).size());

		// test task 1000
		ItemCollection task = model.getProcessEntity(3000, VERSION);
		Assert.assertNotNull(task);

		// test activity for task 3000
		List<ItemCollection> activities = model.getActivityEntityList(3000, VERSION);
		Assert.assertNotNull(activities);
		Assert.assertEquals(1, activities.size());

		// test activity 3000.10 submit
		ItemCollection activity = model.getActivityEntity(3000, 10, VERSION);
		Assert.assertNotNull(activity);
		Assert.assertEquals(3000, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("submit", activity.getItemValueString("txtname"));

	}
}
