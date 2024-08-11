package org.imixs.workflow;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.xml.sax.SAXException;

/**
 * Test class for Imixs WorkflowKernel using the test models. The test class
 * verifies complex model situations based on the test models.
 * 
 * @author rsoika
 * 
 */
public class TestWorkflowKernelModels {

	private static final Logger logger = Logger.getLogger(TestWorkflowKernelModels.class.getName());
	private MockWorkflowEnvironment workflowContext;

	@Before
	public void setup() throws PluginException {
		workflowContext = new MockWorkflowEnvironment();
	}

	/**
	 * Simple test
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws ParseException
	 * @throws ModelException
	 */
	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testSimpleModel() {
		try {
			workflowContext.loadBPMNModel("/bpmn/simple.bpmn");

			ItemCollection itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("txtTitel", "Hello");
			itemCollection.setTaskID(1000);
			itemCollection.setEventID(10);

			itemCollection.replaceItemValue("$modelversion", "1.0.0");

			itemCollection = workflowContext.getWorkflowKernel().process(itemCollection);
			Assert.assertEquals("Hello", itemCollection.getItemValueString("txttitel"));
			Assert.assertEquals("workitem", itemCollection.getItemValueString("type"));
			Assert.assertEquals(1000, itemCollection.getTaskID());

			itemCollection.event(20);
			itemCollection = workflowContext.getWorkflowKernel().process(itemCollection);
			Assert.assertEquals("workitemarchive", itemCollection.getItemValueString("type"));
			Assert.assertEquals(1100, itemCollection.getTaskID());

		} catch (Exception e) {
			Assert.fail();
			e.printStackTrace();
		}

	}

	/**
	 * ticket.bpmn test
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws ParseException
	 * @throws ModelException
	 */
	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testTicketModel() {
		try {
			workflowContext.loadBPMNModel("/bpmn/ticket.bpmn");

			ItemCollection itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("txtTitel", "Hello");
			itemCollection.setTaskID(1100);
			itemCollection.setEventID(20);

			itemCollection.replaceItemValue("$modelversion", "1.0.0");
			itemCollection = workflowContext.getWorkflowKernel().process(itemCollection);
			Assert.assertEquals("Hello", itemCollection.getItemValueString("txttitel"));
			Assert.assertEquals(1200, itemCollection.getTaskID());
			Assert.assertEquals("in Progress", itemCollection.getItemValueString("$workflowstatus"));
			Assert.assertEquals("Ticket", itemCollection.getItemValueString("$workflowgroup"));

			// test support for deprecated items
			Assert.assertEquals("in Progress", itemCollection.getItemValueString("txtworkflowstatus"));
			Assert.assertEquals("Ticket", itemCollection.getItemValueString("txtworkflowgroup"));

		} catch (Exception e) {
			Assert.fail();
			e.printStackTrace();
		}

	}

	/**
	 * Test model conditional_event1.bpmn.
	 * 
	 * Here we have two conditions: both to a task.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws ParseException
	 * @throws ModelException
	 */
	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testConditionalEventModel1() {
		try {
			workflowContext.loadBPMNModel("/bpmn/conditional_event1.bpmn");

			// test Condition 1
			ItemCollection itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("txtTitel", "Hello");
			itemCollection.setTaskID(1000);
			itemCollection.setEventID(10);
			itemCollection.replaceItemValue("$modelversion", "1.0.0");

			itemCollection.replaceItemValue("_budget", 99);

			itemCollection = workflowContext.getWorkflowKernel().process(itemCollection);
			Assert.assertEquals("Hello", itemCollection.getItemValueString("txttitel"));
			Assert.assertEquals(1200, itemCollection.getTaskID());

			// test Condition 2
			itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("txtTitel", "Hello");
			itemCollection.setTaskID(1000);
			itemCollection.setEventID(10);
			itemCollection.replaceItemValue("$modelversion", "1.0.0");

			itemCollection.replaceItemValue("_budget", 9999);

			itemCollection = workflowContext.getWorkflowKernel().process(itemCollection);
			Assert.assertEquals("Hello", itemCollection.getItemValueString("txttitel"));

			Assert.assertEquals(1100, itemCollection.getTaskID());

		} catch (Exception e) {
			Assert.fail();
			e.printStackTrace();

		}

	}

	/**
	 * Test model conditional_event2.bpmn.
	 * 
	 * Here we have two conditions: one to a task, the other to a event.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws ParseException
	 * @throws ModelException
	 */
	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testConditionalEventModel2() {
		try {
			workflowContext.loadBPMNModel("/bpmn/conditional_event2.bpmn");

			// test Condition 1
			ItemCollection itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("txtTitel", "Hello");
			itemCollection.setTaskID(1000);
			itemCollection.setEventID(10);
			itemCollection.replaceItemValue("$modelversion", "1.0.0");

			itemCollection.replaceItemValue("_budget", 9999);

			itemCollection = workflowContext.getWorkflowKernel().process(itemCollection);
			Assert.assertEquals("Hello", itemCollection.getItemValueString("txttitel"));
			Assert.assertEquals(1100, itemCollection.getTaskID());

			// test Condition 2
			itemCollection = new ItemCollection().model("1.0.0").task(1000).event(10);
			itemCollection.replaceItemValue("txtTitel", "Hello");

			itemCollection.replaceItemValue("_budget", 99);

			itemCollection = workflowContext.getWorkflowKernel().process(itemCollection);
			Assert.assertEquals("Hello", itemCollection.getItemValueString("txttitel"));

			Assert.assertEquals(1200, itemCollection.getTaskID());

		} catch (Exception e) {
			Assert.fail();
			e.printStackTrace();

		}

	}

	/**
	 * Test model split_event1.bpmn.
	 * 
	 * Here we have two conditions: both to a task.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws ParseException
	 * @throws ModelException
	 */
	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testSplitEventModel1() {
		try {
			workflowContext.loadBPMNModel("/bpmn/split_event1.bpmn");

			// test Condition 1
			ItemCollection itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("_subject", "Hello");
			itemCollection.setTaskID(1000);
			itemCollection.setEventID(10);
			itemCollection.replaceItemValue("$modelversion", "1.0.0");

			itemCollection = workflowContext.getWorkflowKernel().process(itemCollection);
			Assert.assertEquals("Hello", itemCollection.getItemValueString("_subject"));
			Assert.assertEquals(1100, itemCollection.getTaskID());
			Assert.assertEquals(10, itemCollection.getItemValueInteger("$lastEvent"));

			// test new version...
			List<ItemCollection> versions = kernel.getSplitWorkitems();
			Assert.assertNotNull(versions);
			Assert.assertTrue(versions.size() == 1);
			ItemCollection version = versions.get(0);

			Assert.assertEquals("Hello", version.getItemValueString("_subject"));
			Assert.assertEquals(1200, version.getTaskID());
			// $lastEvent should be 20
			Assert.assertEquals(20, version.getItemValueInteger("$lastEvent"));

			// Master $uniqueid must not match the version $uniqueid
			Assert.assertFalse(itemCollection.getUniqueID().equals(version.getUniqueID()));

			// $uniqueidSource must match $uni1ueid of master
			Assert.assertEquals(itemCollection.getUniqueID(),
					version.getItemValueString(WorkflowKernel.UNIQUEIDSOURCE));

			// $uniqueidVirsions must mach $uniqueid of version
			Assert.assertEquals(version.getUniqueID(),
					itemCollection.getItemValueString(WorkflowKernel.UNIQUEIDVERSIONS));

		} catch (Exception e) {
			Assert.fail();
			e.printStackTrace();

		}

	}

	/**
	 * Test model split_event1_invalid.bpmn.
	 * 
	 * This model is invalid as a outcome of the split-event is evaluated to 'false'
	 * and no follow-up event is defined!
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws ParseException
	 * @throws ModelException
	 */
	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testSplitEventInvalidModel() {

		workflowContext.loadBPMNModel("/bpmn/split_event1_invalid.bpmn");

		// test Condition 1
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("_subject", "Hello");
		itemCollection.replaceItemValue("$processid", 1000);
		itemCollection.setEventID(10);
		itemCollection.replaceItemValue("$modelversion", "1.0.0");

		// model exception expected!
		try {
			itemCollection = workflowContext.getWorkflowKernel().process(itemCollection);
			Assert.fail();
		} catch (ModelException e) {
			// expected behavior
		} catch (PluginException e) {
			// not expected
			e.printStackTrace();
			Assert.fail();
		}

	}

}
