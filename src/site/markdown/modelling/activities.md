#The Workflow Event Properties
When a Imixs BPMN Event Element is selected in the Drawing Canvas, different settings can be
 configured from the tabbed property sheets displayed in the Property View.
 
<img src="../images/modelling/bpmn_screen_05.png"/>

The property settings are grouped into different sections.
 
##General Properties
The property tab 'General' defines basic informations for an Imixs-BPMN Event element. 

<img src="../images/modelling/bpmn_screen_19.png"  />

###Name 

The name of the Event element is used to identify the processing action. The name typical describes the activity triggered by the user to process a WorkItem. E.g. "submit", "approve" "close". The event name can be used by applications to display event based worflow actions. 

<center><img src="../images/modelling/bpmn_screen_30.png" width="200px" /></center>


###Documentation

The documentation of the Event element can be used to provide the user with additional information about the event. Typically the documentation provides information how the event will change the current status of the process instance. The documentation can be used  by applications to display event based information. 
 
 
 
##Workflow Properties
The Tab 'Workflow' contains Processing Information for an Event element. These information
are evaluated by the Worklfow Engine while a WorkItem is processed 
 
<img src="../images/modelling/bpmn_screen_20.png" width="600px" />  
  
 
###ID 
Every Workflow Event assigned to a Workflow Task has an unambiguously ID. The Event ID is used  by the Workfow Engine to identify the Event.
 
 
###Result
The 'Result' defines application specific processing information evaluated by the Workflow Engine. These information are evaluated by plug-ins and applications. See the section [ResultPlugin](../engine/plugins/resultplugin.html) for further information.

 
###Visiblity
The section 'visibility' defines whether an event is visible to a workflow user or not. The visibility is separated into three levels of visibility. 

**Public Event:** <br />
Per default each Imixs-Event is public and visible to the workflow user. If the property 'Public Event' is set to 'No', the event will not be displayed to the workflow user.

**Restrict to:**<br />
A 'Public Event' can be restricted to a specific user group defined by the state of a workflow instance. The restriction is based on the 'Actor Properties' which can be defined by the [process properties](./main_editor.html) of a workflow model. If a restriction is set for a Imixs-Event element, the event is only visible to the workflow user if he is listed in one of the Actor Properties. With the restriction feature, the visibility of a workflow event can be configured on a very fine grained user level.    

**Read Access:**<br />
The 'Read Access' list can be used to define a general read permission for a Imixs-Event element. The list can contain any application specific role or userid. If a 'read access' is specified only workflow users with the corresponding access role or userid are able to trigger this event. 


  
##ACL Properties
The ACL Tab defines the Access Control List for a workitem processed by the Imixs-Workflow engine.

<img src="../images/modelling/bpmn_screen_21.png"/>  

The ACL defines the read- and write access a user will be granted for, after a WorkItem was successful  processed. This is one of the most important features of the Imixs Workflow System.
The ACL can be defined in a dynamic or a static way. To activate the feature the option 'Update ACL' has to be checked.

###Owner, Read and Write Access
The ACL of a WorkItem is defined in three different layers.  The 'Owner' defines the users assigned to the WorkItem for the next Workflow Task. This setting typically 
 adds the WorkItem to the users task list.  The 'Read Access' is used to restrict the read access for a WorkItem. Only users which are assigned to the Read Access of a WorkItem can access the workitem from the application. If no Users or Roles are defined in the Read Access not read restrictions are set to a WorkItem.
 
The 'Write Access' restricts the author access for a WorkItem. The Author Access depends on the 
 AccessLevel a user is granted for. If a user is assigned to the role 'org.imixs.ACCESSLEVEL.AUTHORACCESS' and is not listed in the Write Access List of a WorkItem, the user is not allowed to change or process the WorkItem. 
 
See the section [security settings](../engine/acl.html) for more details.    

###Dynamic ACL
The dynamic ACL settings are used to compute the access list based on the definition of 'Actors'.  Actors play an essential role in a user-centric workflow system. The actors are computed dynamically based on the properties of a WorkItem. See the section [Process Property Editor](./main_editor.html) for more  information how to define Actors in an Imixs BPMN model. 

###Static ACL
It is also possible to define the ACL in a static way. The UserIDs and Role names have to match the  login name and role definitions of the workflow application. The following Imixs standard roles can be used here:


 * org.imixs.ACCESSLEVEL.READACCESS
 
 * org.imixs.ACCESSLEVEL.AUTHORACCESS
 
 * org.imixs.ACCESSLEVEL.EDITORACCESS
 
 * org.imixs.ACCESSLEVEL.MANAGERACCESS


##History Properties
This property defines the history information added to a WorkItem during a process step. 

<img src="../images/modelling/bpmn_screen_22.png"/>  

For each Event processed by the Imixs-Workflow engine a new History entry will be added into the WorkItem. The history results in a user-friendly process documentation like in the following example:

	02.10.2006 13:36:47 : Document saved by Ralph Soika/IMIXS .
	02.10.2006 13:46:37 : Document assigened by Ralph Soika/IMIXS .
	02.10.2006 13:36:47 : Document saved by Ralph Soika/IMIXS.


##Mail Properties
The property section 'Mail' defines information for mail messaging during a process step.

<img src="../images/modelling/bpmn_screen_23.png"/>  

A mail message is defined by the mail subject and mail body. Both fields support the [Text Replacer feature](./textreplacement.html). A mail message can define different recipients in the sections 'To', 'CC' and 'BCC'. See the section [Process Property Editor](./main_editor.html) for more information how to define Actors in an Imixs BPMN model. 


##Rule Properties
<img src="../images/modelling/bpmn_screen_24.png"/>


##Report Properties
<img src="../images/modelling/bpmn_screen_25.png"/>  


##Version Properties
<img src="../images/modelling/bpmn_screen_26.png"/>

##Timer Properties

<img src="../images/modelling/bpmn_screen_27.png"/>


  