package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.MessageFlow;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.xml.sax.SAXException;

/**
 * Test class test the Imixs BPMNParser
 * 
 * Special cases with collaboration diagrams
 * 
 * @author rsoika
 */
public class TestModelManagerCollaborationMessageFlow {

	BPMNModel model = null;
	ModelManager modelManager = null;
	MockWorkflowContext workflowContext;

	@BeforeEach
	public void setup() {
		try {
			workflowContext = new MockWorkflowContext();
			modelManager = new ModelManager(workflowContext);
		} catch (PluginException e) {
			fail(e.getMessage());
		}
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
			workflowContext.loadBPMNModelFromFile("/bpmn/collaboration_messageflow.bpmn");
			model = workflowContext.fetchModel("1.0.0");
			assertNotNull(model);
		} catch (ModelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// test count of elements
		assertEquals(2, model.findAllActivities().size());

		// test task 1000
		ItemCollection task1 = modelManager.findTaskByID(model, 1000);

		assertNotNull(task1);
		Collection<ItemCollection> events = modelManager.findEventsByTask(model, 1000);
		assertNotNull(events);
		assertEquals(1, events.size());

		// load event 1000.10 and test the message flow
		ItemCollection event = modelManager.findEventByID(model, 1000, 10);
		assertNotNull(event);
		assertEquals("submit", event.getItemValueString(BPMNUtil.EVENT_ITEM_NAME));
		BPMNElementNode eventElement = (BPMNElementNode) model.findElementById(event.getItemValueString("id"));
		assertNotNull(eventElement);
		// we expect one outgoing message flow
		Set<MessageFlow> messageFlows = eventElement.getOutgoingMessageFlows();
		assertNotNull(messageFlows);
		assertEquals(1, messageFlows.size());
		// load taget
		BPMNElementNode target = messageFlows.iterator().next().getTargetElement();
		assertNotNull(target);
		// we expect the Task 2
		ItemCollection task2 = modelManager.findTaskByID(model, 1100);
		assertEquals(target.getId(), task2.getItemValueString("id"));

	}

	/**
	 * Find the message flow between the event 1000.20 and 2000.5
	 * 
	 * 
	 */
	@Test
	public void testComplex() {
		try {
			workflowContext.loadBPMNModelFromFile("/bpmn/collaboration_messageflow_complex.bpmn");
			model = workflowContext.fetchModel("1.0.0");
			assertNotNull(model);
		} catch (ModelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// test count of elements
		assertEquals(4, model.findAllActivities().size());

		// test task 1000
		ItemCollection task = modelManager.findTaskByID(model, 1000);
		assertNotNull(task);
		Collection<ItemCollection> events = modelManager.findEventsByTask(model, 1000);
		assertNotNull(events);
		assertEquals(2, events.size());

		// load activity 1000.20
		ItemCollection event = modelManager.findEventByID(model, 1000, 20);
		assertNotNull(event);
		assertEquals("submit", event.getItemValueString(BPMNUtil.EVENT_ITEM_NAME));

		// Test the message flow
		// we expect one outgoing message flow
		BPMNElementNode eventElement = (BPMNElementNode) model.findElementById(event.getItemValueString("id"));
		Set<MessageFlow> messageFlows = eventElement.getOutgoingMessageFlows();
		assertNotNull(messageFlows);
		assertEquals(1, messageFlows.size());
		// load taget
		BPMNElementNode target = messageFlows.iterator().next().getTargetElement();
		assertNotNull(target);
		// we expect the target Event 2000.5
		ItemCollection eventTarget = modelManager.findEventByID(model, 2000, 5);
		assertEquals(target.getId(), eventTarget.getItemValueString("id"));

	}

}
