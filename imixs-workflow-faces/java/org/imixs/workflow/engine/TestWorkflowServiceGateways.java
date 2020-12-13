package org.imixs.workflow.engine;

import java.util.ArrayList;
import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Before;
import org.junit.Test;

import org.junit.Assert;

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

	protected WorkflowMockEnvironment workflowMockEnvironment;

	@Before
	public void setup() throws PluginException, ModelException {
		workflowMockEnvironment = new WorkflowMockEnvironment();
		workflowMockEnvironment.setup();

		workflowMockEnvironment.loadModel("/bpmn/TestWorkflowService.bpmn");

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
	public void testConditionalEvent1()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		workflowMockEnvironment.loadModel("/bpmn/conditional_event1.bpmn");

		// load test workitem
		ItemCollection workitem = workflowMockEnvironment.database.get("W0000-00001");
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, WorkflowMockEnvironment.DEFAULT_MODEL_VERSION);

		// test none condition ...
//		workitem.setTaskID(1000);
//		workitem.setEventID(10);
//		workitem = workflowMockEnvironment.workflowService.processWorkItem(workitem);
//		Assert.assertEquals("1.0.0", workitem.getItemValueString("$ModelVersion"));
//		Assert.assertEquals(1000, workitem.getTaskID());

		// test _budget<100
		workitem.setTaskID(1000);
		workitem.replaceItemValue("_budget", 99);
		workitem.setEventID(10);
		workitem = workflowMockEnvironment.workflowService.processWorkItem(workitem);
		Assert.assertEquals(1200, workitem.getTaskID());

		// test _budget>100
		workitem.setTaskID(1000);
		workitem.replaceItemValue("_budget", 9999);
		workitem.setEventID(10);
		workitem = workflowMockEnvironment.workflowService.processWorkItem(workitem);
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

		workflowMockEnvironment.loadModel("/bpmn/split_event1.bpmn");

		// load test workitem
		ItemCollection workitem = workflowMockEnvironment.database.get("W0000-00001");
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, WorkflowMockEnvironment.DEFAULT_MODEL_VERSION);

		// test none condition ...
		workitem.replaceItemValue("_subject", "Hello");
		workitem.setTaskID(1000);
		workitem.setEventID(10);
		workitem = workflowMockEnvironment.workflowService.processWorkItem(workitem);
		Assert.assertEquals("1.0.0", workitem.getItemValueString("$ModelVersion"));
		Assert.assertEquals(1100, workitem.getTaskID());

		// lookup the version.....
		List<ItemCollection> versions = new ArrayList<ItemCollection>();
		for (ItemCollection doc : workflowMockEnvironment.database.values()) {
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
