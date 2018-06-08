# The ItemCollection
The Imixs ItemCollection is a generic value object used by the Imixs-Workflow API. You can see an ItemCollection as a kind of document which contains a set of properties (called _Items_). Each Item of an ItemCollection consists of a name and a value. The value of an Item can be any serializeable data object. So an ItemCollection is a very flexible data structure. 

Each Workitem processed by the Imixs-Workflow engine is represented by an instance of an ItemCollection. But also other Entities used by the Imixs-Workflow - like the Model Entities - are represented in the same object structure. The ItemCollection provides methods which makes it very easy to create, access or modify the properties of an ItemCollection. See the following example:  

    import org.imixs.workflow.ItemCollection;
     
    ItemCollection myItemCollection=new ItemCollection;
    myItemCollection.replaceItemValue("FirstName","Anna");
    myItemCollection.replaceItemValue("CostCenter",new Integer(4010));
    myItemCollection.appendItemValue("team","mark");
    myItemCollection.appendItemValue("team","anna");

The method _replaceItemValue_ adds a new Item or replaces the value of an existing item. This code example creates a new empty ItemCollection and adds three new items. The first Item with the name "FirstName" contains a String value and the second item "CostCenter" contains an Integer value. 
For the third item 'team' the  method _appendItemValue_ is used, which allows to add multiple values into one item.  

### Getter Methods
The ItemCollection provides different methods to access items values. Some of the  methods are type-save and allow to access a value of a specific type. See the following example:
  
    String name=myItemCollection.getItemValueString("FirstName");
    int costcenter=myItemCollection.getItemValueInteger("CostCenter");

The following table gives an overview about the getter methods:


|Method						| Return Type  		|
|---------------------------|-------------------|
|getItemValueString(name)	| String			|
|getItemValueInteger(name)	| Integer			|
|getItemValueDouble(name)	| Double			|
|getItemValueFloat(name)	| Float				|
|getItemValueLong(name)		| Long				|
|getItemValueBoolean(name)	| Boolean (true/false)|
|getItemValueDate(name)		| Date Object		|
    
### Remove Attributes
With the method removeItem an existing Item can be removed from a ItemCollection:

    myItemCollection.removeItem("FirstName");

### Test Attributes
The ItemCollection provides also methods to test the existence of an Item. See the following table:

|Method						| Return 	  		|
|---------------------------|-------------------|
|hasItem(name)				| returns true if an Item with the name exists		|
|isItemValueInteger(name)	| returns true if the item contains an Integer value|
|isItemValueDouble(name)	| returns true if the item contains a Double value	|
|isItemValueFloat(name)  	| returns true if the item contains a Float	value	|
|isItemValueLong(name)		| returns true if the item contains an Long	value	|




### Multivalue Attributes
The value of an Item can also be a value list (multi-value). Multi-values can be added to an ItemCollection with the method _appendItemValue_ or as a List of objects:

    List<String> multiValue=new ArrayList<String>();
    multiValue.add("Anna");
    multiValue.add("John");
    myItemCollection.replaceItemValue("team", multiValue);

The method _appendItemValue_ creates the same list:

    myItemCollection.appendItemValue("team", "Anna");
    myItemCollection.appendItemValue("team", "John");

  
This code example adds a teamlist with two String values as an item with the Name "team" to an ItemCollection. To access a multi-value list you can use the method getItemValue() which always returns a List of values:
  
    List multiValue=myItemCollection.getItemValue("team");
    String name=(String)multiValue.firstElement();

Using the ItemCollection it is possible to store not only basic data types but also any serializable Java Object:
  
    // create a Java HashMap
    HashMap hashMap = new HashMap();
    hashMap.put( "One", new Integer(1) ); // adding value into HashMap
    hashMap.put( "Two", new Integer(2) );
    hashMap.put( "Three", new Integer(3) );
    // put data object into ItemCollection
    myItemCollection.replaceItemValue("MyData", hashMap);
    //....
   
    // read hashMap form itemCollection
    hashMap = (HashMap) myItemCollection.getItemValue("MyData").firstElement();
  
As an ItemCollection is not serializable it is not recommanded to store a ItemCollection as a value into another ItemCollection. Instead use the method getAllItems() which returns a serializable Map interface:
  
    ItemCollection teamMember=new ItemCollection();
    teamMember.replaceItemValue("name", "Ralph");
    teamMember.replaceItemValue("city", "munich");
    
    ItemCollection project=new ItemCollection();
    project.replaceItemValue("projectname", "my first workflow project");
    // get value map....
    project.replaceItemValue("projectManager", teamMember.getAllItems());
		
In this code example the values of the ItemCollection "teamMemer" are stored as a HashMap into the ItemCollection "project" with is mapped to the item "projectManager".


### Working with FileData

The class _org.imixs.workflow.FileData_ can be used to transfere files into a ItemCollection. A FileData object contains the following attributes:

 * name - the file name
 * content - a byte array of the file data
 * contentType - the file content type.

To attach a file into a ItemColleciton the method addFileData can be used:


	FileData fileData=new FileData("test.jpg",  content, "image/jpg");
	myItemCollection.addFileData(fileData);

To get an attached file form a ItemCollection the method getFileData can be used:


	myFileData=myItemCollection.getFileData("test.jpg");
	    
  
### Get and Set all Items
With the methods setAllItems() and getAllItems() it is possible to set and get the internal Map ValueObject. See the following example:

    // create a Java HashMap
    HashMap hashMap = new HashMap();
    hashMap.put( "FirstName", "Anna" ); 
    hashMap.put( "CostCenter", new Integer(2222) );
    // put data object into ItemCollection
    myItemCollection = new ItemCollection();
    myItemCollection.setAllItems(hashMap);
    // read value
    String firstname=myItemCollection.getItemValueString("firstname");

### Accessing Workflow Data   
The ItemCollection provides convenience methods to access typical attributes of a process iInstance. See the following table:

|Method				| Return 	  										|
|-------------------|---------------------------------------------------|
|getType()			| returns the type attribute of a workitem 			|
|getTaskID()		| returns the $taskID attribute of a workitem 	|
|getEventID()	| returns the $eventID attribute of an workitem 	|
|setEventID(id)	| set the $eventID attribute of a workitem 		|
|getModelVersion()	| returns the $modelversion attribute of a workitem |
|getUniqueID()		| returns the $uniqueid attribute of a workitem 	|


### Create an ItemCollection by Reference

The ItemCollection provides constructor methods to create or clone an existing ItemCollection:

	// create a ItemCollection from an existing instance
	ItemCollection myItemCollection=new ItemCollection(origin);

This method call is equal to clone an instance:

	// clone ItemCollection from an existing instance
	ItemCollection myItemCollection = (ItemCollection)origin.clone();

In both cases a deep copy of the given value map is created. A deep copy avoids write conflicts with value references. Particular in cases of embedded value arrays a deep copy is recommended. In cases where the values of a map are only read, a given value map can also be copied by reference. The method call _createByReference()_ has a better performance but copies the values by reference.

	// create a ItemCollection by reference
	ItemCollection myItemCollection = ItemCollection.createByReference(origin);
 	
It is recommended to use the constructor methods if the origin of the reference is not clear. 	
For more information see the [javaDoc](http://www.imixs.org/doc/apidocs/org/imixs/workflow/ItemCollection.html).