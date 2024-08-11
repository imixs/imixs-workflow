package org.imixs.workflow;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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

	private MockWorkflowEnvironment workflowEnvironment;

	@Before
	public void setup() throws PluginException {
		workflowEnvironment = new MockWorkflowEnvironment();
		// load default model
		workflowEnvironment.loadBPMNModel("/bpmn/conditional_complex_event0.bpmn");
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
			workitemProcessed = workflowEnvironment.getWorkflowKernel().process(workItem);
			// We expect 2100
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
		// we expect 2 rund because of the conditional event
		Assert.assertEquals(2, workitemProcessed.getItemValueInteger("runs"));
		Assert.assertEquals(2100, workitemProcessed.getTaskID());

		// try {
		// model = BPMNParser.parseModel(inputStream, "UTF-8");

		// ItemCollection task2000 = model.getTask(2000);
		// ItemCollection task2200 = model.getTask(2200);

		// List<ItemCollection> events2000 = model.findAllEventsByTask(2000);
		// Assert.assertEquals(3, events2000.size());

		// List<ItemCollection> events2200 = model.findAllEventsByTask(2200);
		// Assert.assertEquals(5, events2200.size());

		// // NOTE:
		// // The following check is not resolvelable because in the demo model
		// // task 2200 contains a duplicate eventID which is not detected by the
		// Parser!!

		// // Check event 2200.100 pointing to 2100
		// ItemCollection event = model.getEvent(2200, 100);
		// Assert.assertEquals(2100, event.getItemValueInteger("numnextprocessid"));

		// event = model.getEvent(2200, 100);
		// Assert.assertEquals(2100, event.getItemValueInteger("numnextprocessid"));

		// } catch (UnsupportedEncodingException e) {
		// e.printStackTrace();
		// Assert.fail();
		// } catch (ModelException e) {
		// e.printStackTrace();
		// Assert.fail();
		// }
		// Assert.assertNotNull(model);

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
			workitemProcessed = workflowEnvironment.getWorkflowKernel().process(workItem);
			// We expect 2100
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
		// we expect 2 rund because of the conditional event
		Assert.assertEquals(2, workitemProcessed.getItemValueInteger("runs"));
		Assert.assertEquals(2001, workitemProcessed.getTaskID());

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
			workitemProcessed = workflowEnvironment.getWorkflowKernel().process(workItem);
			// We expect 2100 and 2 runs because of the conditional event
			Assert.assertEquals(2, workitemProcessed.getItemValueInteger("runs"));
			Assert.assertEquals(2100, workitemProcessed.getTaskID());
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		// next test event 20
		try {
			workItem.event(20);
			workitemProcessed = workflowEnvironment.getWorkflowKernel().process(workItem);
			// We expect 2200 and 3 runs
			Assert.assertEquals(3, workitemProcessed.getItemValueInteger("runs"));
			Assert.assertEquals(2200, workitemProcessed.getTaskID());
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		// Test Escalate we trigger 2200.100 - no status change expected
		try {
			workItem.event(100);
			workitemProcessed = workflowEnvironment.getWorkflowKernel().process(workItem);
			// We expect 2200 and 4 runs
			Assert.assertEquals(4, workitemProcessed.getItemValueInteger("runs"));
			Assert.assertEquals(2200, workitemProcessed.getTaskID());
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		// Finally we trigger 2200.20 that should lead again to 2100 next test event 20
		try {
			workItem.event(20);
			workitemProcessed = workflowEnvironment.getWorkflowKernel().process(workItem);
			// We expect 2100 and 6 runs because of the condition
			Assert.assertEquals(6, workitemProcessed.getItemValueInteger("runs"));
			Assert.assertEquals(2100, workitemProcessed.getTaskID());
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

	}

}