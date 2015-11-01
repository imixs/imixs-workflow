# GlassFish
The following section holds deployment strategies for the GlassFish platform.

## Security 
The security roles defined by the Imixs-Workflow Engine need to be mapped in an application to corresponding groups defined by a authentication realm.

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
