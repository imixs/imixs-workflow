#The EntityService Interface
The Imixs EntityService is used to save and load instances of the Imixs Data Object _org.imixs.workflow.ItemCollection_ into a Database. The Service Bean is based on the Java Persistence API (JPA) which provides a simple and powerful API to store and load Entity Beans. 
 
The class [ItemCollection](../core/itemcollection.html) is the general Data Object used by the Imixs Workflow Core API. Each Workitem processed by the Imixs Workflow Manager is represented as an ItemCollection. The Imixs EntityService provides methods to query Entities using JPQL statements. This makes the service EJB very flexible to access datasets of ItemCollections.  Additional the EntityService can be used to restrict the read- and write access for ItemCollection objects by providing accessvalues.

The following example shows how an instance of an ItemCollection can be saved using the Imixs EntityService:
 
	  @EJB
	  org.imixs.workflow.jee.ejb.EntityService entityService;
	  //...
	
	  ItemCollection myItemCollection=new ItemCollection;
	  myItemCollection.replaceItemValue("type","product");
	  myItemCollection.replaceItemValue("name","coffee");
	  myItemCollection.replaceItemValue("weight",new Integer(500));
	  	
	  // save ItemCollection
	  myItemCollection=entityService.save(myItemCollection);

In this example a new ItemCollection is created and two properties ('name' and 'age') are stored into the ItemCollection. The save() method stores the ItemCollection into the database. If the ItemCollection is stored the first time, the method generates an ID which can be used to identify the ItemCollection for later access. This ID is provided in the property '$uniqueid' which will be added by the Entity Service. If the ItemCollection was saved before the method updates the ItemCollection stored in the database.
  
The next example shows how to use the $uniqueid of a stored ItemCollection to load the ItemCollection from the EntityService. For this the ID is passed to the load() method.
 
	  @EJB
	  org.imixs.workflow.jee.ejb.EntityService entityService;
	  //...
	  // save ItemCollection
	  myItemCollection=entityService.save(myItemCollection);
	  // get ID from ItemCollection 
	  String id=myItemCollection.getItemValueString("$uniqueid");
	  // load ItemCollection from database
	  myItemCollection=entityService.load(id);
 
The Imixs EntityService also create TimeStamps to mark the creation and last modified date. These properties are also included in the ItemCollection returned by the save method. The properties are named "$created" and "$modified".
 
	  //...
	  // save ItemCollection
	  myItemCollection=entityService.save(myItemCollection);
	  Date created=myItemCollection.getItemValueDate("$Created");
	  Date modified=myItemCollection.getItemValueDate("$Modified");
  
## How to query entities
As the Imixs EntityService stores all entities using the Java Persistence API (JPA), it is also possible to use the Java Persitence Query Language (JPQL) so select a collection of ItemCollection objects. In the example before the ItemCollection saved by the Imixs EntityService provides the property "type" with the value "product". With the following example you can select all ItemCollection Objects of this type:
  
	  //...
	  String sQuery = "SELECT wi FROM Entity as wi WHERE wi.type='product' ";
	  Collection<ItemCollection> col = entityService.findAllEntities(sQuery, 0, -1);
	  for (ItemCollection aworkitem : colEntities) {
	    //.....
	  }

It is also possible to create complex queries to select a subset of ItemCollection objects with specific properties. The following query selects all products with a weight of 500
  
	  SELECT product from Entity AS product
	  JOIN product.integerItems AS t
	  WHERE product.type='product'
	  AND t.itemName = 'weight'
	  AND t.itemValue = '500' 
  
See the section [JPQL](./queries.html) for more details.

 
  
## How to count elements of a JPQL Query 
As the Imixs EntityService provides the method countAllEntities() to compute the max count of a  specific JPQL query.  The method expects the same JPQL query as for the findAllEntities() method but returns only the count of entities. The method counts only ItemCollections which are readable by the CallerPrincipal.  With the startpos and count parameters it is possible to read chunks of entities. The jPQL Statement must match the  conditions of the JPA Object Class Entity.
	
 