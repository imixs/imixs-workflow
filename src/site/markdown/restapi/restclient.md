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

The following methods can be used to GET and POST Imxis Workflow Data

| Method                         | Description                                                         | 
|--------------------------------|---------------------------------------------------------------------|
| setServiceEndpoint(String)     | builds the serviceEndpoint based on a given URI |
| setEncoding(String)            | defines the encoding used for post requests  |
| setRequestProperty(String, String)     | Set a single header request property. |
| post(String, String, String)     | Posts a String data object with a specific Content-Type to a Rest Service UR Endpoint |
| postCollection(url, DocumentCollection)   |POST MEHTHOD  for a document collection |
| get(url)              |  Gets the content of a GET request from a Rest Service URI Endpoint. |


The following example shows how to post a workitem:

	ItemCollection workitem = new ItemCollection().model(MODEL_VERSION).task(1000).event(10);
	workitem.replaceItemValue("_subject", "some data");
    // create client
    org.imixs.workflow.services.rest.RestClient restCLient = 
          new org.imixs.workflow.services.rest.RestClient(BASE_URL);
    // process workitem
    String resultData = restCLient.post(BASE_URL + "workflow/workitem",
                       XMLDocumentAdapter.writeItemCollection(workitem), 
                       MediaType.APPLICATION_XML,	MediaType.APPLICATION_XML);
    List<ItemCollection> result = XMLDataCollectionAdapter.readCollection(resultData.getBytes());




The next example shows how to request the tasklist of a user:

    // create client
    org.imixs.workflow.services.rest.RestClient restCLient = 
          new org.imixs.workflow.services.rest.RestClient(BASE_URL);
          
	String resultData = restCLient.get(BASE_URL + "workflow/tasklist/creator/admin");
    List<ItemCollection> result = XMLDataCollectionAdapter.readCollection(resultData.getBytes());
		

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
			
# The Imixs Melman Project

The [Imixs Melman Project](https://github.com/imixs/imixs-melman) provides a more convenient  component library to interact with the Imixs-Workflow Rest API. The project is agnostic from an Imixs-Workflow Implementation and can be used in a microservice architecture. The components are based on Java JAX-RS and JAX-B.

[https://github.com/imixs/imixs-melman](https://github.com/imixs/imixs-melman)


