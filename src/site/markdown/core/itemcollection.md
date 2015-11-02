#The ItemCollection
The Imixs ItemCollection is a generic value object used by the Imixs-Workflow API. You can see an ItemCollection as a kind of document which contains a various set of properties (Items). Each Item of an ItemCollection consists of a name and a value. The value of an Item can be any serializeable data object. So an ItemCollection is a very flexible data structure. 

Each Workitem processed by the Imixs-Workflow engine is represented by an instance of an ItemCollection. But also other Entities used by the Imixs-Workflow - like the Model Entities - are represented as instances of an ItemCollection. The ItemCollection provides methods which makes it very easy to create, access or modify the properties of an ItemCollection. See the following example:  

    import org.imixs.workflow.ItemCollection;
     
    ItemCollection myItemCollection=new ItemCollection;
    myItemCollection.replaceItemValue("FirstName","Anna");
    myItemCollection.replaceItemValue("CostCenter",new Integer(4010));

This code example demonstrates the usage of an ItemCollection. The code creates a new empty ItemCollection and adds two new items. The first Item with the name "FirstName" contains a String value and the second item "CostCenter" contains an Integer value. There are also methods to access items values of an ItemCollection. Some of the  methods are type-save and allow to access a value of a specific type. See the following example:
  
    String name=myItemCollection.getItemValueString("FirstName");
    int costcenter=myItemCollection.getItemValueInteger("CostCenter");
  

## Multivalue Attributes
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
   
  
  