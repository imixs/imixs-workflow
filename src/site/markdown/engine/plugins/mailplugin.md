# MailPlugin 
The Imixs-MailPlugin provides a convenient way to send e-mail messages through a corresponding BPMN event.
The Imixs-MailPlugin is based on the Java Mail API and can be activated by adding the plugin class to the corresponding model definition:

    org.imixs.workflow.engine.plugins.MailPlugin

Sending an E-Mail can be configured by the corresponding BPMN event using the [Imixs-BPMN modeling tool](../../modelling/activities.html) in various ways. 

<img src="../../images/modelling/bpmn_screen_23.png"/>  

The subject and the body of the e-mail can contain any information from the corresponding workitem. The content can either be plain text or HTML mail. The recipients of the e-mail can be computed on naming attributes of the workitem or by fixed mail addresses or distribution lists. 


## The Mail Content

The content of the email can either be plain text or HTML. Using the item-Tag a subject or the e-mail body can be combined with any information from the current workitem as also with properties from the imixs.properties file. 

See the following plain text example:


    Dear <itemValue>firstName</itemValue> <itemValue>lastName</itemValue>,
    
    A new task needs your attention. Please click on the following link to see further information: 
	<propertyvalue>application.url</propertyvalue>index.jsf?workitem=<itemvalue>$uniqueid</itemvalue>
    
	In case of questions or problems, please contact the Workflow team.    


<br />

### HTML E-Mail

The Mail body can contain plain text or HTML. 
A HTML Mail must start with the tags <!doctype...>" or <html...>. Also a html mail can be combined with values from the current workitem. 

See the following HTML example:

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
				<h1>Dear <itemValue>firstName</itemValue> <itemValue>lastName</itemValue>, </h1>
				<p>A new task needs your attention.</p>
          ....
    </body>
    </html>

<br />

### XSL Templates

For a more complex e-mail message, the Imixs-MailPlugin supports also e-mail Templates. With this feature the e-mail output is based on a XSL Template. This opens up a powerful way to configure the mail content.

To activate the template mode, a valid XSL document need to put into the mail body definition. The template will be processed automatically with the XML representation of the current workitem.

See the following XSL Template example:

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



<br />

## Configuration

Further configuration is supported by the Imixs-MailPlugin.



### Default Sender Address
The default sender address will be set with the current user name.  The sender address can be changed by the imixs property 'mail.defaultSender'. If the property is defined, the plugin overwrites the 'From' attribute of every mail with the DefaultSender address.  If no value is set the mail will be send from the current users mail address.
 
Example (imixs.properties):

	# Marty Mail Plugin
	mail.defaultSender=info@imixs.com

A subclass of the MailPlugin can change the behavior by overwriting the method 'getFrom()'
 

### Testing Mode
During development you can switch the Imixs-MailPlugin into a Testing-Mode by defining the imixs property: 

    mail.testRecipients
 
The property value can contain one ore many (comma separated) Email addresses. If the property is  defined, than an e-mail message will be send only to those recipients. The Subject will be prefixed with the text 'TEST: '.

Example (imixs.properties):

	#Testmode
	mail.testRecipients=test@development.com




### CharSet
The default character-set used for the mail subject and body parts is set to 'ISO-8859-1'.
It is possible to switch to a specific character set . There for the imixs.property key 'mail.charSet' can be used. 

Example (imixs.properties):

	#Charset
	mail.charSet=UTF-8


### Cancel e-mail

Sending a e-mail message can be canceled by the application or another plugin by setting the attribute 'keyMailInactive' to 'true'. The attribute is part of the corresponding BPMN event. 

    keyMailInactive=true

The attribute can be set by the [Imixs-RulePlugin](./ruleplugin.html).

### Receipients and Event Properties

A E-Mail message consists of a list of recipients. The recipients can be mapped to name-fields defined in the BPMN definition.
 
The following event properties are supported:


|Name                  |Type       | Description                                   |
|----------------------|-----------|-----------------------------------------------| 
| txtMailSubject       | String    | Mail Subject                                  |
| rtfMailBody          | String    | Mail Body (can be plain text or HTML)          |
| namMailReplyToUser   | String    | Reply To address. If not set the reply address is the sender address |
| namMailFrom          | String    | Sender (default is the current user)          |
| namMailReceiver      | String (list)   | Receiver list (TO)                      |
| namMailReceiverCC    | String (list)   | Receiver list (CC)                      |
| namMailReceiverBCC   | String (list)   | Receiver list (BCC)                     |
| keyMailInactive      | Boolean    | If true, the e-mail will be canceled (can be set by other plugins)  |



## Deployment

Running the Imixs MailPlugin in a EJB container requires a valid JNDI mail resource named:

    mail/org.imixs.workflow.mail

The mail resource object is used to send outgoing mails to mail server. See Java EE spec for details about Java managing mail sessions.

### Deployment Descriptors

Depending on the server environment the Mail resource need also be defined in the deployment descriptor. See the following example for JBoss/Wildfly Server: 


	<?xml version="1.0" encoding="UTF-8"?>
	<jboss-web>
		....
		<resource-ref>
			<res-ref-name>mail/org.imixs.workflow.mail</res-ref-name>
			<res-type>javax.mail.Session</res-type>
			<jndi-name>java:/mail/org.imixs.workflow.mail</jndi-name>
		</resource-ref>
		....
	</jboss-web>

