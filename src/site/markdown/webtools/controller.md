#The Imixs-Workflow-Faces Controller
The project imixs-workflow-faces provides set of CDI beans which can be used to controll ItemCollections, WorkItems and Views.

##The DataController
The DataController is a managed bean controlling the data of a single  WorkItem. The DataController is typically used in sessionScope to store the data of a WorkItem over several pages. The DataController provides a set of Action and ActionListener methods:
 

| Method        | Type            |Description                                |       
|---------------|-----------------|-------------------------------------------|
|create()       | ActionListener  | creates a new empty WorkItem. The new  WorkItem is assigend to the current user  |
|reset()        | ActionListener  | resets all data of the current WorkItem   |
|save(action)   | Action          | saves the data of the current WorkItem    |
|load(id,action)| Action          | loads an existing WorkItem by ID          |
|delete(id,action)| Action        | deletes an existing workitem by ID        |

All action methods accept a String param 'action' with the default action result. (Can be 'null')
 
## The WorkflowController
The WorkflowController is a subclass from the DataController and provides methods to process a  workitem based on a specific workflow model.  The workflow model version can be defined by the WorkItem property '$modelversion'. To process a WorkItem the properties '$taskid' and '$eventid' need to be defined. The WorkflowController provides the following addition Action and ActionListener methods:
 
### init(actiion) 
This method initializes a new created WorkItem based  on the workflow model definition    

### process()
The process method processes the current workItem and returns an action result.  The method expects that the current workItem provides a valid $ActiviytID.  The method returns the value of the property 'action' if provided by the workflow model or a plugin. The 'action' property is typically evaluated from the ResultPlugin. Alternatively the property can be provided by an application. If no 'action' property is provided the method evaluates the default property 'txtworkflowResultmessage' from the model as an action result.	 

###process(activityID)
This ActionListener processes the current workItem with the provided activityID. The method can be used as a ActionListener for a ajax events.


##The ViewController
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
 