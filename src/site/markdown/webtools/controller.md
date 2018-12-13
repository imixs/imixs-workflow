# The Imixs-Workflow-Faces Data

The  Imixs-Workflow-Faces Data package contains CDI beans which can be used to control Imxis-Workflow data.

## The DocumentController


The DocumentController is a @ConversationScoped CDI bean to control the life cycle of a ItemCollection in an JSF application without any workflow  functionality. The bean can be used in single page applications, as well for complex page flows. The controller is easy to use and supports bookmarkable  URLs.
 
The DocumentController fires CDI events from the type WorkflowEvent. A CDI  bean can observe these events to participate in the processing life cycle.
 
To load a document the methods load(id) and onLoad() can be used. The method load expects the uniqueId of a document to be loaded. The onLoad() method  extracts the uniqueid from the query parameter 'id'. This is the recommended  way to support bookmarkable URLs. To load a document the onLoad method can be  triggered by an jsf viewAction placed in the header of a JSF page:
  
	<f:metadata>
      <f:viewAction action="... documentController.onLoad()" />
    </f:metadata> }
 
A bookmarkable URL looks like this:

	/myForm.xthml?id=[UNIQUEID] 
 
In combination with the viewAction the DocumentController is automatically  initialized.
 
After a document is loaded, a new conversation is started and the CDI event  WorkflowEvent.DOCUMENT_CHANGED is fired.
 
After a document was saved, the conversation is automatically closed. Stale conversations will automatically timeout with the default session timeout.

After each call of the method save the Post-Redirect-Get is initialized with the default URL from the start of the conversation. This guarantees
 bookmakrable URLs.

  Within a JSF form, the items of a document can be accessed by the getter
  method getDocument().
 
    #{documentController.document.item['$workflowstatus']}
 
 The default type of a entity created with the DataController is 'workitem'. This property can be changed from a client.

 
## The WorkflowController

The WorkflowController is a @ConversationScoped CDI bean to control the processing life cycle of a workitem in JSF an application. The bean can be used in single page applications, as well for complex page flows. The controller is easy to use and supports bookmarkable URLs.

The WorkflowController fires CDI events from the type WorkflowEvent. A CDI bean can observe these events to participate in the processing life cycle.

To load a workitem the methods load(id) and onLoad() can be used. The method load expects the uniqueId of a workItem to be loaded. The onLoad() method extracts the uniqueid from the query parameter 'id'. This is the recommended way to support bookmarkable URLs. To load a workitem the onLoad method can be triggered by an jsf viewAction placed in the header of a JSF page:

	<f:metadata>
      <f:viewAction action="... workflowController.onLoad()" />
    </f:metadata> }
 
A bookmarkable URL looks like this:

	/myForm.xthml?id=[UNIQUEID] 
	
In combination with the viewAction the WorkflowController is automatically initialized. After a workitem is loaded, a new conversation is started and the CDI event WorkflowEvent.WORKITEM_CHANGED is fired.
 
After a workitem was processed, the conversation is automatically closed. Stale conversations will automatically timeout with the default session timeout.

After each call of the method process the Post-Redirect-Get is initialized with the default URL from the start of the conversation. If an alternative action result is provided by the workflow engine, the WorkflowController automatically redirects the user to the new form outcome. This guarantees bookmakrable URLs.

Call the close() method when the workitem data is no longer needed.

Within a JSF form, the items of a workitem can be accessed by the getter method getWorkitem().

	#{workflowController.workitem.item['$workflowstatus']}




## The ViewController
The ViewController controls a collection of workItems. The result of the collection can be controlled by different properties of the ViewController.  The property 'view' defines the view type returned by a method call of getWorkitems. The result of a collection is computed by a ViewAdapter.  IViewAdapter can be adapted by any custom implementation. The ViewController can be be used in ViewScope. Long result lists can be paged using an  internal paginator implementation. The length of a page result is defined by the property 'maxResult'
 

|  Method       |  Type           | Description                               |       
|---------------|-----------------|-------------------------------------------|
|reset ()       | ActionListener  | reset the cached result list and the paginator      |
|refresh()      | ActionListener  | resets the cached result list and set the paginator to 0 |
|doLoadNext()   | ActionListener  | loads the next page                       |
|doLoadPrev()   | ActionListener  | loads the previous page                   |
|getWorkitems() | Getter          | returns the list of workitems in the current page  |
 

##The WorklistController
The WorklistController extends the ViewControler and provides a set of workflow specific sortable view types.
  
  * worklist.owner = returns all workitems where the current user is owner from
  * worklist.creator =  returns all workitems  created by the current user
  * worklist.author = returns all workitems where the current user is author from 
  * worklist.writeaccess = returns all workitems where the current user has author access
  
Also the sort order of the result list can be configured by the property 'sortOder'
  
  * 0 = order by creation date descending
  * 1 = order by creation date ascending
  * 2 = order by modified date descending
  * 3 = order by modified date ascending  
  
The general the worklistController can be controlled by a JSF page 
 
	 <h:commandLink action="/pages/workitems/worklist" immediate="true"
		actionListener="#{worklistController.doReset}">
		<f:setPropertyActionListener
			target="#{worklistController.view}" value="worklist.owner" />
		<f:setPropertyActionListener
			target="#{worklistController.sortOrder}" value="2" />
	 </h:commandLink>


###Customizing the WorklistController
The following example shows how the WorklistController can be used in jsf application to display  the users task list. The controller bean is declared and confgured in the faces-config.xml or beans.xml file. 

faces-config.xml:
 
	... 
	  <managed-bean>
			<managed-bean-name>view</managed-bean-name>
			<managed-bean-class>org.imixs.workflow.jee.faces.workitem.WorklistController</managed-bean-class>
			<managed-bean-scope>view</managed-bean-scope>
			<managed-property>
				<property-name>maxResult</property-name>
				<property-class>int</property-class>
				<value>5</value>
			</managed-property>
			<managed-property>
				<property-name>sortOrder</property-name>
				<property-class>int</property-class>
				<!-- SORT_ORDER_MODIFIED_DESC -->
				<value>2</value>
			</managed-property>
			<managed-property>
				<property-name>view</property-name>
				<property-class>java.lang.String</property-class>
				<value>worklist.owner</value>
			</managed-property>
		</managed-bean>
	 ...

The bean portletWorklistTasks can now be used in any JSF page:

	<!-- **** show Workitems ***** -->
	<h:panelGroup id="portlet_worklist_body">
			<ui:repeat var="workitem" value="#{view.workitems}">
				<h:commandLink
					action="#{workflowController.load(workitem.item['$uniqueid'],'/pages/workitems/workitem')}">
					<h:outputText style="font-weight: bold;" escape="false"
						value="#{workitem.item['txtWorkflowSummary']}" />
				</h:commandLink>
			</ui:repeat>
			<!-- navigation -->
			<h:panelGroup layout="block" id="portlet_worklist_nav"
				style="float: right;">
				<span style="margin-right: 10px;"> <h:commandLink
						actionListener="#{view.doLoadPrev}" disabled="#{view.row == 0}"
						value="#{message.prev}">
						<f:ajax render="portlet_worklist_body" />
					</h:commandLink>
				</span>
				<h:commandLink actionListener="#{view.doLoadNext}"
					disabled="#{view.endOfList}" value="#{message.next}">
					<f:ajax render="portlet_worklist_body"></f:ajax>
				</h:commandLink>
			</h:panelGroup>
	</h:panelGroup>
 