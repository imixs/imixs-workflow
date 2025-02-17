# How to Model with Imixs-BPMN?

**Imixs-Workflow** is based on the [BPMN 2.0 modeling standard](http://www.bpmn.org/). BPMN enables you to describe your business process from different perspectives. You can describe the organizational aspects just to give people an understanding of your process. And you can as well model the technical details to execute your process within the Imixs-Workflow engine.

You create an Imixs-Workflow model with the Eclipse based modeling tool [Imixs-BPMN](index.html). To install Imixs-BPMN see the [installation guide](./install.html).

Let's take look at a simple example:

<img src="../images/modelling/example_01.png"  />

The blue boxes symbolize a **Task**, while the yellow symbols describe an **Event** that changes the state within the process.
This example contains two tasks: '_New Ticket_' and '_Open_' and the event '_Submit_'.

A process instance is always assigned to exactly one Task within your model. An event is typically triggered by a process participant within your process. This type of modeling is also known as event-driven modeling.

## Event Gateways

If a Workflow Task has more than one Workflow Event, the BPMN element "Event Gateway" can be used to model alternative process flows:

<img src="../images/modelling/example_02.png"/>

A BPMN "Event Gateway" is an exclusive gateway and should always have the gateway direction 'Diverging'. This means that the Gateway has only one incoming sequence flow but can have one or many outgoing sequence flows. The gateway direction can be changed in Eclipse-BPMN after the feature 'Show Advanced Property Tabs' is activated in the general workspace preferences.

## Loop Events

In some cases a Workflow Event is used to update a process instance without changing the process state. For example if a user just updates a process instance:

<img src="../images/modelling/example_03.png"/>

In this case the Workflow Event can have an outgoing sequence flow pointing to the current workflow task. This is called a Loop Event. A Loop event can also be modeled using an Event Gateway:

<img src="../images/modelling/example_04.png"/>

In this example the Loop Event has one incoming and one outgoing sequence flow.

## Follow-Up Events

A Workflow Event can also point to another Workflow Event. This is called a _Follow-Up Event_'\*.

<img src="../images/modelling/example_05.png"/>

In this example the event 'Submit' triggers the follow-up event 'Send E-Mail' which finally completes the transition into the new process state 'Open'.

Follow-Up Events are typically used in more complex scenarios where additional business rules need to be evaluated:

<img src="../images/modelling/example_06.png"/>

In this example a business rule is applied after the new order was accepted. In case the amount is below 100,- EUR the order is processed by the sales team. In case the amount is greater than 100,- EUR the order will be forwarded to the management by the follow-up event 'forward'. The follow-up event can be used to change the properties of the workitem like ACL settings or additional notifications. A follow-up event is typically not visible. See the [section Event Properties](./activities.html) for further details.

## Async Events

An _Async Event_ is executed asynchronous after a processing life-cycle. In difference to _Follow-Up Events_ the process instance will be persisted in a new status before a async event is executed.

Async Events are modeled as a BPMN Boundary Event with an outgoing sequence flow to an Imixs-Event element:

<img src="../images/modelling/example_13.png"/>

The event element connected to the boundary event will be executed by the Imixs-Workflow engine after a process instances reaches the corresponding task.
See the section [Async Events](../engine/asyncevents.html) for more details.

## Conditional Events

A conditional event is used to evaluate the output of an event during the processing life-cycle. A conditional-event can be placed before a _ExclusiveGateway_, an _InclusiveGateway_ or an _EventBasedGateway_. Each output of the event must define a boolean rule expression.

<img src="../images/modelling/example_08.png"/>
 
In this example two conditions are added to the ExclusiveGateway output evaluating the attribute "_budget" to continue with either 'Task 2' or 'Task 3'. 
A boolean expression can look like the following example, which is evaluating the attribute '_budget':

    (workitem.getItemValueDouble('_budget')>100)

**Note:** To add conditional sequence flows the full BPMN profile must be activated in the Imixs-BPMN Modeler.

A conditional event can define conditions for either a Task element or an Event element. See the next example:

<img src="../images/modelling/example_09.png"/>
 
The second condition defines a Follow-Up Event in case the '_budget' is <= 100:

    (workitem.getItemValueDouble('_budget')<=100)

The script language for the boolean expression is 'JavaScript'. See the [RulePlugin](../engine/plugins/ruleplugin.html) for further details about business rules in Imixs-Workflow.

**Note:** In BPMN one so called "Default Flow" can be defined for an exclusive gateway.

<img src="../images/modelling/example_14.png"/>

The default flow is evaluated only if none of the other conditions matches.

## Split Events

A _split-event_ is used to create a new version of the current process instance.
A _split-event_ is always followed by a _Parallel Gateway_. This is also called a _parallel-workflow_.

<img src="../images/modelling/example_11.png"/>

Imixs workflow evaluates the outcome of the _ParallelGateway_. The Gateway must be followed by exactly one _Task_ element and at least one _Event_ element. For each Event element that follows the Gateway, a new version of the current process instance is created and processed directly by the Event.

| Outcome |     Type      | Description                                             |
| :-----: | :-----------: | ------------------------------------------------------- |
|  Task   | Main WorkItem | describes the outcome for the current process instance. |
|  Event  |    Version    | triggers the creation of a new version.                 |

As a Split Event is creating a new independent version of the current process instance, a join is typically not modeled. This means that the current process instance will not wait for all incoming flows of parallel versions. This is the default behavior in Imixs-Workflow. Split-Events are typically used to create sub-workflow, archive the current status or finalize a certain state of processing.

## Link Events

In a complex process model, it can be helpful to avoid too many overlapping sequence flows. Therefore the BPMN Link Events can be used. The link event describes a connection between two elements.

<img src="../images/modelling/example_07.png"/>

In a Imixs-Workflow model a Link Event can be modeled using one intermediate "Throw Event" and one intermediate "Catch Event" from the event type 'Link'. Both link events should have the same name to indicate the connection between them.

## Number Conventions

Events and Tasks should have unique IDs.
A 2-digit ID is used for public events. 3-digit IDs are typically used for non-public events (e.g background tasks)
The following table shows a list of event IDs following the Imixs number convention:

### Events

|  Event  |    Type    | Description                                  | Example                 |
| :-----: | :--------: | -------------------------------------------- | ----------------------- |
| 1[0-9]  |   PUBLIC   | update/save (task did not change)            | 10-Save                 |
| 2[0-9]  |   PUBLIC   | submit/forward (next task)                   | 20-Submit, 21-Approve   |
| 3[0-9]  |   PUBLIC   | reject (previous task)                       | 30-Reject , 31-Callback |
| 4[0-9]  |   PUBLIC   | create subtask (task did not change)         | 40-Create Invoice       |
| 5[0-9]  |   PUBLIC   | exception/error (new task)                   | 50-Cancel Approval      |
| 9[0-9]  |   PUBLIC   | archive/delete (change into final task)      | 90-Archive              |
| 1[0-99] | NON-PUBLIC | creates a new workitem (task did not change) |
| 2[0-99] | NON-PUBLIC | update a workitem (task did not change)      |
| 3[0-99] | NON-PUBLIC | exception/error (new task)                   |
| 9[0-99] | NON-PUBLIC | archive/delete (change into final task)      |

### Task

|   Task    | Description    |
| :-------: | -------------- |
| 1000-9999 | Main Process   |
| 2000-9999 | Sub Process    |
|  100-999  | System Process |

## What's Next...

Continue reading more about:

- [How to Manage your Business Data](../quickstart/workitem.html)
- [Why You Should Use Imixs-Workflow](../quickstart/why.html)
- [What Means Human Centric Workflow?](../quickstart/human.html)
- [Imixs-BPMN - The Modeler User Guide](../modelling/index.html)
- [Modeling Roles & Responsibilities](../quickstart/roles_responsibilities.html)
- [The Imixs-Workflow Plugin API](../engine/plugins/index.html)
- [The Imixs-Workflow Rest API](../restapi/index.html)
