#The Imixs Web Tools
Imixs Web Tools is a component library based on JSF and jQuery, which helps you to develop JEE business  applications much faster. This library contains a set of components to be used in a JSF 2.0 Web  Application and builds up on the capabilities of the Imixs-Workflow Engine.
 
<img src="../images/webtools/imixs-architecture_web.png"/>
 
Since Java EE6 the lightweight Web Profiles simplifies the deployment of Java EE applications. This means that you can use Imixs Web Tools in your Web application without worry about the details of EJB and JPA deployment.
Imixs Web Tools providing widgets to design modern user interfaces. These widgets are based on [jQuery and jQuery UI](http://jquery.com/) which is a powerful JavaScript framework to design Web 2.0 UIs. If you like to work with other JSF Web frameworks you can also use only a subset of this component library.  

##The JSF Components 
The Imixs Web Tools provides a set of components which can be used to implement workflow management systems in a JSF based web application. 
 
### The WorkflowController
The WorkflowController is the main component in the Web Tools. This component provides a  Backing Bean providing the workflow management functionality.  The Component provides methods to create, access and process workitems.   One of the main concepts of the Imixs-Workflow API is a dynamic Document structure. This means that the component will automatically store and process any data provided by a web form. 
  	
	<h:inputText value="#{workflowMB.item['txtSubject']}" id="subject_id" />
						  
In this case the attribute "txtSubject" will be managed by the WorkflowController. The WorkflowController can be subclassed to implement a backing bean with application specific properties.
  
 
###SecurityHashMap and LoginMB
Imixs-Workflow provides a strong security management inside a single process. Imixs Web Tools provide some components to be used to manage the User Login- and Logout mechanism and also an easy access to user specific 
roles. This simplifies in many cases the implementation of secured web applications.  The following example shows how an input field is hidden for users without the access  role 'org.imixs.ACCESSLEVEL.MANAGERACCESS' 
 
	 <h:inputText value="#{workflowMB.item['txtSubject']}" id="subject_id"
	   rendered="#{IsUserInRole['org.imixs.ACCESSLEVEL.MANAGERACCESS']}" />
 
  
### BLOBWorkitemController
This BLOBWorkitemController is an additional BackingBean to be used to save large binary objects into   separate workitem. This is an implementation for lazy loading large data objects attached to an existing workitem. The advantage of using a BlobWorkitem is that an application can store large data into a separate
worktitem wich is attached to a parent workitem. So in this case the BlobWorkitem can be loaded just   in the moment all details of a parent workitem need to be accessible by the user. For example during  editing a form. So the BlobWorkitem needs not to be loaded by the workflow manager during each search method which typical access a lot of workitems in the same time. Using a BLOBWorkitemController can optimize the data management. The Bean supports also the management for file attachments. See the examples for more details.
  
##Maven Support
All binaries of the Imixs Workflow Project are provided as Maven artifacts  through the Maven Central repository.  To browse the Imixs artifacts provided by this project you can go to the [Maven repository](http://search.maven.org/#browse) and simply search for the keyword 'imixs'.

Find more information in the [section Maven](../maven.html). 

###How to add a Imixs Workflow maven dependency
To add the necessary dependencies for the Imixs Workflow engine add the following dependency to your pom.xml

	<dependency>
		<groupId>org.imixs.workflow</groupId>
		<artifactId>imixs-workflow-faces</artifactId>
		<version>RELEASE</version>
		<type>jar</type>
	</dependency>
  
    
