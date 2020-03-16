# Deployment Guide
The following section gives an overview how to deploy the Imixs-Workflow engine into a Jakarta EE container. Imixs-Workflow consists of different modules which simply can be bundled together with other components of a business application. All Imixs-Workflow components are based on the [Jakarta EE](https://jakarta.ee/) and the [Eclipse Microprofile](https://microprofile.io/) component model. 
 
The examples illustrate how to deploy the components on a [Wildfly Application server](http://www.wildfly.org) which is an open source application server based on the Jakarta EE specification. The deployment is similar to all other application servers (e.g. [Glassfish](http://www.glassfish.org), [Payara](http://www.payara.fish/)...).
 
The Imixs-Workflow engine is based on JDK 1.8. The packaging will differ in some details depending on the environment you use. To deploy the Imixs-Workflow engine on a specific platform see also the chapters:

 * [Deployment Guide Wildfly](./wildfly.html)
 * [Deployment Guide GlassFish](./glassfish.html)
 * [Deployment Guide Apache TomEE](./tomee.html)

## Imixs-Workflow components
To bundle the Imixs-Workflow engine together with a business application the following components need to be added into the deployment:
 
  * _imixs-workflow-core-x.x.x.jar_  - contains the core api and xml api
  * _imixs-workflow-engine-x.x.x.jar_ - the workflow engine containing jpa and ejb components
  * _imixs-workflow-jax-rs-x.x.x.jar_ - contains the Imixs RESTful Web Service

For JSF Web Applications add also:
  
  * _imixs-workflow-faces-x.x.x.jar_ - contains optional JSF components
  
All artifacts can be downloaded from the [Maven central repository page](https://search.maven.org/). In case of maven the components will be downloaded and bundled automatically during the maven build process.
  
### Search Index  

The Imixs-Workflow engine includes a search index based on the [Lucene Search Technology](https://lucene.apache.org/). The search index is used to query documents. There are two different index implementations available:
  
  * _imixs-workflow-index-lucene-x.x.x.jar_ - Search index based on the Lucene Core engine
  * _imixs-workflow-index-solr-x.x.x.jar_ - Search index based on Lucene Solr
  
The Lucene-Core index is the default index for Imixs-Workflow it can be deployed by teh 
  
   
 
## Building an Web Application (WAR)
To install and deploy the Imixs-Workflow engine into a web application it is sufficient to bundle the  Imixs-Workflow components into the WEB-INF/lib folder. This is called the "EJB Lightweight Runtime Environment". To customize the deployment the deployment descriptor _ejb-jar.xml_ can be added into to WEB-INF/ folder. The persitence.xml is placed into the /WEB-INF/classes folder:
 
	  / 
	  +- WEB-INF/
	  |  |- ejb-jar.xml  (optional)
	  |  +- lib/
	  |  |  |- imixs-workflow-core-x.x.x.jar
	  |  |  |- imixs-workflow-engine-x.x.x.jar
	  |  |  |- imixs-workflow-faces-x.x.x.jar
	  |  |  |- imixs-workflow-jax-rs-x.x.x.jar
	  |  |  |- imixs-workflow-index-lucene-x.x.x.jar
	  |  |  |- ....
	  |  +- classes/
	  |  |  +- META-INF/
	  |  |  |  |- persistence.xml

The following example shows the maven dependencies used in a maven project:


	...
	<properties>
		<org.imixs.workflow.version>5.1.9</org.imixs.workflow.version>
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
			<artifactId>imixs-workflow-index-lucene</artifactId>
			<version>${org.imixs.workflow.version}</version>
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
	  |  |- imixs-workflow-core-x.x.x.jar
	  |  |- imixs-workflow-engine-x.x.x.jar

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
			<jta-data-source>jdbc/workflow</jta-data-source>
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
	
In the example the jta-data-source point to a JDBC Resource with the JNDI Name 'jdbc/workflow'. The jar-file points to the imixs-workflow-engine.jar part of your application. 
 
  