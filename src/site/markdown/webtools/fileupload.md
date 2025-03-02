# Imixs-Faces Fileupload

Imixs-Faces Fileupload is a custom component to provide a fileUpload widget. The component consists of the following parts

- the FileUploadController - a front-end controller to push uploaded files into a workitem (based on JSF 4.0)
- the i:imixsFileUpload - a JSF component to be used in JSF pages

The `<i:imixsFileUpload />` component can be included into any JSF form using the tag 'imixsFileUpload'

```xml
<i:imixsFileUpload id="file_upload_id" />
```

<img src="../images/webtools/fileupload-01.png"/>

You can show the attachment list of a given `ItemCollection`. Using this feature you need to provide the contexturl and the workitem stored the FileData objects.

```xml
<i:imixsFileUpload showattachments="true"
	workitem="#{documentController.document}"
	context_url="#{facesContext.externalContext.requestContextPath}/api/" />
```

<img src="../images/webtools/fileupload-02.png"/>

## Style

The FileUpload component provides a set of style classes which can be used to customized the layout:

| CSS class              | Description                             |
| ---------------------- | --------------------------------------- |
| .imixsfileupload       | the main component                      |
| .imixsfileupload-table | table containing the new uploaded files |
| .imixsfileuploadinput  | the file input element                  |
| .remove-link           | link symbol to remove a file            |
| .drop-area             | area to drop files                      |

## The FileUploadController

The FileUploadController provides methods to extract a file from a mulitpart http request. File will be automatically attached to the current workflow or document context by observing the CDI event `DocumentEvent` and `WorkflowEvent`.
Optional a CDI Bean can use the controller method `attacheFiles` to manually attache files.

```java
...
protected ItemCollection modelUploads = null;
@Inject
FileUploadController fileUploadController;
...

public void doUploadModel(ActionEvent event)
		throws ModelException {

	try {
		fileUploadController.attacheFiles(modelUploads);
	} catch (PluginException e) {
		e.printStackTrace();
	}
	List<FileData> fileList = modelUploads.getFileData();
	....
}
```
