package org.imixs.workflow;



public class MokWorkflowContext implements WorkflowContext {

	@Override
	public Object getSessionContext() {
		return null;
	}

	@Override
	public Model getModel() {
		return new MokModel();
	}


}
