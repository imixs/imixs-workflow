#Split & Joins 
The Imixs Split & Join Plugin provides functionality to create and update sub-process instances from a workflow event (origin process),  or update the origin process from a sub-process instance.

	org.imixs.workflow.plugins.jee.SplitAndJoinPlugin

A 'split' means that a new subprocess is started which is linked to the current process instance (origin process). A 'join' means that a subprocess can update the origin process or provide data from the subprocess instance. The Split & Join definition can be specified using the [Imixs-BPMN modelling tool](../../modelling/index.html).  

The plugin evaluates the following items defined in the workflow result ("txtactivityResult"):

 
 * <strong>subprocess_create</strong> = create a new subprocess assigned to the current workitem 
 * <strong>subprocess_update</strong> = update an existing subprocess assigned to the current workitem
 * <strong>origin_update</strong> = update the origin process assigned to the current workitem
 
A subprocess will contain the $UniqueID of the origin process stored in the property $uniqueidRef. The origin process will contain a link to the subprocess stored in the property txtworkitemRef. So both workitems are linked together.
 
 
## Create a Subprocess
 
To create a new subprocess during the processing life cycle a item named 'subprocess_create' need to be defined in the activity result. See the following example: 
 
	<item name="subprocess_create">
		<modelversion>1.0.0</modelversion>
		<processid>100</processid>
		<activityid>10</activityid>
		<items>namTeam</items>
	</item>

In this example a new subprocess with the model version '1.0.0' and the intial processID 100 will be processed by the activityID 10. The tag 'items' defines a list of attributes to be added from the origin process into the new subprocess.
 It is possible to define multiple subprocess definitions. For each definition a new subprocess will be created. Both workitems are connected to each other. The subprocess will contain the $UniqueID of the origin process stored in the property $uniqueidRef. The origin process will contain a link to the subprocess stored in the property txtworkitemRef.
 
 
## Update a Subprocess

To update an existing subprocess during the processing life cycle a item named 'subprocess_update' need to be defined in the activity result. See the following example: 
 
	<item name="subprocess_update">
		<modelversion>1.0.0</modelversion>
		<processid>100</processid>
		<activityid>20</activityid>
		<items>namTeam</items>
	</item>


The activityid defines the workflow event to be processed on the matching subprocess instance. The tag 'items' defines a list of attributes to be added from the origin process into the new subprocess.
Subprocesses and the origin process are connected to each other. The subprocess will contain the $UniqueID of the origin process stored in the property $uniqueidRef. The origin process will contain a link to the subprocess stored in the property txtworkitemRef.


### Regular Expressions
The definition accepts regular expressions to filter a subset of existing process instances. See the following example:

	<item name="subprocess_update">
		<modelversion>(^1.0)|(^2.0)</modelversion>
		<processid>(^1000$|^1010$)</processid>
		<activityid>20</activityid>
		<items>namTeam</items>
	</item>

This example applies to all existing subprocess instances with model versions starting with '1.0' or '2.0' and the processId 1000 or 1010.
To match all processIds between 1000 and 1999 the following regular expression can be applied:

	<item name="subprocess_update">
		<modelversion>(^1.0)|(^2.0)</modelversion>
		<processid>(1\d{3}))</processid>
		<activityid>20</activityid>
		<items>namTeam</items>
	</item>
 



## Update the Origin Process

To join the data and status of a subprocess with the origin process a item named 'origin_update' can be defined in the txtactivityResult. 
Only one definition to update the orgian process is allowed in a subprocess event. See the following example:

	<item name="origin_update">
		<activityid>20</activityid>
		<items>namTeam</items>
	</item>

The definition will update the origin process instance linked to the current subprocess. 
The tag 'items' defines a list of attributes to be updated from the subprocess into the origin process.