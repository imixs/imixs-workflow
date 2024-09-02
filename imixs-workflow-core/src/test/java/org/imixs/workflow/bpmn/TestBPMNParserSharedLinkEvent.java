package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.text.ParseException;

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
 * Test class test the Imixs BPMNParser.
 * 
 * This test verifies the linking an imixs-event with an imixs-task using a
 * intermediate catch and intermediate throw link-event.
 * 
 * @author rsoika
 */
public class TestBPMNParserSharedLinkEvent {

	BPMNModel model = null;
	ModelManager openBPMNModelManager = null;

	@Before
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new ModelManager();
	}

	/**
	 * This test class tests intermediate link events
	 */
	@Test
	public void testLinkEventSimple() {

		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/shared-link-event.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			Assert.assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		// Test Environment
		Assert.assertTrue(openBPMNModelManager.findAllGroupsByModel(model).contains("Simple"));

		// test count of elements
		Assert.assertEquals(3, model.findAllActivities().size());

		// test task 1000
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 1000);
		Assert.assertEquals("Task Shared Link Event1", task.getItemValueString("txtName"));

		// test shared events
		Assert.assertEquals(3, openBPMNModelManager.findEventsByTask(model, 1000).size());

		ItemCollection event = openBPMNModelManager.findEventByID(model, 1000, 99);
		Assert.assertEquals("cancel", event.getItemValueString("txtName"));

		// test shared events
		Assert.assertEquals(2, openBPMNModelManager.findEventsByTask(model, 1100).size());
		Assert.assertEquals(0, openBPMNModelManager.findEventsByTask(model, 1200).size());

	}

}
