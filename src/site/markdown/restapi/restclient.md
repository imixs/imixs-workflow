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
			restClient.setRequestProperty("Accept", MediaType.APPLICATION_XML);
			restClient.get("http://localhost:8080/workflow/rest-service/workflow/tasklist/owner/admin");
			String xmlResult = restClient.getContent();
			JAXBContext context = JAXBContext.newInstance(DocumentCollection.class);
			Unmarshaller u = context.createUnmarshaller();
			StringReader reader = new StringReader(xmlResult);
			DocumentCollection xmlDocuments = (DocumentCollection) u.unmarshal(reader);
			List<ItemCollection> documents = XMLItemCollectionAdapter.getCollection(xmlDocuments);
			logger.info("Read " + documents.size() + " documents");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

## Credentials

The Imixs-RestClient also upports BASIC authentication. In this case the *principal* and *credentials* can be set. 

	RestClient restClient = new RestClient();
	restClient.setUser("admin");
	restClient.setPassword("password");
	...		

## Cookies

The RestClient provides methods to add or read Cookies from a given URI location:


| Method                         | Description                                                         | 
|--------------------------------|---------------------------------------------------------------------|
| getCookies()                       | Returns all cookies set during the last request  |
| setCookies(CookieManager cookieManager)   | Set the cookies to be used for the next request                              |
| readCookies(HttpURLConnection connection)     | reads cookies form a http connection                              |
| addCookies(HttpURLConnection connection)   | adds all cookies from the CookieManager into a http connection  |

