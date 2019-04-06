package org.imixs.workflow;

import org.imixs.workflow.exceptions.PluginException;

/**
 * Mokup Plugin
 * 
 * increases the property runs per each run .
 * 
 * @author rsoika
 * 
 */
public class MokPlugin implements Plugin {

	@Override
	public void init(WorkflowContext actx) throws PluginException {
	}
 
	@Override 
	public ItemCollection run(ItemCollection documentContext,
			ItemCollection documentActivity) throws PluginException {

		int i = documentContext.getItemValueInteger("runs");
		i++;
		documentContext.replaceItemValue("runs", i);

		return documentContext;
	}

	@Override
	public void close(boolean rollbackTransaction) throws PluginException {
		// TODO Auto-generated method stub

	}

}
