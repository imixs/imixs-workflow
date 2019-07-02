package org.imixs.workflow.bpmn;

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

import junit.framework.Assert;

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

	@Test
	public void testSimple() throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

	
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
		ItemCollection profile = model.getDefinition();
		Assert.assertNotNull(profile);

		// test count of elements
		Assert.assertEquals(9, model.findAllTasks().size());

		// test task 1000
		ItemCollection task = model.getTask(1000);
		Assert.assertNotNull(task);

		// test activity for task 1000
		List<ItemCollection> activities = model.findAllEventsByTask(1000);
		Assert.assertNotNull(activities);
		Assert.assertEquals(1, activities.size());

		// test activity 1000.10 submit
		ItemCollection activity = model.getEvent(1000, 10);
		Assert.assertNotNull(activity);
		Assert.assertEquals(1000, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("submit", activity.getItemValueString("txtname"));

	}
	
	
	
	

	@Test
	public void testFollowUp() throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

	
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
		ItemCollection profile = model.getDefinition();
		Assert.assertNotNull(profile);

	
		/* Test 2000 Task */

		ItemCollection task = model.getTask(2000);
		Assert.assertNotNull(task);

		// test activity for task 2000
		List<ItemCollection> activities = model.findAllEventsByTask(2000);
		Assert.assertNotNull(activities);
		Assert.assertEquals(2, activities.size());

		// test activity 2000.10 submit
		ItemCollection activity = model.getEvent(2000, 10);
		Assert.assertNotNull(activity);
		Assert.assertEquals(2000, activity.getItemValueInteger("numProcessID"));
		Assert.assertEquals(2000, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("submit", activity.getItemValueString("txtname"));
		Assert.assertFalse("1".equals( activity.getItemValueString("keyFollowUp")));
		Assert.assertEquals(0, activity.getItemValueInteger("numNextActivityID"));
		
		
		
		// test activity 2000.20 followup
		activity = model.getEvent(2000, 20);
		Assert.assertNotNull(activity);
		Assert.assertEquals(2000, activity.getItemValueInteger("numProcessID"));
		Assert.assertEquals(2100, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("followup", activity.getItemValueString("txtname"));
		Assert.assertFalse("1".equals( activity.getItemValueString("keyFollowUp")));
		Assert.assertEquals(0, activity.getItemValueInteger("numNextActivityID"));

	}
	
	
	
	
	
	
	
	@Test
	public void testSimpleNoGateway() throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

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
		ItemCollection profile = model.getDefinition();
		Assert.assertNotNull(profile);

		// test count of elements
		Assert.assertEquals(9, model.findAllTasks().size());

		// test task 1000
		ItemCollection task = model.getTask(3000);
		Assert.assertNotNull(task);

		// test activity for task 3000
		List<ItemCollection> activities = model.findAllEventsByTask(3000);
		Assert.assertNotNull(activities);
		Assert.assertEquals(1, activities.size());

		// test activity 3000.10 submit
		ItemCollection activity = model.getEvent(3000, 10);
		Assert.assertNotNull(activity);
		Assert.assertEquals(3000, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("submit", activity.getItemValueString("txtname"));

	}
}
