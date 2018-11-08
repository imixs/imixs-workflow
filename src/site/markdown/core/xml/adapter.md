# XML Adapter Classes

The Imixs-Core API provides adapter classes to transform a Imixs ItemCollection into a XML object and vice versa.

## XMLDocumentAdapter

The XMLDocumentAdapter can be used to transform a Imixs ItemCollection in XML or transform a XML source into a ItemCollection:

	ItemCollection workitem;
	....
	XMLDocument xmlData = XMLDocumentAdapter.getDocument(workitem);
	...


## XMLDataCollectionAdapter     

The XMLDataCollectionAdapter can be used to transform a List of Imixs ItemCollection elements in XML or transform a XML source into a List of ItemCollection:

	List<ItemCollection> dataList;
	...
	XMLDataCollection xmlData = XMLDataCollectionAdapter.getDataCollection(dataList);
	...

The next example shows how to read a XML source into a XMLDataCollectionAdapter


	List<ItemCollection> col = null;
	try {
		col = XMLDataCollectionAdapter
				.readCollectionFromInputStream(getClass().getResourceAsStream("/document-example.xml"));
	} catch (JAXBException e) {
		Assert.fail();
	} catch (IOException e) {
		Assert.fail();
	}

You can also write the data into a byte array using the XMLDataCollectionAdapter:

	byte[] data = null;
	try {
		data = XMLDocumentAdapter.writeItemCollection(itemColSource);
		Assert.assertTrue(data.length > 100);
	} catch (JAXBException | IOException e) {
		Assert.fail();
	}