# The Imixs Admin Client
The Imixs Admin Client is a Web Front-End to administrate an instance of the Imixs-Workflow Engine.  The Client can be connected via the [Imixs Rest API](./restapi/index.html) with an instance of the Imixs-Workflow engine. The Imixs Admin Client can be deployed on any Java EE Server to administrate different instances of the Imixs-Workflow engine running on the same host. 
 
##Installation & Deployment
The sources of the Imixs Admin Client can be download form [GitHub](https://github.com/imixs/imixs-admin/releases). The Maven based client supports different web server profiles for GlassFish and JBoss/Wildfly. After the deployment of the Imixs Admin Client the Web module is accessible using the following url:

	http://localhost:8080/adminclient 

The Imixs Admin Client requires the user to authenticate with a valid security realm. The authenticated user (Administrator) needs at least the the Role "IMIXS-WORKFLOW-Manager". The default security realm used by the Imixs Admin Client is "imixsrealm" which need to be configured on the Application Server. 

To connect to a worklfow engine the corresponding Imixs Rest Service URL  need to be entered. 

<img src="images/imixs-admin-client-01.png" /> 
 
After entered a valid Rest URI the client can be connected to the Imixs-Workflow engine to query data using JPQL statements.

<img src="images/imixs-admin-client-02.png" /> 
  
See the[JPQL section](./engine/queries.html) for futher details about using the Query Languate in Imixs-Workflow. 

## Update Enities

The Imixs Admin Client provides a interface to update or delete entities (Workitems) managed by the Imxis-Workflow engine. It is possible to run bulk updates on a result set to update a specifiy item or to process workflow instances with a specific workflow event. 

<img src="images/imixs-admin-client-03.png" /> 

## Administrate the Imixs Index Properties
The Imixs Admin Client provides also a interface to administrate the Imixs Index tables. 

<img src="images/imixs-admin-client-04.png" /> 

New indexes can be added and existing indexes can be removed from the Back-End. . 


##Backup & Restore
With the Backup feature it is also possible to export entities into a file system and later re-import them into any exiting workflow instance.

<img src="images/imixs-admin-client-04.png" /> 

 
To backup a collection of workitems a JPQL statement can be specified.
 