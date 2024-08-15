package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowEnvironment;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;
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

	private MockWorkflowEnvironment mockEnvironment;
	private BPMNModel model;

	@Before
	public void setup() throws PluginException {
		mockEnvironment = new MockWorkflowEnvironment();
	}

	/**
	 * This test tests the model structure of the complex model
	 */
	@Test
	public void testModelElements() {

		// load default model
		OpenBPMNModelManager openBPMNModelManager = new OpenBPMNModelManager();
		try {
			model = BPMNModelFactory.read("/bpmn/event_rules.bpmn");
			openBPMNModelManager.addModel(model);
		} catch (BPMNModelException | ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(model);
		try {
			// Test Environment
			ItemCollection workitem = new ItemCollection();
			workitem.model("1.0.0").task(1000);
			ItemCollection profile = openBPMNModelManager.loadDefinition(workitem);
			Assert.assertNotNull(profile);

			// test count of task elements
			Set<Activity> tasks = model.openDefaultProces().getActivities();
			Assert.assertEquals(9, tasks.size());

			// test task 1000
			ItemCollection task;

			task = mockEnvironment.getWorkflowContext().getModelManager().loadTask(workitem);

			Assert.assertNotNull(task);

			// test activity for task 1000
			List<ItemCollection> events = openBPMNModelManager.findEventsByTask(model, 1000);

			Assert.assertNotNull(events);
			Assert.assertEquals(1, events.size());

			// test activity 1000.10 submit
			ItemCollection event = openBPMNModelManager.findEventByID(model, 1000, 10);
			Assert.assertNotNull(event);
			Assert.assertEquals("submit", event.getItemValueString("txtname"));
		} catch (ModelException | BPMNModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * This test tests a follow up event situation
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException
	 */
	@Test
	public void testFollowUp() {

		mockEnvironment.loadBPMNModel("/bpmn/event_rules.bpmn");
		try {
			model = mockEnvironment.getWorkflowContext().getModelManager().getModel("1.0.0");

			Assert.assertNotNull(model);

			// Test Environment
			ItemCollection workItem = new ItemCollection();
			workItem.model("1.0.0").task(2000).event(10);
			ItemCollection profile = mockEnvironment.getWorkflowContext().getModelManager().loadDefinition(workItem);
			Assert.assertNotNull(profile);

			/* Test 2000.10 - FollowUp Event */
			workItem = mockEnvironment.getWorkflowKernel().process(workItem);
			Assert.assertEquals(2, workItem.getItemValueInteger("runs"));
			// Test model switch to mode 1.0.0
			Assert.assertEquals("1.0.0", workItem.getModelVersion());
			Assert.assertEquals(2100, workItem.getTaskID());
		} catch (ModelException | PluginException e) {
			Assert.fail(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * What should happen here?
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException
	 */
	@Test
	public void testSimpleNoGateway() {

		mockEnvironment.loadBPMNModel("/bpmn/event_rules.bpmn");
		try {
			model = mockEnvironment.getWorkflowContext().getModelManager().getModel("1.0.0");
			ItemCollection workItem = new ItemCollection();
			workItem.model("1.0.0").task(3000).event(10);
			Assert.assertNotNull(model);
			workItem = mockEnvironment.getWorkflowKernel().process(workItem);

			// Should not be possible
			Assert.fail();

			Assert.assertEquals(1, workItem.getItemValueInteger("runs"));
			// Test model switch to mode 1.0.0
			Assert.assertEquals("1.0.0", workItem.getModelVersion());
			Assert.assertEquals(3100, workItem.getTaskID());
		} catch (ModelException | PluginException e) {
			Assert.fail(e.getMessage());
			e.printStackTrace();
		}

	}
}
