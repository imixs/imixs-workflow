# Security Configuration
Each method call to the Imixs-Workflow engine have to possess an applicable authentication process to grant the demands of the Imixs-Workflow security layer. The security layer of Imixs-Workflow defines the following access roles:

  * org.imixs.ACCESSLEVEL.NOACCESS  
  * org.imixs.ACCESSLEVEL.READACCESS
  * org.imixs.ACCESSLEVEL.AUTHORACCESS
  * org.imixs.ACCESSLEVEL.EDITORACCESS
  * org.imixs.ACCESSLEVEL.MANAGERACCESS

Each user accessing the Imixs-Workflow engine should be mapped to one of these roles. The user roles can be mapped in an application to corresponding groups based on a authentication realm.
You will find more information about the general ACL concept of the Imixs-Workflow engine in the [section ACL](../engine/acl.html). 

See the following section for details

## JBoss/Wildfly

Running Imixs-Workflow on JBoss/Wildfly the definition of a security-domain is mandatory. For Webservices or Web Front-Ends the security-domain can be defined in the file jboss-web.xml which is located in the WEB-INF folder of each web module. The following example defiens a security domain 'imixsrealm':

	<?xml version="1.0" encoding="UTF-8"?>
	<jboss-web>
	  <security-domain>imixsrealm</security-domain>
	</jboss-web>

For WildFly there is no explicit role-group mapping necessary. The Roles defined by an application can be directly used in the security configuration.
In case you have existing group mappings (e.g. in a database group table or in a LDAP directory) you can add the mapping by defining a file _app.properties_, where app is the name of the security domain, as defined above (e.g. imixsrealm.properties). This file is located unter the /configuration directory of Wildfly.

The following example file _imixsrealm.properties_  maps the individual group names to Imixs-Workflow access roles:

	IMIXS-WORKFLOW-Reader=org.imixs.ACCESSLEVEL.READERACCESS
	IMIXS-WORKFLOW-Author=org.imixs.ACCESSLEVEL.AUTHORACCESS
	IMIXS-WORKFLOW-Editor=org.imixs.ACCESSLEVEL.EDITORACCESS
	IMIXS-WORKFLOW-Manager=org.imixs.ACCESSLEVEL.MANAGERACCESS

Groupnames are listed on the left of the equal operator and roles are listed on the right. In the example above, users in the group ‘IMIXS-WORKFLOW-Reader’ fulfill the role ‘org.imixs.ACCESSLEVEL.READACCESS’.

Finally an appropriate security-domain have to be configured in the standalone.xml file of Wildfly. See the following example of a database realm:

	<security-domain name="imixsrealm">
	 <authentication>
	 	<login-module code="Database" flag="required">
	 		<module-option name="dsJndiName" value="java:/jdbc/imixs-workflow"/>
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
 

## GlassFish
The following section holds deployment strategies for the GlassFish platform.
For Glassfish the security roles defined by the Imixs-Workflow Engine need to be mapped in an application to corresponding groups defined by a authentication realm.

The following example shows the glassfish-web.xml deployment descriptor for GlassFish Server which maps these roles to corresponding groups:
 
	<?xml version="1.0" encoding="UTF-8"?>
	<!DOCTYPE glassfish-web-app PUBLIC "-//GlassFish.org//DTD GlassFish Application Server 3.1 Servlet 3.0//EN" "http://glassfish.org/dtds/glassfish-web-app_3_0-1.dtd">
	<glassfish-web-app>
		<context-root>/imixs-workflow</context-root>
		<security-role-mapping>
			<role-name>org.imixs.ACCESSLEVEL.NOACCESS</role-name>
			<group-name>Noaccess</group-name>
			<group-name>IMIXS-WORKFLOW-Noaccess</group-name>
		</security-role-mapping>
		<security-role-mapping>
			<role-name>org.imixs.ACCESSLEVEL.READERACCESS</role-name>
			<group-name>Reader</group-name>
			<group-name>IMIXS-WORKFLOW-Reader</group-name>
		</security-role-mapping>
		<security-role-mapping>
			<role-name>org.imixs.ACCESSLEVEL.AUTHORACCESS</role-name>
			<group-name>Author</group-name>
			<group-name>IMIXS-WORKFLOW-Author</group-name>
		</security-role-mapping>
		<security-role-mapping>
			<role-name>org.imixs.ACCESSLEVEL.EDITORACCESS</role-name>
			<group-name>Editor</group-name>
			<group-name>IMIXS-WORKFLOW-Editor</group-name>
		</security-role-mapping>
		<security-role-mapping>
			<role-name>org.imixs.ACCESSLEVEL.MANAGERACCESS</role-name>
			<group-name>Manager</group-name>
			<group-name>IMIXS-WORKFLOW-Manager</group-name>
		</security-role-mapping>
	</glassfish-web-app>


This mapping can also be done in the glassfish-ejb-jar.xml inside an EJB module or the glassfish-application.xml for a EAR deployment. See the following example shows an glassfish-application.xml with a corresponding roles mapping for the realm 'imixsrealm':

	 <?xml version="1.0" encoding="UTF-8"?>
	<!DOCTYPE glassfish-application PUBLIC "-//GlassFish.org//DTD GlassFish Application Server 3.1 Java EE Application 6.0//EN" "http://glassfish.org/dtds/glassfish-application_6_0-1.dtd">
	<glassfish-application>
		<security-role-mapping>
			<role-name>org.imixs.ACCESSLEVEL.NOACCESS</role-name>
			<group-name>IMIXS-WORKFLOW-Noaccess</group-name>
		</security-role-mapping>
		<security-role-mapping>
			<role-name>org.imixs.ACCESSLEVEL.READERACCESS</role-name>
			<group-name>IMIXS-WORKFLOW-Reader</group-name>
		</security-role-mapping>
		<security-role-mapping>
			<role-name>org.imixs.ACCESSLEVEL.AUTHORACCESS</role-name>
			<group-name>IMIXS-WORKFLOW-Author</group-name>
		</security-role-mapping>
		<security-role-mapping>
			<role-name>org.imixs.ACCESSLEVEL.EDITORACCESS</role-name>
			<group-name>IMIXS-WORKFLOW-Editor</group-name>
		</security-role-mapping>
		<security-role-mapping>
			<role-name>org.imixs.ACCESSLEVEL.MANAGERACCESS</role-name>
			<group-name>IMIXS-WORKFLOW-Manager</group-name>
			<principal-name>IMIXS-WORKFLOW-Service</principal-name>
		</security-role-mapping>	
		<realm>imixsrealm</realm>
	</glassfish-application>


### security-role-ref 
To map these roles in a web application directly without the deployment descriptors above use  the security-role-ref in the web.xml and ejb-jar.xml as defined in the JEE specification.

	  ...  
	   <security-role-ref>
	    <role-name>author</role-name>
	    <role-link>org.imixs.ACCESSLEVEL.AUTHORACCESS</role-link>
	  </security-role-ref>
	...

In this example the name in the tag "role-link" must match the name of the imixs security role and the role-name to a group or role in your security context.


The deployment is similar to other application servers. 
  
## How to Define Individual Access Role
In addition to the standard security model of the Imixs-Workflow engine, it is also possible to define application specific roles. These roles can be used in a custom workflow application to restrict the access in a more fine grained way. An application specific role is typical mapped to a workitem by using the [Imixs-BPMN Modeler](../modelling/index.html). You can add such a Role to the corresponding ACL configuration in the model. The Imixs-Workflow engine will map the application specific role automatically into a workitem.

<strong>Note:</strong> Users must have at least the general AccessRole 
org.imixs.ACCESSLEVEL.READACCESS to access a workitem. Also if you define application specific roles. Otherwise a uses is not allowed to access the workitems with an application specific role restriction in the WorkfloManager.
 
The following example shows how to add an additional application specific role named "PROJECTMANAGER" to a workflow application.
To use such an application specific role, the role definition need to be added into the EJB deployment descriptor (ejb-jar.xml).
The following example illustrates how to define an application specific role in the ejb-jar.xml deployment descriptor:

	  <enterprise-beans>
	 .....
	 <session>
		<ejb-name>DocumentService</ejb-name>
		<env-entry>
			<description>Additional Access Rolls</description>
			<env-entry-name>ACCESS_ROLES</env-entry-name>
			<env-entry-type>java.lang.String</env-entry-type>
			<env-entry-value>com.prosamed.ISESS.ARTIKELMODIFIER</env-entry-value>
		</env-entry>
		<security-role-ref>
			<role-name>PROJECTMANAGER</role-name>
			<role-link>PROJECTMANAGER</role-link>
		</security-role-ref>
	 </session>
	 ...
	 .....
	  <assembly-descriptor>
			<security-role>
				<role-name>PROJECTMANAGER</role-name>
			</security-role>
	  </assembly-descriptor>


The env-entry defines the new role to be used by the access control of the Imixs-Workflow engine. The security-role-ref adds the role to the EJB. It is necessary to specify the role-name and also the role-link for each custom role. 

## How to Inject Custom User Groups
Imixs-Workflow supports also an mechanism to inject additional user groups into the security layer based on CDI events. See the [DocumentService](../engine/documentservice.html) for 
details.
