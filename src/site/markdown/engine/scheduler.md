# The Scheduler Service

The Scheduler Service is an easy way to manage Java EE timer services. A Timer can be configured by a configuration document of the type 'scheduler'.
The Configuration has to provide at least the following fields:

- _type_ - fixed to value 'scheduler'
- _scheduler_definition_ - the chron/calendar definition for the Java EE timer service.
- _scheduler_enabled_ - boolean indicates if the scheduler is enabled/disabled
- _scheduler_class_ - class name of the scheduler implementation
- _scheduler_log_ - optional message log provided by the scheduler implementation

## The Scheduler Interface

A new scheduler can be easily implemented by just implementing the interface `org.imixs.workflow.engine.scheduler.Scheduler`.
The SchedulerService will automatically call the concrete scheduler implementation defined in the scheduler configuration document (_scheduler_class_). The scheduler class will be injected via CDI so all type of beans and resources supported by CDI can be used.

```java
public class MyScheduler implements Scheduler {
    ...
    @Inject
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
```

## The Scheduler Service

The SchedulerService manages scheduler configurations and timer events through a document-based approach. Each scheduler configuration is stored as a document with type='scheduler', it contains a unique identifier and scheduler class name along with timer settings in cron-style format. Upon starting a scheduler, the SchedulerService creates a Jakarta Timer instance based on the configuration, which is then persisted in the Jakarta Timer Service. Timer settings can be adjusted without requiring application restart.
During the execution process, the SchedulerService handles each timer event by loading the current configuration, instantiating the scheduler implementation via CDI, and invoking the run() method within a new transaction. After execution, it persists updated configuration and status information. For error handling, the service implements transaction rollback to maintain data consistency, automatically logs execution status, and cancels timers in case of critical failures.

## Implementing a Custom Scheduler

To create a custom scheduler:

```java
@ApplicationScoped
public class MyScheduler implements Scheduler {

    @Inject
    DocumentService documentService;

    @Override
    public ItemCollection run(ItemCollection configuration) throws SchedulerException {
        // Your scheduling logic here

        // Access configuration....
        String myConfig = configuration.getItemValueString("my-config");

        // Update status...
        configuration.setItemValue("_scheduler_status", "Processing completed....");

        // Return updated configuration
        return configuration;
    }
}
```

The configuration can be started via the SchedulerService:

```java
@Inject
SchedulerService schedulerService;

public void startScheduler() {
    ItemCollection config = new ItemCollection();
    config.setItemValue("type", "scheduler");
    config.setItemValue("name", "my-scheduler");
    config.setItemValue("_scheduler_class", "my.package.MyScheduler");
    // Set schedule - every hour
    config.setItemValue("_scheduler_definition", "hour=*");

    schedulerService.saveConfiguration(config);
    schedulerService.start(config);
}
```
