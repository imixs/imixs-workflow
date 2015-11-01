#Mail Plugin 
The MailPlugin provides an implementation for sending mails via the Java mail api.  To use this plugin in a Workflow application you need to add the following PluginClass to the Model Configuration:

    org.imixs.workflow.jee.plugins.MailPlugin

See the [Imixs-BPMN Modeller](../../modelling/index.html) for details about modeling a mail activity. Running the Imixs MailPlugin in a JEE container requires a valid JNDI mail resource. A mail resource can be configured from the application server environment. 
The expected JNDI resource name to lookup the mail resource by the Imixs MailPlugin is

    org.imixs.workflow.mail

The mail resource object is used to send outgoing mails to mail server. See Java EE spec for details about Java managing mail sessions.

## Deployment Descriptors
As the MailPlugin needs to lookup the Java mail resource using a JNDI Lookup you need to provide a  valid resource reference to the WorkflowService. Therefore you need to add the mail resource into the ejb-jar.xml to provide the WorkflowService EJB with a valid JNDI resource. See the following ejb-jar.xml example for GlassFish V3
   
	......   
		<session>
			<ejb-name>WorkflowService</ejb-name>
			<ejb-class>org.imixs.workflow.jee.ejb.WorkflowService</ejb-class>
			<session-type>Stateless</session-type>
			
			<!-- Mail Configuration -->
			<env-entry>
				<description>Mail Plugin Session name</description>
				<env-entry-name>IMIXS_MAIL_SESSION</env-entry-name>
				<env-entry-type>java.lang.String</env-entry-type>
				<env-entry-value>mail/org.imixs.workflow.mail</env-entry-value>
			</env-entry>
		   ....
			<!-- Mail resource -->
			<resource-ref>
				<res-ref-name>mail/org.imixs.workflow.mail</res-ref-name>
				<res-type>javax.mail.Session</res-type>
				<res-auth>Container</res-auth>
				<res-sharing-scope>Shareable</res-sharing-scope>
			</resource-ref>
	 </session>

 
 
<strong>Note:</strong> In other application servers the resource-ref can have a different jndi name.  E.g. for JBoss/WildFly need to be full specified: "java:/mail/org.imixs.workflow.mail"

##Default Sender Address
The default sender address will be set with the current user name.  To manipulate the sender address the Plugin checks if the imixs property 'mail.defaultSender'. If the property is defined, the plugin overwrites the 'From' attribute of every mail with the DefaultSender address. The default sender address can be changed in the imixs.properties file. If no value is set the mail will be send from the current users mail address.
 
A subclass of the MailPlugin can change the behavior by overwriting the method 'getFrom()'
 
Example (imixs.properties):

	# Marty Mail Plugin
	mail.defaultSender=info@imixs.com


##Test Mode
The Mailplugin can be configured to run in a Test-Mode when the property 'mail.testRecipients' is defined.  The property value can contain one ore many (comma separated) Email addresses. If those email addresses are defined than a email will be send only to this recipients. The Subject will be prefixed with the text 'TEST: '.

	#Testmode
	mail.testRecipients=test@development.com


##CharSet
It is possible to set the character set used for the mail subject and body parts. There for the imixs.property key 'mail.charSet' is used. If this property is not defined the charset defaults to 'ISO-8859-1'!
