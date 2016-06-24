#The Workflow Service
The main resource /workflow is uses to read Workitems and Workitem collections through the Imixs Rest Service Interface.
 
 
## The /workflow GET resources
The /workflow resources are used to get the business objects provided by the Imixs Workflow Manager:

| URI                                           | Description                               | 
|-----------------------------------------------|-------------------------------------------|
| /workflow/worklist/{user}                     | a collection of workitems representing the   worklist by a specific user (or value 'null' for the current user)|             
| /workflow/worklistbycreator/{creator}         | a collection of workitems created by a specific user (or value 'null' for the current user)                           |
| /workflow/worklistbyprocessid/{processid}     | a collection of workitems in a specific    process state             |
| /workflow/worklistbygroup/{processgroup}      | a collection of workitems in a specific    process group                             |
| /workflow/worklistbyowner/{owner}             | a collection of workitems owned by a specific  user (or value 'null' for the current user)   |
| /workflow/worklistbywriteaccess               | a collection of workitems where the current    has a write access                    |
| /workflow/worklistbyref/{uniqueid}            | a collection of workitems referenced to a  specific uniqueid (childs)                |
| /workflow/worklistbyquery/{query}             | a collection of workitems specified by a  JPQL phrase                                |
| /workflow/worklistcountbyquery/{query}        | the count of workitems returned by a    JPQL phrase                                  |
| /workflow/workitem/{uniqueid}                 |a single workitem represented by the   provided uniqueid                              |
| /workflow/workitem/{uniqueid}/file/{file}     | a file attachment located in the property   $file of the spcified workitem           |


## The /worflow resources PUT and POST
The /workflow/xxxx PUT and POST resources URIs are used to write business objects:


| URI                          | Description                               | 
|------------------------------|-------------------------------------------|
| /workflow/workitem           | posts a workitem to be processed by the  workflow manager. The post data can be x-www-form-urlencoded or in xml format   |
| /workflow/workitem/{uniqueid}| posts a workitem to be processed by the  workflow manager. The post data can be x-www-form-urlencoded or in xml format   |
| /workflow/worklist           | posts a worklist to be processed by the  workflow manager. The post data can be x-www-form-urlencoded or in xml format  |
| /workflow/workitem.json      | posts a workitem to be processed by the  workflow manager. The post data is expected in json format. The result in json format     |
| /workflow/workitem.json/{uniqueid}      | posts a workitem to be processed by the  workflow manager. The post data is expected in json format. The result in json format     |



## Resource Options
You can specify additional URI parameters to filter the resultset  or to navigate through a sub list of workitems. Append optional arguments to define the number of workitems returned by a URL, the starting point inside the list or the sort order. Combine any of the following arguments for the desired result. 


| option      | description                                         | example               |
|-------------|-----------------------------------------------------|-----------------------|
| count       | number of workitems returned by a collection        | ..?count=10           |
| start       | position to start  workitems returned by a collection         | ..?start=5&count=10   |
| type        | Optional type property workitems are filtered       | ..?type=workitem      | 
| sortorder   | defines the sortorder of the returned collection <br /> (0=creation date descending <br />,1=creation date ascending<br />,2=modified date descending<br />,3=modified date ascending)     |&sortorder=2  |
		

<strong>Note:</strong> The Imixs JEE Workflow manages the access to workitems by individual access lists per each entity. So the result of a collection of workitems will only contain entities where the current user has a read access right. Without access the workitem will not be returned by the workflowManager and so it will not be included in the list. 
  
   