# The Scheduler Service

The Scheduler Service is an easy way to manage Java EE timer services. A Timer can be configured by a configuration document of the type 'scheduler'.
The Configuration has to provide at least the following fields:

- _type_ - fixed to value 'scheduler'
- _scheduler_definition_ - the chron/calendar definition for the Java EE timer service.
- _scheduler_enabled_ - boolean indicates if the scheduler is enabled/disabled
- _scheduler_class_ - class name of the scheduler implementation
- _scheduler_log_ - optional message log provided by the scheduler implementation

## The Scheduler Interface

A new scheduler can be easily implemented by just implementing the interface _org.imixs.workflow.engine.scheduler.Scheduler_.
The SchedulerService will automatically call the concrete scheduler implementation defined in the scheduler configuration document (\_scheduler_class). The scheduler class will be injected via CDI so all type of beans and resources supported by CDI can be used.

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

The SchedulerService manages scheduler configurations and timer events through a document-based approach. When a scheduler is started, the following process occurs:

1. **Configuration Storage**

   - A scheduler configuration is stored as a document with type='scheduler'
   - Each configuration contains a unique identifier and scheduler class name
   - The configuration includes timer settings in cron-style format

2. **Timer Creation**

   - The SchedulerService creates a Jakarta Timer instance based on the configuration
   - The timer is persisted in the Jakarta Timer Service
   - Timer settings can be adjusted without restarting the application

3. **Execution Process**

   - On each timer event, the SchedulerService:
     - Loads the current configuration
     - Instantiates the scheduler implementation via CDI
     - Invokes the run() method within a new transaction
     - Persists updated configuration and status information

4. **Error Handling**
   - Transaction rollback on errors to maintain data consistency
   - Automatic logging of execution status
   - Timer cancellation on critical failures

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
