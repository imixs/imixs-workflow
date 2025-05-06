package org.imixs.workflow.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.xml.sax.SAXException;

/**
 * Test class test the Imixs BPMNParser.
 * 
 * Test resolving a complex model situation with a conditional event.....
 * 
 * Check event 2200.100 pointing to 2100
 * 
 * Event case-2 (100) is indirectly connected with Task 2200 that also have a
 * event with the ID 100 (escalate)
 * 
 * See Issue #823 for details!
 * 
 * Special case: Conditional-Events
 * 
 * @see issue #299
 * @author rsoika
 */
public class TestWorkflowKernelComplex_Issue_823 {

	MockWorkflowContext workflowEngine;

	@BeforeEach
	public void setup() {
		try {
			workflowEngine = new MockWorkflowContext();
			workflowEngine.loadBPMNModelFromFile("/bpmn/conditional_complex_event0.bpmn");
			BPMNModel model = workflowEngine.fetchModel("1.0.0");
			assertNotNull(model);

		} catch (ModelException | PluginException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Here we test _capacity below 100
	 * 
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException
	 */
	@Test
	public void testComplexCase1()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		ItemCollection workitemProcessed = null;
		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0")
				.task(2000)
				.event(20);
		workItem.replaceItemValue("_capacity", 90); // <100.00

		try {
			workitemProcessed = workflowEngine.getWorkflowKernel().process(workItem);
			// We expect 2100
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			fail();
		}
		// we expect 2 rund because of the conditional event
		assertEquals(2, workitemProcessed.getItemValueInteger("runs"));
		assertEquals(2100, workitemProcessed.getTaskID());

	}

	/**
	 * Here we test _capacity > 100
	 * 
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException
	 */
	@Test
	public void testComplexCase2()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		ItemCollection workitemProcessed = null;
		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0")
				.task(2000)
				.event(20);
		workItem.replaceItemValue("_capacity", 500); // >100.00

		try {
			workitemProcessed = workflowEngine.getWorkflowKernel().process(workItem);
			// We expect 2100
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			fail();
		}
		// we expect 2 rund because of the conditional event
		assertEquals(2, workitemProcessed.getItemValueInteger("runs"));
		assertEquals(2001, workitemProcessed.getTaskID());

	}

	/**
	 * Here we test first with a _capacity > 100 that leads to 2100
	 * than we test the event 20 which has a link event leading to 2200
	 * 
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException
	 */
	@Test
	public void testComplexCase3()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		ItemCollection workitemProcessed = null;
		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0")
				.task(2000)
				.event(20);
		workItem.replaceItemValue("_capacity", 90); // <100.00

		try {
			workitemProcessed = workflowEngine.getWorkflowKernel().process(workItem);
			// We expect 2100 and 2 runs because of the conditional event
			assertEquals(2, workitemProcessed.getItemValueInteger("runs"));
			assertEquals(2100, workitemProcessed.getTaskID());
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			fail();
		}

		// next test event 20
		try {
			workItem.event(20);
			workitemProcessed = workflowEngine.getWorkflowKernel().process(workItem);
			// We expect 2200 and 3 runs
			assertEquals(3, workitemProcessed.getItemValueInteger("runs"));
			assertEquals(2200, workitemProcessed.getTaskID());
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			fail();
		}

		// Test Escalate we trigger 2200.100 - no status change expected
		try {
			workItem.event(100);
			workitemProcessed = workflowEngine.getWorkflowKernel().process(workItem);
			// We expect 2200 and 4 runs
			assertEquals(4, workitemProcessed.getItemValueInteger("runs"));
			assertEquals(2200, workitemProcessed.getTaskID());
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			fail();
		}

		// Finally we trigger 2200.20 that should lead again to 2100 next test event 20
		try {
			workItem.event(20);
			workitemProcessed = workflowEngine.getWorkflowKernel().process(workItem);
			// We expect 2100 and 6 runs because of the condition
			assertEquals(6, workitemProcessed.getItemValueInteger("runs"));
			assertEquals(2100, workitemProcessed.getTaskID());
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			fail();
		}

	}

}