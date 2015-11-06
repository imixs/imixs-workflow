#Deployment Guide
The following section gives an overview how to deploy the Imixs-Workflow engine into a JEE Web- or Enterprise application. Imixs-Workflow consists of different modules which simply can be bundled together with other components of your business application. As all Imixs-Workflow components are based on the JEE6 component model it is quite easy to bundle and configure the Imixs-Workflow engine.
 
The examples illustrates how to deploy the components on a Glassfish V3  application server which is an open source, production-ready, Java EE-compatible application server based on the JEE6 specification. But the deployment is similar to all other JEE6 application servers (e.g. JBoss or WildFly).
 
Either if you deploy the Imixs-Workflow engine into a web application or an enterprise application the packaging  will differ in details. Both deployment concepts will be explained in the following section.  Please share your experiences and ask your questions on the [GitHub Issue Tracker](https://github.com/imixs/imixs-workflow/issues).

##Imixs-Workflow components
To bundle the Imixs-Workflow engine into your business application you need to add the following components into  your application:
 
  * imixs-workflow-core-3.x.x.jar  - contains the core api and xml api
  * imixs-workflow-engine-3.x.x.jar - the jee workflow engine containing jpa and ejb components
  * imixs-workflow-faces-3.x.x.jar - contains optional JSF components
  * imixs-workflow-jax-rs-3.x.x.jar - contains the Imixs RESTful Web Service
   
You can download the Imixs-Workflow components from the [Maven central repository page](https://search.maven.org/). If you are working with maven the components will be downloaded and bundled automatically during the maven build process.
 
##Building an Web Application (WAR)
To install and deploy the Imixs-Workflow engine into a web application it is sufficient to bundle the  Imixs-Workflow components into your WEB-INF/lib folder. This is called the "EJB Lightway Runtime Environment". If you need to customize the deployment you can also add the ejb-jar.xml file into to WEB-INF/ folder. The persitence.xml need to be placed into the /WEB-INF/classes folder
 
	  / 
	  +- WEB-INF/
	  |  |- ejb-jar.xml  (optional)
	  |  +- lib/
	  |  |  |- imixs-workflow-core-3.0.0.jar
	  |  |  |- imixs-workflow-engine-3.0.0.jar
	  |  |  |- imixs-workflow-faces-3.0.0.jar
	  |  |  |- imixs-workflow-jax-rs-3.0.0.jar
	  |  +- classes/
	  |  |  +- META-INF/
	  |  |  |  |- persistence.xml

If you are using maven (which is recommended) you can simply add the following dependency into your existing pom.xml
 
	<!-- Imixs-Workflow -->
	<dependency>
		<groupId>org.imixs.workflow</groupId>
		<artifactId>imixs-workflow-core</artifactId>
		<type>jar</type>
		<version>3.0.0</version>
	</dependency>
	<dependency>
		<groupId>org.imixs.workflow</groupId>
		<artifactId>imixs-workflow-engine</artifactId>
		<type>jar</type>
		<version>3.0.0</version>
	</dependency>
	<dependency>
		<groupId>org.imixs.workflow</groupId>
		<artifactId>imixs-workflow-faces</artifactId>
		<type>war</type>
		<version>3.0.0</version>
	</dependency>
	<dependency>
		<groupId>org.imixs.workflow</groupId>
		<artifactId>imixs-workflow-jax-rs</artifactId>
		<type>jar</type>
		<version>3.00</version>
	</dependency>
	
To check for the latest versions and download components provided by this project you can browse the [Maven repository](http://search.maven.org/#browse) and search for keyword 'imixs'. 
 
 
##Building an Enterprise Archive (EAR)
Deploying the Imixs-Workflow engine into a Enterprise Archive (EAR) differs in some details from the deployment  into a web application as explained before. An EAR splits the business logic (EJBs) and the users web front-end (WAR) into separate modules.  This gives you more flexibility in designing enterprise applications. Building an enterprise workflow application based on the Imixs-Workflow components you typical start with an enterprise archive (EAR).  Basically the structure of an EAR looks typical like this:
 
	  /
	  +- META-INF/
	  |  |- application.xml
	  |- my_ejb_module.jar
	  |- my_web_module.war

The first step to bundle the Imixs-Workflow engine together with your application is simple. 
Just copy the Imixs-Workflow core component and the Imixs-Workflow engine into the root of your EAR structure. So you get the following file structure:
 
	  /
	  +- META-INF/
	  |  |- application.xml
	  |- my_ejb_module.jar
	  |- my_web_module.war
	  |- imixs-workflow-core-3.0.0.jar
	  |- imixs-workflow-engine-3.0.0.jar

The Imixs Workfow Faces and the Imixs RESTful Web Services you can bundle as explained before directly into  your WEB-INF/lib folder of your web module (my_we_module.war). So these components are still part of your web module:

	  /
	  +- lib/
	  |  |- imixs-workflow-faces-3.0.0.jar
	  |  |- imixs-workflow-jax-rs-3.0.0.jar

To get the Imixs-Workflow engine deployed together with your business logic located in your EJB module (my_ejb_module.jar)  you need to configure the classpath of your EJB Module. This step makes the Imixs-Workflow engine visible to all other EAR modules but it allows you also to specify server specific configurations like the JDBC/Database or the security realm by overwriting the default settings used by the Imixs-Workflow eingine. The structure of your EJB Module as part of your EAR will typically look like this:
 
	  /
	  +- META-INF/
	  |  |- MANIFEST.MF
	  |  |- ejb-jar.xml
	  |  |- persistence.xml
 
The MANIFEST.MF file is used to add additional component libraries to be used together with your EJB module.  Simply add the Class-Path definition to the META-INF/MANIFEST.MF file of your ejb module including all Imixs jar files. If the MANIFEST.MF file did not yet exist create an empty file:
 
	Manifest-Version: 1.0
	Class-Path: imixs-workflow-core-3.0.0.jar imixs-workflow-engine-3.0.0.jar

This makes the Imixs-Workflow engine part of your EJB module. The ejb-jar.xml included in your EJB module  can be left empty as EJBs will be deployed automatically.
 But the ejb-jar.xml file gives you more control about the default behavior of the EJBs provided by the Imixs JEE components. For example if you want to define a 'run-as-principal' role to a ejb or a method. Or you would like to rename a ejb or inject a local jndi-mail or jndi-directory resource by name. 
 

##Adding a Database connection 
Finally you need to add a persistence.xml file into your ejb or web module. The persistence.xml file defines how the workitems managed by the Imixs-Workflow Engine will be persisted into a database.  Depending if you have bundled the Imixs-Workflow engine directly with your web application or configured it as a part of your EJB module you need to add a persistence.xml file into your module:
 
###persistence.xml in a web module 
If you have bundled the imixs-workflow-engine.jar directly into your web module add the persistence.xml into the WEB-INF/classes/META-INF folder:
 
	  /
	  +- WEB-INF/classes/META-INF/
	  |  |- persistence.xml
	  +- WEB-INF/lib/
	  |  |- imixs-workflow-core-3.0.0.jar
	  |  |- imixs-workflow-engine-3.0.0.jar

###persistence.xml in a ejb module 
If you have bundled the imixs-workflow-engine.jar into the root of your enterprise archive (ear) 
you need to add the persistence.xml into the /META-INF folder together with the ejb-jar.xml: 
 
	  +- META-INF/
	  |  |- MANIFEST.MF
	  |  |- ejb-jar.xml
	  |  |- persistence.xml
 
##How to configure the persistence.xml 
The persistence.xml describes the location of you database. The following example shows a typical  configuration using the Eclipselink driver provided by most JEE Application servers.
 
	<?xml version="1.0" encoding="UTF-8"?>
	<persistence version="1.0" xmlns="http://java.sun.com/xml/ns/persistence">
		<persistence-unit name="org.imixs.workflow.jee.jpa" transaction-type="JTA">	
			<provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>	
			<jta-data-source>jdbc/workflow-db</jta-data-source>
			<jar-file>lib/imixs-workflow-engine-3.0.0.jar</jar-file>
			<properties>
				<property name="eclipselink.ddl-generation"
					value="create-tables" />
				<property name="eclipselink.logging.level" value="INFO"/>
			</properties>				
		</persistence-unit>
	</persistence>  
 
Take care about the different settings used by the persistence.xml:
 
###persistence-unit:
 
The persistence unit name is fixed defined by the Imixs JEE Workflow Implementation and need to be set to  	"org.imixs.workflow.jee.jpa" which is the package name of the Entity EJBs provided by the Imixs JEE Implementation.
 
###jta-data-source:
The jta-data-source points to a JNDI Database resource located on the Application server. This JNDI Name is a JDBC Resource which is provided by the Application Server running the application. In Glassfish Server a JDBC Resource can be defined in the Section Resources - JDBC
 
###jar-file:
The jar-file defines the java library containing the Entity EJBs to be stored in the Database.
This tag should always point to the imixs-workflow-engine Jar File. Take care about the version number 	used by your application.
	
In the example the jta-data-source point to a JDBC Resource with the JNDI Name 'jdbc/workflow-db'. The jar-file points to the imixs-workflow-engine.jar part of your application. Take care about the right location and version number of this file!
 
The jndi database resource with the name "jdbc/workflow-db" have to be provided in your application server. 
 
 
  
##Using shared libraries
In difference to the deployment example shown above you can deploy part of the Imixs JEE components as shared libraries into a EAR. In this case you put the jars into the /lib/ folder of your EAR. Jars deployed into the /lib folder of an ear are visible to all other modules and components. Except for the imixs-jee-impl.jar you can place all Imixs jars into the lib. So in this case the EAR structure will look like this:
  
	  /
	  +- META-INF/
	  |  |- application.xml
	  |- lib/
	  |  |- imixs-workflow-core-3.0.0.jar
	  |- my_ejb_module.jar
	  |- my_web_module.war
	  |- imixs-workflow-engine-3.0.0.jar
 
If you prefer this EAR layout the EJB module only needs to include the imixs-workflow-engine-3.0.0.jar into the classpath. So the MANIFEST.MF file can be changed to:
   
	Manifest-Version: 1.0
	Class-Path: imixs-workflow-engine-3.0.0.jar

	