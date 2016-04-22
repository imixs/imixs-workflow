#Analysis Plugin 
The org.imixs.workflow.plugins.AnalysisPlugin can be used to measure the time different phases of any workflow task. The plugin can be configured by the activity result :


    <item name='measurepoint' type='start'>p1</item> 

defines a start point named 'p1'

    <item name='measurepoint' type='stop'>p1</item> 

defines an end point named 'p1'. The results will be stored into the txtWorkflowActivityLog (comments). In addition the AnalysisPlugin will create the following fields:

  * datMeasurePointStart_[NAME] : contains the start time points (lists latest entry on top!)
  * datMeasurePointEnd_[NAME] : contains the end time points (list)
  * numMeasurePoint_[NAME]: contains the total time in msec

The [NAME] suffix will be replaced with the name of the measuring point. So it is possible  
 to define several measuring points in one workflow.
  
  