#The Document Service
 The main resource /documents is uses to read documents through the Imixs Rest Service Interface.
 
 
## The /documents GET resources
The /documents resources are used to get direct access to the documentService EJB:

| URI                                     | Description                               | 
|-----------------------------------------|-------------------------------------------|
| /{uniqueid}         | returns a single document defined by $uniqueid   |
| /search/{query}          | Returns a result set of documetns by a lucene search query   |
| /count/query/{query}    | the count of documents returned by a lucene search query      |

 


## POST/DELETE Document
The following resource URIs are used to PUT and POST a document:


| URI                          | Description                               | 
|------------------------------|-------------------------------------------|
| /        | posts a document to be stored by the  DocumentService. The post data can be x-www-form-urlencoded or in xml format   |
| /{uniqueid}  | deletes a document  |



## Resource Options
You can specify additional URI paramters to filter the resultset  or to navigate through a sub list of entities. Append optional arguments to define the number of entities returned by a URL, the starting point inside the list or the sort order. Combine any of the following arguments for the desired result. 

| option      | description                       | example               |
|-------------|---------------- ------------------|-----------------------|
| pagesize    | number of documents returned      | ..?pagesize=10           |
| pageindex   | page index to start               | ..?pageindex=5&pagesize=10   |
| sortBy	  | sort item 					      | ..&sortBy=txtworkflowstatus        |
| sortReverse | sort direction 				      | |
 
<strong>Note:</strong> Imixs-Workflow controls the access for each document by individual access lists.  The result of a query contains only documents which are not read protected and the current user has sufficient read access. 
        

##Administrative resource URIs

The Document Rest Service provides resource URIs for administrative purpose. To access these resources, the caller  must at least be in role "org.imixs.ACCESSLEVEL.MANAGERACCESS". These administrative URIs should not be used in  general business logic.  
 
| METHOD |URI                           | Description                                                                               | 
|--------|------------------------------|-------------------------------------------------------------------------------------------|
| PUT 	 | /backup/{query}        | creates a backup of the result set form a query. The entity list will be stored into the file system. The backup can be restored by calling the restore method | 
| GET    | /restore                    |restore a backup from the filesysem  |
| GET    |/configuration              | Returns the configuration details of the lucen index writer. | 




  
   