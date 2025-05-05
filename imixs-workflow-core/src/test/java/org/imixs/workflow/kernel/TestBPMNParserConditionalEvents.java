package org.imixs.workflow.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.MockWorkflowEngine;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.xml.sax.SAXException;

/**
 * This test class is testing various less and more complex conditional event
 * situations. The test also covers conditional events with parallel gateways
 * (split events).
 * 
 * @see issue #299
 * @author rsoika
 */
public class TestBPMNParserConditionalEvents {

	MockWorkflowContext workflowContext;
	MockWorkflowEngine workflowEngine;

	@BeforeEach
	public void setup() {
		try {
			workflowContext = new MockWorkflowContext();
			workflowEngine = new MockWorkflowEngine(workflowContext);
		} catch (PluginException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSimple() throws ModelException, BPMNModelException {

		// load test models
		workflowContext.loadBPMNModelFromFile("/bpmn/conditional_event1.bpmn");
		BPMNModel model = workflowContext.fetchModel("1.0.0");
		assertNotNull(model);
		// test count of elements
		assertEquals(3, model.findAllActivities().size());

		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0").task(1000).event(10);
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			assertEquals(1200, workItem.getTaskID());
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}

		// next test with budget >100
		workItem = new ItemCollection();
		workItem.model("1.0.0").task(1000).event(10);
		workItem.setItemValue("_budget", 1500.00);
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			assertEquals(1100, workItem.getTaskID());
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}

	}

	/**
	 * Like testSimple() but with a default conditional sequence flow....
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testSimpleDefault()
			throws ModelException {

		// load test models
		workflowContext.loadBPMNModelFromFile("/bpmn/conditional_event_default.bpmn");
		BPMNModel model = workflowContext.fetchModel("1.0.0");
		assertNotNull(model);

		// test activity 1000.10 submit
		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0").task(1000).event(10);
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			assertEquals(1200, workItem.getTaskID());
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}
	}

	/**
	 * Like testSimple() but with a non-imixs Task element between the sequence
	 * flow. The Expectation is that this element will be skipped.
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testSimpleWithSimpleTask()
			throws ModelException {
		// load test model
		workflowContext.loadBPMNModelFromFile("/bpmn/conditional_event3.bpmn");
		BPMNModel model = workflowContext.fetchModel("1.0.0");
		assertNotNull(model);

		// test activity 1000.10 submit
		ItemCollection workItem = new ItemCollection();
		workItem.setItemValue("_budget", 1500.00);
		workItem.model("1.0.0").task(1000).event(10);
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			assertEquals(1100, workItem.getTaskID());
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	// @Ignore
	public void testFollowUp()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {
		// load test model
		workflowContext.loadBPMNModelFromFile("/bpmn/conditional_event2.bpmn");
		BPMNModel model = workflowContext.fetchModel("1.0.0");
		assertNotNull(model);

		// test count of elements
		assertEquals(3, model.findAllActivities().size());

		// test activity 1000.10 submit
		ItemCollection workItem = new ItemCollection();
		workItem.setItemValue("_budget", 50.00);
		workItem.model("1.0.0").task(1000).event(10);
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			assertEquals(1200, workItem.getTaskID());
			assertEquals(2, workItem.getItemValueInteger("runs"));
			// last event = 20
			assertEquals(20, workItem.getItemValueInteger("$lastEvent"));
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}
	}

	/**
	 * This test combines a conditional event with a split event.
	 * 
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testConditionalSplitEvent() throws ModelException {

		// load test model
		workflowContext.loadBPMNModelFromFile("/bpmn/conditional_split_event.bpmn");
		BPMNModel model = workflowContext.fetchModel("1.0.0");
		assertNotNull(model);

		// test count of elements
		assertEquals(4, model.findAllActivities().size());
		// test activity 1000.10 submit
		ItemCollection workItem = new ItemCollection();
		workItem.setItemValue("_budget", 1520.00);
		workItem.model("1.0.0").task(1000).event(10);
		try {
			workItem = workflowEngine.process(workItem);
			assertEquals(1100, workItem.getTaskID());
			assertEquals(2, workItem.getItemValueInteger("runs"));
			// last event = 20
			assertEquals(20, workItem.getItemValueInteger("$lastEvent"));

			// We also expect a Split Workitem with the $lastEvent = 30
			List<ItemCollection> splitWorkitems = workflowEngine.getWorkflowKernel().getSplitWorkitems();
			assertNotNull(splitWorkitems);
			assertEquals(1, splitWorkitems.size());
			ItemCollection splitWorkitem = splitWorkitems.get(0);

			assertEquals(1200, splitWorkitem.getTaskID());
			assertEquals(30, splitWorkitem.getItemValueInteger("$lastEvent"));
			assertEquals(3, splitWorkitem.getItemValueInteger("runs"));

		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}

	}

}