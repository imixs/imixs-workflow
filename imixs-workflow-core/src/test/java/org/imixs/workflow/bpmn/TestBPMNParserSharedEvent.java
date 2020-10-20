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

import org.junit.Assert;

/**
 * Test class test the Imixs BPMNParser in case of shared events (one event used
 * by two different task elements)
 * 
 * @author rsoika
 */
public class TestBPMNParserSharedEvent {


	/**
	 * Simple shared event
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException 
	 */
	@Test
	public void testSharedEvent() throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/shared_event1.bpmn");

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

		// test activity for task 1000
		List<ItemCollection> activities = model.findAllEventsByTask(1000);
		Assert.assertNotNull(activities);
		Assert.assertEquals(2, activities.size());

		// test activity for task 1100
		activities = model.findAllEventsByTask(1100);
		Assert.assertNotNull(activities);
		Assert.assertEquals(1, activities.size());

		// test activity 1000.10 submit
		ItemCollection activity = model.getEvent(1000, 10);
		Assert.assertNotNull(activity);
		Assert.assertEquals(1100, activity.getItemValueInteger("numNextProcessID"));

		// now test shared activity...

		// test activity 1100.90 archive
		activity = model.getEvent(1100, 90);
		Assert.assertNotNull(activity);
		Assert.assertEquals(1200, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("archive", activity.getItemValueString("txtname"));

		// test activity 1000.90 archive
		activity = model.getEvent(1000, 90);
		Assert.assertNotNull(activity);
		Assert.assertEquals(1200, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("archive", activity.getItemValueString("txtname"));

	}

	/**
	 * Shared event with an follow up event
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException 
	 */
	@Test
	public void testSharedEventWithFollowUp()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		
		InputStream inputStream = getClass().getResourceAsStream("/bpmn/shared_event2.bpmn");

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

		// test task 2000
		ItemCollection task = model.getTask(2000);
		Assert.assertNotNull(task);
		List<ItemCollection> activities = model.findAllEventsByTask(2000);
		Assert.assertNotNull(activities);
		Assert.assertEquals(3, activities.size());

		// test activity for task 2100
		activities = model.findAllEventsByTask(2100);
		Assert.assertNotNull(activities);
		Assert.assertEquals(2, activities.size());

		// test activity 2000.10 submit
		ItemCollection activity = model.getEvent(2000, 10);
		Assert.assertNotNull(activity);
		Assert.assertEquals(2100, activity.getItemValueInteger("numNextProcessID"));

		// now test shared activity...

		// test activity 2000.80 archive
		activity = model.getEvent(2000, 80);
		Assert.assertNotNull(activity);
		Assert.assertEquals("1", activity.getItemValueString("keyFollowUp"));
		Assert.assertEquals(90, activity.getItemValueInteger("numNextActivityID"));
		Assert.assertEquals("archive", activity.getItemValueString("txtname"));

		// test activity 2000.90 archive
		activity = model.getEvent(2000, 90);
		Assert.assertNotNull(activity);
		Assert.assertEquals(2200, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("followup", activity.getItemValueString("txtname"));

	}

	/**
	 * Shared event with an follow up event
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException 
	 */
	@Test
	public void testSharedLinkedEventWithFollowUp()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		
		InputStream inputStream = getClass().getResourceAsStream("/bpmn/shared_event3.bpmn");

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

		// test task 3000
		ItemCollection task = model.getTask(3000);
		Assert.assertNotNull(task);
		List<ItemCollection> activities = model.findAllEventsByTask(3000);
		Assert.assertNotNull(activities);
		Assert.assertEquals(3, activities.size());

		// test task 3100
		task = model.getTask(3100);
		Assert.assertNotNull(task);
		activities = model.findAllEventsByTask(3100);
		Assert.assertNotNull(activities);
		Assert.assertEquals(2, activities.size());

		// test task 3200
		task = model.getTask(3200);
		Assert.assertNotNull(task);
		activities = model.findAllEventsByTask(3200);
		Assert.assertNotNull(activities);
		Assert.assertEquals(0, activities.size());

		// test follow up 3000.20
		ItemCollection activity = model.getEvent(3000, 20);
		Assert.assertNotNull(activity);
		Assert.assertEquals("1", activity.getItemValueString("keyFollowUp"));
		Assert.assertEquals(30, activity.getItemValueInteger("numNextActivityID"));
		Assert.assertEquals("archive", activity.getItemValueString("txtname"));

		// test follow up 3100.20
		activity = model.getEvent(3100, 20);
		Assert.assertNotNull(activity);
		Assert.assertEquals("1", activity.getItemValueString("keyFollowUp"));
		Assert.assertEquals(30, activity.getItemValueInteger("numNextActivityID"));
		Assert.assertEquals("archive", activity.getItemValueString("txtname"));

		// test follow up 3000.30
		activity = model.getEvent(3000, 30);
		Assert.assertNotNull(activity);
		Assert.assertFalse("1".equals(activity.getItemValueString("keyFollowUp")));
		Assert.assertEquals(3200, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("followup", activity.getItemValueString("txtname"));
		Assert.assertEquals(3000, activity.getItemValueInteger("numProcessID"));

		// test follow up 3100.30
		activity = model.getEvent(3100, 30);
		Assert.assertNotNull(activity);
		Assert.assertFalse("1".equals(activity.getItemValueString("keyFollowUp")));
		Assert.assertEquals(3200, activity.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("followup", activity.getItemValueString("txtname"));
		Assert.assertEquals(3100, activity.getItemValueInteger("numProcessID"));

	}
	
	
	
	
	/**
	 * Shared event with a direct follow up event
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException 
	 */
	@Test
	public void testSharedEvent_Case4()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		
		InputStream inputStream = getClass().getResourceAsStream("/bpmn/shared_event4.bpmn");

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

		// test task 2000
		ItemCollection task = model.getTask(2000);
		Assert.assertNotNull(task);
		List<ItemCollection> activities = model.findAllEventsByTask(2000);
		Assert.assertNotNull(activities);
		// there are 3 events....
		Assert.assertEquals(2, activities.size());
		Assert.assertNotNull(model.getEvent(2000, 10)); // save
		Assert.assertNotNull(model.getEvent(2000, 90)); // follow up
	//	Assert.assertNotNull(model.getEvent(2000, 100)); // follow up (shared)
		
		// test task 2100
		task = model.getTask(2100);
		Assert.assertNotNull(task);
		activities = model.findAllEventsByTask(2100);
		Assert.assertNotNull(activities);
		// there are no events....
		Assert.assertEquals(0, activities.size());

	
	}

	/**
	 * Shared event with a direct follow up event
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException 
	 */
	@Test
	public void testSharedEvent_Case5()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		
		InputStream inputStream = getClass().getResourceAsStream("/bpmn/shared_event5.bpmn");

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

		// test task 2000
		ItemCollection task = model.getTask(2000);
		Assert.assertNotNull(task);
		List<ItemCollection> activities = model.findAllEventsByTask(2000);
		Assert.assertNotNull(activities);
		// there are 3 events....
		Assert.assertEquals(2, activities.size());
		Assert.assertNotNull(model.getEvent(2000, 10)); // save
		Assert.assertNotNull(model.getEvent(2000, 90)); // follow up
		//Assert.assertNotNull(model.getEvent(2000, 100)); // follow up (shared)
		
		// test task 2100
		task = model.getTask(2100);
		Assert.assertNotNull(task);
		activities = model.findAllEventsByTask(2100);
		Assert.assertNotNull(activities);
		// there are 2 events....
		Assert.assertEquals(2, activities.size());
		Assert.assertNotNull(model.getEvent(2100, 80)); // archive
		Assert.assertNotNull(model.getEvent(2100, 90)); // follow up (shared)

	
	}

}
