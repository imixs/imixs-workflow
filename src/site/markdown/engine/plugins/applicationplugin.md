# ApplicationPlugin
The ApplicationPlugin updates application specific properties for the current WorkItem.

    org.imixs.workflow.engine.plugins.ApplicationPlugin

  * txtWorkflowEditorID - optional EditorID to be used by an application (provided by the activityEntity property 'txtEditorID')
  * txtWorkflowImageURL - visual image can be displayed by an application (provided by the activityEntity property 'txtImageURL')
  * txtWorkflowStatus - evaluated status of new process state
  * txtWorkflowGroup - name of the workflow group from the current process
  * txtWorkflowAbstract - Abstract text  (provided by the processEntity property 'txtworkflowabstract')
  * txtWorkflowSummary - Summary (provided by the processEntity property 'txtworkflowsummary')
 
All these settings can be modeled using the Imixs Modeler on the Application Property Tab off the corresponding ProcessEntity.  The  corresponding ProcessEntity is defined by the ActivityEntity attribute 'numNextID'  
 
 