# WildFly Deployment Guide
This section will explain the configuration steps needed to successfully deploy the [Imixs-Sample Application](../sampleapplication.html) on Wildfly. The deployment guide can be used also for custom projects. See also the section [Deployment Guide](./deployment_guide.html) for general information about deployment of the Imixs-Workflow engine.


## Install Wildfly
The Wildfly Server is supporting the Jakarta EE specification and can be downloaded in the latest version from the [Wildfly project site](http://www.wildfly.org). The site also includes an installation guide how to install Wildfly server on different platforms. Please note that at least version 18.0.1 is needed as this version includes [Eclipse Microprofile](https://microprofile.io/). 

After the server is started it can be opened from a web browser with the following URL:

    http://localhost:8080/
      
## Setting up a Imixs-Workflow database pool

The [Imixs-Sample Application](../sampleapplication.html) expects a database resource with the name "jdbc/workflow". The corresponding datasource configuration can be added into the file _wildfly/standalone/configuration/standalone.xml_ in the subsystem section 'datasources' 


### MySQL
For MySQL the corresponding JDBC driver need to be deployed into Wildfly first. Copy the mysql-connector-java-bin.jar into the /deployment folder of Wildfly.

Next a datasource can be configured in the standlone.xml:

	...
	    <datasource jta="true" jndi-name="java:/jdbc/workflow" pool-name="workflow" enabled="true" use-ccm="true">
	    	 <connection-url>jdbc:mysql://localhost:3306/workflow_db</connection-url>
	    	 <driver-class>com.mysql.jdbc.Driver</driver-class>
	        <driver>mysql-connector-java-5.1.7-bin.jar</driver>
	        <security>
	       		<user-name>...</user-name>
	       		<password>...</password>
	       	 </security>
	       	 <validation>
	       		<valid-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLValidConnectionChecker"/>
	           <validate-on-match>true</validate-on-match>
	           <background-validation>false</background-validation>
	        	</validation>
	     </datasource>
	...
          
### PostgreSQL
For PostgreSQL the corresponding JDBC driver need to be deployed into Wildfly first. Copy the postgresql-jdbc41.jar into the /deployment folder of Wildfly.

Next a datasource can be configured in the standlone.xml:

	...                
	    <datasource jta="true" jndi-name="java:/jdbc/workflow" pool-name="workflow" enabled="true" use-ccm="true">
		   	<connection-url>jdbc:postgresql://localhost/workflow</connection-url>
	   		<driver-class>org.postgresql.Driver</driver-class>
	       	<driver>postgresql-9.3-1102.jdbc41.jar</driver>
	       	<security>
	       		<user-name>...</user-name>
	       		<password>...</password>
	      	</security>
	       	<validation>
	       		<validate-on-match>false</validate-on-match>
	           <background-validation>false</background-validation>
	          	</validation>
       </datasource>
	...
	                
The configuration for any other database like Oracle, Informix, Microsoft SQL Server can be addapted in a simmilar way.



### EclipseLink
The Imixs-Sample Application uses EclipseLink for JPA. Make sure that EclipseLink is configured in your Wildfly. To add EclipseLink to your configuration see the following steps:

First download the eclipseLink from [here](https://www.eclipse.org/eclipselink/downloads/). The Zip file includes the file _eclipselink.jar_. Copy this file _eclipselink.jar_ into the following location:

	modules/system/layers/base/org/eclipse/persistence/main

Next edit the file _module.xml_ located at the same location and add the following new resource definition:

	...
	<resources>
	  ....
	  <resource-root path="eclipselink.jar">
	    <filter>
	            <exclude path="javax/**" />
	    </filter>
	  </resource-root>
	  ...
	</resources>
	...

Finally add the org.jipijapa.eclipselink.JBossArchiveFactoryImpl to your configuration. This can be done by using the jboss-cli tool:

	./jboss-cli.sh --connect '/system-property=eclipselink.archive.factory:add(value=org.jipijapa.eclipselink.JBossArchiveFactoryImpl)'

This command will add the following entry into your standalone.xml configuration file:

	...
	 <system-properties>
	 ...
	 <property name="eclipselink.archive.factory" value="org.jipijapa.eclipselink.JBossArchiveFactoryImpl"/>
	 </system-properties>
	...

You can also edit the standalone.xml file directly if you have problems to use the command line tool.


## Setup a Security Realm
To login to the Imixs-Sample Application a security realm name 'imixsrealm' need to be provided. For Wildfly a security domain can be configured in the standalone.xml file. Wildfly supports a lot of different login modules which can be used.
Each user need to be mapped to one of the [Imixs security roles](../engine/acl.html).
The following table shows an example of a user list with different access levels:

| UserID       |GroupName                |Description                         | 
|--------------|-------------------------|------------------------------------|
|manfred       |IMIXS-WORKFLOW-Manager   | This user will have maximum access |
|eddy          |IMIXS-WORKFLOW-Editor    | User can edit all workitems         |
|anna          |IMIXS-WORKFLOW-Author    | User will be allowed to create workitems and edit his own     |
|ronny         |IMIXS-WORKFLOW-Reader    | This user will be only allowed to read workitems   |
|guest         |                         | This user will have no access (just to be sure security works well) 
  
  

### File Based Login Module
The _UsersRolesLoginModule_ is a simple login module that supports multiple users and user roles loaded from Java properties files:

	...
    <security-domain name="imixsrealm">
        <authentication>
            <login-module code="UsersRoles" flag="required">  
                <module-option name="usersProperties" value="${jboss.server.config.dir}/sampleapp-users.properties"/>  
                <module-option name="rolesProperties" value="${jboss.server.config.dir}/sampleapp-roles.properties"/>  
            </login-module>  
            <login-module code="RoleMapping" flag="required">
    			<module-option name="rolesProperties" value="file:${jboss.server.config.dir}/imixsrealm.properties"/>
        		<module-option name="replaceRole" value="false"/>
      	    </login-module>
        </authentication>
    </security-domain>
    ...
	
The sampleapp-users.properties file uses a username=password format with each user entry on a separate line:

	manfred=password1
	anna=password2
	...
            
The sampleapp-roles.properties file uses the pattern username=role1,role2, with an optional group name value. For example:

	manfred=IMIXS-WORKFLOW-Manager
	anna=IMIXS-WORKFLOW-Author
            

### Database Login Module
The following example shows a security-domain named 'imixs' using a Database login module:

	...
		<security-domain name="imixsrealm">
	   		<authentication>
	       		<login-module code="Database" flag="required">
	           		<module-option name="dsJndiName" value="java:/jdbc/my-user-db"/>
	                  <module-option name="hashAlgorithm" value="SHA-256"/>
	                  <module-option name="hashEncoding" value="hex"/>
	                  <module-option name="principalsQuery" value="select PASSWORD from USERID where ID=?"/>
	                  <module-option name="rolesQuery" value="select GROUP_ID,'Roles' from USERID_USERGROUP where ID=?"/>
	                  <module-option name="unauthenticatedIdentity" value="anonymous"/>
	           </login-module>
	           <login-module code="RoleMapping" flag="required">
	           	<module-option name="rolesProperties" value="file:${jboss.server.config.dir}/imixsrealm.properties"/>
	              	<module-option name="replaceRole" value="false"/>
	           </login-module>
	        </authentication>
		</security-domain>
	...
	                
### RoleMapping

To map the Imxis security roles to the corresponding groups provided by the security-domain a roleMapping section need to be included into the security-domain. The content of the file _imixsrealm.properties_ looks like this:

	IMIXS-WORKFLOW-Reader=org.imixs.ACCESSLEVEL.READERACCESS
	IMIXS-WORKFLOW-Author=org.imixs.ACCESSLEVEL.AUTHORACCESS
	IMIXS-WORKFLOW-Editor=org.imixs.ACCESSLEVEL.EDITORACCESS
	IMIXS-WORKFLOW-Manager=org.imixs.ACCESSLEVEL.MANAGERACCESS

The file can be used to map any other role into the security-domain as well.



## Deploy the Imixs-Sample Application
After the database and security domain are configured, the Imixs-Sample Application can be deployed.
Therefore it is sufficient to copy the .war file into the folder 

	/wildfly/standalone/deployments/

The application can be accessed from the URL

[http://localhost:8080/workflow](http://localhost:8080/workflow) 


## Need Help?

If you have any difficulty in deployment of your application, [contact the community for help](https://www.imixs.org/sub_community.html). Also if you have any tips and suggestions for improvements, please share them as well. 

 