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
| [/eventlog/](./eventlogservice.html)     | This resource provides methods to fetch event log entries |

 
<strong>Note:</strong> The root context of the REST Service is defined by the web application (web.xml) containing the REST Service. The default root context is "/api/".

## The Representation of Business Objects
Each resource published by the Imixs-Workflow REST API is represented by common response and request object format. This format reflects a representation of the internal Business Object [ItemCollection](../core/itemcollection.html). Business objects can be represented different formats:
 
 * text/html
 * application/xml
 * application/json
 * application/x-www-form-urlencoded

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
			<value xsi:type="xs:string">more data...</value>
		</item>
	</document>

You will find details about the XML Schema in the [section XML](../core/xml/index.html).

### JSON Business Object

The following example shows the same business object in JSON representation which can be used for request and response objects:

    {"item": [
        {"name": "$modelversion", "value": ["1.0.1"] },
        {"name": "$taskid", "value": [2000] },
        {"name": "$eventid", "value": [10] },
        {"name": "_subject", "value": ["some data...","more data..."] }
     ]}

This is called 'JSON Mapped Notation'.      

#### Typed JSON Format

The disadvantage of the _JSON Mapped Notation_ is that you can't define the value type mapped to a java object. The Imixs Rest API supports an alternative notatin called _JSON BADGERFISH NOTATION_. This more expressive notation allows the description of complex business objects providing different xs data types. This is more precise and an equivalent for the XML format. See the following example: 

    {"item":[
     {"name":"$modelversion","value":{"@type":"xs:string","$":"1.0.1"}},
     {"name":"$taskid","value":{"@type":"xs:int","$":"1000"}}, 
     {"name":"$eventid","value":{"@type":"xs:int","$":"10"}}, 
     {"name":"_orderid","value":{"@type":"xs:long","$":"42000000001"}}
     {"name":"_subject","value":[
             {"@type":"xs:string","$":"some data"},
             {"@type":"xs:string","$":"more data"}
           ]}
    ]}  

The typed JSON format is supported only by the [workflow resource](workflowservice.html) (/workflow/) to post business data to be processed by the Imixs-Worklfow engine.
     
**Note:** Depending on the Rest Service Implementation the JSON format for response object can vary. 



	
The following sections gives an detailed description of all resource groups defined by the Imixs-Workflow REST Service API:
 
 
  * [Workflow Service](./workflowservice.html)
  * [Model Service](./modelservice.html) 
  * [Report Service](./reportservice.html) 
  * [Document Service](./documentservice.html) 
    