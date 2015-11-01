# Deployment
Imixs-Workflow can be deployed in various ways depending on the kind of the application and the used server environment. As the Imixs-Workflow Engine is based on JEE, the components are typically deployed in a container. Deployment descriptors are used to configure the details of an environment giving a flexible way to setup a workflow application. 

## Maven
All components of Imixs-Workflow are build with Maven which makes it easy to add them into a Maven based project. The following example adds the latest release of the Imixs-Workflow Engine and REST API to a project:

	<dependency>
		<groupId>org.imixs.workflow</groupId>
		<artifactId>imixs-workflow-engine</artifactId>
		<version>RELEASE</version>
	</dependency>
	<dependency>
		<groupId>org.imixs.workflow</groupId>
		<artifactId>imixs-workflow-jax-rs</artifactId>
		<version>RELEASE</version>
	</dependency>

Read the section [Maven](../maven.html) for further details.

## Database
The Imixs-Workflow Engine stores the workflow model and its process instances into a database using the Java Persistence API (JPA). Therefore a database pool need to be provided together with the container the workflow engine is deployed to. The configuration is done via the persistence.xml file deployed together with the Imixs-Workflow Engine. See the following example:


	<?xml version="1.0" encoding="UTF-8"?>
	<persistence version="1.0" xmlns="http://java.sun.com/xml/ns/persistence">
		<persistence-unit name="org.imixs.workflow.jee.jpa" transaction-type="JTA">	
			<provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>	
			<jta-data-source>jdbc/workflow-db</jta-data-source>
			<jar-file>lib/imixs-workflow-engine-${org.imixs.workflow.version}.jar</jar-file>
			<properties>
				<property name="eclipselink.target-database" value="Auto" />
				<property name="eclipselink.ddl-generation" value="create-tables" />
				<property name="eclipselink.deploy-on-startup" value="true" />
			</properties>				
		</persistence-unit>
	</persistence> 

See the section [Deployment Guide](./deployment_guide.html) for further details.


## Security
Each method call to the Imixs Workflow System have to possess an applicable authentication process to grant the demands of the Imixs Workflow technology according security. 
In the security concept of the Imixs-Workflow there are 5 roles defined:

  * org.imixs.ACCESSLEVEL.NOACCESS  
  * org.imixs.ACCESSLEVEL.READACCESS
  * org.imixs.ACCESSLEVEL.AUTHORACCESS
  * org.imixs.ACCESSLEVEL.EDITORACCESS
  * org.imixs.ACCESSLEVEL.MANAGERACCESS

Each user accessing the Imixs-Workflow Engine need to be assigned to one of these roles. The security configuration is typical configured by a security realm inside the application server. See the section [Security](./security.html) for further details.