# The Document Service
The resource _/documents_ provides methods to read, create and modify documents through the Imixs-Rest API.
 
 
## GET Document
The GET method is used to read a document resource:


| URI                     | Method | Description                                                        | 
|-------------------------|--------|------------------------------------------------------------|
| /{uniqueid}             | GET    | returns a single document defined by $uniqueid                     |

## GET Documents by a Search Phrase 
The GET method is used to read and search for a document resource or a set of documents:

### Resource Options
With the following optional URI parameters the GET request can be filtered and sorted:

| option      | description                       | example             		|
|-------------|---------------- ------------------|-----------------------------|
| pagesize    | number of documents returned      | ..?pagesize=10           	|
| pageindex   | page index to start               | ..?pageindex=5&pagesize=10  |
| sortBy	  | sort item 					      | ..&sortBy=txtworkflowstatus |
| sortReverse | sort direction (ascending/descending)   | ..&sourtReverse=true		|
 
See details about the search in the section [Search Index](../engine/luceneservice.html).

## GET Documents by JPQL
Optional the  resource _/documents_ provides the sub-resource _/jpql_ to select a set of documents  selected by a JPQL statement:


| URI                     | Method | Description                                                | 
|-------------------------|--------|------------------------------------------------------------|
| /jpql/{query}           | GET    | Returns a result set of documents by a JQPL statement      |


### Resource Options
With the following optional URI parameters a sub result can be selected:

| option      | description                             | example               |
|-------------|---------------- ------------------------|-----------------------|
| firstResult | 1st element of the query to be returned | ..?firstResult=10     |
| maxResult   | maximum number of documents returned    | ..?maxResult=5  		|
 
 
See the [Document Service](../engine/documentservice.html) for details.




## POST/DELETE a Document
The methods PUT, POST and DELETE allow to create, modify and delete a document:


| URI          | Method      | Description                               | 
|--------------|-------------|------------|
| /            | POST, PUT   | posts a document to be stored by the  DocumentService. The post data can be x-www-form-urlencoded or in xml format   |
| /{uniqueid}  | POST, DELETE | updates ore deletes a document  |




## Administrative resource URIs

The Document Rest Service provides resource URIs for administrative purpose. To access these resources, the caller  must at least be in role "org.imixs.ACCESSLEVEL.MANAGERACCESS". These administrative URIs should not be used in  general business logic.  
 
| METHOD |URI                     | Description                                                                        | 
|--------|------------------------|------------------------------------------------------------------------------------|
| PUT 	 | /backup/{query}        | creates a backup of the result set form a query. The entity list will be stored into the file system. The backup can be restored by calling the restore method | 
| GET    | /restore               |restore a backup from the filesystem  |
| GET    | /configuration         | Returns the configuration details of the lucene index writer. | 



## The Access Control 


Imixs-Workflow controls the access for each document by individual access lists see the section [ACL](../engine/acl.html). The result of a GET request will return only documents which are not read protected or the current user has sufficient read access. 
     
   