#Imixs Header
The Imixs-Faces custom tag 'imixsHeader' provides a jQuery UI integration. This tag will automatically add jQuery and jQuery UI functionality to a JSF page.
 
	<?xml version="1.0" encoding="UTF-8"?>
	<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" 
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
	<html xmlns="http://www.w3.org/1999/xhtml"
	   xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
	   xmlns:f="http://xmlns.jcp.org/jsf/core"
	   xmlns:h="http://xmlns.jcp.org/jsf/html"
	   xmlns:i="http://xmlns.jcp.org/jsf/composite/imixs">
	<h:head>
		<i:imixsHeader />
	</h:head>
	<h:body>
	....

The imixsHeader tag provides a set of optional properties:
 
 
| Attribute     | Value      |Description                                |       
|---------------|------------|-------------------------------------------|
|disablecss     | true/false |disables css styles (default= false)       |
|disablejquery  | true/false | disable jquery (default=false)  can be used if a different version of jquery is provided       |
|disabletinymce | true/false | can be set to true to disable the tinymce editor  |
|dateformat     | yyyy-MM-dd | default format for date picker widget     |
|theme          | URL        | deletes an existing workitem by ID        |
 
 
## jQuery-UI
Imixs-Faces header provides the following additional UI functionality and custom jQuery components:
 
  * [Imixs Header](./header.html)
  * [DatePicker Widget](./datepicker.html)
  * [Workflow Actions Toolbar](./workflowactions.html)
  * [FileUpload component](./fileupload.html)
  * [Tooltips](./tooltip.html)
  * [WYSIWYG Editor TinyMCE](./tinymce.html)
 

### Themes
The ImixsHeader provides a default theme named 'imixs' which is based on the jQuery-UI themes. The theme can be customized by providing a custom jQuery-UI theme. The custom jQuery theme can be placed into a folder of the web application. To activate the custom theme, the path to the theme providing the jQuery UI and css files need to be added into the imixsHeader using the _'theme'_ tag: 

	<i:imixsHeader theme="#{facesContext.externalContext.requestContextPath}/layout/themes/ui-darkness/jquery-ui-1.8.21.custom.css" />
 

 
 