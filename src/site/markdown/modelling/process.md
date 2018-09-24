# The Imixs-BPMN Task Element
When a Imixs BPMN Task Element is selected in the Drawing Canvas, different settings can be
configured from the tabbed property sheets displayed in the Property View.
 
<img src="../images/modelling/bpmn_screen_04.png"/>

The property settings are grouped into different sections.
 
## General Properties
The property tab 'General' defines basic information for an Imixs BPMN Task element. 

<img src="../images/modelling/bpmn_screen_16.png" />

### Name 
The name of the Task element is used to identify the status of a process instance. The Name should describe the status of a workitem in a bussiness model. e.g. "open" or "in approval" "closed".

### Documentation
The documentation of the Task element is used by the Imixs Workflow System to provide the user
with additional information about the task. Typically a documentation can provide information how the task should be performed or which information need to be entered into a application.

The documentation can also be created by assigning a TextAnnotation element to the task. In this case the documentation field of the Task should remain empty. 

<img src="../images/modelling/bpmn_screen_33.png" />
 
  
##Workflow Properties
The Tab 'Workflow' contains Processing Information of a Task element. These information will be
updated after a WorkItem was processed by the Workflow Engine.
 
<img src="../images/modelling/bpmn_screen_17.png"/>


###ID 
Every Workflow Task has an unambiguously ID. The Process ID is assigned by the Workflow Engine
after a WorkItem was processed. The Process ID is used to identify the status of a WorkItem controlled by the Workflow Engine.
 

###Workflow Summary 
The Workflow Summary is a short description describing the status of a WorkItem. A Workflow Summary can be a text message containing also processing information. The following example generates the Workflow Summary out from the WorkItem property 'ticket_number'.
 
    Open Ticket: <itemValue>ticket_number<itemValue>
 
 
###Workflow Abstract 
The Workflow Abstract can be used as a long description describing the status of a WorkItem. 
The Abstract is similar to the Workflow Summary and can also be dynamically computed from WorkItem  properties. The following example shows how the abstract can be computed from WorkItem properties using the HTML markup language:
 
	 Ticket opened by <itemValue>namcreator<itemValue>
	 <br />
	 Ticket No.: <itemValue>ticket_number</itemValue>
	 <hr />
 
See the [Text Replacement feature](./textreplacement.html) how to insert WorkItem Values into a message text. 
 
##Application Properties
The Property Tab 'Application' defines information used to control the behavior of a WorkItem in the workflow application. 

<img src="../images/modelling/bpmn_screen_18.png"/>


###Input Form 
The 'Input Form' can be used to control how a WorkItem in a specific status  is displayed or how it can be edited inside the workflow application.  This allows the model to control the behavior of the application in a specific way.

 
###Status Icon 

A 'Status Icon' can be a Image URL to visualize the current status of a WorkItem.
 
###WorkItem Type 
The 'WorkItem Type' is a category assigned to the WorkItem by the Workflow Engine when the WorkItem was processed. 
 
  
 
##ACL Properties
The ACL Tab is used to define the Access Control List (ACL) for a workitem which is processed by the Imixs-Workflow engine.

<img src="../images/modelling/bpmn_screen_31.png"/>  

The ACL defines the read- and write access a user will be granted for, after a WorkItem was successful  processed. This is one of the most important features of the Imixs Workflow System. Also the ownership can be defined by the ACL properties. The ACL can be set based on the 'Actor Properties' which are defined by the [process properties](./main_editor.html) of the workflow model. To activate the feature the option 'Update ACL' has to be checked.

<strong>Note:</strong> If the ACL setting is defined on Event level, the ACL settings on the Task Level are overwritten.


###Owner, Read and Write Access:
The ACL of a WorkItem is defined in three different layers.  The 'Owner' defines the users assigned to the WorkItem for the next Workflow Task. This setting typically adds the WorkItem to the users task list.  The 'Read Access' is used to restrict the read access for a WorkItem. Only users which are assigned to the Read Access of a WorkItem can access the workitem from the application. If no Users or Roles are defined in the Read Access not read restrictions are set to a WorkItem.
 
The 'Write Access' restricts the author access for a WorkItem. The Author Access depends on the 
 AccessLevel a user is granted for. If a user is assigned to the role 'org.imixs.ACCESSLEVEL.AUTHORACCESS' and is not listed in the Write Access List of a WorkItem, the user is not allowed to change or process the WorkItem. 
 
  

<ul>

<strong>Dynamic ACL:</strong><br />
The dynamic ACL settings are used to compute the access list based on the definition of 'Actors'.  Actors play an essential role in a user-centric workflow system. The actors are computed dynamically based on the properties of a WorkItem. See the section [Process Property Editor](./main_editor.html) for more  information how to define Actors in an Imixs BPMN model. <br /><br />

<strong>Static ACL:</strong><br />
It is also possible to define the ACL in a static way. The UserIDs and Role names have to match the  login name and role definitions of the workflow application. The following Imixs standard roles can be used here:<br />


 <ul><li> org.imixs.ACCESSLEVEL.READACCESS</li>
 
 <li>org.imixs.ACCESSLEVEL.AUTHORACCESS</li>
 
 <li>org.imixs.ACCESSLEVEL.EDITORACCESS</li>
 
 <li>org.imixs.ACCESSLEVEL.MANAGERACCESS</li>
 </ul>

</ul>


See also the section [security settings](../engine/acl.html) for more details.  
 
 