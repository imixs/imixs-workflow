package org.imixs.workflow.engine;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Test;

import junit.framework.Assert;

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
	 * @throws AdapterException 
	 * 
	 */
	@Test
	public void testConditionalEvent1()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException, AdapterException {

		WorkflowSimulationEnvironment wse = new WorkflowSimulationEnvironment();
		wse.setup();
		wse.loadModel("/bpmn/conditional_event1.bpmn");

		// load test workitem
		ItemCollection workitem = new ItemCollection();
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, WorkflowSimulationEnvironment.DEFAULT_MODEL_VERSION);

		// test none condition ...
		workitem.setTaskID(1000);
		workitem.setEventID(10);
		workitem = wse.processWorkItem(workitem);
		Assert.assertEquals("1.0.0", workitem.getItemValueString("$ModelVersion"));
		Assert.assertEquals(1000, workitem.getTaskID());

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
