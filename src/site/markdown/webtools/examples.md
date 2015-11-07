#How to Use the Imixs Web Tools
The following section provide some examples how the Imixs Web Tools can be used in a web application. The examples assume that you are familiar with JSF and Web development. Before your can use the Imixs Web Tools you need to deploy the Imixs Workflow engine into  your web module (WAR) or enterprise archive (EAR). 
 
##Create a Workitem
Before you can edit a Workitem in a JSF Form you need to create an new empty instance of a   Workitem. Therefore you can call the ActionListener Method "doCreateWorkitem" form you WorkflowController. The method creates an empty worktiem which is assinged to a specific processID provided by the model  inside your Workflow instance. You can bind the ActionMethod to a commandButton or commandLink from a menue:
  
<img src="../images/webtools/screenshot_001.png"/>
  
The following code example shows a command button which creates a new wokitem assigned to the ProcessID "100"   form the Model version "0.0.1". 
 
	 <h:commandButton
		value="create new...."
		actionListener="#{workflowMB.doCreateWorkitem}"
		action="show_workitem">
		<f:param name="id"
			value="0.0.1|100" />
		</h:commandButton>

If you don't deal with model versions you can also simply provide a  valid ProcessID without a model version
  
	 <h:commandButton
		value="create new...."
		actionListener="#{workflowMB.doCreateWorkitem}"
		action="show_workitem">
		<f:param name="id"
			value="100" />
		</h:commandButton>
  
  
The actionlistener method doCreateWorkitem will not save any data into your database. The 
method just creates an empty instance of a new workitem which can be processed by any JSF page e.g. a input form.
   	       
   	       
##Processing a Workitem
When you have created an instance of a workitem the values can be edited by a JSF Form and later processed  by the WorkflowController. To bind the workitem values to input fields the WorkflowController provides a dynamic hash-map. To process (save) the input data the controller provides the actionListener  'doProcess'.
 
### The Workitem properties
  
<img src="../images/webtools/screenshot_002.png"/> 
 
The WorkflowController provides you with the dynamic property 'item' which allows you to bind an input value to an arbitrary property name. See the following example were an inputText value is bound to the item property 'txtSubject'.
  
	 <h:inputText required="true"
		value="#{workflowMB.workitem.item['txtSubject']}" id="subject_id">
	 </h:inputText>

The workflow controller will automatically manage the property "txtSubject" and store the input values into the database. 
  
### Processing the Workitem 
The ActionListener method "doProcessWorkitem" can be used to process a workitem by the WorkflowController. The method is similar to the method doCreateWorkitem and can be bound to an commandButton. 
 
<img src="../images/webtools/screenshot_003.png"/> 

The following example shows a command button which will process the workitem and store all values to the database. 
 
	 <h:commandButton action="show_workitem"
		actionListener="#{workflowMB.doProcessWorkitem}"
		value="submit">
		<f:param name="id" value="10" />
	 </h:commandButton>

The method doProcessWorkitem expects the param 'id' with a valid Activity ID defined by the corresponding workflow model. So if you create a new Workitem instance with the ID=100 this means that the Process Entity need to provide a Activity with the ID=10 which can be processed by the WorkflowController. If you don't want to generate all command Buttons hard coded in your form you can access the property "activities" which provides a set of command buttons corresponding to the available activities the workitem is assigned to:
 
	 <ui:repeat value="#{workflowMB.activities}" var="activity">
		<h:commandButton action="#{workflowMB.getWorkflowResult}"
			actionListener="#{workflowMB.doProcessWorkitem}"
			value="#{activity.item['txtname']}">
			<f:param name="id" value="#{activity.item['numactivityid']}" />
		</h:commandButton>
	 </ui:repeat>


##The WorkflowController
The Imixs Web-Tools Project provides a backing bean to be used as a workflow controller.  The controller Bean provides several action methods to create, update and delete workitems through the Imixs Workflow Manager. It also provides a simple way to  add any kind of user input in dynamic properties controlled by the Imixs Workflow Manager. The following example shows how to process an instance of an workitem with the workflow activity 100:

	 <h:commandButton
	        value="start a new process"
	        actionListener="#{workflowController.process(100)}"
	        action="show_workitem">
	  </h:commandButton>

The workflowController can also be used to bind input values to an JSF Input Field

	 <h:inputText required="true"
	        value="#{workflowController.workitem.item['txtSubject']}" id="subject_id">
	 </h:inputText>	
		
		
##The BLOBWorkitemController
This BLOBWorkitemController is used to store large objects into a single  ItemCollection mapped to a EntityBean. The BlobWorkitem is always bounded to a parent  workitem by its referrer id ($uniqueidRef). So an application can implement a  lazy loading for BLOBWorkitems. The read- and write access settings of  BLOBWorkitems are always synchronized to the settings of the parent workitem.  Before the BlobWorkitem can be accessed the workitem needs to be loaded by
  the load() method. The Data can be accessed by the embedded ItemCollection  through the method getWorkitem(). The BlobWorkitem can be saved by calling  the save() method. Both - the load() and the save() method expect the Parent  ItemCollection where the BlobWorkitem should be bound.  This will be shown now in an example:
  
The LOBWorkitemController can be used easily as a backing bean defined in the facesContext.xml file:
  
	  .....	
		<managed-bean>
			<managed-bean-name>workitemBlobMB</managed-bean-name>
			<managed-bean-class>org.imixs.workflow.jee.jsf.util.BLOBWorkitemController</managed-bean-class>
			<managed-bean-scope>session</managed-bean-scope>
		</managed-bean>
	  .....	
  

The BLOBWorkitemMB can be used exactly the same way as the SimpleWorkflowController. To access a  item the following code example can be used.
  
	 <h:inputText required="true"
	        value="#{workitemBlobMB.item['txtLargeDataField']}" id="blob_data_id">
	 </h:inputText>
  
The next code example shows a WorkitemController (extended by the AbstractWorkflowControler) which overwrites  the doProcess() method to save some large data into an separate Blobworkitem using the BLOBWorkitemController:
  
	public void doProcessWorkitem(ActionEvent event) throws Exception {
		// do default processing 
		super.doProcessWorkitem(event);
		
		// get the BLOBWorkitemController from the FacesContext
		workitemBlobMB = (BLOBWorkitemController) FacesContext
		.getCurrentInstance().getApplication().getELResolver()
		.getValue(FacesContext.getCurrentInstance().getELContext(),
				null, "workitemBlobMB");
		// load/create new LOBworkitem
		workitemBlobMB.load(workitemItemCollection);
		// now add additional Large data into the blob bean
		workitemBlobMB.getWorkitem().replaceItemValue("lobDataField", myLargeDataObject);
		workitemBlobMB.save(workitemItemCollection);
	}

You can also use the BLOBWorkitemController to add file attachments to the BLOBWorkitem. This can be done  for example also during the doProcesWorkitem() method of the main Workitem 
       
	 .....
	 workitemBlobMB.addFile(item.getData(), item.getFileName(),
					item.getContentType());
       
File attachments will be stored into the property $file. The property value holds a hashMap with the filename,  content type and byte array data. The following example shows how to add a RichFaces Upload Control element using the LOBWorkitemControler.  Files will be saved into the LOBWorkitem.  