package org.imixs.workflow.kernel;

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
import org.imixs.workflow.MockWorkflowEngine;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

	private BPMNModel model;

	private MockWorkflowEngine workflowEngine;

	@BeforeEach
	public void setup() throws PluginException {
		workflowEngine = new MockWorkflowEngine();
	}

	/**
	 * This test tests the model structure of the complex model
	 */
	@Test
	public void testModelElements() {

		// load default model
		ModelManager modelManager = new ModelManager();
		try {
			model = BPMNModelFactory.read("/bpmn/event_rules.bpmn");

		} catch (BPMNModelException e) {
			e.printStackTrace();
			fail();
		}

		assertNotNull(model);
		try {
			// Test Environment
			ItemCollection workitem = new ItemCollection();
			workitem.model("1.0.0").task(1000);
			ItemCollection profile = workflowEngine.getWorkflowKernel().getModelManager().loadDefinition(model);
			assertNotNull(profile);

			// test count of task elements
			Set<Activity> tasks = model.openDefaultProces().getActivities();
			assertEquals(9, tasks.size());

			// test task 1000
			ItemCollection task;

			task = workflowEngine.getWorkflowKernel().getModelManager().loadTask(workitem, model);

			assertNotNull(task);

			// test activity for task 1000
			List<ItemCollection> events = modelManager.findEventsByTask(model, 1000);

			assertNotNull(events);
			assertEquals(1, events.size());

			// test activity 1000.10 submit
			ItemCollection event = modelManager.findEventByID(model, 1000, 10);
			assertNotNull(event);
			assertEquals("submit", event.getItemValueString("txtname"));
		} catch (ModelException e) {
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

		try {

			// load test models
			workflowEngine.loadBPMNModelFromFile("/bpmn/event_rules.bpmn");
			BPMNModel model = workflowEngine.loadModel("1.0.0");
			assertNotNull(model);

			// Test Environment
			ItemCollection workItem = new ItemCollection();
			workItem.model("1.0.0").task(2000).event(10);
			ItemCollection profile = workflowEngine.getWorkflowKernel().getModelManager().loadDefinition(model);
			assertNotNull(profile);

			/* Test 2000.10 - FollowUp Event */
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			assertEquals(2, workItem.getItemValueInteger("runs"));
			// Test model switch to mode 1.0.0
			assertEquals("1.0.0", workItem.getModelVersion());
			assertEquals(2100, workItem.getTaskID());
		} catch (ModelException | PluginException e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Invalid situation - we do not expect that we have more than one target task
	 * 
	 */
	@Test
	public void testSimpleAmbiguousSequenceFlow() {

		try {

			// load test models
			workflowEngine.loadBPMNModelFromFile("/bpmn/event_rules.bpmn");
			BPMNModel model = workflowEngine.loadModel("1.0.0");
			assertNotNull(model);

			ItemCollection workItem = new ItemCollection();
			workItem.model("1.0.0").task(3000).event(10);
			assertNotNull(model);
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			// Should not be possible
			fail("ambiguous sequence flow expected!");
		} catch (ModelException e) {
			// ambiguous sequence flow Exception expected
			assertTrue(e.getMessage().contains("ambiguous sequence flow"));
		} catch (PluginException e) {
			fail(e.getMessage());
		}
	}
}
