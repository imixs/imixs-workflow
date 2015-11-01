#The Imixs Admin Client
The Imixs Admin Client is a Web Application which allows you to administrate a Imixs Workflow Engine.  The Imixs Admin Client can be deployed on any Java EE Server independent from a workflow Application. So the Imixs Admin Client can be used to administrate different instances of the Imixs Workflow implementation on the same application server. To administrate an existing workflow application the Imixs Admin Client uses a remote ejb connection to the 
 Imixs EntityService Interface.
 
##Installation & Deployment
The latest version of the Imixs Admin Client can be download form the  {{{http://java.net/projects/imixs-workflow/downloads}Download Center}}.  The client is supported as a single web module (.war file) which can be easily installed into the web container on an application server like GlassFish. After the deployment of the Imixs Admin Client the Web module is accessible using the following url:

 {{{http://localhost:8080/adminclient}http://localhost:8080/adminclient}}

The Imixs Admin Client requires the user to authenticate and also the authenticated user (Administrator) needs at least the the Role "IMIXS-WORKFLOW-Manager". To authenticate the users the Imixs Admin Client uses the default Realm "imixsrealm" which need to be configured on the Server. 
After you have logged into the Imixs Admin client you will see the JNDI Name Page.   On this page you can enter the module name of an existing Imixs Workflow Instance on your  Server.

[images/admin-client-03.png] 
 
The module name looks something like "imixs-workflow-web-sample-0.0.6". If you did not know the module name you can see it in the deployment section of the GlassFish admin console.
 
After you enter a valid module Name and click on the refresh symbol behind the input field or press  "Return" the remote connection will be established. 

After connecting you can use the different function of the Admin client.  

## Manage IndexProperties
The Index management can be use to verify existing IndexProperties inside a Workflow Instance.  An IndexProperty is necessary to query workitems by a JPQL statement. Each workflow instanceis 
adding indexes automatically after deployment. So you will see a pre defined List of IndexProperties:
 
[images/admin-client-04.png]  
 
The Index Management allows you to delete an existing Index or add a new index one. 

##Search Workitems
The Imixs-Admin client supports a function to search Workitems using JPQL Statements. 
This gives you a powerful feature to query workitems in a existing Workflow Instance. 
So a administrator is able to control or verify  the content of workitems. As the Administrator is granted to the Role "IMIXS-WORKFLOW-Manager" it is possible to read any dataset independent form the individual read access of a single workitem!

[images/admin-client-02.png] 
 
Also this feature gives you the possibility to test a JPQL statement before implementing a statement  into a business logic.

Read more about using Queries based on JPQL {{{./examples/queries.html}here}}.
 The search function also allows you to delete one or a set of worktiems.


##View Model Versions

Using the function "Models" allows you to inspect all deployed workflow models on a running Imixs JEE  Workflow instance.

[images/admin-client-05.png] 

For each deployed model you can see the creation date and the version number of the model. To delete a  model version you can call the "delete" function for a specific model version.

##Import & Export
With the import and export feature you can export entities into a file system and later re-import them into  a exiting workflow instance

[images/admin-client-06.png] 
 
To export a Collection of workitems you need to specify a JPQL Statement. You can test the statement before a  export using the search function. The export will be stored into a binary file specified by the export page.

With the import function you can import / re-import an existing export file into a workflow Instance.  The import function will override existing workitems with the same $uniqueID. So be careful if you import a subset of data. 

 