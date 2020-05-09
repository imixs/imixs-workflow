# Split & Joins 

With the _Split & Join_ functionality you can split up a process instance (origin) in one or more sub-process instances. The _Split & Join_ functionality is added by the plug-in '_SplitAndJoinPlugin_'.

	org.imixs.workflow.engine.plugins.SplitAndJoinPlugin

<img src="../../images/modelling/splitandjoin-example-01.png"/>

Depending on the model configuration a sub-process instances can also update the origin process instance. This means that data and the state can join the origin process instance.  

The _Split & Join_ defines the following functions:
 
 * <strong>subprocess_create</strong> = creates a new sub-process instance 
 * <strong>subprocess_update</strong> = update an existing subprocess 
 * <strong>origin_update</strong> = updates the origin process 
 
The _Split & Join_ functionality can be defined within the [Imixs-BPMN modelling tool](../../modelling/index.html) by adding a _Split & Join_ function into the workflow result of a Imixs-BPMN Event. 

<img src="../../images/modelling/bpmn_screen_36.png"/> 
 
## Creating a New Sub-Process Instance
 
To create a new sub-process instance, the following item definition need to be added into the workflow result of an Imixs-BPMN Event: 
 
	<item name="subprocess_create">
		<modelversion>1.0.0</modelversion>
		<task>100</task>
		<event>10</event>
		<items>namTeam</items>
	</item>

A new sub-process instance can run in the same model as the origin process instance or run in a complete different model.

This example will create a new subprocess instance with the model version '1.0.0' and the initial task 100, which will be processed by the eventID 10. 
Of course you can also define multiple sub-process definitions in one workflow result. For each separate definition a new sub-process instance will be created.

### Adding Item Data

The optional tag 'items' defines a list of attributes to be copied from the origin process into the new sub-process. The tag may contain a list of items separated by comma. 

    <items>namTeam,txtName,_orderNumber</items>

To avoid item name conflicts the item name in the target workitem can be mapped by separating the new item name with the a '|' char. 

    <items>namTeam,txtName,_orderNumber|_origin_orderNumber</items>

In this example the item '_ordernumber' will be copied into the target workitem with the new item name '_origin_ordernumber'.

### Copy Items by Regex

The _items_ tag also suppors regular expression. See the following example with will copy all items starting with alphabetical characters or '_':

	<items>$workflowsummary|_parentworkflowsummary,(^[a-zA-Z]|^_)</items>

A regular expression must always be included in brackets.
 
__Note:__ In case of a regular expression you can not use item name mapping with the '|' character. 
 

### The Action result

After a new subprocess was created, an optional action result can be evaluated to overwrite the action result provided by the ResultPlugin.
The following example computes a new action result based on the uniqueId of the new subprocess:

	<item name="subprocess_create">
	    <modelversion>1.1.0</modelversion>
	    <task>1000</task>
	    <event>10</event>
	    <action>/pages/workitems/workitem.jsf?id=<itemvalue>$uniqueid</itemvalue></action>
	</item>

## Updating a Sub-Process Instance

To update an existing sub-process instance the following item definition need to be added into the workflow result of an Imixs-BPMN Event: 
 
	<item name="subprocess_update">
		<modelversion>1.0.0</modelversion>
		<task>100</task>
		<event>20</event>
		<items>namTeam</items>
	</item>

The _task_ tag defines the matching sub-process instance. The sub-process instance will be processed by the EvenID defined by the tag _event_.  


### Regular Expressions
The matching algorithm for a sub-process instance accepts also regular expressions. See the following example:

	<item name="subprocess_update">
		<modelversion>(^1.0)|(^2.0)</modelversion>
		<task>(^1000$|^1010$)</task>
		<event>20</event>
		<items>namTeam</items>
	</item>

This example applies to all existing sub-process instances with a model version starting with '1.0' or '2.0' and the taskID 1000 or 1010.

To match all taskIds between 1000 and 1999 the following regular expression can be applied:

	<item name="subprocess_update">
		<modelversion>(^1.0)|(^2.0)</modelversion>
		<task>(1\d{3}))</task>
		<event>20</event>
		<items>namTeam</items>
	</item>

## Updating the Origin-Process Instance

To join the data and status of a sub-process instance back to the origin process, the following item definition need to be added into the workflow result of an Imixs-BPMN Event: 

	<item name="origin_update">
		<event>20</event>
		<items>namTeam</items>
	</item>

The origin-process instance will be processed by the EvenID defined by the tag _event_.  

**Note:** In case of an origin_update, only one definition is allowed in a subprocess event!

### The Action result

After the origin process was updated, an optional action result can be evaluated to overwrite the action result provided by the ResultPlugin.
The following example computes a new action result based on the uniqueId of the newly created subprocess:

	<item name="subprocess_create">
	    <modelversion>1.1.0</modelversion>
	    <task>1000</task>
	    <event>10</event>
	    <action>/pages/workitems/workitem.jsf?id=<itemvalue>$uniqueid</itemvalue></action>
	</item>


## Linking

After a new sub-process instance was created, both workitems are connected to each other by the items ' _$uniqueidRef_' and 
'_$workitemRef_'.

<img src="../../images/engine/split-and-join-ref.png"/> 

After a _split_ the new sub-process instance is linked to the origin process instance by the item _$uniqueidRef_. The origin process holds the $uniqueids of all sub-process instances in the item _$workitemRef_. So both workitems are linked together.
 
