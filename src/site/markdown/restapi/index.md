#The REST Service API
REST is an architectural style that can be used to guide the construction of web services  in a more easy kind as a service which uses the SOAP specification.  The Imixs XML & Web Services provide a REST Service Implementation which allows you to use the Imixs Workflow in a RESTful way.
 
 
## Resources and URIs
An URI is a unique resource identifier. In REST an URI represents a resource and is used to  get or modify a resource. In the meaning of the Imixs Workflow Technologies the URIs represent  WorkItems, Attachments, Reports or any other kind of Object provided by the Imixs Workflow. There are three main resources available where each represents one different aspect of the Imixs  Workflow components:

*---------------------------*---------------------------------------------------* 
| URI                       | Resource Description                              | 
*---------------------------+---------------------------------------------------+
| /workflow                 | The Workflow resource provides resources and      |
|                           | methods to get, create or modify workitems        |
*---------------------------+---------------------------------------------------+
| /model                    | The Model resource provides resources and methods |
|                           | to get, create or modify a workflow model entities|
*---------------------------+---------------------------------------------------+
| /report                   | The Report resource provides resources and methods|
|                           | to get the result of a report definition          |
*---------------------------+---------------------------------------------------+
| /entity                   | The Entity resource provides resources and methods|
|                           | to query entities and the indexList from the EntityService EJB |
*---------------------------+---------------------------------------------------+
 
<strong>Note:</strong> The root context of the REST Service is defined by the web application (web.xml)   where the REST Service is deployed. If you are using the Imixs-workflow-rest war file the rest service is mapped per default to root context '/workflow-rest/'. You can change the root context by configuration in the application.xml file.

Each of three main resources provides a set of sub resources which represent different  business objects managed by the Imixs Workflow components. Each business object can be provided in different formats depending on one of the following  request header
 
 * text/html
 * application/xml
 * application/json
	
So if you typing in the URI into a Web Browser you will typical receive an HTML representation of the business Objects.  The following sections gives an overview of all URIs defined by the Imixs REST Service.
 
 
 
  * [Workflow Service](./workflowservice.html)
  * [Model Service](./modelservice.html) 
  * [Report Service](./reportservice.html) 
  * [Entity Service]./entityservice.html) 
    