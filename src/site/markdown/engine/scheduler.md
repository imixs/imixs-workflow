# The Scheduler Service
The Scheduler Service is an easy way to manage Java EE timer services. A Timer can be configured by a configuration document of the type 'scheduler'. 
The Configuration has to provide at least the following fields:

 * type - fixed to value 'scheduler'
 * _scheduler_definition - the chron/calendar definition for the Java EE timer service.
 * _scheduler_enabled - boolean indicates if the scheduler is enabled/disabled
 * _scheduler_class - class name of the scheduler implementation
 * _scheduler_log - optional message log provided by the scheduler implementation


## The Scheduler Interface

A new scheduler can be easily implemented by just implementing the interface _org.imixs.workflow.engine.scheduler.Scheduler_. 
The SchedulerService will automatically call the concrete scheduler implementation defined in the scheduler configuration document (_scheduler_class). The scheduler class will be injected via CDI so all type of beans and resources supported by CDI can be used. 


	public class MyScheduler implements Scheduler {
		...
		@EJB
		WorkflowService workflowService;
	
		public ItemCollection run(ItemCollection configuration) throws SchedulerException {
	
			try {
				// do the job...
				.....
			} catch (Exception e) {
				// set item _schduler_enabled to false to cancel the timer.
				configuration.setItemValue("scheduler_enabled",false); 
			}
			return configuration;
		}
	}  