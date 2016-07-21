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
		// TODO Auto-generated method stub

	}
 
	@Override
	public int run(ItemCollection documentContext,
			ItemCollection documentActivity) throws PluginException {

		int i = documentContext.getItemValueInteger("runs");
		i++;
		documentContext.replaceItemValue("runs", i);

		return 0;
	}

	@Override
	public void close(int status) throws PluginException {
		// TODO Auto-generated method stub

	}

}
