# The Document Service
The resource _/documents_ provides methods to read, create and modify documents.
 
Imixs-Workflow controls the access for each document by individual access list. The result of a GET request will return only documents which are not read protected or the current user has sufficient read access. 
A POST/PUT request will be rejected if the current user has insufficient write access.
See the section [ACL](../engine/acl.html) for details.  
 
## GET Document
The GET method can be used to read a single document resource by its UniqueID:


| URI                     | Method | Description                                                        | 
|-------------------------|--------|--------------------------------------------------------------------|
| /{uniqueid}             | GET    | returns a single document resource defined by its $uniqueid        |

**Example:**

	/api/documents/ed8ad2c7-f460-113d-a9b5-bb71bf61cf09

## GET Document Collection
The sub-resource _/search/_ can be used to search for a set of documents using the [search index](../engine/luceneservice.html):


| URI                     | Method | Description                                                        | 
|-------------------------|--------|--------------------------------------------------------------------|
| /search/{query}         | GET    | Returns a result set of documents by a lucene search query         |


**Example:**

	/api/documents/search/type:"workitem"





### Resource Options

With the following optional URI parameters the GET request can be filtered and sorted:


| option                  | description | example                                                        | 
|-------------------------|-------------|--------------------------------------------------------------------|
| pageSize    | number of documents returned      | ..?pagesize=10           	|
| pageIndex   | page index to start               | ..?pageindex=5&pagesize=10  |
| sortBy	  | sort item 					      | ..&sortBy=txtworkflowstatus |
| sortReverse | sort direction (ascending/descending)   | ..&sourtReverse=true		  |
| items       | filter item values to be returned | ..&items=$taskid,$modellversion   |
| format      | optional output format JSON/XML   | ..&format=json   or   &format=xml |


 
 
**Example:**

	/api/documents/search/type:"workitem"?pageSize=10&pageIndex=2
 
See details about the search in the section [Search Index](../engine/luceneservice.html).



### Count documents

With the _sub-resouces_ '/count/' and '/countpages/' the result size can be requested:

| URI                     | Method | Description                                                        | 
|-------------------------|--------|--------------------------------------------------------------------|
| /count/{query}    | GET    | the total hits of lucene search query                        		    |
| /countpages/{query}?pagesize= | GET    | the total pages of lucene search query for a given page size |





### GET Documents by Java Persistence Query Language
Optional the  resource _/documents_ provides the sub-resource _/jpql_ to select a set of documents using the Java Persistence Query Language (JPQL). JPQL can be used to select documents independent from the existence of the Lucene search index.


| URI                     | Method | Description                                                | 
|-------------------------|--------|------------------------------------------------------------|
| /jpql/{query}           | GET    | Returns a result set of documents by a JQPL statement      |



Example:

	/api/documents/jpql/SELECT document FROM Document AS document WHERE document.type='workitem'
 
See the [Document Service](../engine/documentservice.html) for details.




## POST/PUT/DELETE a Document
The methods POST, PUT and DELETE allow to create, modify and delete a document:


| URI          | Method | Description                              | 
|--------------|--------|------------|-----------------------------|
| /            | POST   | posts a new document to be stored by the  DocumentService. The post data can be x-www-form-urlencoded or in xml format   |
| /{uniqueid}  | PUT	| updates  a document. The post data can be x-www-form-urlencoded or in xml format  					|
| /{uniqueid}  | DELETE | deletes a document  						|




## Administrative resource URIs

The Document Rest Service provides resource URIs for administrative purpose. To access these resources, the caller  must at least be in role "org.imixs.ACCESSLEVEL.MANAGERACCESS". These administrative URIs should not be used in  general business logic.  
 
| METHOD |URI                     | Description                                                                        | 
|--------|------------------------|------------------------------------------------------------------------------------|
| PUT 	 | /backup/{query}        | creates a backup of the result set form a query. The entity list will be stored into the file system. The backup can be restored by calling the restore method | 
| GET    | /restore               |restore a backup from the filesystem  |
| GET    | /configuration         | Returns the configuration details of the lucene index writer. | 



     
   