package org.imixs.workflow;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

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

	ItemCollection workitem = null;
	WorkflowKernel workflowKernel = null;
	RuleContext ruleContext = null;

	// setup the workflow envirnment
	@Before
	public void setup() throws PluginException, ModelException {

		ruleContext = new RuleContext();
		workflowKernel = new WorkflowKernel(ruleContext);
		long lLoadTime = System.currentTimeMillis();
		InputStream inputStream = getClass().getResourceAsStream(MODEL_PATH);

		BPMNModel model = null;
		try {
			model = BPMNModelFactory.read(inputStream);
			logger.info("loading model: " + MODEL_PATH + "....");
			ruleContext.getModelManager().addModel(model);
			logger.log(Level.FINE, "...loadModel processing time={0}ms", System.currentTimeMillis() - lLoadTime);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
		}

		// test model...
		Assert.assertNotNull(ruleContext.getModelManager().getModel(MODEL_VERSION));
	}

	@Test
	public void testRuleMatch() {
		long l = System.currentTimeMillis();
		workitem = new ItemCollection();
		workitem.model(MODEL_VERSION).task(100).event(10);
		workitem.setItemValue("a", 1);
		workitem.setItemValue("b", "DE");

		try {
			Assert.assertEquals(200, workflowKernel.eval(workitem));
			logger.log(Level.INFO, "evaluate BPMN-Rule in {0}ms", System.currentTimeMillis() - l);

			// task and event must still be set to 100.10
			Assert.assertEquals(10, workitem.getEventID());
			Assert.assertEquals(100, workitem.getTaskID());
		} catch (PluginException | ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void testRuleNoMatch() {
		long l = System.currentTimeMillis();
		workitem = new ItemCollection();
		workitem.model(MODEL_VERSION).task(100).event(10);
		workitem.setItemValue("a", 1);
		workitem.setItemValue("b", "I");

		try {
			Assert.assertEquals(900, workflowKernel.eval(workitem));
			logger.log(Level.INFO, "evaluate BPMN-Rule in {0}ms", System.currentTimeMillis() - l);

			// task and event must still be set to 100.10
			Assert.assertEquals(10, workitem.getEventID());
			Assert.assertEquals(100, workitem.getTaskID());
		} catch (PluginException | ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/**
	 * Helper Class to mock a workflow kernel
	 * 
	 * @author rsoika
	 *
	 */
	class RuleContext implements WorkflowContext, ModelManager {

		private BPMNModel model = null;

		@Override
		public Object getSessionContext() {
			return null;
		}

		@Override
		public ModelManager getModelManager() {
			return this;
		}

		@Override
		public BPMNModel getModel(String version) throws ModelException {
			return model;
		}

		@Override
		public void addModel(BPMNModel model) throws ModelException {
			this.model = model;
		}

		@Override
		public void removeModel(String version) {
		}

		@Override
		public BPMNModel getModelByWorkitem(ItemCollection workitem) throws ModelException {
			return model;
		}

	}
}
