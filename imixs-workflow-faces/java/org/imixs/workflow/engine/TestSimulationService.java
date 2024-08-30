package org.imixs.workflow.engine;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Test;

import org.junit.Assert;

/**
 * Test class for the SimulationService
 * 
 * This test simulates the processing life cycle of a workitem using the
 * SimulationService.
 * 
 * @author rsoika
 */
public class TestSimulationService {

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
	public void testConditionalEvent1()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		WorkflowSimulationEnvironment wse = new WorkflowSimulationEnvironment();
		wse.setUp();
		wse.loadModel("/bpmn/conditional_event1.bpmn");

		// load test workitem
		ItemCollection workitem = new ItemCollection();
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, WorkflowSimulationEnvironment.DEFAULT_MODEL_VERSION);

		// test _budget<100
		workitem.setTaskID(1000);
		workitem.replaceItemValue("_budget", 99);
		workitem.setEventID(10);
		workitem = wse.simulationService.processWorkItem(workitem, null);
		Assert.assertEquals(1200, workitem.getTaskID());

		// test _budget>100
		workitem.setTaskID(1000);
		workitem.replaceItemValue("_budget", 9999);
		workitem.setEventID(10);
		workitem = wse.simulationService.processWorkItem(workitem, null);
		Assert.assertEquals(1100, workitem.getTaskID());

	}

}
