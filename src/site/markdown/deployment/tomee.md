# Apache TomEE Deployment Guide

This section will explain the configuration steps needed to successfully deploy the [Imixs-Sample Application](../sampleapplication.html) on [Apache TomEE Application Server](http://tomee.apache.org/). The deployment is similar for other custom projects. For general information about the deployment of the Imixs-Workflow engine, see also the section [Deployment Guide](./deployment_guide.html).

## Install Apache TomEE
You can download Apache TomEE the [TomEE project site](http://tomee.apache.org/). This site also includes an Installation guide for installing TomEE on different platforms. It's recommended to use TomEE-plus. 

After the server is started you can access the TomEE web console from your web browser with the following URL:

    http://localhost:8080/
      
## Setting up a Imixs-Workflow database pool

The [Imixs-Sample Application](../sampleapplication.html) expects a database resource with the name "jdbc/workflow". 

Thus you need first to set up a Database Pool and a JDBC resource before you can deploy the application successful. You can run any SQL database like MySQL, Oracle, Informix, Microsoft SQL Server, ...

* Install Mysql version 5.7.20 (CE)
* created the database imixs in MySQL

**Note:** You can change the details of the persistence.xml located in _/src/main/resourceses/META-INF_ if necessary. In most cases, however, there is no need for it.



### EclipseLink
The Imixs-Sample Application uses EclipseLink for JPA. So you need to download EclipseLInk and deploy the library into your TomEE.

* Download the eclipseLink from [here](https://www.eclipse.org/eclipselink/downloads/). The Zip file includes the file _eclipselink.jar_.
* Drop the eclipselink-2.4.2.jar into the lib folder of tomee-plus


## Setup Security

To run Imixs-Worklfow you need to authenticate against your application with one of the Imixs Access Roles. 

So fist create a user role in tomcat-users.xml (conf folder) 

	...
	 <role rolename="IMIXS-WORKFLOW-Manager"/>
	... 

next create a user named "both" in tomcat-users.xml

	...
	 <user username="both" password="xxxxxx" roles="tomcat,role1,org.imixs.ACCESSLEVEL.MANAGERACCESS" />
	...
	
	

## Deploy The Sample Application

Now you can deploy your application:


1. drop the imixs-jsf-example.war file in webapps folder of tomee-plus
2. start tomee-plus startup script
3. restarted tomee-plus    
 
 
Run The application

	http://localhost:8080/
    
You can login as user "both" and password

**Note:** In case of a console error about missing imixs core libs in folder WEB-INF/classes for imixs-jsf-example webapp, stop  the server and copy entire lib folder from WEB-INF to to WEB-INF/classes folder. See also discussion [here](https://github.com/imixs/imixs-jsf-example/issues/26). 



## Need Help?

If you have any difficulty in deployment of your application, [contact the community for help](https://www.imixs.org/sub_community.html). Also if you have any tips and suggestions for improvements, please share them as well. 

