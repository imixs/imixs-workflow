# Split & Joins

With the _Split & Join_ functionality of Imixs-Workflow it is possible to split one process instance (origin workitem) in one or more sub-process instances (sub workitems). This functionality is provided by the '_SplitAndJoinPlugin_'.

    org.imixs.workflow.engine.plugins.SplitAndJoinPlugin

The _Split & Join_ functionality can be defined within the [Imixs-BPMN modelling tool](../../modelling/index.html) by adding a _Split & Join_ function definition into the workflow result of the corresponding BPMN Event.

<img src="../../images/modelling/splitandjoin-example-01.png"/>

After a new sub-process instance was created, the origin workitem and the new sub-workitem are connected to each other by the item `$workitemRef`.
In addition the sub-workitem contains the item `$uniqueIdRef` with a reference to the origin workitem.

<center><img src="../../images/engine/split-and-join-ref.png"/></center>

In this way it is possible to navigate in both directions.

## Functions & Definitions

The _Split & Join_ functionality can be defined in the workflow result of a BPMN event. The SplitAndJoinPlugin provides different functions to create, update or synchronize process instances. This means that data and the state can be split and joined in various ways.

The _Split & Join_ defines the following functions:

- <strong>subprocess_create</strong> = creates a new sub-process instance
- <strong>subprocess_update</strong> = update an existing subprocess
- <strong>origin_update</strong> = updates the origin process
- <strong>subprocess_sync</strong> = synchronizes a list of item values from the origin process

## Creating a New Sub-Process Instance

To create a new sub-process instance, the following XML definition need to be added into the workflow result of an Imixs-BPMN Event:

    <split name="subprocess_create">
    	<modelversion>1.0.0</modelversion>
    	<task>100</task>
    	<event>10</event>
    	<items>namTeam</items>
    </split>

The `name` attribute of the `split` tag defines the function name. In this example Plugin will create a new subprocess instance with the model version `1.0.0` and the initial task `100`, which will be processed by the eventID `10`.

**Note:** A new sub-process instance can run in the same model as the origin process instance or run in a complete different model.

Of course you can also define multiple split functions in one workflow result.

### Adding Item Data

The optional tag `items` defines a list of attributes to be copied from the origin process into the new sub-process. The tag may contain a list of items separated by comma.

    <items>namTeam,txtName,_orderNumber</items>

To avoid name conflicts, the item name can be mapped to a different name by separating the new name with the '|' char.

    <items>namTeam,txtName,_orderNumber|_origin_orderNumber</items>

In this example the item `_ordernumber` will be copied into the target workitem with the new item name `_origin_ordernumber`.

### Copy Items by Regex

The _items_ tag also supports regular expression. See the following example with will copy all items starting with alphabetical characters or `_`:

    <items>$workflowsummary|_parentworkflowsummary,(^[a-zA-Z]|^_)</items>

A regular expression must always be included in brackets.

**Note:** In case of a regular expression you can not use item name mapping with the '|' character.

### The Action result

After a new subprocess was created, an optional action result can be evaluated to overwrite the default action result.
The following example computes a new action result based on the uniqueId of the new subprocess:

    <split name="subprocess_create">
        <modelversion>1.1.0</modelversion>
        <task>1000</task>
        <event>10</event>
        <action>/pages/workitems/workitem.jsf?id=<itemvalue>$uniqueid</itemvalue></action>
    </split>

## Updating a Sub-Process Instance

To process an existing sub-process instance within the processing live-cycle of the origin workitem, the function `subprocess_update` can be defined in a BPMN event of the origin workitem:

    <split name="subprocess_update">
    	<modelversion>1.0.0</modelversion>
    	<task>100</task>
    	<event>20</event>
    	<items>namTeam</items>
    </split>

During processing the origin workitem, the SplitAndJoinPlugin will also process all existing sub process instances matching the model and task definition.
In this example all sub process instances in the model `1.0.0` with the task `100` will be process by teh event `20`.

### Regular Expressions

The matching algorithm for a sub-process instance accepts also regular expressions. See the following example:

    <split name="subprocess_update">
    	<modelversion>(^1.0)|(^2.0)</modelversion>
    	<task>(^1000$|^1010$)</task>
    	<event>20</event>
    	<items>namTeam</items>
    </split>

This example applies to all existing sub-process instances with a model version starting with '1.0' or '2.0' and the taskID 1000 or 1010.

To match all taskIds between 1000 and 1999 the following regular expression can be applied:

    <split name="subprocess_update">
    	<modelversion>(^1.0)|(^2.0)</modelversion>
    	<task>(1\d{3}))</task>
    	<event>20</event>
    	<items>namTeam</items>
    </split>

## Updating the Origin-Process Instance

To join the data and status of a sub-process instance back to the origin process, the function `origin_update` can defined:

    <split name="origin_update">
    	<event>20</event>
    	<items>namTeam</items>
    </split>

The origin-process instance will be processed by the EvenID defined by the tag _event_.

**Note:** The split function `origin_update` is only allowed once in the same workflow result definition!

## Synchronize Origin Items

To synchronize the data of a sub-process instance with the origin process, the function `subprocess_sync` can be defined:

    <split name="subprocess_sync">
    	<items>facility,project</items>
    </split>

This function will copy the values of the items `facility` and `project` from the origin process instance. Also here regular expressions are allowed as explained before.
