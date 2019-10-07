# XML

The Imixs-Script library IMIXS.org.imixs.xml provides several methods to convert a imixs.ItemCollection into a XML object. XML Objects are used to exchange the data via the Imxixs Rest API. It is recommended to use XML objects instead of the JSON format to support  xsi object types.

The following example shows how to convert a document received form the Imixs Rest API into a imixs.ItemCollection:


	var url = "http://localhost:8080/workflow/rest-api/workflow/workitem/" + id;

	$.ajax({
		type : "GET",
		url : url,
		dataType : "xml",
		success : function(response) {
			console.debug(response);
			myDocument = imixsXML.xml2document(response);
		}
	});
		
The method json2xml can be used to convert a json object (ItemCollection) into a XML object to post to the back-end:

	var myDocument=new imixs.ItemCollection();
	myDocument.setItem('name','Bill');
	....
	// convert to xml
	var xmlData = imixsXML.json2xml(myDocument);
	var url = "http://localhost:8080/workflow/rest-api/workflow/workitem/";

	$.ajax({
		type : "POST",
		url : url,
		data : xmlData,
		contentType : "text/xml",
		dataType : "xml",
		cache : false,
		success : function(xml) {
			console.debug("success");
			....
		}
	});

	
	