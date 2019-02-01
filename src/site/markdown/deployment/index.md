# Deployment
The Imixs-Workflow engine can be deployed in various ways depending on the kind of the application and server environment. As the Imixs-Workflow engine is based on Java EE, the components are typically deployed into a container (EJB or Web Container). The deployment descriptors can be used to configure various details and the behavior of the Imixs-Workflow engine. This concept gives the flexibility to setup the Imixs-Workflow engine individually for custom environments and infrastructure. 

Before you deploy the Imixs-Workflow engine into an application server, consider the following:
 
  * Provide a database where the workflow data can be stored
  * Configure a [security realm](./deployment/security.html) for granting access to different actors
  * Design a workflow model using the [Imixs-BPMN](./modelling/index.html) 

## Database
The Imixs-Workflow engine stores the workflow model and its process instances into a database by using the Java Persistence API (JPA). Therefore a database pool need to be provided together with the container the Imixs-Workflow engine is deployed to. The configuration is done via the persistence.xml file. The persistence-unit name is 

	org.imixs.workflow.jpa

See the following example file, which referes to a JNDI Database Pool named 'jdbc/workflow-db':


	<?xml version="1.0" encoding="UTF-8"?>
	<persistence version="1.0" xmlns="http://java.sun.com/xml/ns/persistence">
		<persistence-unit name="org.imixs.workflow.jpa" transaction-type="JTA">	
			<provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>	
			<jta-data-source>jdbc/workflow-db</jta-data-source>
			<jar-file>lib/imixs-workflow-engine-${org.imixs.workflow.version}.jar</jar-file>
			<properties>
				<!-- target-database Auto MySQL PostgreSQL  -->
				<property name="eclipselink.target-database" value="Auto" />
				<property name="eclipselink.ddl-generation" value="create-tables" />
				<property name="eclipselink.deploy-on-startup" value="true" />
				<property name="eclipselink.logging.level" value="INFO" />	
			</properties>				
		</persistence-unit>
	</persistence> 


		
	

__Note:__ The jar-file must match the deployed version of the Imixs-Workflow engine jar. See the section [Deployment Guide](./deployment_guide.html) for further details.


## Security
Each back-end call to the Imixs-Workflow engine have to propagate an applicable user principal and security role to be verified by the back-end services. The security concept of Imixs-Workflow defines the following roles:

  * org.imixs.ACCESSLEVEL.READACCESS
  * org.imixs.ACCESSLEVEL.AUTHORACCESS
  * org.imixs.ACCESSLEVEL.EDITORACCESS
  * org.imixs.ACCESSLEVEL.MANAGERACCESS

Each user accessing the Imixs-Workflow Engine need to be assigned at least to one of these roles. To deploy the Imixs-Workflow engine a corresponding security realm have to be configured in the application server. See the [section Security](./security.html) for further details.

  
## Maven
All components of Imixs-Workflow are build with Maven which makes it easy to add them into a Maven based project. The following example adds the Imixs-Workflow Engine and the Imixs REST API into a maven based project:

	
	<properties>
		.....
		<org.imixs.workflow.version>4.0.0</org.imixs.workflow.version>
	</properties>
	...
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

Read the [section Maven](../maven.html) for further details about how to work with maven artifacts.


## Further Information

See the following section for details about how to deploy the Imixs-Workflow engine:

 * [General Deployment Guide](./deployment_guide.html)
 * [Wildfly Deployment Guide](./wildfly.html)
 * [GlassFish Deployment Guide](./glassfish.html)
 * [Maven](../maven.html)
 * [Database Schema](./database_schema.html)
 * [Security Issues](./security.html) 
 * [Concurrency](./concurrency.html) 
