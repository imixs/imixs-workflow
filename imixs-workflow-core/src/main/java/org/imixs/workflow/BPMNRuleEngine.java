package org.imixs.workflow;

import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * The BPMN Rule Engine can be used to evaluate a business Rule based on a BPMN
 * model with conditional events.
 * <p>
 * The rules are evaluated as a chain of conditional events
 * 
 * @author rsoika
 *
 */
public class BPMNRuleEngine {

	private Model model = null;

	public BPMNRuleEngine(Model model) {
		super();
		this.model = model;
	}

	/**
	 * Evaluates a BPMN Business Rule based on the data provided by a workitem.
	 * 
	 * @param workitem
	 * @return evaluated task id
	 * @throws ModelException
	 */
	public int eval(ItemCollection workitem) throws ModelException {
		// setup the workflow rule context
		RuleContext ruleContext = new RuleContext();
		WorkflowKernel workflowKernel = new WorkflowKernel(ruleContext);
		ruleContext.getModelManager().addModel(model);
		int result;
		try {
			result = workflowKernel.eval(workitem);
		} catch (PluginException e) {
			throw new ModelException(e.getErrorCode(), e.getMessage(), e);
		}

		return result;
	}

	/**
	 * Helper Class to mock a workflow kernel
	 * 
	 * @author rsoika
	 *
	 */
	class RuleContext implements WorkflowContext, ModelManager {

		private Model model = null;

		@Override
		public Object getSessionContext() {
			return null;
		}

		@Override
		public ModelManager getModelManager() {
			return this;
		}

		@Override
		public Model getModel(String version) throws ModelException {
			return model;
		}

		@Override
		public void addModel(Model model) throws ModelException {
			this.model = model;
		}

		@Override
		public void removeModel(String version) {
		}

		@Override
		public Model getModelByWorkitem(ItemCollection workitem) throws ModelException {
			return model;
		}

	}

}
