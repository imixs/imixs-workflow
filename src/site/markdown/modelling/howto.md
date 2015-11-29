#How to Create a Workflow Model?
The Imixs-BPMN Plugin is based on the BPMN 2.0 standard. This means every BPMN 2.0 model can be used to be extended with workflow elements provided by the Imixs-BPMN Plugin. A BPMN model containing Imixs-Workflow elements can be executed within the Imixs-Worklfow Engine.

An Imixs-Workflow Model can contain the elements "Workflow Task" and "Workflow Event". The Workflow-Task element is used to describe a state in a process. The "Workflow Event" describes the  transition from one state to another.

<img src="../images/modelling/example_01.png"/>

This example contains two workflow states: 'New Ticket' and 'Open'. The Workflow Event 'Submit' changes the state of the process instance form 'New Ticket' to 'Open'.

A 'Workflow Event' is an instance of a BPMN Intermediate Catch Event. A Catch Event should always have an outgoing sequence flow. The outgoing sequence flow of a Workflow Event point to the Workflow Task assigned to a process instance after the Event was processed. 

##Event Gateways
If a Workflow Task have more than one Workflow Event, the BPMN element "Event Gateway" can be used to model alternative process flows: 

<img src="../images/modelling/example_02.png"/>

A BPMN "Event Gateway" is an exclusive gateway and should always have the gateway direction 'Diverging'. This means that the Gateway has only one incoming sequence flow but can have one ore many outgoing sequence flows. The Gateway direction can be changed in Eclipse-BPMN after the feature 'Show Advanced Property Tabs' is activated in the general workspace preferences. 


##Loop Events

In some cases a Workflow Event is used to update a process instance without changing the workflow state. For example if a user added new information to a workitem the status of the process instance did not change: 

<img src="../images/modelling/example_03.png"/>

In this case the Workflow Event can have an outgoing sequence flow pointing to the current workflow task. This is called a Loop Event. A Loop event can also be modeled using an Event Gateway:

<img src="../images/modelling/example_04.png"/>

In this example the Loop Event has one incoming and one outgoing sequence flow.  


##Follow-Up Events

A Workflow Event can also point to another Workflow Event. This is called a 'Follow-Up Event'. 

<img src="../images/modelling/example_05.png"/>

In this example the Event 'Submit' triggers the follow-up event 'Send E-Mail' which finally completes the transition into the new process state 'Open'.

Follow-Up Events are typically used in more complex scenarios where additional business rules need to be evaluated:

<img src="../images/modelling/example_06.png"/>

In this example a business rule is applied after the new order was accepted. In case the amount is below 100,- EUR the order is processed by the sales team. In case the amount is greater than 100,- EUR the order will be forwarded to the management by the follow-up event 'forward'. The follow-up event can be used to change the properties of the workitem like ACL settings or additional notifications. A follow-up event is typically not visible. See the [section Event Properties](./activities.html) for further details. 
 

##Link Events

In a complex process model, it can be helpful to avoid too many overlapping sequence flows. Therefor the BPMN Link Events can be used. The link event describes a connection between two elements. 

<img src="../images/modelling/example_07.png"/>

In a Imixs-Workflow model a Link Event can be modeled using one intermediate "Throw Event" and one intermediate "Catch Event" from the event type 'Link'. Both link events should have the same name to indicate the connection between them.



 