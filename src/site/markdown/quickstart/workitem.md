
#How to manage business data

In a business application information need to be managed and distributed in a predefined way. Users edit data, which need to be made available for various actors. Back-end applications need the data to track the status or trigger different events. The data which is  processed by a workflow management system is stored into a database and synchronized with a [business process](./businessprocess.html). 
This data is called the 'workitem'. 
 
The data, which is usually edited by a user or controlled by an application is called the 'business data'. The Data which is managed by the workflow system is called the 'workflow data'. The workflow data contains all information about the current state of a 'process instance'.
So a workitem holds business data, entered by a user or controlled by an application, and also processing information controlled by the workflow system.
 
<img src="../images/imixs-workitem.png"/>
 
## Business data
 
To manage the data of a workitem, the Imixs Workflow project provides the simple data object - called ItemCollection. A ItemCollection contains a list of items that can store any type of data. There are no restrictions in the amount or size of the data stored into an ItemCollection. See the following example how to use the ItemCollection object:
 
    import org.imixs.workflow.ItemCollection;
    .....
    ItemCollection workitem=new ItemCollection();
    workitem.replaceItemValue("name","Anna");
    workitem.replaceItemValue("age",new Integer(32));
 
Each item stored into a ItemCollection has a item-name and a item-value. The item-name identifies a specific part of data contained in a ItemCollection. You can access a item-value by its name:
  
    ....
    String name=workitem.getItemValueString("name");
    int age=workitem.getItemValueInteger("age");
    ....
  
 
## Workflow data

As explained before, a workitem can not only contain business data, but also information about the process instance which is controlled by the workflow system. This workflow data of a process instance contains for example data like the creation date or the processing status. This is an example how to access this kind of data:
 
   
    String uniqueID=workitem.getItemValueInteger("$UniqueID");
    int processID=workitem.getItemValueInteger("$ProcessID");
    Date created=workitem.getItemValueDate("$created");
    Date modified=workitem.getItemValueDate("$modified");
    String status=workitem.getItemValueString("txtWorkflowStatus");
    Vector history=workitem.getItemValue("txtWorkflowHistory");
  
The following table provides an overview about the data of a process instance managed by the Imixs-Workflow Engine. The column 'read/write' indicates if the property can be controlled by an application:
 
 
| Property        | Type   |read/write  | Description												 	|
|-----------------|--------|------------|---------------------------------------------------------------|
|$ModelVersion    |String  |yes   		| The Version of the model the workitem belongs to  			|
|$ProcessID       |Integer |yes   		| The current ProcessID of the workItem         				|
|$ActivityID      |Integer |yes   		| The next activity to be processed by the workflow engine  	|
|$workflowGroup   |String  |no    		| The name of the current process group   						|
|$workflowStatus  |String  |no   		| The name of the current workflow status      					|
|$Created         |Date    |no    		| Date of creation                              				|
|$Modified        |Date    |no    		| Date of last modification                     				|
|$ReadAccess      |List    |no    		| String list of User/Roles with read access    				|
|$WriteAccess     |List    |no    		| String list of User/Roles with write access   				|
|$uniqueId        |String  |no    		| The unique ID of this workItem                				|
|$uniqueIdRef     |String  |yes   		| A reference to a connected workItem (child process) 			|
|$workitemId      |String  |no    		| A unique process instance id of a workitem ad all its versions|
|$uniqueIDSource  |String  |no    		| The UniqueID of the Source workitem for a version (See [Workflow Kernel split-events](../core/workflowkernel.html))     |
|$uniqueIDVersions|String  |no    		| A list of UniqueIDs to all created versions of this workitem  (See [Workflow Kernel split-events](../core/workflowkernel.html))|
|$lastEvent       |Integer |no    		| The last processed event ID           						|
|$lastTask        |Integer |no    		| The last assigned Task ID (processid)          				|
|$lastEventDate   |Date    |no     		| The timestamp of the last processing action    				|
|$creator         |String  |no    		| The user who created the workItem.             				|
|$editor          |String  |no    		| The user who invoked the processWorkitem() method.       		|
|$lasteditor      |String  |no    		| The last user, that invoked the process method before the $editor |
|$processingLog   |List    |no    	    | Log of processed workflow events    						|
|namOwner         |List    |no    		| String list of User/Roles, that are owners of that WorkItem. 	|
|txtworkflowsummary |String|no 			| A short description of the current status      				|
|txtworkflowabstract|String|no 			| A long description of the current status       				|
|txtworkflowimageurl|String|no 			| A link to an image which displays the current status 			|
|txtworkflowresultmessage  |String |no  | The result message of last process step 						|
 

### Temporary Attributes 
 
The Imixs-Workflow engine provides additional temporarily attributes. These attributes are not persisted into the database.
Temporarily attributes are indicating the session state of a workitem:
 
 
| Property 		| Type   |Scope      | Description                               						|
|---------------|--------|-----------|------------------------------------------------------------------|
|$isAuthor      |Boolean |read       | Indicates if the current user has write access. The attribute is computed when a workitem is read.	|
|$isVersion     |Boolean |processing | Indicates if the current instance is a version to a source workitem. This attribute is computed during the processing phase.  |



