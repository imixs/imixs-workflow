#Split & Joins 
The Imixs Split & Join plugin provides the functionality to create and update sub-process instances from an existing process instance (origin process),  or to update the origin process instance based on the processing instruction of a subprocess instance. 

	org.imixs.workflow.engine.plugins.SplitAndJoinPlugin

A 'split' means that a new subprocess instance will be created and linked to the current process instance (origin process). A 'join' means that a subprocess instance will update the origin process instance. The Split & Join definition is defined in the workflow result of a Imixs-BPMN Event definition using the [Imixs-BPMN modelling tool](../../modelling/index.html).   

The plugin evaluates the following items definition of a workflow result ("txtactivityResult"):
 
 * <strong>subprocess_create</strong> = create a new subprocess assigned to the current workitem 
 * <strong>subprocess_update</strong> = update an existing subprocess assigned to the current workitem
 * <strong>origin_update</strong> = update the origin process assigned to the current workitem
 
A subprocess instance will contain the ID of the origin process instance stored in the property $uniqueidRef. The origin process will contain a link to the subprocess stored in the property txtworkitemRef. So both workitems are linked together.
 
<img src="../../images/engine/split-and-join-ref.png"/> 
 
## Creating a new Subprocess
 
To create a new subprocess instance during the processing life cycle of an origin process, a item named 'subprocess_create' need to be defined in the corresponding event result. See the following example: 
 
	<item name="subprocess_create">
		<modelversion>1.0.0</modelversion>
		<task>100</task>
		<event>10</event>
		<items>namTeam</items>
	</item>

This example will create a new subprocess instance with the model version '1.0.0' and the initial task 100, which will be processed by the eventID 10. The tag 'items' defines a list of attributes to be copied from the origin process into the new subprocess.

Both workitems will be connected to each other. The subprocess will contain the $UniqueID of the origin process stored in the property $uniqueidRef. The origin process will contain a link to the subprocess stored in the property txtworkitemRef. So it is possible to navigate between the process instances.
 
It is also possible to define multiple subprocess definitions in one workflow result. For each separate definition a new subprocess will be created.

### Copy Items Source to Target

The tag 'items' specifies a list of items to be copied from the origin workitem into the subprocess workitem. The tag may contain a list of items separated by comma. 

    <items>namTeam,txtName,_orderNumber</items>

### Copy Items With Mapped ItemName

To avoid item name conflicts the item name in the target workitem can be mapped by separating the new item name with the a '|' char. 

    <items>namTeam,txtName,_orderNumber|_origin_orderNumber</items>

In this example the item '_ordernumber' will be copied into the target workitem with the new item name '_origin_ordernumber'.

### Copy Items by Regex

You can also define the items to be copied into the target workitem by a regular expression. See the following example with will copy all items starting with alphabetical characters or '_':

	<items>$workflowsummary|_parentworkflowsummary,(^[a-zA-Z]|^_)</items>

A regular expression must always be included in brackets.
 
__Note:__ In case of a regular expression you can not use item name mapping with the '|' character. 
 

### Action result

After a new subprocess was created, an optional action result can be evaluated to overwrite the action result provided by the ResultPlugin.
The following example computes a new action result based on the uniqueId of the new subprocess:

	<item name="subprocess_create">
	    <modelversion>1.1.0</modelversion>
	    <task>1000</task>
	    <event>10</event>
	    <action>/pages/workitems/workitem.jsf?id=<itemvalue>$uniqueid</itemvalue></action>
	</item>



 
## Updating a Subprocess

To update an existing subprocess instance during the processing life cycle of the origin process, a item named 'subprocess_update' can be defined in the corresponding event result. See the following example: 
 
	<item name="subprocess_update">
		<modelversion>1.0.0</modelversion>
		<task>100</task>
		<event>20</event>
		<items>namTeam</items>
	</item>


The evntId defines the workflow event to be processed on the matching subprocess instance. The tag 'items' defines the list of attributes to be added or updated from the origin process into the new subprocess.
Subprocesses and the origin process are connected to each other. The subprocess will contain the $UniqueID of the origin process stored in the property $uniqueidRef. The origin process will contain a link to the subprocess stored in the property txtworkitemRef.


### Regular Expressions
The definition accepts regular expressions to filter a subset of existing process instances. See the following example:

	<item name="subprocess_update">
		<modelversion>(^1.0)|(^2.0)</modelversion>
		<task>(^1000$|^1010$)</task>
		<event>20</event>
		<items>namTeam</items>
	</item>

This example applies to all existing subprocess instances with model versions starting with '1.0' or '2.0' and the task 1000 or 1010.
To match all taskIds between 1000 and 1999 the following regular expression can be applied:

	<item name="subprocess_update">
		<modelversion>(^1.0)|(^2.0)</modelversion>
		<task>(1\d{3}))</task>
		<event>20</event>
		<items>namTeam</items>
	</item>
 



## Updating the Origin Process

To join the data and status of a subprocess instance with the origin process instance a item named 'origin_update' can be defined in the event result of a subprocess definition. 
Only one definition to update the origin process is allowed in a subprocess event. See the following example:

	<item name="origin_update">
		<event>20</event>
		<items>namTeam</items>
	</item>

The definition will update the origin process instance linked to the current subprocess. As the origin process instance is uniquely defined by the attribute $UniqueIDRef no further expression is needed in this case.   
The tag 'items' defines the list of attributes to be updated from the subprocess into the origin process.


### Action result

After the origin process was updated, an optional action result can be evaluated to overwrite the action result provided by the ResultPlugin.
The following example computes a new action result based on the uniqueId of the originprocess:

	<item name="subprocess_create">
	    <modelversion>1.1.0</modelversion>
	    <task>1000</task>
	    <event>10</event>
	    <action>/pages/workitems/workitem.jsf?id=<itemvalue>$uniqueid</itemvalue></action>
	</item>


