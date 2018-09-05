package org.imixs.workflow.engine.scheduler;

import org.imixs.workflow.ItemCollection;

public interface Scheduler {

	public final static String ITEM_SCHEDULER_ENABLED = "_scheduler_enabled";
	public final static String ITEM_SCHEDULER_CLASS = "_scheduler_class";

	/**
	 * Called by the Timeout event. The scheduler configuration object contains
	 * information for the processor of a concrete implementation:
	 * <ul>
	 * <li>type - fixed to value 'scheduler'</li>
	 * <li>_scheduler_description - the chron/calendar description for the Java EE
	 * timer service.</li>
	 * <li>_scheduler_enabled - boolean indicates if the scheduler is
	 * enabled/disabled</li>
	 * <li>_scheduler_class - class name of the scheduler implementation</li>
	 * </ul>
	 * 
	 * After the run method is finished the genericScheduelrService will save the
	 * scheduler configuration if a configuration object is returned. In case of an
	 * exception the Timer service will not be canceled. To cancel the timer, an
	 * implementation must set the item _scheduler_enabled to 'false'.
	 * <p>
	 * To start or stop the timer service the method start() and stop() can be
	 * called.
	 * 
	 * @param scheduler the scheduler configuration
	 * @return updated scheduler configuration
	 */
	public ItemCollection run(ItemCollection job) throws SchedulerException;
}
