#The Imixs-Faces Web Components
Imixs-Faces is a component library based on JSF and jQuery, which makes development of JSF font-ends together with the Imixs-Workflow engine much easier. This library contains a set of UI components to be used in a JSF 2.2 Web Application and builds up on the capabilities of the Imixs-Workflow Engine.
 
<img src="../images/webtools/imixs-architecture_web.png"/>
 
Since Java EE6 the lightweight Web Profiles simplifies the deployment of Java EE applications. In case of a Web application the Imixs-Faces components can be deployed together with the Imixs-Workflow engine. See the following maven depencency configuration 


	....
	<properties>
		<org.imixs.workflow.version>3.9.0-SNAPSHOT</org.imixs.workflow.version>
	</properties>
	<dependencies>
		....
		<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-workflow-engine</artifactId>
			<version>${org.imixs.workflow.version}</version>
		</dependency>
		<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-workflow-jax-rs</artifactId>
			<version>${org.imixs.workflow.version}</version>
		</dependency>
		<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-workflow-faces</artifactId>
			<version>${org.imixs.workflow.version}</version>
		</dependency>
		....

Find more information about the Maven support of Imixs-Workflow in the [section Maven](../maven.html). 

The [Imixs-JSF sample application](../sampleapplication.html) provides a good starting point to learn how to setup a JSF Web Application with Imixs-Workflow.


## The JSF Components 
The Imixs-Faces  component library provides widgets to design modern user interfaces. These widgets are based on [jQuery and jQuery UI](http://jquery.com/) which is a powerful JavaScript framework to design Web 2.0 UIs. Imixs-Faces can also be combined with any other JSF Library or font-end framework. The following section gives an overview about the different components. 
 
 
 
### The DataController
One of the core concepts of the Imixs-Workflow API is a dynamic document structure represented by the class [ItemCollection](../core/itemcollection.html).
This concept makes it possible to store any data fields of a web form into the Imixs back-end service. The DataControler is a CDI Bean providing the interface to an ItemCollection and basic methods to access the Imixs DocumentService. The Component provides methods to save, load and delete an entity.

	<h:inputText value="#{dataController.item['txtSubject']}"/>
	...
	<h:commandButton action="#{dataController.save('home')}"	value="save" />
						  
In this example the attribute "txtSubject" will be managed by the DataController. The WorkflowController can be subclassed to implement a backing bean with application specific properties.

 
### The WorkflowController
The WorkflowController extends the DataController, and provides an interface to the Imixs-Workflow engine. The CDI Bean provides methods to create, access and process workitems. The following code example shows how a new process instance based on the model 'ticket' can be created by the controller:

	<h:commandButton actionListener="#{workflowController.create}"
				action="/pages/workflow/workitem"  value="Create New Ticket">
					 <f:setPropertyActionListener
					target="#{workflowController.workitem.item['$ModelVersion']}"
					value="1.0.1" />
				<f:setPropertyActionListener
					target="#{workflowController.workitem.item['$taskID']}"
					value="1000" />
	</h:commandButton>

  
 
###SecurityHashMap and LoginMB
Imixs-Workflow provides a strong security management inside a single process. Imixs-Faces provide some components to be used to manage the User Login- and Logout mechanism and also an easy access to user specific 
roles. This simplifies in many cases the implementation of secured web applications.  The following example shows how an input field is hidden for users without the access  role 'org.imixs.ACCESSLEVEL.MANAGERACCESS' 
 
	 <h:inputText value="#{workflowMB.item['txtSubject']}" id="subject_id"
	   rendered="#{IsUserInRole['org.imixs.ACCESSLEVEL.MANAGERACCESS']}" />
 
  
### BLOBWorkitemController
This BLOBWorkitemController is an additional BackingBean to be used to save large binary objects into   separate workitem. This is an implementation for lazy loading large data objects attached to an existing workitem. The advantage of using a BlobWorkitem is that an application can store large data into a separate
worktitem wich is attached to a parent workitem. So in this case the BlobWorkitem can be loaded just   in the moment all details of a parent workitem need to be accessible by the user. For example during  editing a form. So the BlobWorkitem needs not to be loaded by the workflow manager during each search method which typical access a lot of workitems in the same time. Using a BLOBWorkitemController can optimize the data management. The Bean supports also the management for file attachments. See the examples for more details.



  
    
