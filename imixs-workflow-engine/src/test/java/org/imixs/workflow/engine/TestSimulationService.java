package org.imixs.workflow.engine;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.SessionContext;
import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.bpmn.BPMNParser;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.xml.sax.SAXException;

import junit.framework.Assert;

/**
 * Test class for the SimulationService
 * 
 * This test simulates the processing life cylce of a workitem using the
 * SimulationService.
 * 
 * @author rsoika
 */
public class TestSimulationService { //extends WorkflowSimulationEnvironment {

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
	public void testConditionalEvent1()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		WorkflowSimulationEnvironment wse=new WorkflowSimulationEnvironment();
		wse.setup();
		wse.loadModel("/bpmn/conditional_event1.bpmn");

		// load test workitem
		ItemCollection workitem = new ItemCollection();
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, wse.DEFAULT_MODEL_VERSION);

		// test none condition ...
		workitem.replaceItemValue(WorkflowKernel.PROCESSID, 1000);
		workitem.replaceItemValue(WorkflowKernel.ACTIVITYID, 10);
		workitem = wse.processWorkItem(workitem);
		Assert.assertEquals("1.0.0", workitem.getItemValueString("$ModelVersion"));
		Assert.assertEquals(1000, workitem.getProcessID());

		// test _budget<100
		workitem.replaceItemValue(WorkflowKernel.PROCESSID, 1000);
		workitem.replaceItemValue("_budget", 99);
		workitem.replaceItemValue(WorkflowKernel.ACTIVITYID, 10);
		workitem = wse.simulationService.processWorkItem(workitem, null);
		Assert.assertEquals(1200, workitem.getProcessID());

		// test _budget>100
		workitem.replaceItemValue(WorkflowKernel.PROCESSID, 1000);
		workitem.replaceItemValue("_budget", 9999);
		workitem.replaceItemValue(WorkflowKernel.ACTIVITYID, 10);
		workitem = wse.simulationService.processWorkItem(workitem, null);
		Assert.assertEquals(1100, workitem.getProcessID());

	}

	

}
