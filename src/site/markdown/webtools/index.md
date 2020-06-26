# The Imixs-Faces Web Components
Imixs-Faces is a component library based on JSF and jQuery, which makes development of JSF font-ends together with the Imixs-Workflow engine much easier. This library contains a set of CDI beans and UI components to be used in a JSF 2.2 Web Application.
 
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
		<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-workflow-index-lucene</artifactId>
			<version>${org.imixs.workflow.version}</version>
		</dependency>
		....

You will find more information about the Maven support of Imixs-Workflow in the [section Maven](../maven.html). 

The [Imixs-JSF Sample Application](../sampleapplication.html) provides a good starting point to learn how to setup a JSF Web Application with Imixs-Workflow.


## Imixs-Faces Data

Imixs-Faces provides different CDI beans to control the data flow within a JSF application. 

 * DocumentController - A @ConversationScoped CDI bean to view and edit a single document entity 
 * WorkflowController - A @ConversationScoped CDI bean to control the processing life cycle of a single workitem
 * ViewController - A @ViewScoped CDI bean to control a query defintion, pagination and filtering of a data result.
 * ViewHandler - A @RequestScoped CDI bean to display the data result provided by the ViewController
 
To interact a Document or WorkflowController the CDI Event 'WorkflowEvent' can be observed by a client. This simplifies the integration of custom CDI beans and controllers. Read the section [controller](./controller.html) for details. 

## Imixs-Faces Util

Imixs-Faces includes some utility classes to support typical UI flows in a JSF application in combination with Imixs-Workflow

 * LoginController - A  @RequestScoped CDI bean providing information about the current user session
 * ErrorHandler - this bean can be used to translate Imixs-Workflow PluginExceptions into a JSF Message
 * ValidationExcepiton - a custom exception type to handle Imixs-Workflow exceptions. 
 * VectorConverter - translates a String into a List
 * ViewExpiredExceptionHandler -  handle expired JSF sessions


## Imixs-Faces FileUpload

This package contains a FileUploadController and a Servlet to handle multi-file uploads.


## Imixs-Faces UI

Imixs-Faces provides UI widgets to be used in a JSF Page.

 * [Page Header](./header.html) - a header component to load scripts and layout resources 
 * [Datepicker](./datepicker.html) - a date picker widget based on jQuery
 * [WYSIWIG Editor](./tinymce.html) - a WYSIWYG editor based on tinymce
 * [Tooltips](./tooltip.html) - jQuery tooltip component
 * [Workflow Action Toolbar](./workflowactions.html) - a action toolbar to show workflow actions for a workitem controlled by the WorkflowController 
    
