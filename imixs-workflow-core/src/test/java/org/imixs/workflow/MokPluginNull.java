package org.imixs.workflow;

import org.imixs.workflow.exceptions.PluginException;

/**
 * Mokup Plugin which returns null after run but changes the field "txtname"!
 * 
 * @author rsoika
 * 
 */
public class MokPluginNull implements Plugin {

	@Override
	public void init(WorkflowContext actx) throws PluginException {
		// no op
	}

	@Override
	public ItemCollection run(final ItemCollection documentContext, ItemCollection documentActivity)
			throws PluginException {
		documentContext.replaceItemValue("txtName", "should not be null");
		return null;
	}

	@Override
	public void close(boolean rollbackTransaction) throws PluginException {
		// no op
	}

}
