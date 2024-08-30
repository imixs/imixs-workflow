package org.imixs.workflow.engine;

import java.util.ArrayList;
import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for the WorkflowService to test conditional and parallel gateways.
 * 
 * For Testcases test model files are loaded. This test verifies specific method
 * implementations of the workflowService by mocking the WorkflowService with
 * the @spy annotation.
 * 
 * 
 * @author rsoika
 */
public class TestWorkflowServiceGateways {

	// protected OldWorkflowMockEnvironment workflowMockEnvironment;

	protected WorkflowMockEnvironment workflowEnvironment;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {

		workflowEnvironment = new WorkflowMockEnvironment();
		workflowEnvironment.setUp();

		// @Before
		// public void setup() throws PluginException, ModelException {
		// workflowMockEnvironment = new OldWorkflowMockEnvironment();
		// workflowMockEnvironment.setup();

		// workflowMockEnvironment.loadModel("/bpmn/TestWorkflowService.bpmn");

	}

	/**
	 * This test tests the conditional event gateways....
	 * 
	 * This is just a simple simulation...
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * @throws ModelException
	 * @throws AdapterException
	 * 
	 */
	@Test
	public void testConditionalEvent1() {

		workflowEnvironment.loadBPMNModel("/bpmn/conditional_event1.bpmn");
		// load test workitem
		ItemCollection workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		try {

			workitem.replaceItemValue(WorkflowKernel.MODELVERSION, OldWorkflowMockEnvironment.DEFAULT_MODEL_VERSION);

			// test _budget<100
			workitem.setTaskID(1000);
			workitem.replaceItemValue("_budget", 99);
			workitem.setEventID(10);
			workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
			Assert.assertEquals(1200, workitem.getTaskID());

			// Next test _budget>100
			workitem.setTaskID(1000);
			workitem.replaceItemValue("_budget", 9999);
			workitem.setEventID(10);
			workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
			Assert.assertEquals(1100, workitem.getTaskID());

		} catch (AccessDeniedException | ProcessingErrorException | PluginException | ModelException e) {
			Assert.fail(e.getMessage());
		}

		// Test without _budget item. This results into a processing error:
		try {
			workitem.removeItem("_budget");
			workitem.setTaskID(1000);
			workitem.setEventID(10);
			workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
			Assert.fail(); // Exception expected!
			Assert.assertEquals("1.0.0", workitem.getItemValueString("$ModelVersion"));
			Assert.assertEquals(1000, workitem.getTaskID());
		} catch (AccessDeniedException | ProcessingErrorException | PluginException e) {
			Assert.fail(e.getMessage());
		} catch (ModelException e) {
			// Expected
			Assert.assertEquals(ModelException.INVALID_MODEL_ENTRY, e.getErrorCode());
		}

	}

	/**
	 * This test tests the conditional event gateways with a default condition....
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * @throws ModelException
	 * @throws AdapterException
	 * 
	 */
	@Test
	public void testConditionalDefaultEvent()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		workflowEnvironment.loadBPMNModel("/bpmn/conditional_default_event.bpmn");

		// load test workitem
		ItemCollection workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, OldWorkflowMockEnvironment.DEFAULT_MODEL_VERSION);

		// test _budget<100
		workitem.setTaskID(1000);
		workitem.replaceItemValue("_budget", 99);
		workitem.setEventID(10);
		workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
		Assert.assertEquals(1200, workitem.getTaskID());

		// test _budget>100
		workitem.setTaskID(1000);
		workitem.replaceItemValue("_budget", 9999);
		workitem.setEventID(10);
		workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
		Assert.assertEquals(1300, workitem.getTaskID());

		// test without any budget
		workitem.setTaskID(1000);
		workitem.removeItem("_budget");
		workitem.setEventID(10);
		workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
		Assert.assertEquals(1100, workitem.getTaskID());

	}

	/**
	 * This test tests the conditional event gateways....
	 * 
	 * This is just a simple simulation...
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * @throws ModelException
	 */
	@Test
	public void testSplitEvent1()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		workflowEnvironment.loadBPMNModel("/bpmn/split_event1.bpmn");

		// load test workitem
		ItemCollection workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, OldWorkflowMockEnvironment.DEFAULT_MODEL_VERSION);

		// test none condition ...
		workitem.replaceItemValue("_subject", "Hello");
		workitem.setTaskID(1000);
		workitem.setEventID(10);
		workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
		Assert.assertEquals("1.0.0", workitem.getItemValueString("$ModelVersion"));
		Assert.assertEquals(1100, workitem.getTaskID());

		// lookup the version.....
		List<ItemCollection> versions = new ArrayList<ItemCollection>();
		for (ItemCollection doc : workflowEnvironment.database.values()) {
			if (workitem.getUniqueID().equals(doc.getItemValueString(WorkflowKernel.UNIQUEIDSOURCE))) {
				versions.add(doc);
			}
		}
		// test new version...
		Assert.assertNotNull(versions);
		Assert.assertTrue(versions.size() == 1);
		ItemCollection version = versions.get(0);
		Assert.assertNotNull(version);

		Assert.assertEquals("Hello", version.getItemValueString("_subject"));
		Assert.assertEquals(1200, version.getTaskID());
		Assert.assertEquals(20, version.getItemValueInteger("$lastevent"));

	}

}
