#Marshalling an Imixs Data object
The JAXB runtime framework provides marshall and unmarshall operations. Marshalling is the process of converting instances of JAXB-annotated classes to XML representations. Likewise, unmarshalling is the process of converting an XML representation to a tree of objects. The following example shows how to marshal an Imixs XMLItemCollection into a XML Stream.
  
	   .....
		// create an simple ItemCollection with some data....
		ItemCollection itemCol=new ItemCollection();
		itemCol.replaceItemValue("txtTitel", "Hello world");
		itemCol.replaceItemValue("numAge", 40);
		itemCol.replaceItemValue("keyVisible", true);
		
	// convert the ItemCollection into a XMLItemcollection...
	XMLItemCollection xmlItemCollection= XMLItemCollectionAdapter.putItemCollection(itemCol);

	// marshal the Object into an XML Stream....
	StringWriter writer = new StringWriter();
	JAXBContext context = JAXBContext.newInstance(XMLItemCollection.class);
	Marshaller m=context.createMarshaller();
	m.marshal(xmlItemCollection,writer);
	System.out.println(writer.toString());
    .....

The next example shows how to unmarshal a XML Stream into a Imixs XML Data Object:
  
  
    	public void readStream(InputStream isXML) throws Exception {

			ItemCollection itemCollection;
			XMLItemCollection entity;
			// extract item collections from request stream.....
			JAXBContext context = JAXBContext.newInstance(EntityCollection.class);
			Unmarshaller u = context.createUnmarshaller();
			EntityCollection ecol = (EntityCollection) u.unmarshal(isXML);
			......
 
   
 <strong>Note: </strong> The JAXBContext must always match the XMLRoot Class to be marshaled or unmarshalled.
    