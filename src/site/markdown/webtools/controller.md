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
 
In combination with the viewAction the DocumentController is automatically  initialized. After a document is loaded, a new conversation is started and the CDI event  WorkflowEvent.DOCUMENT_CHANGED is fired. After a document was saved, the conversation is automatically closed. Stale conversations will automatically timeout with the default session timeout. After each call of the method save the Post-Redirect-Get is initialized with the default URL from the start of the conversation. This guarantees
 bookmakrable URLs.

Within a JSF form, the items of a document can be accessed by the getter  method getDocument().
 
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

The ViewController is a @ViewScoped CDI bean to select data defined by a search query.
This bean is used to display a data result in a JSF page and suppors also a pagination mechanism.

A custom ViewController can be defined by sub-classing:

	@Named
	@ViewScoped
	public class TasklistController extends ViewController implements Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		@PostConstruct
		public void init() {
			super.init();
			this.setQuery("(type:\"workitem\")");
			this.setSortBy("$modified");
			this.setSortReverse(true);
		}
	}


The ViewController also provides a pagination mechanism to navigate through a big data set. See the following example:

	<h:dataTable value="#{tasklistController.data}" var="record">

		<h:column>
			<h:outputText value="#{record.item['txtName']} " />
		</h:column>
		<h:column>
			<h:outputText value="#{record.item['$modified']}" />
		</h:column>
		<!-- edit -->
		<h:column>
			<h:link outcome="/workitem?faces-redirect=true">
				<h:outputText value="#{global.edit}" />
				<f:param name="id" value="#{record.item['$uniqueid']}" />
			</h:link>
		</h:column>

		</h:dataTable>

		<h:commandButton 
			actionListener="#{tasklistController.back()}"
			disabled="#{tasklistController.pageIndex==0}" value="#{global.prev}">
		</h:commandButton>

		<h:commandButton 
			actionListener="#{tasklistController.next()}"
			disabled="#{tasklistController.endOfList}" value="#{global.next}">
		</h:commandButton>
	</h:dataTable>	

