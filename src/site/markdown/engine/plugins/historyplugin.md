#History Plugin
The HistoryPlugin generates a workflow history for each process instance depending on the configuration in the workflow model. 

_Plugin Class Name:_

    org.imixs.workflow.plugins.HistoryPlugin

For each workflow step the HistoryPlugin generates a separate entry which is added into the history list (txtworkflowhistory) of the WorkItem. Each history entry provides the following entries:
 
  * date of creation (Date)
  * comment (String)
  * userID (String)
  

The number of entries for the history list can be restricted to a maximum number of entries by adding the attribute "numworkflowhistoryLength" into the workitem. The Attribute indicates the maximum number of entries. If lower 0 no limit is set.

The History entries can be configured in the workflow model using the [Imixs-BPMN Modeler](../../modelling/index.html). The following example shows how to output the workflow history list using facelets:
 
	<ui:fragment rendered="#{!empty workflowController.workitem.item['txtworkflowhistory']}">
		<h:dataTable var="log"
			value="#{workflowController.workitem.itemListArray['txtworkflowhistory']}">
			<h:column>
				<h:outputText value="#{log[0]}">
					<f:convertDateTime timeZone="#{message.timeZone}" type="both"
						pattern="#{message.dateTimePattern}" /> 
				</h:outputText>
			</h:column>
			<h:column>
				<h:outputText value="#{log[1]}" />
			</h:column>
		</h:dataTable>
	</ui:fragment>

