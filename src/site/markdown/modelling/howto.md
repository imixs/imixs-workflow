#How to Create a Workflow Model?
The Imixs-BPMN Plugin is based on the BPMN 2.0 standard. This means every BPMN 2.0 model can be used to be extended with workflow elements provided by the Imixs-BPMN Plugin. A BPMN model containing Imixs-Workflow elements can be executed within the Imixs-Workflow Engine.

An Imixs-Workflow model can contain the elements "Workflow Task" and "Workflow Event". The Workflow-Task element is used to describe a state in a process. The "Workflow Event" describes the  transition from one state to another.

<img src="../images/modelling/example_01.png"/>

This example contains two workflow states: 'New Ticket' and 'Open'. The workflow event 'Submit' changes the state of the process instance form 'New Ticket' to 'Open'.

A 'Workflow Event' is an instance of a BPMN Intermediate Catch Event. A Catch Event should always have an outgoing sequence flow. The outgoing sequence flow of a workflow event points to the Workflow Task assigned to a process instance after the event was processed. 

## Event Gateways
If a Workflow Task have more than one Workflow Event, the BPMN element "Event Gateway" can be used to model alternative process flows: 

<img src="../images/modelling/example_02.png"/>

A BPMN "Event Gateway" is an exclusive gateway and should always have the gateway direction 'Diverging'. This means that the Gateway has only one incoming sequence flow but can have one or many outgoing sequence flows. The gateway direction can be changed in Eclipse-BPMN after the feature 'Show Advanced Property Tabs' is activated in the general workspace preferences. 



##Loop Events

In some cases a Workflow Event is used to update a process instance without changing the workflow state. For example if a user added new information to a workitem the status of the process instance did not change: 

<img src="../images/modelling/example_03.png"/>

In this case the Workflow Event can have an outgoing sequence flow pointing to the current workflow task. This is called a Loop Event. A Loop event can also be modeled using an Event Gateway:

<img src="../images/modelling/example_04.png"/>

In this example the Loop Event has one incoming and one outgoing sequence flow.  


## Follow-Up Events

A Workflow Event can also point to another Workflow Event. This is called a 'Follow-Up Event'. 

<img src="../images/modelling/example_05.png"/>

In this example the event 'Submit' triggers the follow-up event 'Send E-Mail' which finally completes the transition into the new process state 'Open'.

Follow-Up Events are typically used in more complex scenarios where additional business rules need to be evaluated:

<img src="../images/modelling/example_06.png"/>

In this example a business rule is applied after the new order was accepted. In case the amount is below 100,- EUR the order is processed by the sales team. In case the amount is greater than 100,- EUR the order will be forwarded to the management by the follow-up event 'forward'. The follow-up event can be used to change the properties of the workitem like ACL settings or additional notifications. A follow-up event is typically not visible. See the [section Event Properties](./activities.html) for further details. 
 
 
## Conditional Events

A conditional event is used to evaluate the output of an event during the processing life-cycle. A conditional-event can be placed before a _ExclusiveGateway_, an _InclusiveGateway_ or an _EventBasedGateway_. Each output of the event must define a boolean rule expression.

<img src="../images/modelling/example_08.png"/>
 
In this example two conditions are added to the ExclusiveGateway output evaluating the attribute "_budget" to continue with either 'Task 2' or 'Task 3'. 
A boolean expression can look like the following example, which is evaluating the attribute '_budget':

    (workitem._budget && workitem._budget[0]>100)

**Note:** To add conditional sequence flows the full BPMN profile must be activated in the Imixs-BPMN Modeler. 

A conditional event can define conditions for either a Task element or an Event element. See the next example:
 
<img src="../images/modelling/example_09.png"/>
 
The second condition defines a Follow-Up Event in case the '_budget' is <= 100:     

    (workitem._budget && workitem._budget[0]<=100) 

The script language for the boolean expression is 'JavaScript'. See the [RulePlugin](../engine/plugins/ruleplugin.html) for further details about business rules in Imixs-Workflow. 



## Split Events

A _split-event_ is used to create a new version of the current process instance. 
A _split-event_ is always followed by a _Parallel Gateway_. This is also called a  _parallel-workflow_. 

<img src="../images/modelling/example_11.png"/>


Imixs-Workflow evaluates the conditions assigned to the outcome of the _Parallel Gateway_. The conditions are either evaluated to the boolean value _true_ or _false_. 
If the condition evaluates to '_true_', this outcome is followed by the current process instance (Source).
If the condition evaluates to '_false_', then a new version of the current process instance is created.  

 
| Condition 	| Type              | Description                               						|
|:-------------:|:-----------------:|-------------------------------------------------------------------|
|true           | Source            | describes the outcome for the  current process instance.			|
|false          | Version           | triggers the creation of a new version. 							|


**Note:** The outcome path for a new version must be followed by an Event element! 
 
As a Split Event is creating a new independent version of the current process instance, a join is typically not modeled. This means that the current process instance will not wait for all incoming flows of parallel versions. This is the default behavior in Imixs-Workflow. Split-Events are typically used to archive or end a certain state of processing.

The script language for the boolean expression is 'JavaScript'. See the [RulePlugin](../engine/plugins/ruleplugin.html) for further details about business rules in Imixs-Workflow. See the section [WorkflowKernel](../core/workflowkernel.html#Split_Events) for further information about how Split Events are handled internally. 

## Link Events

In a complex process model, it can be helpful to avoid too many overlapping sequence flows. Therefore the BPMN Link Events can be used. The link event describes a connection between two elements. 

<img src="../images/modelling/example_07.png"/>

In a Imixs-Workflow model a Link Event can be modeled using one intermediate "Throw Event" and one intermediate "Catch Event" from the event type 'Link'. Both link events should have the same name to indicate the connection between them.



 