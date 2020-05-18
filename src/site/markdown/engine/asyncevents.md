# The Async Events

An *Async Event* is executed asynchronous after a processing life-cycle. In difference to *Follow-Up Events* the process instance will be persisted in a new status before a async event is executed. 

Async Events are modeled as BPMN Boundary Events. 

<img src="../images/modelling/example_13.png"/>  

The Async Event is identified by the following task attributes:

	boundaryEvent.timerEventDefinition.timeDuration=1000
	boundaryEvent.targetEvent=30

The *timerEventDefinition* is optional.

In BPMN2 the definition of a Async Event looks like this:

	<bpmn2:boundaryEvent id="BoundaryEvent_1" name="" attachedToRef="Task_1">
	  <bpmn2:outgoing>SequenceFlow_8</bpmn2:outgoing>
	  <bpmn2:timerEventDefinition id="TimerEventDefinition_2">
	    <bpmn2:timeDuration xsi:type="bpmn2:tFormalExpression" id="FormalExpression_4">1000</bpmn2:timeDuration>
	  </bpmn2:timerEventDefinition>
	</bpmn2:boundaryEvent>
	<bpmn2:sequenceFlow id="SequenceFlow_8" sourceRef="BoundaryEvent_1" targetRef="IntermediateCatchEvent_3"/>
    
## The AsyncEventProcessor

The *AsyncEventProcessor* is a managed execution service observing the life cycle of a process instance. If a process instance reaches a task with a Async Event a asynchronous log event will be created and the targetEvent will be executed after the processing life-cycle is completed. 

### Configuration

The AsyncEventProcessor runs on a scheduled base defined by the following environment settings:

    ASYNCEVENT_PROCESSOR_INTERVAL - timeout period in milliseconds
    ASYNCEVENT_PROCESSOR_INITIALDELAY - To enable the Processor
    ASYNCEVENT_PROCESSOR_ENABLED - must be set to true (default=false).
    ASYNCEVENT_PROCESSOR_DEADLOCK - deadlock in milliseconds (default 1 minute)

To prevent concurrent processes to handle the same workitems the batch process uses a Optimistic lock strategy. The expiration time on the lock can be set by the environment variable ASYNCEVENT_PROCESSOR_DEADLOCK.

