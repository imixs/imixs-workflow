#Post XML Data to the Imixs REST Service 
The following code example shows how to transform external xml data with a xsl
template provided in a file:

	private EntityCollection transform(Document xmldoc) throws TransformerException {
		DOMSource domSource =null;
		// the xml data source
		domSource = new DOMSource(xmldoc);
		// get the xsl template
		String sXSLPath="/home/imixs.xsl";
		StreamSource stylesource = new StreamSource(sXSLPath);
		// create a transformer factory
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer(stylesource);
		// create a ByteArray Output Stream
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		transformer.transform(domSource, new StreamResult(outputStream));
		// now unmarshal the result outputStream into a XML EntityCollection object...
	    JAXBContext context;
		try {
			context = JAXBContext.newInstance(EntityCollection.class);
	        Unmarshaller u = context.createUnmarshaller();
	        EntityCollection ecol = (EntityCollection) u.unmarshal(new ByteArrayInputStream(outputStream.toByteArray()));
	        return ecol;
		} catch (JAXBException e) {
			// Unable to unmarshal!!
			e.printStackTrace();
			return new EntityCollection();
		}
	}
  

To see how the transformation between an external XML data format an the Imixs EntityCollection  format works in detail, see the section [XSL Transformation](./transform_xml.html).
The result of the transformation can be posted via the Imixs REST API into the Imixs-Workflow engine. The Imixs-Workflow core API provides a RestClient which is easy to use. See the following code example:
  
  
	....
		/**
		 * This Method posts an Imixs EntityCollection to the RESTful service uri 
		 * http://localhost:8080/workflow/workitems
		 * 
		 * @param uri
		 * @param sUserID
		 * @param sPassword
		 * @param entCol
		 * @throws Exception
		 */
		public static int postWorkitems(String sUserID,
				String sPassword, EntityCollection entCol) throws Exception {
	
			if (entCol!=null && entCol.getEntity().length==0) {
				System.out
				.println("[ImixsWorkflow] ***** worklist is empty!");
				return 200;
			}
			// create RestClient ....
			restClient = new RestClient();
			restClient.setCredentials(sUserID, sPassword);
			// post the model to the provided location with additional model version
			int result=restClient.postCollection("http://localhost:8080/workflow/workitems", entCol);
			
			if (result>=200 && result<=299)
				System.out
				.println("[ImixsWorkflow] ***** postWorkitems finished - HTTP OK (" +result+")");
			else
				System.out
				.println("[ImixsWorkflow] ***** postWorkitems finished - HTTP ERROR (" +result+")");
			return result;
		}

 
 