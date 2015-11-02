#The Business Process
 
Before a new process instance can be started by the Imixs-Workflow engine, you need to define the corresponding business process model (workflow model). The process model describes the process flow with its different states and transitions. The model consists of a set 
of process entities (Tasks) and activity entities (Events). A process entity defines the state of a process instance controlled by the workflow engine. The activity entity defines the event or condition to change a process instance from one state to another. This is also called a transition.
 
The process activity can be triggered either by an actor or an event. When the workflow engine executes a workitem, different actions defined by an activity can be triggered.
 
<img src="../images/modelling/bpmn_screen_00.png"/>
 
Imixs-Workflow provides an eclipse based graphical editor to manage workflow models based on the BPMN 2.0 standard. You can create a model from the eclipse IDE and synchronize your model with the Imixs Workflow engine. For more details about how to create a workflow mode Read the [Modelling Section](../modelling/index.html). 
 
 
## Creating a process instance
Each entity stored in a workflow model has a unique ID. The process-id and the activity-id.  Before a workitem can be processed by the workflow engine a workitem need to be bound to a process entity and assigned to a valid activity entity form the model. This can be done by setting the items '$ProcessID' and '$ActivityID': 

    .....
    workitem.replaceItemValue("$processID",20);
    workitem.replaceItemValue("$activityID",20);
 
After the workitem was processed the first time, the workitem is called a 'process instance' as it is an instance of the process entity defined by the process model. After the workflow engine has processed the workitem the next process entity will be automatically assigned to the workitem. In this example the workitem which is assigned to the process-id=20 and the activity-id=20 will be assigned to the process-id=30 when the process-step is completed.
 