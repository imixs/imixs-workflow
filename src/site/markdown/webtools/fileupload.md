#Imixs Fileupload
Imixs Fileuplaod is a custom component to provide a fileUpload widget.  The component consists of the follwing parts
 
  * a MulitpartRequestFilter which is wrapping the servlet request
  * a MultipartRequestWrapper which extracts the content of an uploaded file
  * the UploadFile object which represents the content of an uploaded file
  * the fileUploadConroller which provides an frontend controller to extract uploaded files and providing information about uploaded files
 
The MulitpartRequestFilter is used ot wrapp the servlet http request into a  MultipartRequestWrapper if the content type is "multipart/form-data". This content type allows to upload files. The FileUpploadBean provides methods to extract an uploadef file from the http request using the  MulitpartRequestWrapper. The custom component is implemented by the ressource 'imixsFileUpload.xhtml' This component can be included directly into any jsf form with the enctype="multipart/form-data".  To add the FileUpload component into a JSF page use the tag 'imixsFileUpload'
 

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


##The FileUploadConroller 
The FileUploadConroller provides methods to extract a file from a mulitpart http request. So the controller can be used in JSF pages to provide a fileUpload feature. The controller provides a build-in functionality to store the uploaded files into a ItemCollection. A client can set a target WorkItem by calling setWorkitem(). If the WorkItem contains a property "$BlobWorkitem" than the controller stores and adds files into the WorkItem referenced by this property. (The property "$BlobWorkitem" than contains then the $UniqueID of the blobWorkItem)  The list of all currently uploaded files will be stored into the property $file.
 
  
###Lazy loading
In case a BlobWorkitem was defined the file content of the property $file will be empty and the files are only stored physically into the corresponding BlobWorkitem. This mechanism provides a lazy loading for file attachments. In this case the BlobWorkitem will only be loaded by the Controller if a new Attachment need to to be stored. All other information can be read from the property $File stored in the parent WorkItem. The property 'RestServiceURI' allows the fileUploadController also to provide a download url which is pointing directly to the RestService interface of the Imixs Workflow.

 
### Ajax support
The component can not be wrapped with an f:ajax tag because of the restriction that   ajax requests can not deal with "multipart/form-data" forms. For that reason the component imixsFileUploadFrame is provided and included into a iFrame. 
 