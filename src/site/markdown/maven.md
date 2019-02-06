# Maven 
The Imixs-Workflow project contains a great deal of technologies which helps building powerful workflow applications. To integrate the Imixs-Workflow Engine into your own code, the project provides Maven artifacts. [Maven](http://maven.apache.org) is a build and configuration tool which helps you to organize your project and finding necessary libraries and artifacts automatically through the Internet.  
 
All components of Imixs-Workflow are build with Maven which makes it easy to add them into a Maven based project. The following example adds the Imixs-Workflow Engine and the Imixs REST API into a maven based project:

	
	<properties>
		.....
		<org.imixs.workflow.version>4.5.0</org.imixs.workflow.version>
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

The latest version number can be look up by browsing the [maven-central-repository](http://search.maven.org/#browse). Also detailed information about each artifact are provided in separate sections of the different Imixs-Workflow subprojects.

## Downloads
All binaries from the Imixs-Workflow project are provided in the [maven-central-repository](http://search.maven.org/#browse). To download a binary directly from there you cab browse the [maven-central-repository](http://search.maven.org/#browse) and search for the keyword 'imixs'.
 


### Snapshot Releases 
Snapshot releases are newer releases of the Imixs-Workflow engine which are still under development.  A Snapshot release should only be used in cases where the latest final release did not provide a specific feature or bug-fix needed to work with.  The Imixs-Workflow snapshot releases are published into the [Sonatype Snapshot repository](http://oss.sonatype.org/content/repositories/snapshots).

<strong>Note:</strong> The snapshot repository should only be used if snapshot releases are necessary for a specific build!

To access the Snapshot repository the following additional repository location can be added into the Maven settings.xml configuration file or the project pom.xml. 
 
	
	...
		<repositories>
	      <!-- Sonatype Snapshot repository -->
	      <repository>
	          	<id>sonatype-snaptshots</id>
	          	<name>Sonatype Snapshot repository</name>
	          	<url>http://oss.sonatype.org/content/repositories/snapshots</url>
	      </repository>				
	    </repositories>
	....


## Java EE Module Configuration Using Maven

If you are using Imixs-Workflow in a Web Application or a a Microservice, there is no special configuration necessary. Maven will put all libraries into the location /WEB-INF/lib/ which is the default location for web modules. You can skip the following section if your are working with a maven web module. 

### Packaging the Imixs-Workflow engine into a EJB Module

Imixs-Workflow is based on EJB and can be combined with a custom EJB module within your application.  
Using the maven-ejb-plugin you can add a declaration for additional manifest entries. The following example shows how to add the additional configuration to your pom.xml 
 
	<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
		<parent>
			<artifactId>imixs-workflow-jsf-sample</artifactId>
			<groupId>org.imixs.workflow</groupId>
			<version>0.0.2-SNAPSHOT</version>
		</parent>
		<modelVersion>4.0.0</modelVersion>
		<groupId>org.imixs.workflow</groupId>
		<artifactId>imixs-workflow-jsf-sample-ejb</artifactId>
		<packaging>ejb</packaging>
		<version>0.0.2-SNAPSHOT</version>
		<properties> 
	    	<org.imixs.workflow.version>3.9.0</org.imixs.workflow.version>
	   </properties>
		<build>
			<plugins>
				<plugin>
					<groupId>org.apache.maven.plugins</groupId>
					<artifactId>maven-compiler-plugin</artifactId>
					<configuration>
						<source>1.7</source>
						<target>1.7</target>
					</configuration>
				</plugin>
				<plugin>
					<groupId>org.apache.maven.plugins</groupId>
					<artifactId>maven-ejb-plugin</artifactId>
					<configuration>
						<ejbVersion>3.0</ejbVersion>
						<archive>
							<!-- add the EJB module imixs-workflow-engine -->
							<manifestEntries>
								<Class-Path>imixs-workflow-engine-${org.imixs.workflow.version}.jar imixs-workflow-core-${org.imixs.workflow.version}.jar</Class-Path>
							</manifestEntries>
						</archive>
					</configuration>
				</plugin>
			</plugins>
		</build>
		<dependencies>
		</dependencies>
	</project>
 
__Note:__ The jar versions defined by the Class-Path entry need to match the jar versions provided by the module!
 
### Packaging the Imixs-Workflow engine into an EAR
To package the Imixs-Workflow engine into an EAR the pom.xml can be configured as shown in the following example:
 
	...
		<build>
			<plugins>
				<plugin>
					<groupId>org.apache.maven.plugins</groupId>
					<artifactId>maven-ear-plugin</artifactId>
					<configuration>
						<version>5</version>
						<modules>
							<!-- Web Module -->
							....
							<!-- EJB Module -->
							...
							<!-- Imixs-Workflow -->
							<JarModule>
								<groupId>org.imixs.workflow</groupId>
								<artifactId>imixs-workflow-engine </artifactId>
							</JarModule>	
							<JarModule>
								<groupId>org.imixs.workflow</groupId>
								<artifactId> imixs-workflow-core </artifactId>							
							</JarModule>
							....
						</modules>
					</configuration>
				</plugin>
			</plugins>
		</build>
		<dependencies>
			<!-- Imixs Workflow  -->
			<dependency>
				<groupId>org.imixs.workflow</groupId>
				<artifactId>imixs-workflow-core</artifactId>
				<type>jar</type>
			</dependency>
			<dependency>
				<groupId>org.imixs.workflow</groupId>
				<artifactId>imixs-workflow-engine</artifactId>
				<type>jar</type>
			</dependency>
		
			....
		</dependencies>

 
__Note:__ The deployment descriptors for the EJB Module (ejb-jar.xml) are not part of the Imixs-Workflow jar files and need to be provided by the deployed application. This enables the application to provide custom environment specific configurations. See the following example of a ejb-jar.xml file providing the JNDI Mail resource to the Imixs-Workflow engine:


	<?xml version="1.0" encoding="UTF-8"?>
	<ejb-jar xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xmlns="http://java.sun.com/xml/ns/javaee" xmlns:ejb="http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd"
		xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd"
		version="3.0">
		<enterprise-beans>
			<session>
				<ejb-name>WorkflowService</ejb-name>
				<ejb-class>org.imixs.workflow.jee.ejb.WorkflowService</ejb-class>
				<session-type>Stateless</session-type>			
				<!-- Mail Configuration -->
				<env-entry>
					<description>Mail Plugin Session name</description>
					<env-entry-name>IMIXS_MAIL_SESSION</env-entry-name>
					<env-entry-type>java.lang.String</env-entry-type>
					<env-entry-value>java:/mail/org.imixs.workflow.mail</env-entry-value>
				</env-entry>
				<ejb-ref>
					<ejb-ref-name>ejb/PropertyService</ejb-ref-name>
					<ejb-ref-type>Session</ejb-ref-type>
					<remote>org.imixs.workflow.jee.util.PropertyService</remote>
				</ejb-ref>
				<!-- Mail resource -->
				<resource-ref>
					<res-ref-name>java:/mail/org.imixs.workflow.mail</res-ref-name>
					<res-type>javax.mail.Session</res-type>
					<res-auth>Container</res-auth>
					<res-sharing-scope>Shareable</res-sharing-scope>
				</resource-ref>
			</session>
		</enterprise-beans>
	</ejb-jar>
 