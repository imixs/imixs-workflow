# Meta Workflows

A **Meta Workflow** can be used to combine several independent core processes into one higher-level business process. It allows a process owner to define the business-level flow of a project, while the core processes — including their technical realization (forms, ACLs, plugins, integrations) — are still designed by the process modeler with Open-BPMN.

**Example:** A company runs the core processes _Inquiry, Offer, Order and Invoice_. The process owner defines a new meta workflow _"Project Order"_ that combines these core processes in exactly this sequence:

    Project Order: Inquiry → Offer → Order → Invoice

Neither side needs to know the internals of the other. The core processes do not know which meta workflows are using them, and the meta workflow does not need to know task or event IDs of the core processes.

## The Concept

A meta workflow consists of two levels:

- **The meta workflow instance** is a high-level business process instance based on a meta model. It holds the business-level status, project-wide data, its own ACL and the list of all subprocesses.
- **The core workflow instances** are independent technical process instances. They run independently in the system with their own forms, events, ACLs and plugins.

The two levels communicate in two directions:

| Direction   | Mechanism                                                       |
| ----------- | --------------------------------------------------------------- |
| Meta → Core | The meta process instance **starts** core processes             |
| Core → Meta | A core process instance sends **messages** to its meta instance |

A core process model publishes messages, similar to the public methods of an API. The meta model decides which messages it reacts to. If the meta model does not define a reaction, a message is ignored and the meta instance remains unchanged.

### The `MetaWorkflowPlugin`

On the model level, the connector component between these two model types is the `MetaWorkflowPlugin`. It must be registered in both workflow models - the meta model and the core models:

| Model      | The plugin is used to...                             |
| ---------- | ---------------------------------------------------- |
| Meta model | start subprocesses (`metaworkflow.subprocess.group`) |
| Core model | send messages (published messages and end tasks)     |

The messages are received and dispatched by the `MetaWorkflowHandler` called by teh `AsyncEventScheduler`.

### Roles and Responsibilities

| Who                                 | Does                                                                   | Knows about the meta workflow            |
| ----------------------------------- | ---------------------------------------------------------------------- | ---------------------------------------- |
| Process modeler (core model)        | Models end tasks and, if needed, published messages                    | nothing                                  |
| User working on a subprocess        | Processes the subprocess with his own access rights                    | nothing - not even read access is needed |
| `MetaWorkflowPlugin`                | Writes an EventLog entry with the `$uniqueID` of the meta instance     | only the reference (`$metaprocess.ref`)  |
| Process owner (meta model)          | Starts core processes and defines message events for relevant messages | everything                               |
| `MetaWorkflowHandler` (system user) | Loads the meta instance and processes the matching message event       | everything                               |

This design keeps both levels strictly decoupled:

- **No access rights needed.** The plugin does not read the meta instance to send a message. The reference is already stored in the core process. So a message is sent even if the user is not allowed to see the meta instance.
- **Transactional consistency.** The EventLog entry used by the `MetaWorkflowPlugin` is created in the same transaction as the processing of the core process. If the processing fails, no message is sent. So a message never refers to a step that did not happen.
- **The meta model decides.** Whether a message has any effect is defined in the meta model only. Without a matching message event the meta instance remains unchanged.

## Starting a new Core Process

A meta instance starts a new core process by a BPMN **Message**. The message is attached to a regular Imixs event of the meta model by a `messageEventDefinition`. When the event is processed - for example when the process owner clicks on _Create Offer_ - the `MetaWorkflowPlugin` starts the core process.

### Message Names

A message in the meta model always has the form:

    <workflow group>:<message>

The first part is the workflow group of the core process, the second part is the name of the message in the core model. For example, the message `Offer:create` refers to the message `create` of the core process _Offer_.

### The Message Start Event

A core model defines how it can be started by a **Message Start Event** - a BPMN Start Event with a `messageEventDefinition`. The Message Start Event is followed by the init event and the start task of the core process, like any other start event.

```
(✉ Start "create") → (Init Event "Create") → [Open]
```

Similar to the constructor of a class, the Message Start Event is part of the public interface of a core model. A core model can define several Message Start Events, for example `create` and `createFromTemplate`, each leading to its own init event.

**BPMN 2.0 Definition - Core Model _Offer_**

```xml
<bpmn2:message id="Message_1" name="create"/>
...
<bpmn2:startEvent id="StartEvent_1" name="create">
  <bpmn2:outgoing>SequenceFlow_1</bpmn2:outgoing>
  <bpmn2:messageEventDefinition id="MessageEventDefinition_1" messageRef="Message_1"/>
</bpmn2:startEvent>
```

**BPMN 2.0 Definition - Meta Model _Project Order_**

```xml
<bpmn2:message id="Message_7" name="Offer:create"/>
...
<bpmn2:intermediateCatchEvent id="CatchEvent_1" name="Create Offer">
  <bpmn2:messageEventDefinition id="MessageEventDefinition_7" messageRef="Message_7"/>
</bpmn2:intermediateCatchEvent>
```

### Processing

When the meta instance is processed by an event with such a message, the plugin:

1. splits the message name into the workflow group and the message,
2. resolves the latest model version of the workflow group,
3. resolves the Message Start Event with the matching message, and the init event and start task following it,
4. creates and processes the new core process,
5. stores the `$uniqueID` of the meta instance in the item `$metaprocess.ref` of the core process,
6. appends the `$uniqueID` of the core process to the item `$metaprocess.subprocesses` of the meta instance.

If the core model defines no matching Message Start Event, the processing of the meta instance fails.

Events without a message (e.g. a _Save_ event) do not start a core process. An event can be processed several times, so a meta instance can hold any number of core processes of the same workflow group, for example a second offer or a list of requirements collected for a project.

## Core Process Messages

Also a core process communicates with its meta instance by messages. A message has a name and is sent in two cases: when the core process reaches a task publishing a message, and when it reaches an end task.

The term follows the BPMN semantics: a BPMN _Message_ is sent from one process to a specific other process. A message of a core process is always addressed to exactly one meta instance (`$metaprocess.ref`).

### Published Messages

The process modeler can publish a message on any task of a core model by attaching a BPMN **Message Throw Event** to the task. The Message Throw Event refers to a BPMN **Message**, whose name is the name of the message.

```
[Check offer] → (release) → [Offer released] → ✉ Message Throw Event "Offer released"
```

Each time a core process reaches the task, the message is sent to the meta instance - also if an event leads back into the same task (e.g. a _Save_ event). Whether a message is relevant is decided by the meta model alone.

**BPMN 2.0 Definition**

```xml
<bpmn2:message id="Message_2" name="Initial sampling requested"/>
...
<bpmn2:task id="Task_5" name="Initial sampling requested">
  <bpmn2:outgoing>SequenceFlow_9</bpmn2:outgoing>
</bpmn2:task>
<bpmn2:intermediateThrowEvent id="ThrowEvent_1" name="Initial sampling requested">
  <bpmn2:incoming>SequenceFlow_9</bpmn2:incoming>
  <bpmn2:messageEventDefinition id="MessageEventDefinition_2" messageRef="Message_2"/>
</bpmn2:intermediateThrowEvent>
<bpmn2:sequenceFlow id="SequenceFlow_9" sourceRef="Task_5" targetRef="ThrowEvent_1"/>
```

The Message Throw Event has no outgoing sequence flow and does not affect the processing of the core process.

Published messages allow a meta instance to react while the core process is still running. For example, a customer may request an initial sampling for an offer. The offer is not accepted yet, but from the business point of view the project moves on into the initial sampling phase.

The process modeler does not need to know which meta workflows are using a message. The published messages are the public interface of a core model, similar to the public methods of an API. A message name should therefore be stable and should describe the business meaning.

The BPMN Message element can also hold additional data (`itemRef`), which can be used for detailed configurations in future scenarios.

### Completion Messages

A core process is completed when it reaches an **end task** of its workflow group. An end task is a task followed by a BPMN End Event. When a core process reaches an end task, the name of the end task is sent as a message. For example, the core process _Offer_ may define the end tasks _Accepted_ and _Rejected_.

End tasks always send a message. No further configuration is needed, so a core process never ends without notice. A completed core process is marked with the items `$metaprocess.completed=true` and `$metaprocess.result` (the name of the end task) and sends its completion message only once.

### Delivery

Each message is stored as an EventLog entry with the topic `metaworkflow.message` and the `$uniqueID` of the meta instance as reference. The messages are processed asynchronously in the order they were sent.

## Message Events

The reaction of a meta instance to a message is defined by _Message Events_ in the meta model. In BPMN terms, a message event is an _Intermediate Message Catch Event_: it is not triggered by a user but waits for a message of a core process.

A message event is a regular Imixs event with a `messageEventDefinition` referring to a BPMN Message in the form `<workflow group>:<message>`. For example, the message `Offer:Accepted` reacts to the message `Accepted` sent by a core process of the workflow group _Offer_.

**BPMN 2.0 Definition**

```xml
<bpmn2:message id="Message_8" name="Offer:Accepted"/>
...
<bpmn2:intermediateCatchEvent id="CatchEvent_2" name="Offer accepted">
  <bpmn2:messageEventDefinition id="MessageEventDefinition_8" messageRef="Message_8"/>
</bpmn2:intermediateCatchEvent>
```

The message name must match exactly - there is no wildcard. Messages are matched by name, not by their BPMN id.

Messages are processed asynchronously by the `AsyncEventScheduler`, running as a system user. This is necessary because the user who processed the core process may not have write access to the meta instance.

For each message, the `MetaWorkflowHandler` looks for a message event **in the current task** of the meta instance that refers to the matching message. If a matching event is found, the meta instance is processed by this event. Otherwise the message is ignored.

Message events should be modeled as non-public events (`workflow.public=false`), so they are not shown to the user.

**Note:** A message only takes effect if the meta instance is in a task with a matching message event at the time the message is processed. If the meta instance has moved on to another task, the process owner must define the message event in that task as well. The result of a completed core process is still stored in the core process itself (`$metaprocess.completed`, `$metaprocess.result`).

### Starting and Receiving

In the meta model, both starting a core process and receiving a message use the same construct: an event with a message `<workflow group>:<message>`. The difference is defined by the core model:

- If the core model defines a **Message Start Event** with this message, the event starts a new core process.
- Otherwise the event is a **message event** and waits for this message from a running core process.

For this reason, a core model must not use the same name for a start message and a published message.

An event can refer to several messages. For example, the event _Offer accepted_ can receive the message `Offer:Accepted` and at the same time start a new order with the message `Order:create`.

### Available Messages

A meta modeler can offer the process owner the list of available messages for each workflow group:

- **Start messages:** the messages of all Message Start Events of the core model,
- **Published messages:** the messages of all Message Throw Events of the core model (`ModelManager.findAllMessagesByGroup()`),
- **Completion messages:** the names of all end tasks of the core model (`ModelManager.findEndTasks()`).

## Example: Project Order

The core process _Offer_ defines the Message Start Event `create`, publishes the message _Initial sampling requested_ on its task _Initial sampling requested_ and defines the end tasks _Accepted_ and _Rejected_. The core processes _Inquiry_, _Order_ and _Invoice_ also define a Message Start Event `create`, and _Inquiry_ and _Order_ define an end task named _Completed_.

The meta model _Project Order_ could define the following tasks and events:

| Task             | Event                      | Type                 | BPMN Messages                       |
| ---------------- | -------------------------- | -------------------- | ----------------------------------- |
| (Start)          | Create                     | public               | `Inquiry:create`                    |
| Inquiry          | Inquiry completed          | message (not public) | `Inquiry:Completed`                 |
| Offer            | Create Offer               | public               | `Offer:create`                      |
| Offer            | Initial sampling requested | message (not public) | `Offer:Initial sampling requested`  |
| Offer            | Offer accepted             | message (not public) | `Offer:Accepted`, `Order:create`    |
| Initial Sampling | Offer accepted             | message (not public) | `Offer:Accepted`, `Order:create`    |
| Order            | Order completed            | message (not public) | `Order:Completed`, `Invoice:create` |

When the customer requests an initial sampling, the meta instance moves from _Offer_ to _Initial Sampling_, although the offer is still running. When the offer is accepted later, the message event in _Initial Sampling_ moves the meta instance on to _Order_ and starts a new order.

The event _Create Offer_ can be triggered by the process owner several times, for example to create a second offer. A rejected offer does not change the meta instance, because there is no message event for the message `Offer:Rejected`.

## Configuration

Messages are processed by the `AsyncEventScheduler`. The scheduler must be enabled:

    ASYNCEVENT_PROCESSOR_ENABLED - must be set to true (default=false)
    ASYNCEVENT_PROCESSOR_INTERVAL - timeout period in milliseconds
    ASYNCEVENT_PROCESSOR_DEADLOCK - deadlock in milliseconds (default 1 minute)

See the section [Async Events](./asyncevents.html) for details.

## Reference

Meta workflows use BPMN elements only. No Imixs extension items are needed.

**BPMN elements**

| Element                                | Model      | Description                                                                    |
| -------------------------------------- | ---------- | ------------------------------------------------------------------------------ |
| Message Start Event                    | Core model | The core process can be started by this message                                |
| Message Throw Event (following a task) | Core model | The task sends the referred message each time it is reached                    |
| End Event (following a task)           | Core model | The task sends its own name as completion message (once)                       |
| Event with message `<group>:<message>` | Meta model | Starts a core process (start message) or reacts to a message of a core process |

**Workitem items**

| Item                        | Stored in     | Description                                             |
| --------------------------- | ------------- | ------------------------------------------------------- |
| `$metaprocess.ref`          | Core process  | `$uniqueID` of the meta instance                        |
| `$metaprocess.subprocesses` | Meta instance | List of all core processes started by the meta instance |
| `$metaprocess.completed`    | Core process  | `true` if the completion message was sent               |
| `$metaprocess.result`       | Core process  | Result of the core process (name of the end task)       |

**EventLog entry** (topic `metaworkflow.message`, reference = `$uniqueID` of the meta instance):

| Item                            | Description                        |
| ------------------------------- | ---------------------------------- |
| `$metaprocess.subprocess.ref`   | `$uniqueID` of the core process    |
| `$metaprocess.subprocess.group` | Workflow group of the core process |
| `$metaprocess.message`          | Name of the message                |

## Limitations

The current implementation supports the following scenarios:

- sequences of core processes, controlled by completion messages,
- branches of the meta instance, controlled by published messages of running core processes,
- multiple core processes of the same workflow group, started manually by the process owner.

The following scenarios are not supported yet:

- waiting until **all** core processes of a workflow group are completed,
- propagating a cancellation of the meta instance to running core processes.
