package org.imixs.workflow.engine;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class for the WorkflowService to test conditional and parallel gateways. 
 * 
 * For Testcases test model files are loaded. 
 * This test verifies specific method implementations of the workflowService by
 * mocking the WorkflowService with the @spy annotation.
 * 
 * 
 * @author rsoika
 */
public class TestWorkflowServiceGateways extends AbstractWorkflowEnvironment {
	public static final String DEFAULT_MODEL_VERSION = "1.0.0";

	



	/**
	 * This test tests the conditional event gateways....
	 * 
	 * This is just a simple simulation...
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * @throws ModelException 
	 * 
	 */
	@Test
	public void testConditionalEvent1() throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {
		
		loadModel("/bpmn/conditional_event1.bpmn");
		
		
		// load test workitem
		ItemCollection workitem = database.get("W0000-00001");
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, DEFAULT_MODEL_VERSION);
		
		
		// test none condition ...
		workitem.replaceItemValue(WorkflowKernel.PROCESSID, 1000);
		workitem.replaceItemValue(WorkflowKernel.ACTIVITYID, 10);
		workitem = workflowService.processWorkItem(workitem);
		Assert.assertEquals("1.0.0", workitem.getItemValueString("$ModelVersion"));
		Assert.assertEquals(1000, workitem.getProcessID());
		
		// test _budget<100
		workitem.replaceItemValue(WorkflowKernel.PROCESSID, 1000);
		workitem.replaceItemValue("_budget", 99);
		workitem.replaceItemValue(WorkflowKernel.ACTIVITYID, 10);
		workitem = workflowService.processWorkItem(workitem);
		Assert.assertEquals(1200, workitem.getProcessID());
		
		// test _budget>100
		workitem.replaceItemValue(WorkflowKernel.PROCESSID, 1000);
		workitem.replaceItemValue("_budget", 9999);
		workitem.replaceItemValue(WorkflowKernel.ACTIVITYID, 10);
		workitem = workflowService.processWorkItem(workitem);
		Assert.assertEquals(1100, workitem.getProcessID());

	}

}
