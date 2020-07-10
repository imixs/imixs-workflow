# Imixs-Faces Fileupload

Imixs-Faces Fileupload is a custom component to provide a fileUpload widget.  The component consists of the following parts
 
  * the FileUploadController - a front-end controller to push uploaded files into a workitem
  * the AjaxFileUploadServlet - a Multipart-Servlet 3.0. to handle file uploads from a ajax component.
  * the i:imixsFileUpload  - a JSF widget
 
The i:imixsFileUpload JSF widget can be included into any JSF form using the tag 'imixsFileUpload'
 

	<i:imixsFileUpload id="file_upload_id" />

You can hide the attachment list and the list of new uploaded files if you like to display these information somewhere else in your page using the fileUploadController. 
 
	<i:imixsFileUpload id="file_upload_id" hideattachments="true" hideuploads="true"/>

## Style
The FileUpload component provides a set of style classes which can be used to customized the layout:

| CSS class/id          | Description                                           |       
|-----------------------|-------------------------------------------------------|
|#imixsfileuploadform   | the main form of the component                        |
|#imixsfileuploadcontrol| div containing the controls and content section       |
|#imixsfileuploadcontent| div containing the file content                       |
|#imixsfileuploadattachments| table containing the attached files           |
|#imixsfileuploaduploads| table containing the new uploaded files               |
|.imixsfileuploadtable  | general table layout                                  |


## The FileUploadController 
The FileUploadController provides methods to extract a file from a mulitpart http request. In this way the controller can be used in JSF pages 
to provide a fileUpload feature. The controller provides a build-in functionality to store the uploaded files into a ItemCollection. A client can set a target WorkItem by calling setWorkitem().  The list of all currently uploaded files will be stored into the workitem property $file.


### Ajax support
The component can not be wrapped with an f:ajax tag because of the restriction that  ajax requests can not deal with "multipart/form-data" forms. For that reason the component imixsFileUploadFrame is provided and included into a iFrame. 
 
 
### File Size
The AjaxFileUploadServlet is derived from the Multipart-Servlet 3.0 and used by the *i:imixsFileUpload* widget. The widget is using a jQuery component to handle the upload of multiple files and supports drag & drop functionality.
The servlet is configured with a max file size to 10MB, and a max request size of 50MB.

These settings can be overwritten with the following web.xml settings:


	....
		<!-- Fileupload servlet -->
		<servlet>
			<servlet-name>FileuploadServlet</servlet-name>
			<servlet-class>org.imixs.workflow.faces.fileupload.AjaxFileUploadServlet</servlet-class>
			<load-on-startup>0</load-on-startup>		
			<multipart-config>
				<location>/tmp</location>
				<!-- 20971520 = 20MB -->
				<max-file-size>20971520</max-file-size>
				<max-request-size>52428800</max-request-size>
				<file-size-threshold>1048576</file-size-threshold>
			</multipart-config>
		</servlet>
		<servlet-mapping>
			<servlet-name>FileuploadServlet</servlet-name>
			<url-pattern>/fileupload/*</url-pattern>
		</servlet-mapping>
	....

	
## Callback Method onFileUploadChange

The optional JavaScript callback method *onFileUploadChange* can be implemented by a web front end to react on new uploaded or removed files. This callback method is triggered immediately when the user added or removed a file but before the data is submitted to the Imixs backend. The callback method must not be registered on a special component. 

	function onFileUploadChange() {
		// do something
	}


