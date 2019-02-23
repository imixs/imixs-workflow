#The Process Property Editor
When you click on a free area in the Canvas Edior of the Workbench you can edit the main prperties of the BPMN file. If you have activated the 'Full' Profile in the Toolbar Palette you will see the property tab 'Workflow':
 
<img src="../images/modelling/bpmn_screen_14.png"/>  
 
   
##The Imixs Workflow Properties
The prperty tab 'Workflow' of the Process Definition provides different main settings to configure your workflow model:
    
<img src="../images/modelling/bpmn_screen_15.png" /> 
 
###The Model Version
The Model Version is a unique identifier for the model and there included workflow groups.
 
###The Actor Properties
Actors play an essential role in a user-centric workflow system. The actors are users who actively  interact with a workflow application. For example, actors can start a new process instance to create a Workitem or trigger actions on an existing WorkItem. Actors can also be passively involved into a workflow, for instance, as recipients of an e-mail notification. 
 
An actor is defined as an abstract role in a workflow model. The application specific user names (UserIDs) behind the actor are computed dynamically by the Imixs-Workflow engine. The Workflow engine maps the actor to the corresponding property of the WorkItem during runtime.
  
    Delegate | namDelegate
 
The first part is the name of the actor displayed in the property sections of an Imixs BPMN Event element. The last part is the WorkItem property evaluated by the workflow engine.

 
As an alternative Actors can also be defined by a static list of userIds. In this case the last part of the  definition has to be included into curled or squared rackets. The comma separated list of values will be  evaluated by the workflow engine.
 
    Delegate | {user1,user2} 
 

###The Date Properties
Timers perform a similar function as Actors. They are used to map attributes of the WorkItem of  type "Date" in an object of workflow modeling. If your workflow application contains a field named  "datFrom" that defines a specific point of time after which an action should be launched, you can enter "Begin | datFrom" as the Timer. During modeling, you can then work in the activities with the "Begin" time value. This is especially useful for schedule activities.

###The Plugin Properties
In this section, the plug-ins used by the workflow manager as well as their order are defined.  The configuration is normally determined by the workflow system being used. Only plug-ins can be 
configured that are known to the application server being used. If you have questions with respect to the required plug-ins, please contact your system administrator.   

