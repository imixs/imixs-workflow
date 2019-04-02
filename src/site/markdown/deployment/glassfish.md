# GlassFish/Payara Deployment Guide
This section will explain the configuration steps needed to successfully deploy the [Imixs-Sample Application](../sampleapplication.html) on [Payara Application Server](https://www.payara.fish/). Payara is based on the [GlassFish implementation](https://github.com/javaee/glassfish). The deployment is similar for other custom projects. For general information about the deployment of the Imixs-Workflow engine, see also the section [Deployment Guide](./deployment_guide.html).

## Install GlassFish
You can download the GlassFish Server from the [GlassFish project site](http://www.glassfish.org). This site also includes an Installation guide for installing GlassFish on different platforms. GlassFish is provided in two versions - the latest [official version GlassFish](https://glassfish.java.net/) and the [Payara Project](http://www.payara.fish/). Both versions are based on the Java EE7 specification. After you have installed the GlassFish Server you can start the server by switching into the directory and execute the startup script:
 
    GLASSFISH_DIST/domains/domain1/bin
 
After the server is started you can access the GlassFish web console from your web browser with the following URL:

    http://localhost:4848/
      
## Setting up a Imixs-Workflow database pool
The [Imixs-Sample Application](../sampleapplication.html) expects a database resource with the name "jdbc/workflow-db". Thus you need first to set up a Database Pool and a JDBC resource before you can deploy the application successful. In this example we create a database pool for the build in derby database from GlassFish.  You can also configure any other database like MySQL, Oracle, Informix, Microsoft SQL Server,....

To create a new database pool in GlassFish follow these steps:

   1. make sure the derby database is started   
       
    GLASSFISH_DIST/bin/asadmin start-database

   2. start admin console -> http://localhost:4848/   
   3. navigate to   Application Server  >>  Resources  >>  JDBC  >>  Connection Pools
   4. click "new" to create a new database source
      * name: your database name (e.g. "imixs_db_pool")
      * resource type : javax.sql.DataSource
      * Database Vendor : Derby
   5. click "next". Now only the following property settings are necessary:
      * ConnectionAttributes: ;create=true
      * DatabaseName: "imixs_db_pool"
      * Password: "APP"
      * User: "APP"
      * ServerName : "loacalhost"
      * portnumber: 1527
   6. Now create a JDBC Resource - Navigate to "Application Server  >>  Resources  >>  JDBC  >>  JDBC Resources"
   7. click "new" to create a new resource
       - jndiName: jdbc/workflow-db
       - PoolName: imixs_db_pool 

## Setup a Security Realm
To login to the Imixs-Sample Application you need also to configure a security realm.  Follow the steps below:
 
   1. start admin console -> http://localhost:4848/   
   2. navigate to  Configuration->Security->realms
   3. add a new file realm named "imixsrealm"
   4. choose the class Name "com.sun.enterprese.security.auth.realm.file.FileRealm"
   5. Set the JAAS Context to "fileRealm"
   6. Set the Key File to a new File name. e.g. "keyfile"
   7. open the newly created realm configuration and click on button "manage users"
   8. Add the following test accounts:

| UserID       |GroupName                |Description                         | 
|--------------|-------------------------|------------------------------------|
|admin         |IMIXS-WORKFLOW-Manager   | This user will have maximum access |
|gloria        |IMIXS-WORKFLOW-Editor    | User can edit all workitems         |
|alex          |IMIXS-WORKFLOW-Author    | User will be allowed to create workitems and edit his own     |
|rico          |IMIXS-WORKFLOW-Reader    | This user will be only allowed to read workitems   |
|private       |                         | This user will have no access (just to be sure security works well) 
  
It is also possible to configure other security bindings as the file based described here.  Only the realm name should match to "imixsrealm". 

## Deploy the Imixs-Sample Application
Now install the .war or .ear file of your application by the following steps:

   1. Be sure, that your database server is up and running. 
      in case you use the glassfish embedded database, start it with
      >asadmin start-database
      otherwise start your external database server.
   2. Be sure, that the domain, which you will deploy the application on, is started and alive
   3. Now you can start the Admin Console in the browser. The port is listed in the output while starting the domain, i.e. in the default domain domain1 it is
      >http://localhost:4848
   4. Choose Applications ->  click the button "deploy" in the main frame
   5. Choose as type "Enterprise Application (.ear)"
   6. select your appliation (.war or .ear file)
   7. press the button "ok" located in the upper right corner --> ear will be deployed

## Use Docker

The [Imixs-JSF-Sample project](https://github.com/imixs/imixs-jsf-example) contains also a Docker setup. This allows you to run the sample application on Payara Server on a docker stack. You can find more details about the Docker Payara Setup [here](https://github.com/imixs/imixs-jsf-example/tree/master/src/docker/configuration/payara).

 
## Need Help?

If you have any difficulty in deployment of your application, [contact the community for help](https://www.imixs.org/sub_community.html). Also if you have any tips and suggestions for improvements, please share them as well. 


