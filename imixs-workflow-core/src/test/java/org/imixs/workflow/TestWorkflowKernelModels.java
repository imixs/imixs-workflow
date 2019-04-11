package org.imixs.workflow;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.Principal;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.SessionContext;
import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

/**
 * Test class for Imixs WorkflowKernel using the test models. The test class
 * verifies complex model situations based on the test models.
 * 
 * @author rsoika
 * 
 */
public class TestWorkflowKernelModels {

	WorkflowKernel kernel = null;
	protected SessionContext ctx;
	protected WorkflowContext workflowContext;
	private static Logger logger = Logger.getLogger(TestWorkflowKernelModels.class.getName());

	@Before
	public void setup() throws PluginException, ModelException, ParseException, ParserConfigurationException,
			SAXException, IOException {

		ctx = Mockito.mock(SessionContext.class);
		// simulate SessionContext ctx.getCallerPrincipal().getName()
		Principal principal = Mockito.mock(Principal.class);
		when(principal.getName()).thenReturn("manfred");
		when(ctx.getCallerPrincipal()).thenReturn(principal);

		workflowContext = Mockito.mock(WorkflowContext.class);

		// MokWorkflowContext ctx = new MokWorkflowContext();
		kernel = new WorkflowKernel(workflowContext);

		MokPlugin mokPlugin = new MokPlugin();
		kernel.registerPlugin(mokPlugin);

		logger.fine("init mocks completed");
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
			// provide a mock modelManger class
			when(workflowContext.getModelManager()).thenReturn(new MokModelManager("/bpmn/simple.bpmn"));

			ItemCollection itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("txtTitel", "Hello");
			itemCollection.setTaskID(1000);
			itemCollection.setEventID(10);

			itemCollection.replaceItemValue("$modelversion", MokModel.DEFAULT_MODEL_VERSION);

			itemCollection = kernel.process(itemCollection);
			Assert.assertEquals("Hello", itemCollection.getItemValueString("txttitel"));
			Assert.assertEquals("workitem", itemCollection.getItemValueString("type"));
			Assert.assertEquals(1000, itemCollection.getTaskID());

			itemCollection.event(20);
			itemCollection = kernel.process(itemCollection);
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
			// provide a mock modelManger class
			when(workflowContext.getModelManager()).thenReturn(new MokModelManager("/bpmn/ticket.bpmn"));

			ItemCollection itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("txtTitel", "Hello");
			itemCollection.setTaskID(1100);
			itemCollection.setEventID(20);

			itemCollection.replaceItemValue("$modelversion", MokModel.DEFAULT_MODEL_VERSION);

			itemCollection = kernel.process(itemCollection);
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
			// provide a mock modelManger class
			when(workflowContext.getModelManager()).thenReturn(new MokModelManager("/bpmn/conditional_event1.bpmn"));

			// test Condition 1
			ItemCollection itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("txtTitel", "Hello");
			itemCollection.setTaskID(1000);
			itemCollection.setEventID(10);
			itemCollection.replaceItemValue("$modelversion", MokModel.DEFAULT_MODEL_VERSION);

			itemCollection.replaceItemValue("_budget", 99);

			itemCollection = kernel.process(itemCollection);
			Assert.assertEquals("Hello", itemCollection.getItemValueString("txttitel"));
			Assert.assertEquals(1200, itemCollection.getTaskID());

			// test Condition 2
			itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("txtTitel", "Hello");
			itemCollection.setTaskID(1000);
			itemCollection.setEventID(10);
			itemCollection.replaceItemValue("$modelversion", MokModel.DEFAULT_MODEL_VERSION);

			itemCollection.replaceItemValue("_budget", 9999);

			itemCollection = kernel.process(itemCollection);
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
			// provide a mock modelManger class
			when(workflowContext.getModelManager()).thenReturn(new MokModelManager("/bpmn/conditional_event2.bpmn"));

			// test Condition 1
			ItemCollection itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("txtTitel", "Hello");
			itemCollection.setTaskID(1000);
			itemCollection.setEventID(10);
			itemCollection.replaceItemValue("$modelversion", MokModel.DEFAULT_MODEL_VERSION);

			itemCollection.replaceItemValue("_budget", 9999);

			itemCollection = kernel.process(itemCollection);
			Assert.assertEquals("Hello", itemCollection.getItemValueString("txttitel"));
			Assert.assertEquals(1100, itemCollection.getTaskID());

			// test Condition 2
			itemCollection = new ItemCollection().model(MokModel.DEFAULT_MODEL_VERSION).task(1000).event(10);
			itemCollection.replaceItemValue("txtTitel", "Hello");

			itemCollection.replaceItemValue("_budget", 99);

			itemCollection = kernel.process(itemCollection);
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
			// provide a mock modelManger class
			when(workflowContext.getModelManager()).thenReturn(new MokModelManager("/bpmn/split_event1.bpmn"));

			// test Condition 1
			ItemCollection itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("_subject", "Hello");
			itemCollection.setTaskID(1000);
			itemCollection.setEventID(10);
			itemCollection.replaceItemValue("$modelversion", MokModel.DEFAULT_MODEL_VERSION);

			itemCollection = kernel.process(itemCollection);
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

		// provide a mock modelManger class
		try {
			when(workflowContext.getModelManager()).thenReturn(new MokModelManager("/bpmn/split_event1_invalid.bpmn"));
		} catch (ModelException | ParseException | ParserConfigurationException | SAXException | IOException e1) {
			e1.printStackTrace();
			Assert.fail();
		}

		// test Condition 1
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("_subject", "Hello");
		itemCollection.replaceItemValue("$processid", 1000);
		itemCollection.setEventID(10);
		itemCollection.replaceItemValue("$modelversion", MokModel.DEFAULT_MODEL_VERSION);

		// model exception expected!
		try {
			itemCollection = kernel.process(itemCollection);
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
