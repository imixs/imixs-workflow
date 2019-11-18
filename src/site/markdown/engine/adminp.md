# The Administration Process
The Imixs-Workflow engine provides an administration process called _AdminP_. This service schedules so called _AdminP Jobs_. An AdminP job can be used to process documents and process instances in a batch processing. 
The AdminP process can also be monitored by the [Admin Client Tool](../administration.html). 


## Standard Job Handlers

The Imixs-Workflow engine provides a set of standard AdminP Job Handlers which can be triggered directly form the Rest API or the [Admin Client Tool](../administration.html).

<img src="../images/imixs-admin-client-04.png" /> 
 

### Rebuild the Lucene Index

With the function '_Rebuild Index_' the lucene index can be updated. After the job is started the existing documents will be re-indexed. The blocksize
defines the maximum number of workflow documents to be processed in one run. After the blocksize was updated, the job will pause for a given interval specified in minutes.  

Example of a a Job Description:

	<document xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xs="http://www.w3.org/2001/XMLSchema"> 
           <item name="type"><value xsi:type="xs:string">adminp</value></item> 
           <item name="job"><value xsi:type="xs:string">JOB_REBUILD_INDEX</value></item> 
           <item name="numblocksize"><value xsi:type="xs:int">1000</value></item> 
           <item name="numindex"><value xsi:type="xs:int">0</value></item> 
           <item name="numinterval"><value xsi:type="xs:int">1</value></item> 
	       <item name="typelist"><value xsi:type="xs:string">workitem</value></item> 
	       <item name="datfrom"><value xsi:type="xs:string">2018-01-01</value></item> 
	       <item name="datto"><value xsi:type="xs:string">2018-12-31</value></item> 
	</document>
	
The item 'typelist' is optional to restrict the type of documents to be updated by the job. 

The items 'datfrom' and 'datto' are optional and can be used to restrict the update to a timerange of creation. 

### Rename User

The function '_Rename User_' is used if a userID must be replaced or a deputy userid must be added into the ACL of a workitem.
The function updates the workitem items:

 * $ReadAccess
 * $WriteAccess
 * owner
 
The new userId can either be replaced with the old one or be appended. The blocksize
defines the maximum number of workflow documents to be processed in one run. After the blocksize was updated, the job will pause for a given interval specified in minutes.  

Example of a a Job Description:


	<document xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xs="http://www.w3.org/2001/XMLSchema">
	       <item name="type"><value xsi:type="xs:string">adminp</value></item> 
	       <item name="job"><value xsi:type="xs:string">RENAME_USER</value></item> 
	       <item name="typelist"><value xsi:type="xs:string">workitem</value></item> 
	       <item name="namfrom"><value xsi:type="xs:string">FROM USER ID</value></item> 
	       <item name="namto"><value xsi:type="xs:string">NEW USER ID</value></item> 
	       <item name="keyreplace"><value xsi:type="xs:boolean">false</value></item> 
	</document>


If the job item 'keyReplace' is set to 'true' then the old user id will be removed.  

If a workitem has the item '_private_" set to _true_ the workitem will be ignored by the job. This is useful for personal workitems which should not be delegated to the deputy. E.g. 'personnel file' or a 'application for leave'.


### Upgrade Workitems

The administration process provides a feature to upgrade exsiting workitems to the latest version of Imixs-Workflow . This feature can be used to synchronize a workflow instance with the current engien version.


	<document xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xs="http://www.w3.org/2001/XMLSchema">
	       <item name="type"><value xsi:type="xs:string">adminp</value></item> 
	       <item name="job"><value xsi:type="xs:string">JOB_UPGRADE</value></item> 
	</document>

## Implementing a Custom JobHandler

An application can provide custom AdminP jobs. An AminP job must implement the interface _'org.imixs.workflow.engine.adminp.JobHandler'_.

The JobHandler is injected by CDI. This means that the custom implementation must be an plain java bean and no EJB service.
The AdminP Service calls the run method of the custom JobHandler and provides the job description in an ItemCollection object. The JobHandler must return the job description with the predefined field _isCompleted_ to signal the current status. If the field _isCompleted_ is set to true, the AdminP Service will terminate the Job. Otherwise the AdminPServcie will wait for the next timeout to call the run method again. 

The following fields part of the Job description are defined by the AdminP service: 

 * type - fixed to value 'adminp'
 * job - the job type/name, defined by handler
 * $WorkflowStatus - status controlled by AdminP Service
 * $WorkflowSummary - summary of job description 
 * isCompleted - boolean indicates if job is completed - controlled by job handler
 
**Note:** The AdminPService will not call the JobHandler if the job description field 'isCompleted==true'
 
A JobHandler may throw a AdminPException if something went wrong. See the following example of a JobHandler implementation:


	public class JobHandlerDemo implements JobHandler {
		@EJB
		DocumentService documentService;
		
		@Override
		public ItemCollection run(ItemCollection adminp) throws AdminPException {
			// find documents...
			List<ItemCollection> result=documentService.fine("type:'workitem'",0,100);
			// do something...
			try {
			    ....
		   } catch (Exception e) {
		    	// throw exception if something went wrong
			    throw new AdminPException("ERROR", "Error...", e);
		   }
			....
			// more work to do?
			if (... more work ...) {
			   adminp.replaceItemValue(JobHandler.ISCOMPLETED, true);
			}
			return adminp;
		}
	}

To start the JobHandler via the AdminP Service interface the attriubte 'job' must be set to the class name of the CDI Bean. 


## Initialize an JobHandler via a Rest API call

It is  possible to trigger an AdminP job via the Rest API. See the following example with a curl command to start the LuceneIndexJob:

	curl --user admin:adminpassword -H "Content-Type: text/xml" -d \
       '<document xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xs="http://www.w3.org/2001/XMLSchema"> \
           <item name="type"><value xsi:type="xs:string">adminp</value></item> \
           <item name="job"><value xsi:type="xs:string">JOB_REBUILD_INDEX</value></item> \
           <item name="numblocksize"><value xsi:type="xs:int">1000</value></item> \
           <item name="numindex"><value xsi:type="xs:int">0</value></item> \
           <item name="numinterval"><value xsi:type="xs:int">1</value></item> \
        </document>' \
    http://localhost:8080/api/adminp/jobs

 