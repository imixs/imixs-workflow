# The Imixs RestClient

The Imixs-RestClient is a helper class to establish connections to the Imixs Rest API.
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
		

## Set Credentials

The Imixs-RestClient also upports BASIC authentication. In this case the *principal* and *credentials* can be set. 

	RestClient restClient = new RestClient();
	restClient.setUser("admin");
	restClient.setPassword("password");
	...		