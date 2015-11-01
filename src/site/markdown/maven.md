# Maven 
The Imixs-Workflow project contains a great deal of technologies which helps building powerful workflow applications. To integrate the Imixs-Workflow Engine into a business application, the project provides Maven artifacts.

All results from the Imixs-Workflow project are provided as maven artifacts.  Maven is a build and configuration tool which helps you to organize your project and finding necessary libraries and artifacts through the Internet.  Working with maven makes it almost simple to build applications based on different libraries or frameworks.
 
Imixs-Workflow supports maven and allows you to access all libraries easily to build your own project with workflow components. General information about using maven in a project you will
 find in the [Maven project site](http://maven.apache.org).

##Downloads
All binaries from the Imixs-Workflow project are provided in the [maven-central-repository](http://search.maven.org/#browse). You can download each binary directly from there if you 
 browse the [maven-central-repository](http://search.maven.org/#browse) and search for the keyword 'imixs'.
 

##Maven Dependency 
To add the dependency of Imixs-Workflow to your own maven proejct just add a new dependency to your pom.xml like  shown in the following example:

	<dependency>
		<groupId>org.imixs.workflow</groupId>
		<artifactId>imixs-workflow-core</artifactId>
		<version>RELEASE</version>
		<type>jar</type>
	</dependency>
  
Please take care about the version number. You will find the latest version number by browsing the [maven-central-repository](http://search.maven.org/#browse). You will find also more detailed informations about adding dependencies in the separate sections of the different Imixs-Workflow subprojects.

##Snapshot Releases 
Snapshot releases are newer releases of a component which are still under development.  A Snapshot release should only be used in cases where the latest final release did not provide a
 special feature or implementation you need to work with.  You will find the Imixs snapshot releases at the [Sonatype Snapshot repository](http://oss.sonatype.org/content/repositories/snapshots).

<strong>Note:</strong> The snapshot repository should only be used if snapshot releases are necessary for a specific build.

To access the Snapshot repository you need to add an additional configuration into your Maven settings.xml configuration file. On windows this config file is typically found or to be created  in 'Documents and Settings/USERNAME/.m2'. On Linux the folder /.m2 will be found on the users home directory. To access the Sonatype Snapshot repository you only need to add the repository location. Here is an example of a settings.xml file. 
 
	<settings xmlns="http://maven.apache.org/POM/4.0.0"
	  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
	                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
	 
	  <profiles>
	      <profile>
	          <id>default</id>
	          <activation>
	          </activation>
	          <repositories>
	            <!-- Sonatype Snapshot repository -->
	            <repository>
	            	<id>sonatype-snaptshots</id>
	            	<name>Sonatype Snapshot repository</name>
	            	<url>http://oss.sonatype.org/content/repositories/snapshots</url>
	            </repository>				
	          </repositories>
	      </profile>
	  </profiles>
	  <activeProfiles>
	    <activeProfile>default</activeProfile>
	  </activeProfiles> 
	</settings> 



##Deployment using Maven
Using [Maven](/api/maven_howto.html) makes it easy to setup an Enterprise Workflow Application.
Most of the steps described in the {{{./deployment.html}deplyoment guide}} can be simplified.
The following section will describe how you can use maven in your EAR and how you should setup the EAR and EJB pom.xml files.
 
###Configuring the EJB Module using maven
Using the maven-ejb-plugin you can add a declaration for additional manifest entries. This makes it easy to add the Imixs JEE Components into your EJB module.  The following example shows how to add the additional configuration to your pom.xml 
 
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
		<build>
			<plugins>
				<plugin>
					<groupId>org.apache.maven.plugins</groupId>
					<artifactId>maven-compiler-plugin</artifactId>
					<configuration>
						<source>1.6</source>
						<target>1.6</target>
					</configuration>
				</plugin>
				<plugin>
					<groupId>org.apache.maven.plugins</groupId>
					<artifactId>maven-ejb-plugin</artifactId>
					<configuration>
						<ejbVersion>3.0</ejbVersion>
						<archive>
							<!-- add the EJB module imixs-workflow-jee-impl -->
							<manifestEntries>
								<Class-Path>imixs-workflow-engine-3.0.0.jar imixs-workflow-core-3.0.0.jar</Class-Path>
							</manifestEntries>
						</archive>
					</configuration>
				</plugin>
			</plugins>
		</build>
		<dependencies>
		</dependencies>
	</project>
 
Be careful that the jar versions defined by the Class-Path entry are matching the versions provided by  the EAR module!
 
###Packaging the Imixs JEE Component into an EAR using maven
To package the Imixs JEE components into your EAR you can extend the pom.xml of your ear module. The following example shows how you define the structure of your EAR using maven:
 
	<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
		<description>jsf sample application</description>
		<parent>
			<artifactId>imixs-workflow-jsf-sample</artifactId>
			<groupId>org.imixs.workflow</groupId>
			<version>0.0.2-SNAPSHOT</version>
		</parent>
		<modelVersion>4.0.0</modelVersion>
		<groupId>org.imixs.workflow</groupId>
		<artifactId>imixs-workflow-jsf-sample-ear</artifactId>
		<packaging>ear</packaging>
		<version>0.0.2-SNAPSHOT</version>
		<build>
			<plugins>
				<plugin>
					<groupId>org.apache.maven.plugins</groupId>
					<artifactId>maven-ear-plugin</artifactId>
					<configuration>
						<version>5</version>
						<modules>
							<webModule>
								<groupId>org.imixs.workflow</groupId>
								<artifactId>imixs-workflow-jsf-sample-web </artifactId>
								<contextRoot>/workflow</contextRoot>
							</webModule>
							<!--  -->
							<ejbModule>
								<groupId>org.imixs.workflow</groupId>
								<artifactId>imixs-workflow-jsf-sample-ejb</artifactId>
							</ejbModule>
							
							<!-- EJB JPA -->
							<JarModule>
								<groupId>org.imixs.workflow</groupId>
								<artifactId>imixs-workflow-engine </artifactId>
							</JarModule>	
							
							<!-- Imixs Shared Libs -->
							<JarModule>
								<groupId>org.imixs.workflow</groupId>
								<artifactId> imixs-workflow-core </artifactId>							
							</JarModule>
							
						
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
		
			<!-- Application dependencies -->
			<dependency>
				<groupId>org.imixs.workflow</groupId>
				<artifactId>imixs-workflow-jsf-sample-web</artifactId>
				<type>war</type>
				<version>0.0.2-SNAPSHOT</version>
			</dependency>
			<dependency>
				<groupId>org.imixs.workflow</groupId>
				<artifactId>imixs-workflow-jsf-sample-ejb</artifactId>
				<type>ejb</type>
				<version>0.0.2-SNAPSHOT</version>
			</dependency>
		</dependencies>
	</project>

 
 
 