# The Imixs Admin Client
The Imixs Admin Client is a Web Front-End to administrate an instance of the Imixs-Workflow engine. The Client can be connected via the [Imixs Rest API](./restapi/index.html) with an instance of the Imixs-Workflow engine. The Imixs Admin Client can be deployed on any Java EE Server to administrate different instances of the Imixs-Workflow engine running on the same host. 
 
##Installation & Deployment
The sources of the Imixs Admin Client can be download form [GitHub](https://github.com/imixs/imixs-admin/releases). The maven based client supports different web server profiles for GlassFish and JBoss/Wildfly. After the deployment, the Imixs Admin is accessible using the following url:

	http://localhost:8080/imixsadmin 

The Imixs Admin Client requires the user to authenticate with a valid security realm. The authenticated user (Administrator) needs at least the the Role "IMIXS-WORKFLOW-Manager". The default security realm used by the Imixs Admin Client is "imixsrealm" which can to be configured on the Application Server. 

To connect to an instance of the Imixs-Workflow engine the corresponding Imixs Rest Service URL need to be entered. 

<img src="images/imixs-admin-client-01.png" /> 
 
Next the client can be connected to the Imixs-Workflow engine to query data using a search terms.

<img src="images/imixs-admin-client-02.png" /> 
  
See the section [Query Syntax](./engine/queries.html) for further details. 

## Update Enities

The Imixs Admin Client provides a interface to update or delete entities (Workitems) managed by the Imxis-Workflow engine. It is possible to run bulk updates on a result set to update a single item or to process workflow instances with a specific workflow event. 

<img src="images/imixs-admin-client-03.png" /> 

## The Administration Process
The Imixs Admin Client provides an administration process (AdminP) which can be used to maintain the documents and process instances managed by the Imixs-Workflow engine. 

<img src="images/imixs-admin-client-04.png" /> 

### Rebuild the Lucene Index

With the function 'Rebuild Index' the lucene index can be updated. After the job is started the existing documents will be re-indexed. The blocksize
defines the maximum number of workflow documents to be processed in one run. After the blocksize was updated, the job will pause for a given interval specified in minutes.  


### Rename User

The function 'Rename User' updates name files (starting with 'nam') and the ACL for existing documents. This feature can be used if a userID changed in a running workflow environment. The new userId can either be replaced with the old one or be appended. The blocksize
defines the maximum number of workflow documents to be processed in one run. After the blocksize was updated, the job will pause for a given interval specified in minutes.  

### Migration

The administration process provides a feature to migrate a business application from Imixs-Workflow version 3.x to 4.x. This feature is for migration purpose only. 


## Backup & Restore
With the Backup feature it is also possible to export entities into a file system and later re-import them into any exiting workflow instance.

<img src="images/imixs-admin-client-05.png" /> 

 
To backup a collection of workitems a search term can be specified.
 