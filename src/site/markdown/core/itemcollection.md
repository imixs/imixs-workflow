#The ItemCollection
The Imixs ItemCollection is the core data object used by the Imixs-Workflow API.  Each Workitem processed by the Imixs Workflow is represented by an instance of ItemCollection.  But also other Entities used by the Imixs Workflow - like the Model Entities - are represented as instances of an ItemCollection.

An ItemColleciton is a kind of document with a set of properties (Items).  Each Item of an ItemCollection has a name and a value.   The value of an Item can be any java based data object which is serializeable.  The ItemCollection provides methods which makes it very easy to create, access or modify   the properties of an ItemCollection.  

    import org.imixs.workflow.ItemCollection;
     
    ItemCollection myItemCollection=new ItemCollection;
    myItemCollection.replaceItemValue("name","Anna");
    myItemCollection.replaceItemValue("age",new Integer(40));

This code example demonstrates the usage of an ItemCollection. The code creates a new empty ItemCollection and adds two new items.   The first Item with the "name" contains a String value and the second item "age" contains an Integer value.
  
There are also methods to access items values of an ItemCollection. Some of the  methods are Type save and allow to access a value of an specific type
  
    String name=myItemCollection.getItemValueString("name");
    int age=myItemCollection.getItemValueInteger("age");
  

## Multivalue Attributes
A value of an Item can also be a collection of objects. Multi values can be added to an ItemCollection in a List object:

    Vector multiValue=new Vector();
    multiValue.add("Anna");
    multiValue.add("John");
    myItemCollection.replaceItemValue("team", multiValue);

  
This code example adds a teamlist with two String values as an item with the Name "team" to an ItemCollection.  
  
To access a multivalue item you can use the following method:
  
    List multiValue=myItemCollection.getItemValue("team");
    String name=(String)multiValue.firstElement();

When you access an Item of an ItemCollection with the method getItemValue()  the ItemCollection returns always a List of values. So you will need to care about the type casting of the values stored in the List object.Using the getItemValue() method allows you also to store any type of serializable Java Object into a ItemCollection:
  
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
  
As an ItemCollection is also a Dataobject each ItemCollection can be stored in another ItemCollection. See the following example:
  
    ItemCollection teamMember=new ItemCollection();
    teamMember.replaceItemValue("name", "Ralph");
    teamMember.replaceItemValue("city", "munich");
    
    ItemCollection project=new ItemCollection();
    project.replaceItemValue("projectname", "my first workflow project");
    project.replaceItemValue("projectManager", teamMember);
		
In this code example the ItemCollection "teamMemer" is stored into the ItemCollection "project" with an item called "projectManager".
   
  
  