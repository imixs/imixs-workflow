# Scheduling

Imixs-Workflow provides a flexible scheduling architecture for automating business processes and system tasks.

Modern business applications often require automated, time-based execution of tasks and processes. This includes periodic jobs like report generation, automated reminders, data synchronization, or escalation handling. The Imixs scheduling system addresses these requirements through a modular and extensible architecture.

## Architecture

The scheduling system follows a service-oriented design pattern where scheduling tasks are implemented through a standardized interface and managed by a central service. This approach ensures:

- **Reliability**: All scheduling operations are transaction-safe
- **Flexibility**: Custom schedulers can be implemented for specific business needs
- **Maintainability**: Unified configuration and monitoring
- **Scalability**: Independent scheduling processes

The architecture consists of three main components:

1. **Scheduler Interface** - A core interface that defines how schedulers interact with the system:

   - Defines standard configuration attributes
   - Provides the `run()` method contract
   - Enables consistent scheduler implementations

2. **SchedulerService** - The central service managing scheduler lifecycle:

   - Handles timer creation and management
   - Controls scheduler start/stop operations
   - Manages scheduler configurations
   - Provides transaction management and logging

3. **Scheduler Implementations**:
   - **WorkflowScheduler** - Processes scheduled BPMN events
   - Custom implementations for specific business needs

## WorkflowScheduler

The WorkflowScheduler is a core implementation that handles BPMN-based scheduling:

- Timer-based processing of workitems
- Auto-reminding and escalation tasks
- Custom workitem selection via queries
- Transaction-safe execution

[Learn more about the WorkflowScheduler](workflowscheduler.html)

## SchedulerService

The SchedulerService is built on top of the Jakarta EE Timer Service (EJB Timer Service), providing enterprise-grade scheduling capabilities:

- **Transaction Safety**: Timer events are managed within EJB transactions, ensuring data consistency even in failure scenarios
- **Persistence**: Timer configurations survive server restarts
- **Cluster Support**: Timer events can be distributed across cluster nodes
- **Calendar-Based Scheduling**: Sophisticated scheduling patterns using cron-style expressions
- **Resource Efficiency**: Optimized resource usage compared to managed thread approaches

The service provides a comprehensive infrastructure:

- Configuration management through document-based storage
- Lifecycle control (start, stop, monitoring)
- CDI-based implementation handling
- Detailed logging and error handling
- Runtime status monitoring

[Learn more about the Scheduler Service](scheduler.html)
