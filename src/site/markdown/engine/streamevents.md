# The Stream Events

A stream event is an asynchronous event fired after a processing life-cycle. In difference to followup events a process instance will be persisted in a new status before a stream event is executed. 

Stream Events are modeld as BPMN Bounder Events. 

<img src="../images/modelling/example_13.png"/>  

The Stream Event is identified by the following task attributes:

	boundaryEvent.timerEventDefinition.timeDuration=1000
	boundaryEvent.targetEvent=30

The *timerEventDefinition* is optional.

In BPMN2 the definition of a Stream Event looks like this:

	<bpmn2:boundaryEvent id="BoundaryEvent_1" name="" attachedToRef="Task_1">
	  <bpmn2:outgoing>SequenceFlow_8</bpmn2:outgoing>
	  <bpmn2:timerEventDefinition id="TimerEventDefinition_2">
	    <bpmn2:timeDuration xsi:type="bpmn2:tFormalExpression" id="FormalExpression_4">1000</bpmn2:timeDuration>
	  </bpmn2:timerEventDefinition>
	</bpmn2:boundaryEvent>
	<bpmn2:sequenceFlow id="SequenceFlow_8" sourceRef="BoundaryEvent_1" targetRef="IntermediateCatchEvent_3"/>
    
## The EventStreamProcessor

The EventStreamProcessor is a managed execution service observing the life cycle of a process instance. If a process instance reaches a task with a Stream Event a asynchronous log event will be created and the targetEvent will be executed after the processing life-cycle is completed. 

### Configuration

The StreamEventProcessor runs on a scheduled base defined by the following environment settings:

    STREAMEVENT_PROCESSOR_INTERVAL - timeout period in milliseconds
    STREAMEVENT_PROCESSOR_INITIALDELAY - To enable the batchPorcessor
    STREAMEVENT_PROCESSOR_ENABLED - must be set to true (default=false).
    STREAMEVENT_PROCESSOR_DEADLOCK - deadlock in milliseconds (default 1 minute)

To prevent concurrent processes to handle the same workitems the batch process uses a Optimistic lock strategy. The expiration time on the lock can be set by the environment variable BATCH_PROCESSOR_DEADLOCK.