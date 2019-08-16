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

The ViewController is a @ViewScoped CDI bean providing a query and pagination definition. The bean can be used to load a data result from the Imixs-Workflow engine. To display the data result in a JSP page, the CDI bean _ViewHandler_ can be used (see section below). 

The _ViewController_ provides the following properties to define a data selection:

 * query - a Lucene query string
 * pageIndex - the start page for a query
 * pageSize - the count of records to be loaded
 * sortBy - an optional item name to sort the result set
 * sortReverse - optional defining ascending or descending (true=default) sorting. 
 * loadStubs - if true (default) only the Lucene Document Stubs will be loaded (see Lucene Search) 

Read more about the search functionality of Imixs-Worklfow in the [section LuceneService](../engine/luceneservice.html). 

To implement custom filters and converters the _ViewController_ can be easily extended. At least you just need to overwrite the method _init()_ in your sub-class. See the following example defining a query and a sorting:

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


### Display a Data Result

The _ViewController_ itself does not hold any data result. This is because the bean is @ViewScoped and so a data result would pollute the session store. 
To display the data result the @RequestScoped CDI bean 'ViewHandler' can be used. The ViewHandler provides the method _getData(viewConroller)_ to get the data and also provides a pagination mechanism to navigate through a data result. See the following example:

	<h:dataTable value="#{viewHandler.getData(tasklistController)}" var="record">
		<h:column>
			<h:outputText value="#{record.item['txtName']} " />
		</h:column>
		<h:column>
			<h:outputText value="#{record.item['$modified']}" />
		</h:column>
		<!-- edit -->
		<h:column>
			<h:link outcome="/workitem?faces-redirect=true">
				<h:outputText value="edit" />
				<f:param name="id" value="#{record.item['$uniqueid']}" />
			</h:link>
		</h:column>
	</h:dataTable>

	<!-- navigation -->
	<h:commandButton 
		actionListener="#{viewHandler.back(tasklistController)}"
		disabled="#{tasklistController.pageIndex==0}" value="#{global.prev}">
	</h:commandButton>
	<h:commandButton 
		actionListener="#{viewHandler.forward(tasklistController)}"
		disabled="#{tasklistController.endOfList}" value="#{global.next}">
	</h:commandButton>
	....
		


**Note:** The _ViewHandler_ has an effective internal caching mechanism which enables you to display different views in one single JSF page. So it is not necessary to subclass the ViewHandler. 

### Refresh a Data Result with Ajax 

You can use the _ViewHandler_ also to display and navigate a data result with ajax: 
	
	...
	<h:panelGroup id="myView" >
		<!-- pre-compute: #{viewHandler.onLoad(view)}  -->
		<h:commandLink actionListener="#{viewHandler.back(view)}" 
						disabled="#{(view.pageIndex == 0)}">back
						<f:ajax render="myView"/>
		</h:commandLink>
		<h:commandLink actionListener="#{viewHandler.forward(view)}" 
						disabled="#{(view.endOfList)}">forward
						<f:ajax render="myView"/>
		</h:commandLink>
		....
		<!-- show result -->
		<ui:repeat var="workitem" value="#{viewHandler.getData(view)}">
			...
		</ui:repeat>
	</h:panelGroup>
		
The comment section in this example encapsulates the method call 'onLoad(view)'. This will pre compute the data result. In this way the el expression _view.endOfList_ is computed correctly during navigation.  		
		
		
		
## The LoginController

This Backing Bean LoginController provides methods to identify the login state and user roles. 

 * isAuthenticated() - returns true if user is authenticated and has at least on of the Imixs Access Roles
 * isUserInRole(rolename) - tests if the user is assigned to a specific JAAS role
 * getUserPrincipal() - returns the userPrincipal Name
 * getUserNameList() - Returns the current user name list including userId, roles and context. See also the [DocumentService](../engine/documentservice.html)
 * doLogout(ActionEvent event)  - invalidates the current JSF user session
	 * groups.
 
