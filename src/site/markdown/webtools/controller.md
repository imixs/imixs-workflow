# The Imixs-Workflow-Faces Data

The  Imixs-Workflow-Faces Data package contains CDI beans which can be used to control Imxis-Workflow data.

## The DocumentController


The DocumentController is a @ConversationScoped CDI bean to control the life cycle of a ItemCollection in an JSF application without any workflow  functionality. The bean can be used in single page applications, as well for complex page flows. The controller is easy to use and supports bookmarkable  URLs.
 
The DocumentController fires CDI events from the type WorkflowEvent. A CDI  bean can observe these events to participate in the processing life cycle.
 
To load a document the methods load(id) and onLoad() can be used. The method load expects the uniqueId of a document to be loaded. The onLoad() method  extracts the uniqueid from the query parameter 'id'. This is the recommended  way to support bookmarkable URLs. To load a document the onLoad method can be  triggered by an jsf viewAction placed in the header of a JSF page:
  
	<f:metadata>
      <f:viewAction action="... documentController.onLoad()" />
    </f:metadata>
 
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
    </f:metadata>
 
A bookmarkable URL looks like this:

	/myForm.xthml?id=[UNIQUEID] 
	
In combination with the viewAction the WorkflowController is automatically initialized. After a workitem is loaded, a new conversation is started and the CDI event WorkflowEvent.WORKITEM_CHANGED is fired.
 
After a workitem was processed, the conversation is automatically closed. Stale conversations will automatically timeout with the default session timeout.

After each call of the method process the Post-Redirect-Get is initialized with the default URL from the start of the conversation. If an alternative action result is provided by the workflow engine, the WorkflowController automatically redirects the user to the new form outcome. This guarantees bookmakrable URLs.

Call the close() method when the workitem data is no longer needed.

Within a JSF form, the items of a workitem can be accessed by the getter method getWorkitem().

	#{workflowController.workitem.item['$workflowstatus']}




## The ViewController

The ViewController is a @ConversationScoped CDI bean to define a data query.
This bean is used in combination with the ViewHandler to display a data result in a JSF page.

The query can be defined by the jsf tag, <f:viewAction>. The viewAction component must be declared as a child of the metadata facet (<f:metadata>).

	<f:metadata> 
	  <f:viewAction action="#{viewController.setQuery('...)}" />
	</f:metadata>

The ViewController also provides a pagination mechanism to navigate through a big data set.



### Customizing the ViewController
The following example shows how the ViewController can be used in JSF application to display  the users task list. The controller bean is declared and configured in the faces-config.xml or beans.xml file. 

faces-config.xml:
 
	... 
	  <managed-bean>
			<managed-bean-name>tasklist</managed-bean-name>
			<managed-bean-class>org.imixs.workflow.jee.faces.data.ViewController</managed-bean-class>
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
				<property-name>query</property-name>
				<property-class>java.lang.String</property-class>
				<value>type:workitem</value>
			</managed-property>
		</managed-bean>
	 ...

The bean portletWorklistTasks can now be used in any JSF page:


	<f:metadata>
        <f:viewAction action="#{viewController.setQuery('type:team')}" />
		<f:viewAction action="#{viewHandler.loadData(viewController)}" />
    </f:metadata>


	<!-- **** show Workitems ***** -->
	<h:dataTable class="imixsdatatable" style="width:100%"
				value="#{viewHandler.data}" var="workitem">
				....
	</h:dataTable>
 