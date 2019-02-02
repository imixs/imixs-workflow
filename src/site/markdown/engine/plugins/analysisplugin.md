# Analysis Plugin 

To analyze different phases of a workflow process the ImixsAnalysis PlugIn can be added into a process model:


    org.imixs.workflow.plugins.AnalysisPlugin
    
The plugin is used to measure the processing time of the different phases during the life cycle of a process instance. Several individual measuring points can be configured in the workflow result of a BPMN event:


###Example:

    <item name='measurepoint' type='start'>p1</item> 

defines a start point named 'p1'

    <item name='measurepoint' type='stop'>p1</item> 

defines an end point named 'p1'. 

## Data Analysis 
The result of a measuring point will be stored in the following workitem properties:

  * datMeasurePointStart_[NAME] : contains the start time points (lists latest entry on top!)
  * datMeasurePointEnd_[NAME] : contains the end time points (list)
  * numMeasurePoint_[NAME]: contains the total time in seconds

The [NAME] suffix will be replaced with the name of the measuring point. It is possible  
 to define several measuring points in one process model.
  
The following diagram illustrates two measuring points in the ticket-workflow:


<img src="../../images/analysisplugin.png"/>  


The first measuring point 'P1' will measure the total processing time for a ticket. The second measuring point 'P2' will measure the duration of the processing time (in seconds) for the solving.


All measuring points will be also logged into the txtWorkflowActivityLog in the comment section.



