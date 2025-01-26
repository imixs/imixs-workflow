# Event Processing

The Imixs-Workflow Engine provides sophisticated event processing capabilities that enable asynchronous and transactional processing of business events. This is especially useful for complex business processes where tasks need to be executed independently from the main processing life-cycle.

## Overview

Event processing in Imixs-Workflow is designed for reliability and scalability through:

- Transactional integrity via the EventLogService
- Clear separation between synchronous and asynchronous processing
- Scalable event handling through the AsyncEventProcessor
- REST API support for distributed systems

The implementation is based on two main concepts:

1. **Event Log Service** - A service to create and manage event log entries within a running transaction. This service implements the Change Data Capture (CDC) pattern, ensuring that events are only processed after successful transaction completion.

2. **Async Events** - Special BPMN events that are executed asynchronously after a processing life-cycle is completed. These events are modeled as BPMN Boundary Events and provide a powerful way to handle time-based or conditional processing tasks.

## Event Log Service

The EventLogService is the foundation for asynchronous event processing in Imixs-Workflow. It allows you to:

- Create event log entries tied to specific transactions
- Process events in a secure and transactional manner
- Implement the Change Data Capture pattern
- Handle event-specific data and timeout configurations

[Learn more about the EventLogService](eventlogservice.html)

## Async Events

Async Events extend the event processing capabilities by providing:

- BPMN-based modeling of asynchronous tasks
- Timer-controlled execution
- Transaction-safe processing
- Integration with external systems

[Learn more about Async Events](asyncevents.html)

## REST API

The EventLogService provides a REST API for managing event logs in distributed systems. The API supports:

- Fetching event logs by topic
- Locking and unlocking entries
- Deleting event log entries
- Configurable result sets

[Learn more about the EventLog REST API](../restapi/eventlogservice.html)
