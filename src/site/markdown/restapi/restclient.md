# The Imixs RestClient

The Imixs-RestClient is a helper class to establish connections to a Rest Service Endpoint.
The client can be used for general HTTP communication and also for the Imxis-Rest API. 

## Core Methods

The RestClient provides the following GET and POST core methods:

| Method                         | Description                                                         | 
|--------------------------------|---------------------------------------------------------------------|
| get(url)                       | Gets the content of a GET request from a Rest Service URI Endpoint. I case of an error the method throws a RestAPIException.
| get(url)                       |Posts a String data object with a specific Content-Type to a  Rest URI Endpoint. This method can be used to simulate different post scenarios.
| registerRequestFilter(RequestFilter)  | adds a request filter                                |




## Imixs Rest-API Data Methods

The following methods can be used to get and process Imxis Workflow Data

| Method                         | Description                                                         | 
|--------------------------------|---------------------------------------------------------------------|
| postDocument(String, ItemCollection)     | Posts an Imixs ItemCollection to a Rest Service URI endpoint. |
| postXMLDocument(String, XMLItemCollection)     | Posts an Imixs XMLDocument to a Rest Service URI endpoint. |
| postXMLDataCollection(String, XMLDataCollection)     | Posts an Imixs XMLDataCollection to a Rest Service URI endpoint. |
| postJSON(String, String)     | Posts a JSON String to a Rest Service URI endpoint. |
| postCollection(url, DocumentCollection)   |POST MEHTHOD  for a document collection |
| getDocument(url)              | Returns a ItemCollection from a rest service endpoint.   |
| getXMLDocument(url)              | Returns a XMLDocument from a rest service endpoint.  |




This is a simple example how to request the tasklist of a user:

	// create RestClient ....
		RestClient restClient = new RestClient();
		try {
			List<ItemCollection> documents = restClient.
			     getDocumentCollection("http://localhost:8080/api/workflow/tasklist/owner/admin");
			logger.info("Read " + documents.size() + " documents");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

## RequestFilter & Authentication

The Imixs-RestClient supports also custom "RequestFilters". A Requestfilter implements the interface _org.imixs.workflow.services.rest.RequestFilter_ and can be used to handle a HTTP Request. 


	public class MyFilter implements RequestFilter {
	
		public void filter(HttpURLConnection connection) throws IOException {
			// your code goes here...
		}
	}

There a several Request filters available to be used for authentication:

 * BasicAuthenticator - for a BASIC authentication
 * FormAuthenticator - for a form based authentication with a JSESSION cookie
 * JWTAuthenticator - for authentication based on JSON Web Tokens

The following example shows how to use a BASIC authentication filter:

	RestClient restClient = new RestClient();
	// create a default basic authenticator
	BasicAuthenticator basicAuth = new BasicAuthenticator("myuser","mypassword");
	// register the authenticator
	restClient.registerRequestFilter(basicAuth);
	...		
			
			
