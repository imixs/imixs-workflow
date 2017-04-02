#MailPlugin 
The MailPlugin provides an implementation for sending mails via the Java mail api.  To activate this plug-in add the following PluginClass to the Model Configuration:

    org.imixs.workflow.engine.plugins.MailPlugin


## Mail Content

The mail content can be defined by the corresponding BPMN event using the [Imixs-BPMN modeling tool](../../modelling/activities.html). 
The content of the email can either be plain text or HTML.
The e-mail message can be canceled by the application or another plug-in by setting the attribute 

    keyMailInactive=true

A E-Mail message consists of a list of recipients. The following event properties are supported:


|Name                  |Type       | Description                                   |
|----------------------|-----------|-----------------------------------------------| 
| txtMailSubject       | String    | Mail Subject                                  |
| rtfMailBody          | String    | Mail Body (can be plain text or HTML          |
| namMailReplyToUser   | String    | Reply To address. If not set the reply address is the sender address |
| namMailReceiver      | String (list)   | Receiver list (TO)                      |
| namMailReceiver      | String (list)   | Receiver list (TO)                      |
| namMailReceiverCC    | String (list)   | Receiver list (CC)                      |
| namMailReceiverBCC   | String (list)   | Receiver list (BCC)                     |


### HTML E-Mail

The Mail body can contain plain text or HTML. 
A HTML Mail must start with <!doctype...>" or <html...> 

See the following example

	<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
	<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
	<meta name="viewport" content="width=device-width" />
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<title>Workflow Message</title>
	<style>
	/* optional css definition  */
	</style>
	</head>
	
	<body bgcolor="#f6f6f6">
	
		<!-- body -->
		<div class="container" bgcolor="#FFFFFF">
			<!-- content -->
			<div class="content">
				<h1>Welcome </h1>
          ....
    </body>
    </html>



### XSL Transformation

Optional the Mail body can contain a XSL Template. In this case the current document content will be transformed based on the XSL template.

See the following example:

	<?xml version="1.0" encoding="UTF-8" ?>
	<xsl:stylesheet xmlns="http://www.w3.org/1999/xhtml"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
		<xsl:output method="html" media-type="text/html" indent="no"
			encoding="ISO-8859-1" />
		<xsl:template match="/">
			<html>
				<body>
					<h1>Welcome</h1>
					<h2>
						<xsl:value-of select="document/item[@name='txtname']/value" />
					</h2>
				</body>
			</html>
		</xsl:template>
	</xsl:stylesheet>



##Default Sender Address
The default sender address will be set with the current user name.  The sender address can be changed by the imixs property 'mail.defaultSender'. If the property is defined, the plug-in overwrites the 'From' attribute of every mail with the DefaultSender address.  If no value is set the mail will be send from the current users mail address.
 
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

#Deployment

See the [Imixs-BPMN Modeller](../../modelling/index.html) for details about modeling a mail activity. Running the Imixs MailPlugin in a EJB container requires a valid JNDI mail resource. A mail resource can be configured from the application server environment. 
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