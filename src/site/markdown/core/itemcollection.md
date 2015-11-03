#The ItemCollection
The Imixs ItemCollection is a generic value object used by the Imixs-Workflow API. You can see an ItemCollection as a kind of document which contains a various set of properties (Items). Each Item of an ItemCollection consists of a name and a value. The value of an Item can be any serializeable data object. So an ItemCollection is a very flexible data structure. 

Each Workitem processed by the Imixs-Workflow engine is represented by an instance of an ItemCollection. But also other Entities used by the Imixs-Workflow - like the Model Entities - are represented as instances of an ItemCollection. The ItemCollection provides methods which makes it very easy to create, access or modify the properties of an ItemCollection. See the following example:  

    import org.imixs.workflow.ItemCollection;
     
    ItemCollection myItemCollection=new ItemCollection;
    myItemCollection.replaceItemValue("FirstName","Anna");
    myItemCollection.replaceItemValue("CostCenter",new Integer(4010));

The method 'replaceItemValue()' adds a new Item or replaces the value of an existing item. This code example demonstrates the usage of the replaceItemValue method. The code creates a new empty ItemCollection and adds two new items. The first Item with the name "FirstName" contains a String value and the second item "CostCenter" contains an Integer value. 

###Getter Methods
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
The value of an Item can also be a value list (multi-value). Multi-values can be added to an ItemCollection in a List object:

    List<String> multiValue=new ArrayList<String>();
    multiValue.add("Anna");
    multiValue.add("John");
    myItemCollection.replaceItemValue("team", multiValue);

  
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
   
  
###Get and Set all Items
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
|getProcessID()		| returns the $processID attribute of a workitem 	|
|getActivityID()	| returns the $activityID attribute of an workitem 	|
|setActivityID(id)	| set the $activityID attribute of a workitem 		|
|getModelVersion()	| returns the $modelversion attribute of a workitem |
|getUniqueID()		| returns the $uniqueid attribute of a workitem 	|

For more information see the javaDoc.