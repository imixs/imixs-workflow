# The Imixs Admin Client
The Imixs Admin Client is a Web Front-End to administrate an instance of the Imixs-Workflow engine. The Client connects to an instance of the Imixs-Workflow engine via the [Imixs Rest API](./restapi/index.html). Imixs-Admin runs as a self-contained microservice in a docker container and can also be deployedinto any Jakarta EE Server. 


 
## Installation & Deployment

The sources and the Imixs Admin Client application are available on [GitHub](https://github.com/imixs/imixs-admin/releases). The Imixs-Admin client can be build from source with maven. 

	$ mvn clean install

The artifact '_imixs-workflow.war_' can be deployed on a Jakarta EE Application server. 

### Docker

The Imixs-Admin client provides also a Docker Image to be used to run the service as a Docker container in a Docker-Swarm or Kubernetes environment. The docker image is available on DockerHub. You can start the latest version of the Imixs-Admin Tool in a docker container:

	$ docker run -p 8888:8080 imixs/imixs-admin

The Imixs-Admin client can be accessed from your web browser:

	http://localhost:8888/


## Connecting an Imixs Workflow Instance

Connecting an Imixs-Workflow instance the Rest Service endpoint and administrative user credentials need to be provided. The authenticated user (Administrator) needs at least the the Role '_org.imixs.ACCESSLEVEL.MANAGERACCESS_'. 

<img src="images/imixs-admin-client-01.png" class="screenshot" /> 
 
After the client is connected to the Imixs-Workflow instance you can query data using a lucene search term.

<img src="images/imixs-admin-client-02.png" class="screenshot"/> 
  
See the section [Query Syntax](./engine/queries.html) for further details. 

## Update Enities

The Imixs Admin Client provides a interface to update or delete entities (Workitems) managed by the Imxis-Workflow engine. It is possible to run bulk updates on a result set to update a single item or to process workflow instances with a specific workflow event. 

<img src="images/imixs-admin-client-03.png" /> 

## The Administration Process
The Imixs-Workflow engine provides the [administration process 'AdminP'](./engine/adminp.html) used to maintain the documents and process instances managed by the Imixs-Workflow engine. This process can be monitored by the Admin Client

<img src="images/imixs-admin-client-04.png" class="screenshot" /> 


The Imixs-Workflow engine provides a set of standard AdminP Job Handlers that can be triggered form the Imixs-Admin UI: 

### Rebuild the Lucene Index

With the function '_Rebuild Index_' the lucene index can be updated. After the job is started the existing documents will be re-indexed. The blocksize
defines the maximum number of workflow documents to be processed in one run. After the blocksize was updated, the job will pause for a given interval specified in minutes.  


### Rename User

The function '_Rename User_' is used if a userID must be replaced or a deputy userid must be added into the ACL of a workitem.
The function updates the workitem items:

 * $ReadAccess
 * $WriteAccess
 * Owner
 
The new userId can either be replaced with the old one or be appended. The blocksize
defines the maximum number of workflow documents to be processed in one run. After the blocksize was updated, the job will pause for a given interval specified in minutes.  


### Upgrade

The administration process '_Upgrade_' provides a feature to upgrade a business application from an older version of Imixs-Workflow. This feature is needed for migration purpose only. 


## Backup & Restore
With the Backup feature data can be exported to a file system and later re-imported into any exiting workflow instance.

<img src="images/imixs-admin-client-05.png" class="screenshot" /> 

The target file system is the local filesystem of the workflow instance the Admin-Tool is connected to. The backup of a collection of workitems is based on a lucene search term. 
 