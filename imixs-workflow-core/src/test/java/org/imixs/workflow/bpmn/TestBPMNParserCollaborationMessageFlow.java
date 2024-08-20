package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.MessageFlow;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;
import org.xml.sax.SAXException;

/**
 * Test class test the Imixs BPMNParser
 * 
 * Special cases with collaboration diagrams
 * 
 * @author rsoika
 */
public class TestBPMNParserCollaborationMessageFlow {

	BPMNModel model = null;
	OpenBPMNModelManager openBPMNModelManager = null;

	@Before
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new OpenBPMNModelManager();

	}

	/**
	 * Find the message flow between the event 1000.20 and 2000.5
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException
	 */
	@Test
	public void testSimple() {

		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/collaboration_messageflow.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			Assert.assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		// test count of elements
		Assert.assertEquals(2, model.findAllActivities().size());

		// test task 1000
		ItemCollection task1 = openBPMNModelManager.findTaskByID(model, 1000);

		Assert.assertNotNull(task1);
		try {
			Collection<ItemCollection> events = openBPMNModelManager.findEventsByTask(model, 1000);
			Assert.assertNotNull(events);
			Assert.assertEquals(1, events.size());
		} catch (BPMNModelException e) {
			Assert.fail(e.getMessage());
		}

		// load event 1000.10 and test the message flow
		ItemCollection event = openBPMNModelManager.findEventByID(model, 1000, 10);
		Assert.assertNotNull(event);
		Assert.assertEquals("submit", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_NAME));
		BPMNElementNode eventElement = (BPMNElementNode) model.findElementById(event.getItemValueString("id"));
		Assert.assertNotNull(eventElement);
		// we expect one outgoing message flow
		Set<MessageFlow> messageFlows = eventElement.getOutgoingMessageFlows();
		Assert.assertNotNull(messageFlows);
		Assert.assertEquals(1, messageFlows.size());
		// load taget
		BPMNElementNode target = messageFlows.iterator().next().getTargetElement();
		Assert.assertNotNull(target);
		// we expect the Task 2
		ItemCollection task2 = openBPMNModelManager.findTaskByID(model, 1100);
		Assert.assertEquals(target.getId(), task2.getItemValueString("id"));

	}

	/**
	 * Find the message flow between the event 1000.20 and 2000.5
	 * 
	 * 
	 */
	@Test
	public void testComplex() {

		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/collaboration_messageflow_complex.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			Assert.assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		// test count of elements
		Assert.assertEquals(4, model.findAllActivities().size());

		// test task 1000
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 1000);
		Assert.assertNotNull(task);
		try {
			Collection<ItemCollection> events = openBPMNModelManager.findEventsByTask(model, 1000);
			Assert.assertNotNull(events);
			Assert.assertEquals(2, events.size());
		} catch (BPMNModelException e) {
			Assert.fail(e.getMessage());
		}

		// load activity 1000.20
		ItemCollection event = openBPMNModelManager.findEventByID(model, 1000, 20);
		Assert.assertNotNull(event);
		Assert.assertEquals("submit", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_NAME));

		// Test the message flow
		// we expect one outgoing message flow
		BPMNElementNode eventElement = (BPMNElementNode) model.findElementById(event.getItemValueString("id"));
		Set<MessageFlow> messageFlows = eventElement.getOutgoingMessageFlows();
		Assert.assertNotNull(messageFlows);
		Assert.assertEquals(1, messageFlows.size());
		// load taget
		BPMNElementNode target = messageFlows.iterator().next().getTargetElement();
		Assert.assertNotNull(target);
		// we expect the target Event 2000.5
		ItemCollection eventTarget = openBPMNModelManager.findEventByID(model, 2000, 5);
		Assert.assertEquals(target.getId(), eventTarget.getItemValueString("id"));

	}

}
