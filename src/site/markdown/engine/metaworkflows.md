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

An event of the meta model starts a new subprocess if it defines the Imixs extension item `metaworkflow.subprocess.group`. The value is the name of the workflow group of the technical model to be started.

**BPMN 2.0 Definition**

```xml
<bpmn2:intermediateCatchEvent id="CatchEvent_1" name="Create Inquiry">
  <bpmn2:extensionElements>
    <imixs:item name="metaworkflow.subprocess.group" type="xs:string">
      <imixs:value><![CDATA[Inquiry]]></imixs:value>
    </imixs:item>
  </bpmn2:extensionElements>
</bpmn2:intermediateCatchEvent>
```

When the meta instance is processed by such an event, the plugin:

1. resolves the latest model version of the workflow group,
2. resolves the start task and its init event from the BPMN model (the technical model must define exactly one start task),
3. creates and processes the new subprocess,
4. stores the `$uniqueID` of the meta instance in the item `$metaprocess.ref` of the subprocess,
5. appends the `$uniqueID` of the subprocess to the item `$metaprocess.subprocesses` of the meta instance.

Events without this item (e.g. a _Save_ event) are ignored by the plugin.

An event can be processed several times. So a meta instance can hold any number of subprocesses of the same workflow group, for example a second offer or a list of requirements collected for a project.

## Messages

A subprocess communicates with its meta instance by messages. A message has a name and is sent in two cases.

The term follows the BPMN semantics: a BPMN _Message_ is sent from one process to a specific other process, while a BPMN _Signal_ is a broadcast to any listener. A message of a subprocess is always addressed to exactly one meta instance (`$metaprocess.ref`).

**Note:** Meta workflow messages are not related to the [Signal Adapters](../core/adapter-api.html) of Imixs-Workflow. A BPMN Signal Event in an Imixs model always refers to a Java adapter class and must not be used for meta workflows.

### Published Messages

The process modeler can publish a message on any task of a technical model by attaching a BPMN **Message Throw Event** to the task. The Message Throw Event refers to a BPMN **Message**, whose name is the name of the message.

```
[Check offer] → (release) → [Offer released] → ✉ Message Throw Event "Offer released"
```

Each time a subprocess reaches the task, the message is sent to the meta instance - also if an event leads back into the same task (e.g. a _Save_ event). Whether a message is relevant is decided by the meta model alone.

**BPMN 2.0 Definition**

```xml
<bpmn2:message id="Message_1" name="Initial sampling requested"/>
...
<bpmn2:task id="Task_5" name="Initial sampling requested">
  <bpmn2:outgoing>SequenceFlow_9</bpmn2:outgoing>
</bpmn2:task>
<bpmn2:intermediateThrowEvent id="ThrowEvent_1" name="Initial sampling requested">
  <bpmn2:incoming>SequenceFlow_9</bpmn2:incoming>
  <bpmn2:messageEventDefinition id="MessageEventDefinition_1" messageRef="Message_1"/>
</bpmn2:intermediateThrowEvent>
<bpmn2:sequenceFlow id="SequenceFlow_9" sourceRef="Task_5" targetRef="ThrowEvent_1"/>
```

The Message Throw Event has no outgoing sequence flow and does not affect the processing of the subprocess.

Published messages allow a meta instance to react while the subprocess is still running. For example, a customer may request an initial sampling for an offer. The offer is not accepted yet, but from the business point of view the project moves on into the initial sampling phase.

The process modeler does not need to know which meta workflows are using a message. The published messages are the public interface of a technical model, similar to the public methods of an API. A message name should therefore be stable and should describe the business meaning.

The BPMN Message element can also hold additional data (`itemRef`), which can be used for detailed configurations in future scenarios.

### Completion Messages

A subprocess is completed when it reaches an **end task** of its workflow group. An end task is a task followed by a BPMN End Event. When a subprocess reaches an end task, the name of the end task is sent as a message. For example, the workflow _Offer_ may define the end tasks _Accepted_ and _Rejected_.

End tasks always send a message. No further configuration is needed, so a subprocess never ends without notice. A completed subprocess is marked with the items `$metaprocess.completed=true` and `$metaprocess.result` (the name of the end task) and sends its completion message only once.

### Delivery

Each message is stored as an EventLog entry with the topic `metaworkflow.message` and the `$uniqueID` of the meta instance as reference. The messages are processed asynchronously in the order they were sent.

## Message Events

The reaction of a meta instance to a message is defined by _Message Events_ in the meta model. In BPMN terms, a message event is an _Intermediate Message Catch Event_: it is not triggered by a user but waits for a message of a subprocess.

A message event is an Imixs event with:

- a BPMN **messageEventDefinition** referring to a BPMN Message with the name of the expected message,
- the Imixs extension item `metaworkflow.message.group` with the workflow group of the subprocess sending the message.

The message name must match exactly - there is no wildcard. Both the meta model and the technical model define a BPMN Message with the same name. Messages are matched by name, not by their BPMN id.

**BPMN 2.0 Definition**

```xml
<bpmn2:message id="Message_2" name="Accepted"/>
...
<bpmn2:intermediateCatchEvent id="CatchEvent_2" name="Offer accepted">
  <bpmn2:extensionElements>
    <imixs:item name="metaworkflow.message.group" type="xs:string">
      <imixs:value><![CDATA[Offer]]></imixs:value>
    </imixs:item>
  </bpmn2:extensionElements>
  <bpmn2:messageEventDefinition id="MessageEventDefinition_2" messageRef="Message_2"/>
</bpmn2:intermediateCatchEvent>
```

Messages are processed asynchronously by the `AsyncEventScheduler`, running as a system user. This is necessary because the user who processed the subprocess may not have write access to the meta instance.

For each message, the `MetaWorkflowHandler` looks for a message event **in the current task** of the meta instance that matches the workflow group and the name of the message. If a matching event is found, the meta instance is processed by this event. Otherwise the message is ignored.

Message events should be modeled as non-public events (`workflow.public=false`), so they are not shown to the user.

A meta modeler can offer the process owner the list of available messages for each workflow group: the names of all BPMN Messages thrown by tasks of the technical model, and the names of all end tasks. The `ModelManager` provides the methods `findAllMessagesByGroup()` and `findEndTasks()` to resolve these lists.

**Note:** A message only takes effect if the meta instance is in a task with a matching message event at the time the message is processed. If the meta instance has moved on to another task, the process owner must define the message event in that task as well. The result of a completed subprocess is still stored in the subprocess itself (`$metaprocess.completed`, `$metaprocess.result`).

## Example: Project Order

The technical workflow _Offer_ throws the message _Initial sampling requested_ on its task _Initial sampling requested_ and defines the end tasks _Accepted_ and _Rejected_.

The meta model _Project Order_ could define the following tasks and events:

| Task             | Event                      | Type                 | BPMN Message               | Configuration                                                               |
| ---------------- | -------------------------- | -------------------- | -------------------------- | --------------------------------------------------------------------------- |
| (Start)          | Create                     | public               |                            | `metaworkflow.subprocess.group=Inquiry`                                     |
| Inquiry          | Inquiry completed          | message (not public) | Completed                  | `metaworkflow.message.group=Inquiry`                                        |
| Offer            | Create Offer               | public               |                            | `metaworkflow.subprocess.group=Offer`                                       |
| Offer            | Initial sampling requested | message (not public) | Initial sampling requested | `metaworkflow.message.group=Offer`                                          |
| Offer            | Offer accepted             | message (not public) | Accepted                   | `metaworkflow.message.group=Offer`, `metaworkflow.subprocess.group=Order`   |
| Initial Sampling | Offer accepted             | message (not public) | Accepted                   | `metaworkflow.message.group=Offer`, `metaworkflow.subprocess.group=Order`   |
| Order            | Order completed            | message (not public) | Completed                  | `metaworkflow.message.group=Order`, `metaworkflow.subprocess.group=Invoice` |

In this example the workflows _Inquiry_ and _Order_ define an end task named _Completed_.

When the customer requests an initial sampling, the meta instance moves from _Offer_ to _Initial Sampling_, although the offer is still running. When the offer is accepted later, the message event in _Initial Sampling_ moves the meta instance on to _Order_.

The event _Create Offer_ can be triggered by the process owner several times, for example to create a second offer. A rejected offer does not change the meta instance, because there is no message event for the message _Rejected_.

As a message event can also define the item `metaworkflow.subprocess.group`, a message can start the next subprocess automatically.

## Configuration

Messages are processed by the `AsyncEventScheduler`. The scheduler must be enabled:

    ASYNCEVENT_PROCESSOR_ENABLED - must be set to true (default=false)
    ASYNCEVENT_PROCESSOR_INTERVAL - timeout period in milliseconds
    ASYNCEVENT_PROCESSOR_DEADLOCK - deadlock in milliseconds (default 1 minute)

See the section [Async Events](./asyncevents.html) for details.

## Item Reference

**Model items**

| Item                            | Model      | Description                                             |
| ------------------------------- | ---------- | ------------------------------------------------------- |
| `metaworkflow.subprocess.group` | Meta model | Event starts a new subprocess of this workflow group    |
| `metaworkflow.message.group`    | Meta model | Message event reacts to messages of this workflow group |

**BPMN elements**

| Element                                      | Model           | Description                                                      |
| -------------------------------------------- | --------------- | ---------------------------------------------------------------- |
| Message Throw Event (following a task)       | Technical model | The task sends the referred BPMN Message each time it is reached |
| End Event (following a task)                 | Technical model | The task sends its own name as completion message (once)         |
| Message Catch Event (messageEventDefinition) | Meta model      | The event reacts to the referred BPMN Message                    |

**Workitem items**

| Item                        | Stored in     | Description                                           |
| --------------------------- | ------------- | ----------------------------------------------------- |
| `$metaprocess.ref`          | Subprocess    | `$uniqueID` of the meta instance                      |
| `$metaprocess.subprocesses` | Meta instance | List of all subprocesses created by the meta instance |
| `$metaprocess.completed`    | Subprocess    | `true` if the completion message was sent             |
| `$metaprocess.result`       | Subprocess    | Result of the subprocess (name of the end task)       |

**EventLog entry** (topic `metaworkflow.message`, reference = `$uniqueID` of the meta instance):

| Item                            | Description                      |
| ------------------------------- | -------------------------------- |
| `$metaprocess.subprocess.ref`   | `$uniqueID` of the subprocess    |
| `$metaprocess.subprocess.group` | Workflow group of the subprocess |
| `$metaprocess.message`          | Name of the message              |

## Limitations

The current implementation supports the following scenarios:

- sequences of subprocesses, controlled by completion messages,
- branches of the meta instance, controlled by published messages of running subprocesses,
- multiple subprocesses of the same workflow group, started manually by the process owner.

The following scenarios are not supported yet:

- waiting until **all** subprocesses of a workflow group are completed,
- propagating a cancellation of the meta instance to running subprocesses.
