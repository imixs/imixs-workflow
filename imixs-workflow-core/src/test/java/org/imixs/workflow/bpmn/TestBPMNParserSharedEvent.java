package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
	ModelManager openBPMNModelManager = null;

	@BeforeEach
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new ModelManager();

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
			assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// test task 1000
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 1000);
		assertNotNull(task);

		// test activity for task 1000
		List<ItemCollection> events = openBPMNModelManager.findEventsByTask(model, 1000);
		assertNotNull(events);
		assertEquals(2, events.size());

		// test activity for task 1100
		events = openBPMNModelManager.findEventsByTask(model, 1100);
		assertNotNull(events);
		assertEquals(1, events.size());

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
			assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// test task 2000
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 2000);
		assertNotNull(task);
		List<ItemCollection> events = openBPMNModelManager.findEventsByTask(model, 2000);
		assertNotNull(events);
		assertEquals(2, events.size());

		// test activity for task 2100
		events = openBPMNModelManager.findEventsByTask(model, 2100);
		assertNotNull(events);
		assertEquals(1, events.size());

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
			assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		// test task 3000
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 3000);
		assertNotNull(task);
		List<ItemCollection> events = openBPMNModelManager.findEventsByTask(model, 3000);
		assertNotNull(events);
		assertEquals(2, events.size());

		// test task 3100
		task = openBPMNModelManager.findTaskByID(model, 3100);
		assertNotNull(task);
		events = openBPMNModelManager.findEventsByTask(model, 3100);
		assertNotNull(events);
		assertEquals(1, events.size());

		// test task 3200
		task = openBPMNModelManager.findTaskByID(model, 3200);
		assertNotNull(task);
		events = openBPMNModelManager.findEventsByTask(model, 3200);
		assertNotNull(events);
		assertEquals(0, events.size());

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
			assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// test task 2000
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 2000);
		assertNotNull(task);
		List<ItemCollection> events = openBPMNModelManager.findEventsByTask(model, 2000);
		assertNotNull(events);
		// there are 3 events....
		assertEquals(2, events.size());
		assertNotNull(openBPMNModelManager.findEventByID(model, 2000, 10)); // save
		assertNotNull(openBPMNModelManager.findEventByID(model, 2000, 90)); // follow up

		// test task 2100
		task = openBPMNModelManager.findTaskByID(model, 2100);
		assertNotNull(task);
		events = openBPMNModelManager.findEventsByTask(model, 2100);
		assertNotNull(events);
		// there are no events....
		assertEquals(0, events.size());

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
			assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// test task 2000
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 2000);
		assertNotNull(task);
		List<ItemCollection> events = openBPMNModelManager.findEventsByTask(model, 2000);
		assertNotNull(events);
		// there are 3 events....
		assertEquals(2, events.size());
		assertNotNull(openBPMNModelManager.findEventByID(model, 2000, 10)); // save
		assertNotNull(openBPMNModelManager.findEventByID(model, 2000, 90)); // follow up

		// test task 2100
		task = openBPMNModelManager.findTaskByID(model, 2100);
		assertNotNull(task);
		events = openBPMNModelManager.findEventsByTask(model, 2100);
		assertNotNull(events);
		// there are 2 events....
		assertEquals(1, events.size());
		assertNotNull(openBPMNModelManager.findEventByID(model, 2100, 80)); // archive

	}

}
