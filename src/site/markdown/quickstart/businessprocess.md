#The Business Process
 
Before a new process instance can be started by the Imixs-Workflow engine, a business process model (workflow model) need to be defined. The process model describes the process flow with its different states and transitions. The model consists of a set 
of Task elements and Event elements. A task defines the state of a process instance controlled by the workflow engine. The event describes the transition from one state to another. 
 
The Event can be triggered either by an actor or a service. When the workflow engine executes a workitem, different actions defined by an event can be triggered.
 
<img src="../images/modelling/bpmn_screen_00.png"/>
 
Imixs-Workflow provides an eclipse based graphical editor to manage workflow models based on the BPMN 2.0 standard. You can create a model from the eclipse IDE and synchronize your model with the Imixs Workflow engine. For more details about how to create a workflow mode Read the [Modelling Section](../modelling/index.html). 
 
 
## Creating a process instance
Each entity stored in a workflow model has a uniqueID and a taskID.  Before a workitem can be processed by the workflow engine a workitem need to be bound to the model: 

    .....
    ItemCollection workitem=new ItemCollection().model("1.0.0").task(100).event(20);
 
After the workitem was processed the first time, the workitem is called a 'process instance' as it is an instance of the process entity defined by the process model. After the workflow engine has processed the workitem the next task entity will be automatically assigned to the workitem. 
 