package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
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
import org.xml.sax.SAXException;

/**
 * Test class to test the behavior of the ModelManager.
 * 
 * Test class testing message elements
 * 
 * @author rsoika
 */
public class TestBPMNParserMessageText {

	BPMNModel model = null;
	ModelManager modelManager = null;
	MockWorkflowContext workflowContext;

	@BeforeEach
	public void setup() {
		try {
			workflowContext = new MockWorkflowContext();
			modelManager = new ModelManager(workflowContext);
			workflowContext.loadBPMNModelFromFile("/bpmn/message_example.bpmn");
			model = workflowContext.fetchModel("1.0.0");
			assertNotNull(model);

		} catch (ModelException | PluginException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSimple() throws ParseException,
			ParserConfigurationException, SAXException, IOException, ModelException {

		Set<String> groups = modelManager.findAllGroupsByModel(model);
		assertTrue(groups.contains("Message Example"));

		// test count of elements
		assertEquals(2, model.findAllActivities().size());

		// test task 1000
		ItemCollection task = modelManager.findTaskByID(model, 1000);
		assertNotNull(task);

		// test activity for task 1000
		List<ItemCollection> events;

		events = modelManager.findEventsByTask(model, 1000);

		assertNotNull(events);
		assertEquals(1, events.size());

		// test event 1000.10 submit
		ItemCollection event = modelManager.findEventByID(model, 1000, 10);
		assertNotNull(event);

		assertEquals("Some MessageMessage-Text",
				event.getItemValueString(BPMNUtil.EVENT_ITEM_MAIL_SUBJECT));

		String message = event.getItemValueString(BPMNUtil.EVENT_ITEM_MAIL_BODY);

		assertEquals(
				"<h1>Some Message Text</h1>\nThis is some message\nMessage-Text",
				message);

	}

}
