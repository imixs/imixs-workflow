#The Imixs-REST Service API

The **representational state transfer (REST)** is an architectural style, to present a service interface in a Web-based way. The API and core functionality of Imixs-Workflow is published by an open and platform independent RESTful Web service interface. The Imixs-Workflow REST services can be adopted easily by any kind of Internet client component.
 
## Resources and URIs
An URI is a unique resource identifier which can be used to GET or PUT a unique resource through a RESTful service interface.
The REST service interface of Imixs-Workflow publishes a set of resources which are representing  WorkItems, Attachments, Reports and Model information.
The different resources provided by Imixs-Workflow are divided in the following groups, where each group represents a different aspect of the Imixs-Workflow engine:

| URI                       | Resource Description                              | 
|---------------------------|---------------------------------------------------| 
| [/workflow/](./workflowservice.html) | The Workflow resource provides resources and methods to get, create or modify workitems       |
| [/model/](./modelservice.html)       | The Model resource provides resources and methods to get, create or modify a workflow model|
| [/report/](./reportservice.html)     | The Report resource provides resources and methods to create or execute a report based on a report definition|
| [/documents/](./documentservice.html)     | This resource provides methods to query documents managed by the DocumentService EJB |
| [/adminp/](./adminp.html)     | This resource provides methods to create and monitor adminP jobs managed by the AdminPService EJB |

 
<strong>Note:</strong> The root context of the REST Service is defined by the web application (web.xml) containing the REST Service. The default root context is "/api/".

## The Representation of Business Objects
Each resource published by the Imixs-Workflow REST API is represented by common response and request object format. This format reflects a representation of the internal Business Object [ItemCollection](../core/itemcollection.html). Business objects can be represented in XML or JSON format. 

### XML Business Object

The following example shows a business object in XML representation used for request and response objects:

	<document xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xs="http://www.w3.org/2001/XMLSchema">
		<item name="$modelversion">
			<value xsi:type="xs:string">1.0.1</value>
		</item>
		<item name="$taskid">
			<value xsi:type="xs:int">1000</value>
		</item>
		<item name="$eventid">
			<value xsi:type="xs:int">10</value>
		</item>
		<item name="_subject">
			<value xsi:type="xs:string">some data...</value>
		</item>
	</document>

### JSON Business Object

The following example shows a business object in JSON representation used for request and response objects:


    {"item":[
     {"name":"$modelversion","value":{"@type":"xs:string","$":"1.0.1"}},
     {"name":"$taskid","value":{"@type":"xs:int","$":"1000"}}, 
     {"name":"$eventid","value":{"@type":"xs:int","$":"10"}}, 
     {"name":"_subject","value":{"@type":"xs:string","$":"Hello World"}}
    ]}  

Depending on the Rest Service Implementation the JSON format for response object can deviate in the following simplified presentation style (e.g. RESTeasy ):

    {"item": [
        {"name": "$modelversion", "value": ["1.0.1"] },
        {"name": "$taskid", "value": [2000] },
        {"name": "$eventid", "value": [10] },
        {"name": "_subject", "value": ["Hello World"] }
     ]}



The Imixs-Workflow REST API accepts the following media types to return data:
 
 * text/html
 * application/xml
 * application/json
	
The following sections gives an detailed description of all resource groups defined by the Imixs-Workflow REST Service API:
 
 
  * [Workflow Service](./workflowservice.html)
  * [Model Service](./modelservice.html) 
  * [Report Service](./reportservice.html) 
  * [Document Service](./documentservice.html) 
    