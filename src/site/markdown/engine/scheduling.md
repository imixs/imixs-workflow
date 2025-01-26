# Scheduling

Imixs-Workflow provides a flexible scheduling architecture for automating business processes and system tasks.

Modern business applications often require automated, time-based execution of tasks and processes. This includes periodic jobs like report generation, automated reminders, data synchronization, or escalation handling. The Imixs scheduling system addresses these requirements through a modular and extensible architecture.

## Architecture

The Imixs scheduling system follows a service-oriented design pattern where scheduling tasks are implemented through a standardized interface and managed by a central Jakarta EE service. This approach ensures reliability through transaction-safe scheduling operations.
The architecture consists of the `Scheduler` interface and the `SchedulerService`. The Scheduler interface, defines how schedulers interact with the system by providing standard configuration attributes, the run() method contract, and enabling consistent scheduler implementations. The `SchedulerService` acts as the central service, controlling the scheduler lifecycle by creating and managing timers, scheduler configurations, and logging.

## SchedulerService

The `SchedulerService` is built on the Jakarta EE Timer Service (EJB Timer Service), providing enterprise-grade scheduling capabilities.

- **Transaction Safety**: Timer events are managed within EJB transactions, ensuring data consistency even in failure scenarios
- **Persistence**: Timer configurations survive server restarts
- **Cluster Support**: Timer events can be distributed across cluster nodes
- **Calendar-Based Scheduling**: Sophisticated scheduling patterns using cron-style expressions
- **Resource Efficiency**: Optimized resource usage compared to managed thread approaches

The service provides a comprehensive infrastructure encompassing configuration management through document-based storage, lifecycle control (start, stop, monitoring), CDI-based implementation handling, detailed logging and error handling, and runtime status monitoring.

[Learn more about the Scheduler Service](scheduler.html)

## WorkflowScheduler

The WorkflowScheduler is the main implementation for handling BPMN-based scheduling. It enables timer-based processing of workitems, automatic reminder and escalation tasks, custom workitem selection via queries, and transaction-safe execution.

[Learn more about the WorkflowScheduler](workflowscheduler.html)
