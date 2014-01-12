package org.imixs.workflow;



public class MokWorkflowContext implements WorkflowContext {

	@Override
	public Object getSessionContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model getModel() {
		return new MokModel();
	}

	@Override
	public int getLogLevel() {
		return WorkflowKernel.LOG_LEVEL_FINE;
	}
	
	
	

}
