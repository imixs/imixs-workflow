# Imixs-Faces Editor 

The jQuery TinyMCE Editor will be automatically added to all textareas with the  style class 'imixs-editor'
 
	<h:inputTextarea  class="imixs-editor"
					value="#{workflowController.workitem.item['htmldescription']}" />
				 
The TinyMCE feature can be disabled with the option 'disabletinymce=true' inside the ImixsHeader tag
 
	<i:imixsHeader disabletinymce="true" />

This will also disable the TinyMCE Plugin. To add the tinyMCE to custom textareas use the layoutImixsEditor() function during the page setup.

	$('textarea.custom-editor').
		 layoutImixsEditor('#{facesContext.externalContext.requestContextPath}',900,200);

The first param defines the applications root context under which the tinymce scripts will be loaded. This param is required! With the optional last two params you can define the width and height of the editor.

## Customizing TinyMCE
To customize the layout of the tinyMCE the default content.css file can be overwritten by an application. Place the file 'content.css' into the web directory 

	/[APPLICATION_CONTEXT_ROO]/imixs/tinymce/  

To customize the behavior of the tinyMCE you can use the tinyMCE() function during the page setup. See the following example:
 
	....	
	<script type="text/javascript">
			/*<![CDATA[*/
	
			// special layout für wolfwurst tiny mce 
			$(document).ready(function() {
				layoutCustomEditor();
			});
	
				function layoutCustomEditor() {
				$('textarea.custom-editor')
				.tinymce(
					{
						// Location of TinyMCE script
						script_url : '#{facesContext.externalContext.requestContextPath}/imixs/tinymce/jscripts/tiny_mce/tiny_mce.js',
						// General options
						theme : "advanced",
						plugins : "inlinepopups,fullscreen",
						// Theme options
						theme_advanced_buttons1 : "cut,copy,paste,removeformat,cleanup,|,undo,redo,|,bold,italic,underline,strikethrough,|,justifyleft,justifycenter,justifyright,justifyfull,|,hr,bullist,numlist,|,formatselect,outdent,indent,|,link,unlink,image,|,forecolor,backcolor,|,fullscreen",
						theme_advanced_buttons2 : "",
						theme_advanced_toolbar_location : "top",
						theme_advanced_toolbar_align : "left",
						width : 700,
						height : 200,
						style : "border:0px;",
						content_css : "#{facesContext.externalContext.requestContextPath}/layout/css/tinymce.css",
						theme_advanced_resizing : true,
						theme_advanced_resizing_use_cookie : false,
						theme_advanced_path : false,
						theme_advanced_statusbar_location : "none"
					});
	
			}
	
			/*]]>*/
		</script>
	.....
	<body>	
		.....
				
	   <h:inputTextarea  class="custom-editor"
		  value="#{workflowController.workitem.item['htmldescription']}" />
					
		......




 