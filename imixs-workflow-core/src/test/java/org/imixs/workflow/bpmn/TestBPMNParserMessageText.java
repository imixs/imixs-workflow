package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;
import org.xml.sax.SAXException;

/**
 * Test class testing message elements
 * 
 * @author rsoika
 */
public class TestBPMNParserMessageText {
	BPMNModel model = null;
	ModelManager openBPMNModelManager = null;

	@Before
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new ModelManager();

	}

	@Test
	public void testSimple() throws ParseException,
			ParserConfigurationException, SAXException, IOException, ModelException {

		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/message_example.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			Assert.assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail();
		}

		Set<String> groups = openBPMNModelManager.findAllGroupsByModel(model);
		Assert.assertTrue(groups.contains("Message Example"));

		// test count of elements
		Assert.assertEquals(2, model.findAllActivities().size());

		// test task 1000
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 1000);
		Assert.assertNotNull(task);

		// test activity for task 1000
		List<ItemCollection> events;

		events = openBPMNModelManager.findEventsByTask(model, 1000);

		Assert.assertNotNull(events);
		Assert.assertEquals(1, events.size());

		// test event 1000.10 submit
		ItemCollection event = openBPMNModelManager.findEventByID(model, 1000, 10);
		Assert.assertNotNull(event);

		Assert.assertEquals("Some MessageMessage-Text",
				event.getItemValueString(BPMNUtil.EVENT_ITEM_MAIL_SUBJECT));

		String message = event.getItemValueString(BPMNUtil.EVENT_ITEM_MAIL_BODY);

		Assert.assertEquals(
				"<h1>Some Message Text</h1>\nThis is some message\nMessage-Text",
				message);

	}

}
