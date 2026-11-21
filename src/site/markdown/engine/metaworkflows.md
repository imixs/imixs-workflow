# Meta Workflows

A _Meta Workflow_ combines several independent technical workflows into one business process. It allows a process owner to define the business-level flow of a project, while the technical workflows (forms, ACLs, plugins, integrations) are still designed by the process modeler with Open-BPMN.

**Example:** A company runs the technical workflows _Inquiry_, _Offer_, _Order_ and _Invoice_. The process owner defines a meta workflow _Project Order_ that combines these workflows in exactly this sequence:

    Project Order:  Inquiry → Offer → Order → Invoice

Neither side needs to know the internals of the other. The technical workflows do not know that they are part of a meta workflow, and the meta workflow does not need to know task or event IDs of the technical workflows.

## The Concept

A meta workflow consists of two levels:

- **The meta instance** is a normal process instance based on a meta model. It holds the business-level status, project-wide data, its own ACL and the list of all subprocesses.
- **The subprocesses** are normal process instances based on the technical models. They run independently in the system with their own forms, events, ACLs and plugins.

The two levels communicate in two directions:

| Direction         | Mechanism                                                                             |
| ----------------- | ------------------------------------------------------------------------------------- |
| Meta → Subprocess | The `MetaWorkflowPlugin` starts a new subprocess when a meta event is processed       |
| Subprocess → Meta | The `MetaWorkflowHandler` signals the completion of a subprocess to the meta instance |

The completion of a subprocess is only a **signal**. Whether the meta instance reacts to it is defined by the meta model alone. If the meta model does not define a reaction, the meta instance remains unchanged.

## Starting a Subprocess

A meta model must register the plugin `org.imixs.workflow.engine.MetaWorkflowPlugin`.

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

## Completion of a Subprocess

A subprocess is completed when it reaches an **end task** of its workflow group. An end task is a task followed by a BPMN End Event.

The named End Events are the public interface of a technical model. For example, the workflow _Offer_ may define the End Events _Accepted_ and _Rejected_. The name of the reached End Event is the **result** of the subprocess. If the End Event has no name, the name of the end task is used instead.

The `MetaWorkflowHandler` observes the processing life cycle of all process instances. When a subprocess reaches an end task, the handler:

1. marks the subprocess with the items `$metaprocess.completed=true` and `$metaprocess.result`,
2. creates an EventLog entry with the topic `metaworkflow.subprocess.completed` and the `$uniqueID` of the meta instance as reference.

A completed subprocess signals its meta instance only once.

The technical models need no configuration for this. The process modeler only has to model meaningful, named End Events.

## Trigger Events

The reaction of a meta instance to a completed subprocess is defined by _Trigger Events_ in the meta model. A trigger event defines the following Imixs extension items:

| Item                          | Description                                                                              |
| ----------------------------- | ---------------------------------------------------------------------------------------- |
| `metaworkflow.trigger.group`  | Workflow group of the completed subprocess (required)                                    |
| `metaworkflow.trigger.result` | Result of the subprocess, the name of the End Event (optional, empty matches any result) |

**BPMN 2.0 Definition**

```xml
<bpmn2:intermediateCatchEvent id="CatchEvent_2" name="Offer accepted">
  <bpmn2:extensionElements>
    <imixs:item name="metaworkflow.trigger.group" type="xs:string">
      <imixs:value><![CDATA[Offer]]></imixs:value>
    </imixs:item>
    <imixs:item name="metaworkflow.trigger.result" type="xs:string">
      <imixs:value><![CDATA[Accepted]]></imixs:value>
    </imixs:item>
  </bpmn2:extensionElements>
</bpmn2:intermediateCatchEvent>
```

The signal of a completed subprocess is processed asynchronously by the `AsyncEventScheduler`, running as a system user. This is necessary because the user who completed the subprocess may not have write access to the meta instance.

For each signal, the handler looks for a trigger event **in the current task** of the meta instance that matches the workflow group and the result of the subprocess. If a matching event is found, the meta instance is processed by this event. Otherwise the signal is ignored.

Trigger events should be modeled as non-public events (`workflow.public=false`), so they are not shown to the user.

**Note:** A signal only takes effect if the meta instance is in a task with a matching trigger event at the time the signal is processed. The result of each subprocess is still stored in the subprocess itself (`$metaprocess.completed`, `$metaprocess.result`).

## Example: Project Order

The meta model _Project Order_ could define the following tasks and events:

| Task    | Event             | Type                 | Configuration                                                                                                     |
| ------- | ----------------- | -------------------- | ----------------------------------------------------------------------------------------------------------------- |
| (Start) | Create            | public               | `metaworkflow.subprocess.group=Inquiry`                                                                           |
| Inquiry | Inquiry completed | trigger (not public) | `metaworkflow.trigger.group=Inquiry`                                                                              |
| Offer   | Create Offer      | public               | `metaworkflow.subprocess.group=Offer`                                                                             |
| Offer   | Offer accepted    | trigger (not public) | `metaworkflow.trigger.group=Offer`, `metaworkflow.trigger.result=Accepted`, `metaworkflow.subprocess.group=Order` |
| Order   | Order completed   | trigger (not public) | `metaworkflow.trigger.group=Order`, `metaworkflow.subprocess.group=Invoice`                                       |

The event _Create Offer_ can be triggered by the process owner several times, for example to create a second offer. A rejected offer does not change the meta instance, because there is no trigger event for the result _Rejected_.

As a trigger event can also define the item `metaworkflow.subprocess.group`, a completed subprocess can start the next subprocess automatically.

## Configuration

The signals of completed subprocesses are processed by the `AsyncEventScheduler`. The scheduler must be enabled:

    ASYNCEVENT_PROCESSOR_ENABLED - must be set to true (default=false)
    ASYNCEVENT_PROCESSOR_INTERVAL - timeout period in milliseconds
    ASYNCEVENT_PROCESSOR_DEADLOCK - deadlock in milliseconds (default 1 minute)

See the section [Async Events](./asyncevents.html) for details.

## Item Reference

| Item                        | Stored in     | Description                                                |
| --------------------------- | ------------- | ---------------------------------------------------------- |
| `$metaprocess.ref`          | Subprocess    | `$uniqueID` of the meta instance                           |
| `$metaprocess.subprocesses` | Meta instance | List of all subprocesses created by the meta instance      |
| `$metaprocess.completed`    | Subprocess    | `true` if the completion was signaled to the meta instance |
| `$metaprocess.result`       | Subprocess    | Result of the subprocess (name of the End Event)           |

**EventLog entry** (topic `metaworkflow.subprocess.completed`, reference = `$uniqueID` of the meta instance):

| Item                            | Description                             |
| ------------------------------- | --------------------------------------- |
| `$metaprocess.subprocess.ref`   | `$uniqueID` of the completed subprocess |
| `$metaprocess.subprocess.group` | Workflow group of the subprocess        |
| `$metaprocess.result`           | Result of the subprocess                |

## Limitations

The current implementation supports the following scenarios:

- linear sequences of subprocesses,
- multiple subprocesses of the same workflow group, started manually by the process owner.

The following scenarios are not supported yet:

- waiting until **all** subprocesses of a workflow group are completed,
- propagating a cancellation of the meta instance to running subprocesses.
