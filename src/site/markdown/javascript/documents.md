# Documents

Imixs-Script provides the object 'ItemCollection' which provides functionality to work with documents. A document is a set of items haveing a name, a value list and an xsi:type. The ItemColleciton provides methods to access and manges items of a document:


	var myDocument=new imixs.ItemCollection();
	myDocument.setItem('name','Bill');
	myDocument.setItem('age','57','xs:int');  


A item value can be accessd by the getItem methhod:

	var name=myDocument.getItem('name'); // results to 'Bill'

## Create an ItemCollection from XML 

You can also create an ItemCollection from a Imixs XML Data Result in a ajax response:


	xmlData=imixsXML.xml2document(response)
	workitem=new imixs.ItemCollection(xmlData);
	console.log('workflowgroup=' + workitem.getItem('$workflowgroup'));
		                    	

	
## Mulitvalues

An item of a ItemCollection can also contain a value list. 

	var values=[];
	values=myDocument.getItemList('teamlist');


## Working with Date Fields

Imixs-Script provides an easy way to work with Date fields which can be send to the back-end service as Date values. 
Therefor the Imixs-Script ItemCollection object provides a way to set the 'xsi:type' of a item to 'xs:dateTime'. Imixs-Script will handle those items as date objects. 

	myDocument=new ItemCollection();
	myDocument.setItem('datfrom','','xs:dateTime');


To edit such a date item the jQuery datepicker can be used. 

	<input type="text" class="imixs-date" 
			data-ben-model="getItem('datfrom')"></input> 
									
The Imixs-Script method 'imixsLayout()' will convert a input field with the class 'imixs-date' automatically into a jQuery date picker widget. (see section 'Layout')

The imixs-ui method 'convertDateTimeInput' automatically converts the date input values provided by the jQuery DatePicker into a ISO 8601 format. The converted ItemCollection an be send to the Imixs-Workflow Rest API.

	 // convert date objects into ISO 8601 format
	 imixsUI.convertDateTimeInput(myController.model);
	 console.log("new ISO format=" + myController.model.getItem('datfrom')); 

So a date value of German format '15.08.2016' will be converted into the ISO format '2016-08-15'. 
To convert the date values the imixs-ui makes use of the jQuery method '$.datepicker.parseDate'.


