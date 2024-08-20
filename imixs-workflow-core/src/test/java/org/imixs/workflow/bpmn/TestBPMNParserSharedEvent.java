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
 * Test class test the Imixs BPMNParser in case of shared events (one event used
 * by two different task elements)
 * 
 * @author rsoika
 */
public class TestBPMNParserSharedEvent {

	BPMNModel model = null;
	OpenBPMNModelManager openBPMNModelManager = null;

	@Before
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new OpenBPMNModelManager();

	}

	/**
	 * Simple shared event
	 * 
	 */
	@Test
	public void testSharedEvent() {

		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/shared_event1.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			Assert.assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		try {
			// test task 1000
			ItemCollection task = openBPMNModelManager.findTaskByID(model, 1000);
			Assert.assertNotNull(task);

			// test activity for task 1000
			List<ItemCollection> events = openBPMNModelManager.findEventsByTask(model, 1000);
			Assert.assertNotNull(events);
			Assert.assertEquals(2, events.size());

			// test activity for task 1100
			events = openBPMNModelManager.findEventsByTask(model, 1100);
			Assert.assertNotNull(events);
			Assert.assertEquals(1, events.size());
		} catch (BPMNModelException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Shared event with an follow up event
	 * 
	 */
	@Test
	public void testSharedEventWithFollowUp() {

		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/shared_event2.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			Assert.assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		try {

			// test task 2000
			ItemCollection task = openBPMNModelManager.findTaskByID(model, 2000);
			Assert.assertNotNull(task);
			List<ItemCollection> events = openBPMNModelManager.findEventsByTask(model, 2000);
			Assert.assertNotNull(events);
			Assert.assertEquals(2, events.size());

			// test activity for task 2100
			events = openBPMNModelManager.findEventsByTask(model, 2100);
			Assert.assertNotNull(events);
			Assert.assertEquals(1, events.size());

		} catch (BPMNModelException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Shared event with an follow up event
	 * 
	 */
	@Test
	public void testSharedLinkedEventWithFollowUp() {

		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/shared_event3.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			Assert.assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		try {

			// test task 3000
			ItemCollection task = openBPMNModelManager.findTaskByID(model, 3000);
			Assert.assertNotNull(task);
			List<ItemCollection> events = openBPMNModelManager.findEventsByTask(model, 3000);
			Assert.assertNotNull(events);
			Assert.assertEquals(2, events.size());

			// test task 3100
			task = openBPMNModelManager.findTaskByID(model, 3100);
			Assert.assertNotNull(task);
			events = openBPMNModelManager.findEventsByTask(model, 3100);
			Assert.assertNotNull(events);
			Assert.assertEquals(1, events.size());

			// test task 3200
			task = openBPMNModelManager.findTaskByID(model, 3200);
			Assert.assertNotNull(task);
			events = openBPMNModelManager.findEventsByTask(model, 3200);
			Assert.assertNotNull(events);
			Assert.assertEquals(0, events.size());

		} catch (BPMNModelException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Shared event with a direct follow up event
	 * 
	 */
	@Test
	public void testSharedEvent_Case4() {

		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/shared_event4.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			Assert.assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		try {

			// test task 2000
			ItemCollection task = openBPMNModelManager.findTaskByID(model, 2000);
			Assert.assertNotNull(task);
			List<ItemCollection> events = openBPMNModelManager.findEventsByTask(model, 2000);
			Assert.assertNotNull(events);
			// there are 3 events....
			Assert.assertEquals(2, events.size());
			Assert.assertNotNull(openBPMNModelManager.findEventByID(model, 2000, 10)); // save
			Assert.assertNotNull(openBPMNModelManager.findEventByID(model, 2000, 90)); // follow up

			// test task 2100
			task = openBPMNModelManager.findTaskByID(model, 2100);
			Assert.assertNotNull(task);
			events = openBPMNModelManager.findEventsByTask(model, 2100);
			Assert.assertNotNull(events);
			// there are no events....
			Assert.assertEquals(0, events.size());
		} catch (BPMNModelException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Shared event with a direct follow up event
	 * 
	 */
	@Test
	public void testSharedEvent_Case5() {

		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/shared_event5.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			Assert.assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		try {
			// test task 2000
			ItemCollection task = openBPMNModelManager.findTaskByID(model, 2000);
			Assert.assertNotNull(task);
			List<ItemCollection> events = openBPMNModelManager.findEventsByTask(model, 2000);
			Assert.assertNotNull(events);
			// there are 3 events....
			Assert.assertEquals(2, events.size());
			Assert.assertNotNull(openBPMNModelManager.findEventByID(model, 2000, 10)); // save
			Assert.assertNotNull(openBPMNModelManager.findEventByID(model, 2000, 90)); // follow up

			// test task 2100
			task = openBPMNModelManager.findTaskByID(model, 2100);
			Assert.assertNotNull(task);
			events = openBPMNModelManager.findEventsByTask(model, 2100);
			Assert.assertNotNull(events);
			// there are 2 events....
			Assert.assertEquals(1, events.size());
			Assert.assertNotNull(openBPMNModelManager.findEventByID(model, 2100, 80)); // archive
		} catch (BPMNModelException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

}
