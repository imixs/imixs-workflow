#How to Query ItemCollections
Using the EntityService gives you a powerful method to query ItemCollections from your database. To query a set of ItemCollections you can define an JPQL Statement and call the findAllEntities method of the EntityService.
 
JPQL is similar to SQL in it's use of the select clause and the where clause. An JPQL statement in the Imixs JEE Workflow contains typically one view to the Entity Class and a optional set of where conditions.
 
    SELECT entity FROM Entity entity ORDER BY entity.created DESC

You can use the [Imixs Admin Client Tool](../administration.html) to test a Query against a running workflow instance.
 
<strong>Note:</strong> The EntityService cares about the individual read access of the user who is calling  the findAllEntities method. This means that also when the query will contains a large set of  ItemCollections the method returns only these ItemCollections the user has read access for! 
 
You can restrict the collection returned by the EntityService by the definition of a WHERE clause. The where clause can contain conditions to attributes of the Entity Class or properties of the ItemCollection represented by the Entity. 
 
    SELECT entity FROM Entity entity WHERE entity.type='person'
    ORDER BY entity.created DESC
  
This Query selects all ItemCollections from the type "person". Each ItemCollection returned by this statement contains a property 'type' with the value 'person'. The property 'type' is a standard property of the Entity class. This is the reason because you can select this property directly.  All other properties of an ItemCollection stored by the EntityService can be selected if the property is defined as an IndexProperty. 

##Definition of an IndexProperty
 
To define a new IndexProperty you can use the [Imixs Admin Client Tool](../administration.html) or call the method addIndex of the EntityService. Adding a new Index allows the definition of complex JPQL Statements. 
 
You can define the PropertyName the new Index should be created for and the type of the IndexProperty. After adding a new Index the workflow system will be automatically reorganized.  So in large databases adding a new IndexProperty can take some minutes.
 
The programmatic way to add a new IndexProperty is a method call addIndex() from the EntityService. The methode can be called by an EJB or a simple Java Class. The method addIndex() adds an IndexProperty to the Workflow instance. The addIndex method expects the name of the property and the Index type: 

	@EJB
	    org.imixs.workflow.jee.ejb.EntityService entityService;
	.....
	        entityService.addIndex("firstname", EntityIndex.TYP_TEXT);
	        entityService.addIndex("lastname", EntityIndex.TYP_TEXT);
	        entityService.addIndex("amount", EntityIndex.TYP_INTEGER);
	        entityService.addIndex("summary", EntityIndex.TYP_DOUBLE);
	        entityService.addIndex("date", EntityIndex.TYP_CALENDAR);
        
 
This example shows who to define different IndexProperties of different data types.
To select a set of ItemCollections containing IndexProperties allows you the define an JPQL statement with a WHERE clause to filter specify ItemCollections. As each IndexProperty is a sub stream (equals to a seperate table in SQL) you need to define a JOIN to each IndexProperty you want to define in a select statement.
 
	 SELECT project from Entity AS project
	  JOIN project.textItems AS t
	  WHERE project.type='project'
	  AND t.itemName = 'projectname'
	  AND t.itemValue = 'Web Application' 
 
This example selects all ItemCollections from the type 'project' containing an IndexProperty with the name "projectname" and the value "Web Application"
 
##Standard Properties 
Each ItemCollection stored by the EntityService contains a set of standard Properties which did not  need to be defined by an IndexProperty. These Properties are:
 
 * type  - String value of the ItemCollection property type (default = "Entity")
 * created - the point of time where the ItemCollection was first saved by the EntityService 
 * modified - point of time where the ItemCollection was last saved by the EntityService
 * readAccess - a Entity List restricting the ReadAcces to an ItemCollection. Also represented by the Property "$readaccess"
 * writeAccess - a Entity List restricting the WriteAcces to an ItemCollection. Also represented by the Property "$writeaccess"
   
	 SELECT project from Entity AS project
	  JOIN project.writeAccess AS writeaccess
	  WHERE project.type='project'
	  AND writeaccess.value IN ('projectowner', 'projectmanager')

This example selects all ItemCollections from the type 'project' and a write access restriction to  the roles 'projectowner' OR 'projectmanager'
 
<strong>Note:</strong> It is not necessary to define a JPQL Statement with an WHERE clause selecting the property  readaccess. This is because the EntityService contains an Query optimizer which automatically returns only ItemCollection which are accessable to the caller principal. So if you select a Set of ItemCollections the size of the resultset depends on the read restrictions and the  access level of the caller pricipal. This is one of the main security features  of the Imixs JEE Workflow Implementation.
     
## Multiple Joins
You can select ItemCollections with a lot of different properties by defining more than one IndexProperty and using multi joins. The following query select only ItemCollections containg a specific projectname and a minimum team size:

	 SELECT project FROM Entity AS project
	 JOIN project.textItems AS t1
	 JOIN project.integerItems AS t2
	 WHERE project.type='project'
	  AND t1.itemName = 'projectname'
	  AND t1.itemValue IN ('Project-A','Project-B')
	  AND t2.itemName = 'teamsize'
	  AND t2.itemValue >= 10
	 ORDER BY project.created ASC

Notice that as more joins you use as longer the response time of the query will take to be computed.
 
 
## Subqueries
Subqueries are a powerful technique for solving the most complex query scenarios. A subquery is a complete select query inside a pair of parentheses that is embedded within a 
 conditional expression. The following example shows how subqueries work with the concept of IndexProperties.  The example is from a resource management system where rooms can be booked using a booking system. Each booking holds a reference to a booked room ($uniqueidRef). The query selects all rooms where no booking exists for a specified period (2008-09-05 to 2008-09-15)

	 SELECT room FROM Entity AS room
	 WHERE room.type = 'room'
	 AND room.id NOT IN (
	 SELECT tref.itemValue FROM Entity AS booking
	 join booking.textItems AS t1
	 join booking.calendarItems AS dvon
	 join booking.calendarItems AS dbis
	 join booking.textItems AS tref
	 WHERE t1.itemName = 'type' AND t1.itemValue = 'booking'
	 AND tref.itemName='$uniqueidref' 
	 AND dvon.itemName = 'datbookingvon' AND dbis.itemName = 'datbookingbis'
	 AND ( 
		 (dvon.itemValue BETWEEN '2008-09-5' AND '2008-09-15')
		 OR (dbis.itemValue BETWEEN '2008-09-5' AND '2008-09-15')
		 OR (dvon.itemValue <= '2008-09-5' AND dbis.itemValue >='2008-09-15')
		 )
	 ) 
 

 

    
 