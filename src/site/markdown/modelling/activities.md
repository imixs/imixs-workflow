# The Imixs-BPMN Event Element

The Imixs-BPMN Event element is an extension to the BPMN 2.0 Intermediate Catch Event. This element describes the transition from one Task into another. The Event Element contains various properties which are evaluated during the execution by the Imixs-Workflow engine. 

<img src="../images/modelling/bpmn_screen_05.png"/>

The properties of an Imixs-BPMN event element are grouped into different sections:
 
## General Properties
In the section _General_ you provide basic information for the Imixs-BPMN Event element. 

<img src="../images/modelling/bpmn_screen_19.png"  />


### Name 

The name of an event is used to identify the event by the user to process a WorkItem. The name is typically a noun describing the action triggered by the user,  e.g. "submit", "approve" "close".

### Documentation

The documentation of the Event element is optional and provides the user with additional information about the event. Typically the documentation contains  information how the event will change the current status of the process instance. 

The properties name and documentation can be used  by an application to display event based information- e.g. as buttons or hyper links. 

<img src="../images/modelling/bpmn_screen_30.png" />


 
| Property        | Type   	| Description									 				|
|-----------------|---------|---------------------------------------------------------------|
| name            | String 	| name of the event element       								|
| documentation   | String 	| short description				   								|

 
 
## Workflow Properties
In the section _Workflow_ you provides the processing information for an Event. These information are evaluated by the Workflow Engine while a WorkItem is processed 
 
<img src="../images/modelling/bpmn_screen_20.png" width="600px" />  
  
 
### ID 
Every Imixs-Event Element assigned to an Imixs Task Element has an unambiguously ID. The Event ID is used  by the Workflow Engine to identify the Event. You can also use the ID to trigger an event manually.  

	...
	myWorkitem.event(10);
	workflowService.process(myWorkitem);
	....
 
 
### Result
The 'Result' defines application specific processing information evaluated by the Workflow Engine. These information are evaluated by plugins and applications. See the section [ResultPlugin](../engine/plugins/resultplugin.html) for further information.

The result information is usually an XML fragment containing a structured list of elements. 

 
### Visiblity
The section 'visibility' defines whether an event is visible to a workflow user or not. The visibility is separated into three levels of visibility. 

<ul>
 
<strong>Public Events:</strong> <br />
Per default each Imixs-Event is public and visible to all users. If the property 'Public Event' is set to 'No', the event will not be part of the public event list of an workitem and not be displayed by an application as a click action.<br /><br />

<strong>Restricted Events:</strong><br />
A 'Public Event' can be restricted to a specific group of users depending on the state of a workflow instance. The restriction is based on the 'Actor Properties' which are defined by the <a href ="./main_editor.html">process properties</a> of the workflow model. If a restriction is set the event is only visible to the workflow user if the userid is listed in one of the Actor Properties. With the restriction feature, the visibility of a workflow event can be configured on a very fine grained level.  <br />  <br />

<strong>Read Access:</strong><br />
The 'Read Access' property can be used to define a general read permission for a Imixs-Event element. The list can contain any application specific role or userid. If a 'read access' is specified the event is only visible to users with the corresponding access role or userid. 
</ul>


 
| Property        | Type   		| Description									 				|
|-----------------|-------------|---------------------------------------------------------------|
| id	          | Integer		| event identifier which is unique to the associated task element   |
| $readaccess	  | String List	| optional access restriction to user- or group IDs 		|
| workflow.result | String 		| application specific processing information 					|
| workflow.public | Boolean		| indicates if the event is a public event (true) or hidden for backend processing only (false)		|
| workflow.public_actors  | String List	| an optional list of public actors 	|





 
## ACL Properties
The ACL defines the read-, write- and ownership, an actor will be granted for, after processing a WorkItem.

<img src="../images/modelling/bpmn_screen_31.png"/>  

Actors play an essential role within a human-centric workflow system. 
In Imixs-Workflow the actors can be defined statically by adding user- or group IDs, or the ACL can be dynamically computed based on the properties of a process instance. 
The dynamic mapping of actors to a workitem property is defined by the [process properties](./main_editor.html) of the workflow model. 

<strong>Note:</strong> If the ACL setting is defined on Event level, the ACL settings on the Task Level will be overwritten.


### Owner, Read and Write Access:

The ACL of a WorkItem is defined in three different layers.  

* **The 'Owner'** describes the users assigned to the process instance. This setting defines the users task list.  

* **The 'Read Access'** restricts the general access to a process instance. Only users which are assigned to the Read Access can access the process instance. If no read access is defined at all, the process instance is not read restricted.
 
* **The 'Write Access'** restricts the write access to a process instance.  The Write Access depends on the 
 AccessLevel a user is granted for. If a user is assigned to the role _org.imixs.ACCESSLEVEL.AUTHORACCESS_ and is not listed in the Write Access List, the user is not allowed to update the process instance. Find more details in the [security section](../engine/acl.html). 
 

The ACL can be defined either in a static or by a mapping between the properties of the process instance to the ACL definition.  




### The Static ACL List:

User- or Group IDs added to the ACL have to match the login name and role definitions of the workflow application. Imixs-Workflow defines the following functional roles which can be added to a static ACL definition:

* org.imixs.ACCESSLEVEL.READACCESS
* org.imixs.ACCESSLEVEL.AUTHORACCESS
* org.imixs.ACCESSLEVEL.EDITORACCESS
* org.imixs.ACCESSLEVEL.MANAGERACCESS

### The ACL Mapping:

The _ACL Mapping_ is used to  map a property of the  process instance to the ACL. The mapping list can be defined by the <a href="./main_editor.html">Process Property Editor</a>. 

See also the section [security settings](../engine/acl.html) for more details about access control of Imixs-Worklow.  
 
 
 
| Property     		| Type   		| Description									 				|
|-------------------|---------------|---------------------------------------------------------------|
| acl.update 		| boolean 		| indicates an ACL update on the event level						|
| acl.owner_list		| String List 	| a static owner list										|
| acl.owner_list_mapping		| String List 	| defines the mapping of a workitem property to the owner list	|
| acl.readaccess_list	| String List 	| the a static read access list										|
| acl.readaccess_list_mapping	| String List 	| defines the mapping of a workitem property to the read access list	|
| acl.writeaccess_list	| String List 	| the a static write access list										|
| acl.writeaccess_list_mapping	| String List 	| defines the mapping of a workitem property to the write access list	|


  

## History Properties
This history property defines information added by the [HistoryPlugin](../engine/plugins/historyplugin.html) during the processing phase of the imixs-workflow engine. 

<img src="../images/modelling/bpmn_screen_22.png"/>  

For each event processed by the Imixs-Workflow engine a new history entry will be added into the WorkItem. The history is a user-friendly process documentation like in the following example:

	02.10.2006 13:36:47 : Document saved by Tom.
	02.10.2006 13:46:37 : Document assigned by Mark.
	02.10.2006 13:36:47 : Document saved by Anna.

A history entry support the [Text Replacer feature](./textreplacement.html).

    Document saved by <itemvalue>namcurrenteditor</itemvalue>



 
| Property        | Type   		| Description									 				|
|-----------------|-------------|---------------------------------------------------------------|
| history.message | String		| History message definition											|


## Mail Properties

The property section 'Mail' defines information for mail messaging during a process step.

<img src="../images/modelling/bpmn_screen_23.png"/>  

A mail message is defined by the mail subject and mail body. Both fields support the [Text Replacer feature](./textreplacement.html). A mail message can define different recipients in the sections 'To', 'CC' and 'BCC'. See the section [Process Property Editor](./main_editor.html) for more information how to define Actors in an Imixs BPMN model. 

A Mail message can be send by the [Imxis-Mail Plugin](../engine/plugins/mailplugin.html)



 
| Property        			| Type   		| Description									 				|
|---------------------------|---------------|---------------------------------------------------------------|
| mail.subject    			| String		| the message subject											|
| mail.body		  			| String		| the message body												|
| mail.to\_list				| String List 	| a static recipient list												|
| mail.to\_list\_mapping	| String List 	| defines the mapping of a workitem property to the recipient list		|
| mail.cc\_list				| String List 	| a static CC recipient list											|
| mail.cc\_list\_mapping	| String List 	| defines the mapping of a workitem property to the CC recipient list	|
| mail.bcc\_list			| String List 	| a static BCC recipient list											|
| mail.bcc\_list\_mapping	| String List 	| defines the mapping of a workitem property to the BCC recipient list	|




## Rule Properties

In the section 'Rule' you can define business rules to be evaluated during the processing life cycle. 

<img src="../images/modelling/bpmn_screen_24.png"/>

A business rule can be written in different script languages. See the section [Rule Plugin](../engine/plugins/ruleplugin.html) for further information how a business rule is defined.
 
| Property        			| Type   		| Description									 				|
|---------------------------|---------------|---------------------------------------------------------------|
| rule.engine    			| String		| rule engine (default is JavaScript)							|
| rule.definition  			| String		| rule definition												|


## Report Properties

The 'Report' section describes a report definition to be executed the event.

<img src="../images/modelling/bpmn_screen_25.png"/>  

See the section [Rule Plugin](../restapi/reportservice.html) for further information about reports.



| Property      | Type   		| Description									 				|
|---------------|---------------|---------------------------------------------------------------|
| report.name   | String		| report name													|
| report.path	| String		| optional file path											|
| report.options| String List 	| optional param list list										|
| report.target	| String		| traget definition												|



## Version Properties

The section 'Version' defines if a new version of a process instance should be created by the event. Versions are used to archive a process instance or to create a copy of a workitem.   

<img src="../images/modelling/bpmn_screen_26.png"/>

See the section [Version Plugin](../engine/plugins/versionplugin.html) for further information about reports.



| Property      | Type   		| Description									 				|
|---------------|---------------|---------------------------------------------------------------|
| version.mode  | String		| mode option (1=create new, 2=convert to master)				|
| version.event | Integer		| event id														|





## Timer Properties

Events can be triggered by user interaction or based on a timer event. The section 'timer' allows the definition of a timer event. 

<img src="../images/modelling/bpmn_screen_27.png"/>

See the section [Workflow Scheduler](../engine/scheduler.html) for further information.
  
  
| Property      				| Type   	| Description									 				|
|-------------------------------|-----------|---------------------------------------------------------------|
| timer.active  				| Boolean	| true if the event is a timer event							|
| timer.selection 				| String	| optional view selection (lucene query)						|
| timer.delay					| Integer	| delay															|
| timer.delay\_unit 			| String	| delay unit (0=seconds, 1=minutes, 2=hours, 3=days, 4=working days)	|
| timer.delay\_base 			| String	| delay base (1=last event, 2=last modify, 3=creation date, 4=workitem property)|	
| timer.delay\_base\_property 	| String	| name of workitem base property								|

  