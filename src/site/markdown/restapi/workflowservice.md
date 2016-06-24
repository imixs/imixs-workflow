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
| /workflow/tasklist/{user}                     | a collection of workitems representing the worklist by a specific user (or value 'null' for the current user)|             
| /workflow/tasklist/creator/{creator}          | a collection of workitems created by a specific user (or value 'null' for the current user)                           |
| /workflow/tasklist/processid/{processid}      | a collection of workitems in a specific    process state             |
| /workflow/tasklist/group/{processgroup}       | a collection of workitems in a specific    process group                             |
| /workflow/tasklist/owner/{owner}              | a collection of workitems owned by a specific  user (or value 'null' for the current user)   |
| /workflow/tasklist/ref/{uniqueid}             | a collection of workitems referenced to a  specific uniqueid (childs)                |
| /workflow/tasklist/query/{query}              | a collection of workitems specified by a  JPQL phrase                                |
| /workflow/tasklist/query/count/{query}        | the count of workitems returned by a    JPQL phrase                                  |



## PUT/POST Workitem or Task List
The following resource URIs are used to PUT and POST a wokitem or a task list:


| URI                          | Description                               | 
|------------------------------|-------------------------------------------|
| /workflow/workitem           | posts a workitem to be processed by the  workflow manager. The post data can be x-www-form-urlencoded or in xml format   |
| /workflow/workitem/{uniqueid}| posts a workitem to be processed by the  workflow manager. The post data can be x-www-form-urlencoded or in xml format   |
| /workflow/workitem.json      | posts a workitem to be processed by the  workflow manager. The post data is expected in json format. The result in json format     |
| /workflow/workitem.json/{uniqueid}      | posts a workitem to be processed by the  workflow manager. The post data is expected in json format. The result in json format     |
| /workflow/tasklist           | posts a list of workitems to be processed by the  workflow manager. The post data can be x-www-form-urlencoded or in xml format  |



## Resource Options
Additional URI parameters can be used to filter the result set, or to navigate through a sub list of the result set. 


| option      | description                                         | example               |
|-------------|-----------------------------------------------------|-----------------------|
| count       | number of workitems to be returned by a collection  | ..?count=10           |
| start       | start position of a collection of workitems         | ..?start=5&count=10   |
| type        | filter workitems by the 'type' property             | ..?type=workitem      | 
| sortorder   | sortorder of the returned collection <br /> (0=creation date descending <br />,1=creation date ascending<br />,2=modified date descending<br />,3=modified date ascending)     |&sortorder=2  |
		

<strong>Note:</strong> The Imixs-Workflow manages the access to workitems by individual access lists per each entity. The result of a collection of workitems depends on the current user accesslevel and read access permissions for a workitem. Read also the section [Access Control](/engine/acl.html) for further information. 
  
   