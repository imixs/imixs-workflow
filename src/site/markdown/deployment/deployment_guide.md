# Deployment Guide
The following section gives an overview how to deploy the Imixs-Workflow engine into a Java EE container. Imixs-Workflow consists of different modules which simply can be bundled together with other components of a business application. All Imixs-Workflow components are based on the Java EE component model.
 
The examples illustrate how to deploy the components on a [JBoss/Wildfly Application server](http://www.wildfly.org) which is an open source application server based on the Java EE specification. The deployment is similar to all other Java EE application servers (e.g. [Glassfish](http://www.glassfish.org), [Payara](http://www.payara.fish/)...).
 
The Imixs-Workflow engine is based on JDK 1.8 and can be deployed into a Java EE web or EJB container. The packaging will differ in some details. Both deployment concepts will be explained in the following section. 

To deploy the Imixs-Workflow engine on a specific platform see also the chapters:

 * [Deployment Guide Wildfly](./wildfly.html)
 * [Deployment Guide GlassFish](./glassfish.html)
 * [Deployment Guide Apache TomEE](./tomee.html)

## Imixs-Workflow components
To bundle the Imixs-Workflow engine together with a business application the following components need to be added into the deployment:
 
  * _imixs-workflow-core-4.x.x.jar_  - contains the core api and xml api
  * _imixs-workflow-engine-4.x.x.jar_ - the jee workflow engine containing jpa and ejb components

For following components are used for web applications or the Rest API
  
  * _imixs-workflow-faces-4.x.x.jar_ - contains optional JSF components
  * _imixs-workflow-jax-rs-4.x.x.jar_ - contains the Imixs RESTful Web Service
  
All artifacts can be downloaded from the [Maven central repository page](https://search.maven.org/). In case of maven the components will be downloaded and bundled automatically during the maven build process.
  
### Lucene  

The Imixs-Workflow engine uses a [Lucene Search Index](https://lucene.apache.org/) to query documents. Therefore the following lucene artifacts need to be added into the deployment unit:
  
  * lucene-core-6.x.x.jar - Lucene core engine
  * lucene-analyzers-common - Lucene analyzers 
  * lucene-queryparser - Lucene query parser
  * lucene-codecs - codecs
  
  
   
 
## Building an Web Application (WAR)
To install and deploy the Imixs-Workflow engine into a web application it is sufficient to bundle the  Imixs-Workflow components and Luncene libraries into the WEB-INF/lib folder. This is called the "EJB Lightway Runtime Environment". To customize the deployment the deployment descriptor _ejb-jar.xml_ should be added into to WEB-INF/ folder. The persitence.xml is placed into the /WEB-INF/classes folder:
 
	  / 
	  +- WEB-INF/
	  |  |- ejb-jar.xml  (optional)
	  |  +- lib/
	  |  |  |- imixs-workflow-core-4.1.2.jar
	  |  |  |- imixs-workflow-engine-4.1.2.jar
	  |  |  |- imixs-workflow-faces-4.1.2.jar
	  |  |  |- imixs-workflow-jax-rs-4.1.2.jar
	  |  |  |- lucene-codecs-6.3.0.jar
	  |  |  |- lucene-core-6.3.0.jar
	  |  |  |- lucene-analyzers-common-6.3.0.jar
	  |  |  |- lucene-queries-6.3.0.jar
	  |  |  |- lucene-queryparser-6.3.0.jar
	  |  |  |- lucene-sandbox-6.3.0.jar
	  |  +- classes/
	  |  |  +- META-INF/
	  |  |  |  |- persistence.xml

The following example shows the maven dependencies used in a maven project:


	...
	<properties>
		<org.imixs.workflow.version>4.1.2</org.imixs.workflow.version>
		<lucene.version>6.3.0</lucene.version>
	</properties>
	... 
	<dependencies>
	<!-- Imixs Workflow -->
		<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-workflow-engine</artifactId>
			<version>${org.imixs.workflow.version}</version>
		</dependency>
		<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-workflow-jax-rs</artifactId>
			<version>${org.imixs.workflow.version}</version>
		</dependency>
		<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-workflow-faces</artifactId>
			<version>${org.imixs.workflow.version}</version>
		</dependency>
		<!-- Apache Lucene -->
		<dependency>
			<groupId>org.apache.lucene</groupId>
			<artifactId>lucene-core</artifactId>
			<version>${lucene.version}</version>
		</dependency>
		<dependency>
			<groupId>org.apache.lucene</groupId>
			<artifactId>lucene-analyzers-common</artifactId>
			<version>${lucene.version}</version>
		</dependency>
		<dependency>
			<groupId>org.apache.lucene</groupId>
			<artifactId>lucene-queryparser</artifactId>
			<version>${lucene.version}</version>
		</dependency>
		<dependency>
			<groupId>org.apache.lucene</groupId>
			<artifactId>lucene-codecs</artifactId>
			<version>${lucene.version}</version>
		</dependency>
	</dependencies>
	
The latest versions can be found in the [Maven repository](http://search.maven.org/#browse). 
 

 

## Adding a Database connection 
Finally the _persistence.xml_ file need to be added into the ejb or web module. The _persistence.xml_ defines how the entity beans managed by the Imixs-Workflow Engine which will be persisted into the database. 

### persistence.xml in a web module 
If the imixs-workflow-engine.jar is bundled directly into a web module, the persistence.xml need to be placed into the WEB-INF/classes/META-INF folder:
 
	  /
	  +- WEB-INF/classes/META-INF/
	  |  |- persistence.xml
	  +- WEB-INF/lib/
	  |  |- imixs-workflow-core-3.0.0.jar
	  |  |- imixs-workflow-engine-3.0.0.jar

### persistence.xml in a ejb module 
In case the imixs-workflow-engine.jar is bundled into a EJB module of an enterprise archive (ear), the persistence.xml need to be placed into the /META-INF folder together with the ejb-jar.xml: 
 
	  +- META-INF/
	  |  |- MANIFEST.MF
	  |  |- ejb-jar.xml
	  |  |- persistence.xml
 
## How to configure the persistence.xml 
The persistence.xml describes the location of the database and the entity beans to be persisted. The following example shows a typical  configuration using the Eclipselink driver:
 
	<?xml version="1.0" encoding="UTF-8"?>
	<persistence version="1.0" xmlns="http://java.sun.com/xml/ns/persistence">
		<persistence-unit name="org.imixs.workflow.jpa" transaction-type="JTA">	
			<provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>	
			<jta-data-source>jdbc/workflow-db</jta-data-source>
			<jar-file>lib/imixs-workflow-engine-${org.imixs.workflow.version}.jar</jar-file>
			<properties>
				<property name="eclipselink.target-database" value="Auto" />
				<property name="eclipselink.ddl-generation" value="create-tables" />
				<property name="eclipselink.deploy-on-startup" value="true" />
				<property name="eclipselink.logging.level" value="INFO" />	
			</properties>				
		</persistence-unit>
	</persistence>
 
The following section gives a short overview about the different settings used by the persistence.xml:
 
### persistence-unit:
The persistence unit name is fixed defined by the Imixs-Workflow implementation and need to be set to "_org.imixs.workflow.jpa_".
 
### jta-data-source:
The jta-data-source points to a JNDI Database resource located on the application server. This JNDI Name is a JDBC Resource which is provided by the application server running the application.
 
### jar-file:
The jar-file defines the java library containing the Entity Beans to be persisted into the Database.
This tag should always point to the imixs-workflow-engine Jar File. The version number must match the deployed component version.
	
In the example the jta-data-source point to a JDBC Resource with the JNDI Name 'jdbc/workflow-db'. The jar-file points to the imixs-workflow-engine.jar part of your application. 
 
  
## Using shared libraries
In difference to the deployment example shown above it is also possible to deploy part of the Imixs-Workflow components as shared libraries into an EAR. In this case the jars are put into the /lib/ folder of the EAR. Jars deployed into the /lib folder of an ear are visible to all other modules and components. Except for the imixs-workflow-engine.jar all Imixs jars can be placed into the lib/ directory. The EAR structure will look like this:
  
	  /
	  +- META-INF/
	  |  |- application.xml
	  |- lib/
	  |  |- imixs-workflow-core-4.0.0.jar
	  |  |- lucene-codecs-6.2.0.jar
	  |  |- lucene-core-6.2.0.jar
	  |  |- lucene-analyzers-common-6.2.0.jar
	  |  |- lucene-queries-6.2.0.jar
	  |  |- lucene-queryparser-6.2.0.jar
	  |  |- lucene-sandbox-6.2.0.jar
	  |- my_ejb_module.jar
	  |- my_web_module.war
	  |- imixs-workflow-engine-4.0.0.jar
 
In this EAR layout the EJB module only needs to include the imixs-workflow-engine.jar into the classpath. So the MANIFEST.MF file can be changed to:
   
	Manifest-Version: 1.0
	Class-Path: imixs-workflow-engine-4.0.0.jar

 
## Building an Enterprise Archive (EAR)
Deploying the Imixs-Workflow engine into a Enterprise Archive (EAR) differs in some details from the deployment into a web application as explained before. An EAR splits the business logic (EJBs) and the users web front-end (WAR) into separate modules.  This gives more flexibility in designing enterprise applications. Basically the structure of an EAR looks typical like this:
 
	  /
	  +- META-INF/
	  |  |- application.xml
	  |- my_ejb_module.jar
	  |- my_web_module.war

The components imixs-workflow-core and imixs-workflow-engine are added into the root of the EAR structure. The lucene components can be added into the /lib/ folder:
 
	  /
	  +- META-INF/
	  |  |- application.xml
	  |- my_ejb_module.jar
	  |- my_web_module.war
	  |- imixs-workflow-core-3.0.0.jar
	  |- imixs-workflow-engine-3.0.0.jar
	  +- lib/
	  |  |- lucene-codecs-6.2.0.jar
	  |  |- lucene-core-6.2.0.jar
	  |  |- lucene-analyzers-common-6.2.0.jar
	  |  |- lucene-queries-6.2.0.jar
	  |  |- lucene-queryparser-6.2.0.jar
	  |  |- lucene-sandbox-6.2.0.jar



The components imixs-workflow-faces and imixs-workflow-jax-rs are bundled into the corresponding web module as explained before. So these components are still part of your web module:

	  /
	  +- lib/
	  |  |- imixs-workflow-faces-3.0.0.jar
	  |  |- imixs-workflow-jax-rs-3.0.0.jar

To get the Imixs-Workflow engine deployed together with custom business logic located in the EJB module (my_ejb_module.jar) it is necessary to configure the classpath of the EJB Module. This step makes the Imixs-Workflow engine visible to all other EAR modules but it allows also to specify server specific configurations like the JDBC/Database or the security realm by overwriting the default settings. The structure of the EJB Module as part of your EAR will typically look like this:
 
	  /
	  +- META-INF/
	  |  |- MANIFEST.MF
	  |  |- ejb-jar.xml
	  |  |- persistence.xml
 
The MANIFEST.MF file is used to add additional component libraries to be used together with your EJB module.  The Class-Path definition need to be added into the META-INF/MANIFEST.MF file of your ejb module including all Imixs jar files:
 
	Manifest-Version: 1.0
	Class-Path: imixs-workflow-core-4.0.0.jar imixs-workflow-engine-4.0.0.jar

This makes the Imixs-Workflow engine part of your EJB module. The ejb-jar.xml included in your EJB module  can be left empty as EJBs will be deployed automatically. The _ejb-jar.xml_ deployment descriptor allows to control about the default behavior of the EJBs provided by the Imixs-Workflow engine. For example in case to define a 'run-as-principal' role to a ejb or a method or to inject a local jndi-mail or jndi-directory resource. 	