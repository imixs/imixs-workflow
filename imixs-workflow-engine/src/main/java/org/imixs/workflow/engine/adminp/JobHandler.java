package org.imixs.workflow.engine.adminp;

import org.imixs.workflow.ItemCollection;

public interface JobHandler {

	/**
	 * Called by the Timer Service. The JobHandler returns true to signal the
	 * AdminPService that the job is completed. Otherwise the AdminPServcie will
	 * wait for the next timeout.
	 * 
	 * @param job
	 * @return
	 */
	public boolean run(ItemCollection job);
}
