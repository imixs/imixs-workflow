# Why Should I Use Imixs-Workflow?

<p class="lead">
Most business applications do more than store data. A support ticket is opened, assigned, solved and closed. An order is created, checked, approved and shipped. A leave request is submitted, reviewed and then approved or rejected. Behind each of these examples is a business process: the data moves through several states, and different people within an organisation  are responsible at each step.
</p>

At the beginning, this looks mostly simple to solve the status problem. A status field in the database is enough to remember where an object stands. But the questions that follow are no longer about data: Which steps are allowed from here? Who may carry them out? Who approved it, and when? And what happens when the process changes next quarter?

Each answer adds code that has to be written, tested and maintained, whether you write it by hand or have it generated. Storing the data is the easy part. Managing the states, and who may change them, is the part that keeps growing.

A workflow engine takes this part over. You describe the process in a BPMN model, and the engine manages status, responsibilities, access rights and a processing history for every process instance.

The following sections show what that means in practice. The Java examples are optional. If you worked through the [tutorial](../tutorials/tutorial-01.html), you have already seen the status and the available actions in the web form, without writing any code.

## Status and Available Actions

Without a workflow engine, the status is just a field in your data. Any piece of code can write any value into it, and nothing stops a ticket from jumping from "New" straight to "Closed", or from going back to a state it should never return to. To prevent this, an if-else chain somewhere in your code decides which transitions are allowed in which state, and every new feature has to respect it.

With Imixs-Workflow, the model defines a fixed set of tasks (the states) and the events (the actions) that connect them. Together they form a defined process flow, and every process instance has to follow it. The engine knows which events are possible in the current task and rejects every other event. Even the task of a running process instance cannot simply be changed from outside. In the tutorial, the buttons in the web form are exactly these events.

This is the advantage of a workflow engine: the process flow is enforced, every change of status is traceable. It can only happen through a defined event, and the engine records it in the processing history (see below).

The model is also both the documentation and the implementation of the process. You can show the diagram to a colleague from the business side and discuss it, and the flow of your application is exactly the flow in the diagram.

From Java, you can read the status and the process name of a process instance. Both come from your model and need no longer be managed by your code:

    String status = workitem.getItemValueString(WorkflowKernel.WORKFLOWSTATUS);
    String group = workitem.getItemValueString(WorkflowKernel.WORKFLOWGROUP);

You can also ask the engine which events are currently available, and process one of them:

    List<ItemCollection> events = workflowService.getEvents(workitem);
    // process the workitem with a new event
    workitem = workflowService.processWorkItem(workitem, event);

The result depends on the current task. The list of events holds information from the model, for example the name or the description of an event. Models can also contain business rules that describe how a process behaves under certain circumstances:

<img src="../images/modelling/example_06.png"/>

You can find more information in the section ['How to Model'](../modelling/howto.html).

## Responsibilities and Access

Without a workflow engine, you need your own permission tables and a check in every service method that changes the data.

With Imixs-Workflow, the responsibilities (the owners) and the access rights are part of the model and can be set individually for each task or event:

<img src="../images/bpmn-example02.png" width="500px" />

The engine also adds an [Access Control List (ACL)](https://www.imixs.org/doc/engine/acl.html) to each process instance. This ensures that only authorized persons can change it. From Java, you can read this information:

    List<String> owners = workitem.getItemValue("$owner");
    List<String> authors = workitem.getItemValue(WorkflowKernel.WRITEACCESS);
    List<String> readers = workitem.getItemValue(WorkflowKernel.READACCESS);

A shortcut to ask if the current process instance is editable by the current user:

    boolean editable = workitem.getItemValueBoolean(WorkflowKernel.ISAUTHOR);

## Processing Information and History

Without a workflow engine, you add audit columns and a log table, and you have to keep them consistent in every code path.

With Imixs-Workflow, the engine records who created the process instance, who edited it last and when:

    String creator = workitem.getItemValueString(WorkflowKernel.CREATOR);
    String editor = workitem.getItemValueString(WorkflowKernel.EDITOR);
    Date created = workitem.getItemValueDate(WorkflowKernel.CREATED);
    Date modified = workitem.getItemValueDate(WorkflowKernel.MODIFIED);

A complete list of all processing information can be found [here](./workitem.html).

The engine also logs every processing step, so that a continuous log is created. This log can be used to display a processing history:

<img src="../images/modelling/order-02.png" width="500px" />

The processing history is generated by the [HistoryPlugin](../engine/plugins/historyplugin.html). Plugins are a way to adapt the behavior of the workflow engine to your own needs. Learn more about this interface in the [Plugin API](../engine/plugins/index.html).

## Change the Process, Not the Code

The model is data, not code. In the tutorial you added a form field by editing the model, and the web form changed with it. The buttons in the form come from the events of the current task, so a new event in the model gives the user a new action.

You can also upload a new model to a running engine with a single call to the [REST API](../restapi/index.html).

## When You Do Not Need a Workflow Engine

If a business object has only a few states, no different roles and no requirement to document who did what, a status field is enough.

A workflow engine starts to pay off when several people with different roles work on the same object, when rules decide who may do what, when you need a traceable history, or when the process changes over time.

## What's Next...

Continue reading more about:

- [Tutorial: Your First Workflow](../tutorials/tutorial-01.html)
- [What Means Human Centric Workflow?](../quickstart/human.html)
- [Imixs-BPMN - The Modeler User Guide](../modelling/index.html)
- [The Imixs Microkernel Architecture](../architecture/microkernel.html)
- [The Imixs-Workflow Plugin API](../core/plugin-api.html)
- [The Imixs-Workflow REST API](../restapi/index.html)
