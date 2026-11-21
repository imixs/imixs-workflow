# Meta Workflows

A _Meta Workflow_ combines several independent technical workflows into one business process. It allows a process owner to define the business-level flow of a project, while the technical workflows (forms, ACLs, plugins, integrations) are still designed by the process modeler with Open-BPMN.

**Example:** A company runs the technical workflows _Inquiry_, _Offer_, _Order_ and _Invoice_. The process owner defines a meta workflow _Project Order_ that combines these workflows in exactly this sequence:

    Project Order:  Inquiry → Offer → Order → Invoice

Neither side needs to know the internals of the other. The technical workflows do not know which meta workflows are using them, and the meta workflow does not need to know task or event IDs of the technical workflows.

## The Concept

A meta workflow consists of two levels:

- **The meta instance** is a normal process instance based on a meta model. It holds the business-level status, project-wide data, its own ACL and the list of all subprocesses.
- **The subprocesses** are normal process instances based on the technical models. They run independently in the system with their own forms, events, ACLs and plugins.

The two levels communicate in two directions:

| Direction         | Mechanism                                           |
| ----------------- | --------------------------------------------------- |
| Meta → Subprocess | The meta instance **starts** subprocesses           |
| Subprocess → Meta | A subprocess sends **signals** to its meta instance |

A technical model publishes signals, similar to the public methods of an API. The meta model decides which signals it reacts to. If the meta model does not define a reaction, a signal is ignored and the meta instance remains unchanged.

### One Plugin for both Models

On the model level, the only component is the `MetaWorkflowPlugin`. It must be registered in the meta model and in all technical models used as subprocesses:

| Model           | The plugin is used to...                             |
| --------------- | ---------------------------------------------------- |
| Meta model      | start subprocesses (`metaworkflow.subprocess.group`) |
| Technical model | send signals (published signals and end tasks)       |

The receiving side is part of the engine and is not registered in any model: the `AsyncEventScheduler` collects the signals and the `MetaWorkflowHandler` processes the signal events of the meta instance.

### Roles and Responsibilities

| Who                                 | Does                                                               | Knows about the meta workflow            |
| ----------------------------------- | ------------------------------------------------------------------ | ---------------------------------------- |
| Process modeler (technical model)   | Models end tasks and, if needed, published signals                 | nothing                                  |
| User working on a subprocess        | Processes the subprocess with his own access rights                | nothing - not even read access is needed |
| `MetaWorkflowPlugin`                | Writes an EventLog entry with the `$uniqueID` of the meta instance | only the reference (`$metaprocess.ref`)  |
| Process owner (meta model)          | Starts subprocesses and defines signal events for relevant signals | everything                               |
| `MetaWorkflowHandler` (system user) | Loads the meta instance and processes the matching signal event    | everything                               |

This design keeps both levels strictly decoupled:

- **No access rights needed.** The plugin does not read the meta instance to send a signal. The reference is already stored in the subprocess. So a signal is sent even if the user is not allowed to see the meta instance.
- **Transactional consistency.** The EventLog entry is created in the same transaction as the processing of the subprocess. If the processing fails, no signal is sent. So a signal never refers to a step that did not happen.
- **The meta model decides.** Whether a signal has any effect is defined in the meta model only. Without a matching signal event the meta instance remains unchanged.

## Starting a Subprocess

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

## Signals

A subprocess communicates with its meta instance by signals. A signal has a name and is sent in two cases.

### Published Signals

The process modeler can publish a signal on any event of a technical model. The signal is defined in the workflow result of the event, in the same way as other plugin configurations:

```xml
<metaworkflow name="SIGNAL">
  <name>Initial sampling requested</name>
</metaworkflow>
```

When the event is processed, the signal is sent to the meta instance, just as the event may send an e-mail or update the history. An event can define several signals.

Published signals allow a meta instance to react while the subprocess is still running. For example, a customer may request an initial sampling for an offer. The offer is not accepted yet, but from the business point of view the project moves on into the initial sampling phase.

The process modeler does not need to know which meta workflows are using a signal. The published signals are the public interface of a technical model, similar to the public methods of an API. A signal name should therefore be stable and should describe the business meaning, independent of the names of tasks and events.

### Completion Signals

A subprocess is completed when it reaches an **end task** of its workflow group. An end task is a task followed by a BPMN End Event. When a subprocess reaches an end task, the name of the end task is sent as a signal. For example, the workflow _Offer_ may define the end tasks _Accepted_ and _Rejected_.

End tasks always send a signal. No further configuration is needed, so a subprocess never ends without notice. A completed subprocess is marked with the items `$metaprocess.completed=true` and `$metaprocess.result` (the name of the end task) and sends its completion signal only once.

### Delivery

Each signal is stored as an EventLog entry with the topic `metaworkflow.signal` and the `$uniqueID` of the meta instance as reference. The signals are processed asynchronously in the order they were sent.

## Signal Events

The reaction of a meta instance to a signal is defined by _Signal Events_ in the meta model. In BPMN terms, a signal event is an _Intermediate Signal Catch Event_: it is not triggered by a user but waits for a signal of a subprocess. A signal event defines the following Imixs extension items:

| Item                        | Description                                                    |
| --------------------------- | -------------------------------------------------------------- |
| `metaworkflow.signal.group` | Workflow group of the subprocess sending the signal (required) |
| `metaworkflow.signal.name`  | Name of the signal (optional, empty matches any signal)        |

**BPMN 2.0 Definition**

```xml
<bpmn2:intermediateCatchEvent id="CatchEvent_2" name="Offer accepted">
  <bpmn2:extensionElements>
    <imixs:item name="metaworkflow.signal.group" type="xs:string">
      <imixs:value><![CDATA[Offer]]></imixs:value>
    </imixs:item>
    <imixs:item name="metaworkflow.signal.name" type="xs:string">
      <imixs:value><![CDATA[Accepted]]></imixs:value>
    </imixs:item>
  </bpmn2:extensionElements>
</bpmn2:intermediateCatchEvent>
```

Signals are processed asynchronously by the `AsyncEventScheduler`, running as a system user. This is necessary because the user who processed the subprocess may not have write access to the meta instance.

For each signal, the `MetaWorkflowHandler` looks for a signal event **in the current task** of the meta instance that matches the workflow group and the name of the signal. If a matching event is found, the meta instance is processed by this event. Otherwise the signal is ignored.

Signal events should be modeled as non-public events (`workflow.public=false`), so they are not shown to the user.

**Note:** A signal event without `metaworkflow.signal.name` reacts to **any** signal of the workflow group, including published signals of a running subprocess. To react only to the completion of a subprocess, define the names of its end tasks as signals.

A meta modeler can offer the process owner the list of available signals for each workflow group: all signals defined in the workflow results of the technical model, and the names of all end tasks.

**Note:** A signal only takes effect if the meta instance is in a task with a matching signal event at the time the signal is processed. If the meta instance has moved on to another task, the process owner must define the signal event in that task as well. The result of a completed subprocess is still stored in the subprocess itself (`$metaprocess.completed`, `$metaprocess.result`).

## Example: Project Order

The technical workflow _Offer_ publishes the signal _Initial sampling requested_ on its event _Customer requests initial sampling_ and defines the end tasks _Accepted_ and _Rejected_.

The meta model _Project Order_ could define the following tasks and events:

| Task             | Event                      | Type                | Configuration                                                                                                 |
| ---------------- | -------------------------- | ------------------- | ------------------------------------------------------------------------------------------------------------- |
| (Start)          | Create                     | public              | `metaworkflow.subprocess.group=Inquiry`                                                                       |
| Inquiry          | Inquiry completed          | signal (not public) | `metaworkflow.signal.group=Inquiry`                                                                           |
| Offer            | Create Offer               | public              | `metaworkflow.subprocess.group=Offer`                                                                         |
| Offer            | Initial sampling requested | signal (not public) | `metaworkflow.signal.group=Offer`, `metaworkflow.signal.name=Initial sampling requested`                      |
| Offer            | Offer accepted             | signal (not public) | `metaworkflow.signal.group=Offer`, `metaworkflow.signal.name=Accepted`, `metaworkflow.subprocess.group=Order` |
| Initial Sampling | Offer accepted             | signal (not public) | `metaworkflow.signal.group=Offer`, `metaworkflow.signal.name=Accepted`, `metaworkflow.subprocess.group=Order` |
| Order            | Order completed            | signal (not public) | `metaworkflow.signal.group=Order`, `metaworkflow.subprocess.group=Invoice`                                    |

When the customer requests an initial sampling, the meta instance moves from _Offer_ to _Initial Sampling_, although the offer is still running. When the offer is accepted later, the signal event in _Initial Sampling_ moves the meta instance on to _Order_.

The event _Create Offer_ can be triggered by the process owner several times, for example to create a second offer. A rejected offer does not change the meta instance, because there is no signal event for the signal _Rejected_.

As a signal event can also define the item `metaworkflow.subprocess.group`, a signal can start the next subprocess automatically.

## Configuration

Signals are processed by the `AsyncEventScheduler`. The scheduler must be enabled:

    ASYNCEVENT_PROCESSOR_ENABLED - must be set to true (default=false)
    ASYNCEVENT_PROCESSOR_INTERVAL - timeout period in milliseconds
    ASYNCEVENT_PROCESSOR_DEADLOCK - deadlock in milliseconds (default 1 minute)

See the section [Async Events](./asyncevents.html) for details.

## Item Reference

**Model items**

| Item                            | Model      | Description                                             |
| ------------------------------- | ---------- | ------------------------------------------------------- |
| `metaworkflow.subprocess.group` | Meta model | Event starts a new subprocess of this workflow group    |
| `metaworkflow.signal.group`     | Meta model | Signal event reacts to signals of this workflow group   |
| `metaworkflow.signal.name`      | Meta model | Signal event reacts to this signal (empty = any signal) |

**Workflow result** (technical model)

| Definition                                                  | Description                             |
| ----------------------------------------------------------- | --------------------------------------- |
| `<metaworkflow name="SIGNAL"><name>…</name></metaworkflow>` | Event publishes a signal with this name |

**Workitem items**

| Item                        | Stored in     | Description                                           |
| --------------------------- | ------------- | ----------------------------------------------------- |
| `$metaprocess.ref`          | Subprocess    | `$uniqueID` of the meta instance                      |
| `$metaprocess.subprocesses` | Meta instance | List of all subprocesses created by the meta instance |
| `$metaprocess.completed`    | Subprocess    | `true` if the completion signal was sent              |
| `$metaprocess.result`       | Subprocess    | Result of the subprocess (name of the end task)       |

**EventLog entry** (topic `metaworkflow.signal`, reference = `$uniqueID` of the meta instance):

| Item                            | Description                      |
| ------------------------------- | -------------------------------- |
| `$metaprocess.subprocess.ref`   | `$uniqueID` of the subprocess    |
| `$metaprocess.subprocess.group` | Workflow group of the subprocess |
| `$metaprocess.signal`           | Name of the signal               |

## Limitations

The current implementation supports the following scenarios:

- sequences of subprocesses, controlled by completion signals,
- branches of the meta instance, controlled by published signals of running subprocesses,
- multiple subprocesses of the same workflow group, started manually by the process owner.

The following scenarios are not supported yet:

- waiting until **all** subprocesses of a workflow group are completed,
- propagating a cancellation of the meta instance to running subprocesses.
