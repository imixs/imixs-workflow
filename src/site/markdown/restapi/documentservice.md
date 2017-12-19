# The Document Service
The resource _/documents_ provides methods to create, modify and read documents through the Imixs-Rest API.
 
 
## GET Documents
The GET method is used to read and search for documents:


| URI                     | Method | Description                                                        | 
|-------------------------|--------|------------------------------------------------------------|
| /{uniqueid}             | GET    | returns a single document defined by $uniqueid                     |
| /search/{query}         | GET    | Returns a result set of documents by a lucene search query         |
| /count/query/{query}    | GET    | the total hits of lucene search query                              |
| /countpages/query/{query}?pagesize= | GET    | the total pages of lucene search query for a given page size |

 


## POST/DELETE a Document
The methods PUT, POST and DELETE allow to create, modify and delete a document:


| URI          | Method      | Description                               | 
|--------------|-------------|------------|
| /            | POST, PUT   | posts a document to be stored by the  DocumentService. The post data can be x-www-form-urlencoded or in xml format   |
| /{uniqueid}  | POST, DELETE | updates ore deletes a document  |



## Resource Options
With the following optional URI parameters a request can be filtered and sorted:

| option      | description                       | example               |
|-------------|---------------- ------------------|-----------------------|
| pagesize    | number of documents returned      | ..?pagesize=10           |
| pageindex   | page index to start               | ..?pageindex=5&pagesize=10   |
| sortBy	  | sort item 					      | ..&sortBy=txtworkflowstatus        |
| sortReverse | sort direction 				      | |
 
<strong>Note:</strong> Imixs-Workflow controls the access for each document by individual access lists (see section ACL](/engine/acl.html). The result of a query contains only documents which are not read protected or the current user has sufficient read access. 
        

##Administrative resource URIs

The Document Rest Service provides resource URIs for administrative purpose. To access these resources, the caller  must at least be in role "org.imixs.ACCESSLEVEL.MANAGERACCESS". These administrative URIs should not be used in  general business logic.  
 
| METHOD |URI                     | Description                                                                        | 
|--------|------------------------|------------------------------------------------------------------------------------|
| PUT 	 | /backup/{query}        | creates a backup of the result set form a query. The entity list will be stored into the file system. The backup can be restored by calling the restore method | 
| GET    | /restore               |restore a backup from the filesystem  |
| GET    |/configuration          | Returns the configuration details of the lucene index writer. | 




  
   