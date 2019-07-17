
# How to Manage Business Data

In a business application information need to be managed and distributed in a predefined way. Users edit data, which need to be made available for various actors. Back-end applications need the data to track the status or trigger different events. The data which is  processed by a workflow management system is stored into a database and synchronized with a [business process](./businessprocess.html). 
This data is called the 'workitem'. 
 
The data, which is usually edited by a user or controlled by an application is called the 'business data'. The Data which is managed by the workflow system is called the 'workflow data'. The workflow data contains all information about the current state of a 'process instance'.
So a workitem holds business data, entered by a user or controlled by an application, and also processing information controlled by the workflow system.
 
<img src="../images/imixs-workitem.png"/>
 
## Business data
 
To manage the data of a workitem, the Imixs Workflow project provides the simple data object - called _ItemCollection_. A [ItemCollection](../core/itemcollection.html) holds a list of _Items_ that can store any type of data. There are no restrictions in the amount or size of the data stored into an _ItemCollection_. See the following example:
 
    import org.imixs.workflow.ItemCollection;
    .....
    ItemCollection workitem=new ItemCollection();
    workitem.setItemValue("name","Anna");
    workitem.setItemValue("age",new Integer(32));
 
Each item stored into a ItemCollection has a item-name and a item-value. The item-name identifies a specific part of data contained in a ItemCollection. You can access a item-value by its name:
  
    ....
    String name=workitem.getItemValueString("name");
    int age=workitem.getItemValueInteger("age");
    ....
 
You will find more information in the section [ItemCollection](../core/itemcollection.html).  
 
## Workflow data

As explained before, a workitem can not only contain business data, but also information about the process instance which is controlled by the workflow engine. This workflow data of a process instance contains for example the creation date or the processing status. This is an example how to access this kind of data:
 
   
    String uniqueID=workitem.getUnqiueID();
    int taskID=workitem.getTask();
    String status=workitem.getItemValueString(WorkflowKernle.WORKFLOWSTATUS);
    Date created=workitem.getItemValueDate("$created");
    Date modified=workitem.getItemValueDate("$modified");
  
The following table provides an overview about all data items managed by the Imixs-Workflow Engine. The column '_read/write_' indicates if the item can be controlled by an application. The column '_indexed_' indicates if the item can be searched by the [search index](../engine/queries.html#Query_Items):
 
 
| Property        | Type   |read/write  | indexed | Description												 	|
|-----------------|--------|------------|-----------------------------------------------------------------------|
|$ModelVersion    |String  |yes   		| yes 	  | The Version of the model the workitem belongs to  			|
|$TaskID       	  |Integer |yes   		| yes 	  | The current taskID of the workitem							|
|$EventID         |Integer |yes   		| yes 	  | Current event processed by the Workflow Engine. Is 0 if no processing lifecycle is executed.		|
|$workflowGroup   |String  |no    		| yes 	  | The name of the current process group   					|
|$workflowStatus  |String  |no   		| yes 	  | The name of the current workflow status      				|
|$Created         |Date    |no    		| yes 	  | Date of creation                              				|
|$Modified        |Date    |no    		| yes 	  | Date of last modification                     				|
|$ReadAccess      |List    |no    		| yes 	  | String list of User/Roles with read access    				|
|$WriteAccess     |List    |no    		| yes 	  | String list of User/Roles with write access   				|
|$uniqueId        |String  |no    		| yes 	  | The unique ID of this workItem                				|
|$uniqueIdRef     |String  |yes   		| yes 	  | A reference to a connected workItem (child process) 		|
|$workitemId      |String  |no    		| yes 	  | A unique process instance id of a workitem and all its versions|
|$uniqueIDSource  |String  |no    		| yes 	  | The UniqueID of the Source workitem for a version (See [Workflow Kernel split-events](../core/workflowkernel.html))     |
|$uniqueIDVersions|String  |no    		| yes 	  | A list of UniqueIDs to all created versions of this workitem  (See [Workflow Kernel split-events](../core/workflowkernel.html))|
|$lastTask        |Integer |no    		| yes 	  | The last assigned Task ID (processid)          				|
|$lastEvent       |Integer |no    		| yes 	  | The last processed event ID           						|
|$lastEventDate   |Date    |no     		| yes 	  | The timestamp of the last processing action    				|
|$eventLog        |List    |no    	    | no 	  | Log of processed workflow events    					    |
|$creator         |String  |no    		| yes 	  | The user who created the workItem.             				|
|$editor          |String  |no    		| yes 	  | The user who invoked the processWorkitem() method.       	|
|$lasteditor      |String  |no    		| yes 	  | The last user, that invoked the process method before the $editor |
|$noindex    	  |Boolean |yes    		| no 	  | If set to 'true', the document will not be added into the index. (See [DocumentService](../engine/documentservice.html) for details.)   |
|$immutable    	  |Boolean |yes    		| no 	  | If set to 'true' updateing the workitem is no longer allowed. (See [DocumentService](../engine/documentservice.html) for details.) |
|$Owner           |List    |no    		| yes 	  | String list of User/Roles, that are owners of that WorkItem. 	|
|$Participants    |List    |no    		| yes 	  | String list of Users having executed this WorkItem. 		|
|txtworkflowsummary |String|no 			| yes 	  | A short description of the current status      				|
|txtworkflowabstract|String|no 			| yes 	  | A long description of the current status       				|
|txtworkflowimageurl|String|no 			| no 	  | A link to an image which displays the current status 		|
|txtworkflowresultmessage  |String |no  | no 	  | The result message of last process step 					|
 

### Temporary Attributes 
 
The Imixs-Workflow engine provides additional temporarily attributes. These attributes are not persisted into the database.
Temporarily attributes are indicating the session state of a workitem:
 
 
| Property 		| Type   |Scope      | Description                               						|
|---------------|--------|-----------|------------------------------------------------------------------|
|$isAuthor      |Boolean |read       | Indicates if the current user has write access. The attribute is computed when a workitem is read.	|
|$isVersion     |Boolean |processing | Indicates if the current instance is a version to a source workitem. This attribute is computed during the processing phase.  |



## What's Next...

Continue reading more about:

 * [Why You Should Use Imixs-Workflow](../quickstart/why.html)
 * [What Means Human Centric Workflow?](../quickstart/human.html)
 * [Imixs-BPMN - The Modeler User Guide](../modelling/index.html)
 * [The Imixs-Worklfow Plugin API](../engine/plugins/index.html)
 * [The Imixs-Worklfow Rest API](../restapi/index.html)