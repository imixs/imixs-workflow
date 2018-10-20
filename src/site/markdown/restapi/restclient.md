# The Imixs RestClient

The Imixs-RestClient is a helper class to establish connections to the Imixs Rest API.


The RestClient provides the following GET and POST methods:



| Method                         | Description                                                         | 
|--------------------------------|---------------------------------------------------------------------|
| get(url)                       | GET METHOD   |
| setCredentials(userid,password)  | set credentials (username, password)                               |
| getContent()                   | get last response content                               |
| postEntity(url, XMLItemCollection)     | POST MEHTHOD  for one document  |
| postCollection(url, DocumentCollection)   |POST MEHTHOD  for a document collection |
| getDocument(url)              | GET METHOD to read one document    |
| getDocumentCollection(url)              | GET METHOD to read a document  collection  |




This is a simple example how to request the tasklist of a user:

	// create RestClient ....
		RestClient restClient = new RestClient();
		try {
			List<ItemCollection> documents = restClient.
			     getDocumentCollection("http://localhost:8080/workflow/rest-service/workflow/tasklist/owner/admin");
			logger.info("Read " + documents.size() + " documents");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

## RequestFilter & Autentication

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
			
			
