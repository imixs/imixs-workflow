#The Workflow Service

The resource _/workflow_ provides methods to create, process and read workitems through the Imixs-Rest API.
 
 

## GET a Workitem

The subresource _/workflow/workitem/_ provides GET methods to read the content of a workitem:


| URI                                           | Method | Description                               | 
|-----------------------------------------------|--------|-----------------------------------|
| /workflow/workitem/{uniqueid}                 | GET    | a single workitem represented by the   provided uniqueid                              |
| /workflow/workitem/{uniqueid}/file/{file}     | GET    | a file attachment located in the property   $file of the spcified workitem           |


## GET a Task List 
The subresource _/workflow/tasklist/_ provides GET methods to read collections of workitems:

| URI                                           | Method | Description                               | 
|-----------------------------------------------|--------|-----------------------------------|
| /workflow/worklist                            | GET    | a collection of workitems representing the worklist for the current user |             
| /workflow/tasklist/owner/{owner}              | GET    | a collection of workitems owned by a specific  user (or value 'null' for the current user)   |
| /workflow/tasklist/creator/{creator}          | GET    | a collection of workitems created by a specific user (or value 'null' for the current user)                           |
| /workflow/tasklist/processid/{processid}      | GET    | a collection of workitems in a specific    process state             |
| /workflow/tasklist/group/{processgroup}       | GET    | a collection of workitems in a specific    process group                             |
| /workflow/tasklist/ref/{uniqueid}             | GET    | a collection of workitems referenced to a  specific uniqueId (childs)                |




### Resource Options
With the following optional URI parameters the GET request can be filtered and sorted:

| option      | description                       | example             		|
|-------------|---------------- ------------------|-----------------------------|
| pageSize    | number of documents returned      | ..&pagesize=10           	|
| pageIndex   | page index to start               | ..&pageindex=5&pagesize=10  |
| sortBy	  | sort item 					      | ..&sortBy=txtworkflowstatus |
| sortReverse | sort direction (ascending/descending)   | ..&sourtReverse=true		|
| type        | filter workitems by the 'type' property | ..&type=workitem      | 
| items       | filter item values to be returned | ..&items=$processid,$modellversion
| format      | optional output format JSON/XML   | ..&format=json   or   &format=xml |
 
 
**Example:**

	/api/workflow/tasklistbycreator/admin?type=workitem&pageSize=10&pageIndex=2
 
See details about the search in the section [Search Index](../engine/luceneservice.html).


	

<strong>Note:</strong> The Imixs-Workflow manages the access to workitems by individual access lists per each entity. The result of a collection of workitems depends on the current user access-level and read access permissions for a workitem. Read also the section [Access Control](/engine/acl.html) for further information. 
  
   
   

## PUT/POST a Workitem or Task List
The methods PUT, POST allow to create and process a workitem or a task list:


| URI                          | Method  | Description                               | 
|------------------------------|---------|----------------------------------|
| /workflow/workitem           | POST    | posts a workitem, to be processed by the  workflow manager. To update an existing workitem, the attribute $uniqueid must be provided as part of the data structure. The media types application/xml, application/json and x-www-form-urlencoded are supported.   |
| /workflow/workitem/{uniqueid}| POST    | posts a workitem by uniqueid, to be processed by the  workflow manager. The media types application/xml, application/json and x-www-form-urlencoded are supported.   |
| /workflow/tasklist           | POST    | posts a list of workitems to be processed by the  workflow manager. The media type application/xml is supported.   |





## GET Events by Workitem

The subresource _/workflow/events/_ provides a GETer method to read the current workflow events for a running instance:


| URI                                           | Method | Description                               | 
|-----------------------------------------------|--------|-----------------------------------|
| /workitem/events/{uniqueid}                | GET    | Returns a collection of events of a workitem, visible to the current user
