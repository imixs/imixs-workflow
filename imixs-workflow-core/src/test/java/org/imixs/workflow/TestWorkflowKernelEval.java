package org.imixs.workflow;

import static org.mockito.Mockito.when;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.bpmn.OpenBPMNModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

import jakarta.ejb.SessionContext;

/**
 * Test class for testing the eval method of the workflow kernel
 * The method loads a test model and moks a workflow service
 * 
 * @author rsoika
 */
public class TestWorkflowKernelEval {

	private final static Logger logger = Logger.getLogger(TestWorkflowKernelEval.class.getName());

	final static String MODEL_PATH = "/bpmn/workflowkernel_eval.bpmn";
	final static String MODEL_VERSION = "1.0.0";

	protected WorkflowKernel kernel = null;
	protected SessionContext ctx;
	protected WorkflowContext workflowContext;

	@Before
	public void setup() throws PluginException {

		ctx = Mockito.mock(SessionContext.class);
		workflowContext = Mockito.mock(WorkflowContext.class);
		// provide a mock modelManger class
		when(workflowContext.getModelManager()).thenReturn(new OpenBPMNModelManager());
		kernel = new WorkflowKernel(workflowContext);
		BPMNModel model = null;
		// Load Models
		try {
			model = BPMNModelFactory.read("/bpmn/workflowkernel_eval.bpmn");
			workflowContext.getModelManager().addModel(model);
		} catch (BPMNModelException | ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}

	}

	@Test
	public void testRuleMatch() {
		long l = System.currentTimeMillis();
		ItemCollection workitem = new ItemCollection();
		workitem.model(MODEL_VERSION).task(100).event(10);
		workitem.setItemValue("a", 1);
		workitem.setItemValue("b", "DE");
		try {
			ItemCollection targetTask = kernel.eval(workitem);
			Assert.assertNotNull(targetTask);
			Assert.assertEquals("Match", targetTask.getItemValueString("name"));
			logger.log(Level.INFO, "evaluate BPMN Target Task in {0}ms", System.currentTimeMillis() - l);
			// We also expect that the workitem taskID has not changed!
			Assert.assertEquals(100, workitem.getTaskID());
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void testRuleNoMatch() {
		long l = System.currentTimeMillis();
		ItemCollection workitem = new ItemCollection();
		workitem.model(MODEL_VERSION).task(100).event(10);
		workitem.setItemValue("a", 1);
		workitem.setItemValue("b", "I");

		try {
			ItemCollection targetTask = kernel.eval(workitem);
			Assert.assertNotNull(targetTask);
			Assert.assertEquals("No Match", targetTask.getItemValueString("name"));
			logger.log(Level.INFO, "evaluate BPMN-Rule in {0}ms", System.currentTimeMillis() - l);
			// We also expect that the workitem taskID has not changed!
			Assert.assertEquals(100, workitem.getTaskID());
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

}
