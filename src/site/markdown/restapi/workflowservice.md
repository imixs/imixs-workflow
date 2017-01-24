#The Workflow Service
The Imixs-Workflow REST service interface provides different resources located under */workflow* to access and modifiy workitems.

 
## GET Workitem Resource
The resource /workflow/workitem returns a single workitem or meta data:

| URI                                           | Description                               | 
|-----------------------------------------------|-------------------------------------------|
| /workflow/workitem/{uniqueid}                 | a single workitem represented by the   provided uniqueid                              |
| /workflow/workitem/{uniqueid}/file/{file}     | a file attachment located in the property   $file of the spcified workitem           |


## GET Task List Resources
The resource /workflow/tasklist/ returns collections of workitems:

| URI                                           | Description                               | 
|-----------------------------------------------|-------------------------------------------|
| /workflow/worklist                            | a collection of workitems representing the worklist for the current user |             
| /workflow/tasklist/owner/{owner}              | a collection of workitems owned by a specific  user (or value 'null' for the current user)   |
| /workflow/tasklist/creator/{creator}          | a collection of workitems created by a specific user (or value 'null' for the current user)                           |
| /workflow/tasklist/processid/{processid}      | a collection of workitems in a specific    process state             |
| /workflow/tasklist/group/{processgroup}       | a collection of workitems in a specific    process group                             |
| /workflow/tasklist/ref/{uniqueid}             | a collection of workitems referenced to a  specific uniqueid (childs)                |



## PUT/POST Workitem or Task List
The following resource URIs are used to PUT and POST a wokitem or a task list:


| URI                          | Description                               | 
|------------------------------|-------------------------------------------|
| /workflow/workitem           | posts a workitem, to be processed by the  workflow manager. To update an existing workitem, the attribute $uniqueid must be provided as part of the data structure. The media types application/xml, application/json and x-www-form-urlencoded are supported.   |
| /workflow/workitem/{uniqueid}| posts a workitem by uniqueid, to be processed by the  workflow manager. The media types application/xml, application/json and x-www-form-urlencoded are supported.   |
| /workflow/tasklist           | posts a list of workitems to be processed by the  workflow manager. The media type application/xml is supported.   |



## Resource Options
Additional URI parameters can be used to filter the result set, or to navigate through a sub list of the result set. 


| option      | description                                         | example               |
|-------------|-----------------------------------------------------|-----------------------|
| pagesize    | number of documents returned                        | ..?pagesize=10           |
| pageindex   | page index to start                                 | ..?pageindex=5&pagesize=10   |
| type        | filter workitems by the 'type' property             | ..?type=workitem      | 
		

<strong>Note:</strong> The Imixs-Workflow manages the access to workitems by individual access lists per each entity. The result of a collection of workitems depends on the current user accesslevel and read access permissions for a workitem. Read also the section [Access Control](/engine/acl.html) for further information. 
  
   