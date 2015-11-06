# Imixs-Script

Imixs-Script is a JavaScript Framework to build business applications using the [Imixs-Workflow technology](http://www.imixs.org). The framework provides an easy to use interface and a height flexibility to develop powerful workflow applications.

  
  
##Get started....

Imixs-Script! is based on [jQuery](www.jquery.com)  and interacts through the Imixs REST API with the workflow engine in background.
Imixs script can be combined with any other JavaScript library such as for example [Ben.JS](http://www.benjs.org), EmberJS, Angular or React. 

Before you can start you need to deploy an instance of the Imixs-Workflow engine. This can be a custom project or you can install the Imixs-Script Sample Application providing an instance of Imixs-Workflow. See the section [Sample Application](#sample_application) for further details.

To embed Imixs-Script into your JavaScript application simply add the following libraries at the end of your HTML page.

    ...
    <script type="text/javascript" src="./js/jquery-2.1.4.min.js"></script>
    <script type="text/javascript" src="./js/jquery-ui.min.js"></script>
    <script type="text/javascript" src="./js/imixs-core.js"></script>
    <script type="text/javascript" src="./js/imixs-xml.js"></script>
    <script type="text/javascript" src="./js/imixs-workflow.js"></script>
    <script type="text/javascript" src="./js/imixs-ui.js"></script>
    ...
    </body>


As you can see Imixs-Script consists of separate modules which can be loaded also on demand. 
 

 * __imixs-core.js__ - provides the general data model used to map the properties of an imixs workitem into a convenience JavaScript object
   
 * __imixs-xml.js__ - provides methods to convert a XML result from the Imixs REST API into a JSON format. 
  
 * __imixs-workflow.js__ - provides methods to access the Imixs-Workflow engine through the REST API
 
 * __imixs-ui.js___ - provides UI methods  
 

### Downloads
Imixs-Script can be downloaded from [GitHub](https://github.com/imixs/imixs-script/releases). The download contains the libraries and also a JEE sample application. 

Please note that you also need to download jQuery from the [jQuery download page](www.jquery.com)

<a id="sample_application"></a>
 
##The Sample application
The Imixs-Script sample application provides an instance of the Imixs-Workflow engine and can be used as a template for custom project. See the [Installation Guide] how to install the sample application on WildFly or GlassFish application servers.

