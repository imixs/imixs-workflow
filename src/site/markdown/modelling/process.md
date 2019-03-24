# The Imixs-BPMN Task Element
When a Imixs BPMN Task Element is selected in the Drawing Canvas, different settings can be
configured from the tabbed property sheets displayed in the Property View.
 
<img src="../images/modelling/bpmn_screen_04.png"/>

The property settings are grouped into different sections.
 
## General Properties

The property tabs 'General' and 'Task' defines basic attributes of an Imixs BPMN Task element. 

<img src="../images/modelling/bpmn_screen_16.png" />


### ID 
Every Task Element has an unambiguously identifier called the _TaskID_. The _TaskID_ is assigned to a running process instance to identify the status of a WorkItem controlled by the Workflow Engine. 


### Name 
The _name_ of the Task element is used as an human readable status identifier.
The _name_  describes the status of a process instance within the process model (e.g. "open" or "approval" "closed").

### Documentation
The documentation of the Task element is used by the Imixs Workflow System to provide the user
with additional information about the task. Typically a documentation can provide information how the task should be performed or which information need to be entered into a application.

The documentation can also be created by assigning a TextAnnotation element to the task. In this case the documentation field of the Task should remain empty. 

<img src="../images/modelling/bpmn_screen_33.png" />
 


 
| Property        | Type   	| Description									 				|
|-----------------|---------|---------------------------------------------------------------|
| id	          | Integer	| unique task identifier within the model        				|
| name            | String 	| name of the task element       								|
| documentation   | String 	| short description				   								|


 
  
## Workflow Properties
The Tab 'Workflow' contains Processing Information of a Task element. These information will be
updated after a WorkItem was processed by the Workflow Engine.
 
<img src="../images/modelling/bpmn_screen_17.png"/>


 

### Workflow Summary 
The Workflow Summary is a short description describing the status of a WorkItem. A Workflow Summary can be a text message containing also processing information. The following example generates the Workflow Summary out from the WorkItem property 'ticket_number'.
 
    Open Ticket: <itemValue>ticket_number<itemValue>
 
 
### Workflow Abstract 
The Workflow Abstract can be used as a long description describing the status of a WorkItem. 
The Abstract is similar to the Workflow Summary and can also be dynamically computed from WorkItem  properties. The following example shows how the abstract can be computed from WorkItem properties using the HTML markup language:
 
	 Ticket opened by <itemValue>namcreator<itemValue>
	 <br />
	 Ticket No.: <itemValue>ticket_number</itemValue>
	 <hr />
 
See the [Text Replacement feature](./textreplacement.html) how to insert WorkItem Values into a message text. 
 
 
  

 
| Property        | Type   	| Description									 				|
|-----------------|---------|---------------------------------------------------------------|
| workflow.summary 		  | String 	| The definition of a workflow summary							|
| workflow.abstract	      | String 	| The definition of a workflow abstract						|

 
 
 
 
## Application Properties
The Property Tab 'Application' defines information used to control the behavior of a WorkItem in the workflow application. 

<img src="../images/modelling/bpmn_screen_18.png"/>


### Input Form 
The 'Input Form' can be used to control how a WorkItem in a specific status  is displayed or how it can be edited inside the workflow application.  This allows the model to control the behavior of the application in a specific way.

 
### Status Icon 

A 'Status Icon' can be a Image URL to visualize the current status of a WorkItem.
 
### WorkItem Type 
The 'WorkItem Type' is a category assigned to the WorkItem by the Workflow Engine when the WorkItem was processed. 
 
 


 
| Property     		| Type   	| Description									 				|
|-------------------|-----------|---------------------------------------------------------------|
| application.editor 			| String 	| Form editor id to be assigned to a process instance				|
| application.icon				| String 	| Status icon	to be assigned to a process instance				|
| applicaton.type		| String 	| type definition to be assigned to a process instance			|

  
  
 
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
| acl.update 		| boolean 		| indicates an ACL update on the task level						|
| acl.owner_list		| String List 	| the a static owner list										|
| acl.owner_list_mapping		| String List 	| defines the mapping of a workitem property to the owner list	|
| acl.readaccess_list	| String List 	| the a static read access list										|
| acl.readaccess_list_mapping	| String List 	| defines the mapping of a workitem property to the read access list	|
| acl.writeaccess_list	| String List 	| the a static write access list										|
| acl.writeaccess_list_mapping	| String List 	| defines the mapping of a workitem property to the write access list	|


 