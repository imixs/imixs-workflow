# Deployment
The Imixs-Workflow engine can be deployed in various ways depending on the kind of your server environment. As Imixs-Workflow is based on Jakarta EE, the engine runs in a n EJB and Web Container. This concept gives you the flexibility to setup the Imixs-Workflow engine individually for custom applications and server infrastructure. 

Before you deploy the Imixs-Workflow engine into an application server, consider the following:
 
  * Provide a database where the workflow data will be stored
  * Configure a [security realm](./deployment/security.html) for granting access to different actors
  * Optional configure a OR-Mapper like [EclipseLink](https://www.eclipse.org/eclipselink/) 

If you want to start Imixs-Workflow out of the box follow the [Docker Setup Guide](../docker.html).
 
## Database
The Imixs-Workflow engine stores its workflow data into a database. Therefore a database pool need to be provided before you deploy Imixs-Workflow into an application server. The connection between Imixs-Workflow and the database pool is established by the Java Persistence API (JPA). So there is no need to create ad database schema manually. 

See the following example of a persistence.xml:


	<?xml version="1.0" encoding="UTF-8"?>
	<persistence version="1.0" xmlns="http://java.sun.com/xml/ns/persistence">
		<persistence-unit name="org.imixs.workflow.jpa" transaction-type="JTA">	
			<provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>	
			<jta-data-source>jdbc/workflow</jta-data-source>
			<jar-file>lib/imixs-workflow-engine-${org.imixs.workflow.version}.jar</jar-file>
			<properties>
				<!-- target-database Auto MySQL PostgreSQL  -->
				<property name="eclipselink.target-database" value="Auto" />
				<property name="eclipselink.ddl-generation" value="create-or-extend-tables" />
				<property name="eclipselink.deploy-on-startup" value="true" />
				<property name="eclipselink.logging.level" value="INFO" />	
			</properties>				
		</persistence-unit>
	</persistence> 

 * The **jta-data-source** (in this example 'jdbc/workflow') must match the database pool in your application server. 
 * The **persistence-unit** name refers to the Imixs-Workflow engine and must always be set to _org.imixs.workflow.jpa_.  
 * The **jar-file** must match the deployed version of the Imixs-Workflow engine jar within your application (be careful with the right version number)

## Security

To access the Imixs-Workflow engine, users need to be authenticated. This requires setting up a security realm. The security concept of Imixs-Workflow defines the following roles:

  * org.imixs.ACCESSLEVEL.READACCESS
  * org.imixs.ACCESSLEVEL.AUTHORACCESS
  * org.imixs.ACCESSLEVEL.EDITORACCESS
  * org.imixs.ACCESSLEVEL.MANAGERACCESS

Each user accessing the Imixs-Workflow Engine need to be assigned at least to one of these roles. To deploy the Imixs-Workflow engine a corresponding security realm have to be configured in the application server.  

Configuring a security realm depends on the application server platform. See the [section Security](./security.html) for further details.

  
## What's Next...

Read more about deployment:

 * [General Deployment Guide](./deployment_guide.html)
 * [Wildfly Deployment Guide](./wildfly.html)
 * [GlassFish Deployment Guide](./glassfish.html)
 * [TomEE Deployment Guide](./tomee.html)
 * [Database Schema](./database_schema.html)
 * [Security Issues](./security.html) 
 * [Concurrency](./concurrency.html) 
