#The JSON Imixs REST Service
The Imixs-Workflow REST API supports also the JSON format.  So a workitem can be read in JSON format or new data can be processed and updated when provided in JSON format. The following section gives an overview how to use the REST API with JSON.
 
 
## The JSON Workitem Format
The Imixs Workflow JSON format for a workitem is based on the  JAX-B representation of a ItemCollcation provided by the Imixs-Core-API.  A single workitem contains a list of items. Each item has a name and a value attribute. The value can be assigned with a specific *xsd data type* which will be reflected to the back-end. This format is called the *BadgerFish Convention*.
 
See the following example:
 
	{"item":[
	   {"name":"$uniqueid","value":{"@type":"xs:string","$":"141cb98aecc-18544f1b"}},
	   {"name":"$modelversion","value":{"@type":"xs:string","$":"bestandspflege-de-0.0.2"}},
	   {"name":"$taskid","value":{"@type":"xs:int","$":"2000"}},
	   {"name":"namcreator","value":{"@type":"xs:string","$":"admin"}}, 
	   {"name":"$isauthor","value":{"@type":"xs:boolean","$":"true"}},
	   {"name":"_subject","value":{"@type":"xs:string","$":"JUnit Test-6476"}}, 
	   {"name":"txtworkflowstatus","value":{"@type":"xs:string","$":"Vorlauf"}}, 
	   {"name":"txtworkflowresultmessage","value":{"@type":"xs:string","$":""}}
	  ]}


##Creating a new process instance

To create a new process instance with a JSON request object, the POST method of the Imixs-Workflow REST API can be used in the following way: 
 
  * URI= http://localhost/workflow/rest-service/workflow/workitem.json
  * METHOD=POST
  * MEDIA TYPE= application/json

 To create a valid workitem the following attributes are mandatory: 

  * $modelversion
  * $processid
  * $activityid


	  {"item":[
	     {"name":"$modelversion","value":{"@type":"xs:string","$":"bestandspflege-de-0.0.2"}},
	     {"name":"$taskid","value":{"@type":"xs:int","$":"2000"}}, 
	     {"name":"$eventid","value":{"@type":"xs:int","$":"1"}}, 
	     {"name":"_subject","value":{"@type":"xs:string","$":"JUnit Test-6476"}}
	   ]}  



##Java RestClient Example
Using the RestClient provided by the Imixs-Workflow Core API the creation of a new workitem can be tested. See the following example code:
 
	package org.imixs.workflow.jee.jaxrs;
	
	import org.imixs.workflow.ItemCollection;
	import org.imixs.workflow.services.rest.RestClient;
	import org.imixs.workflow.xml.XMLItemCollection;
	import org.imixs.workflow.xml.XMLItemCollectionAdapter;
	import org.junit.Assert;
	import org.junit.Test;
	
	public class TestRestAPIWorkflow {
	
		static String USERID = "Manfred";
		static String PASSWORD = "manfred";
	
		@Test
		public void testPostJsonWorkitem() {
	
			RestClient restClient = new RestClient();
	
			restClient.setCredentials(USERID, PASSWORD);
	
			String uri = "http://localhost:8080/workflow/rest/workflow/workitem.json";
	
			// create a json test string
			String json = "{\"item\":["
					+ "	{\"name\":\"$taskid\",\"value\":{\"@type\":\"xs:int\",\"$\":\"10\"}},"
					+ "	{\"name\":\"$eventid\",\"value\":{\"@type\":\"xs:int\",\"$\":\"10\"}},"
					+ "	{\"name\":\"txtList\",\"value\":[{\"@type\":\"xs:string\",\"$\":\"A\"},{\"@type\":\"xs:string\",\"$\":\"B\"}]},"
					+ "	{\"name\":\"txtname\",\"value\":{\"@type\":\"xs:string\",\"$\":\"workitem json test\"}}"
					+ "]}";
			
			
			// http://www.jsonschema.net/
			try {
				int httpResult = restClient.postJsonEntity(uri, json);
	
				String sContent=restClient.getContent();
				// expected result 200
				Assert.assertEquals(200, httpResult);
				
				Assert.assertTrue(sContent.indexOf("$uniqueid")>-1);
			} catch (Exception e) {
	
				e.printStackTrace();
				Assert.fail();
			}
	
		}
		
	}


   