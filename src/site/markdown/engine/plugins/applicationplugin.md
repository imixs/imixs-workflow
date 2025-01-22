# ApplicationPlugin

The ApplicationPlugin updates application specific properties for the current WorkItem.

    org.imixs.workflow.engine.plugins.ApplicationPlugin

The plugin is responsible to generate the workflow summary and abstract items `$workflowsummary` and `$workflowabstract`. These settings can be modeled using the Imixs Modeler on the Application Property Tab off the corresponding ProcessEntity.

<img src="../../images/modelling/bpmn_screen_38.png"/>

See also the section [Text Replacement](../../modelling/textreplacement.html) for more details how to format item values.

The following items are controlled by the ApplicationPlugin

- $WorkflowStatus - evaluated status of new process state
- $WorkflowGroup - name of the workflow group from the current process
- $WorkflowAbstract - Abstract text (provided by the processEntity property 'txtworkflowabstract')
- $WorkflowSummary - Summary (provided by the processEntity property 'txtworkflowsummary')
- txtWorkflowEditorID - optional EditorID to be used by an application (provided by the activityEntity property 'txtEditorID')
- txtWorkflowImageURL - visual image can be displayed by an application (provided by the activityEntity property 'txtImageURL')
