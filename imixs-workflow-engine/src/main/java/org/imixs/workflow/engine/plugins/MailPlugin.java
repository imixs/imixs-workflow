/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.workflow.engine.plugins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;
import org.imixs.workflow.xml.XSLHandler;

/**
 * This plug-in supports a Mail interface to send a email to a list of
 * recipients. The mail content can be defined by the corresponding BPMN event.
 * 
 * The content of the email can either be plain text or HTML.
 * 
 * The plug-in uses a JNID messaging session named 'org.imixs.workflow.mail'.
 * The JNDI resource can be customized by the deployment constructor.
 * 
 * The e-mail message can be canceled by the application or another plug-in by
 * setting the attribute keyMailInactive=true
 * 
 * @author Ralph Soika
 * 
 */
public class MailPlugin extends AbstractPlugin {

	public static final String INVALID_XSL_FORMAT = "INVALID_XSL_FORMAT";

	// Mail objects
	Session mailSession;
	MimeMessage mailMessage = null;
	Multipart mimeMultipart = null;
	static final String CONTENTTYPE_TEXT_PLAIN = "text/plain";
	static final String CONTENTTYPE_TEXT_HTML = "text/html";
	public static final String INVALID_ADDRESS = "INVALID_ADDRESS";
	String charSet = "ISO-8859-1";

	@Resource(name = "IMIXS_MAIL_SESSION")
	private String sMailSession = "org.imixs.workflow.mail";
	private boolean bHTMLMail = false;
	private boolean noMailSessionBound = false;
	private static Logger logger = Logger.getLogger(MailPlugin.class.getName());

	/**
	 * The run method creates a mailMessage object if recipients are defined by
	 * the corresponding BPMN event. The mail message will finally be send in
	 * the close method. This mechanism avoids that a mail is send before all
	 * plug-ins were processed correctly.
	 * 
	 */
	@SuppressWarnings({ "rawtypes" })
	public ItemCollection run(ItemCollection documentContext, ItemCollection documentActivity) throws PluginException {

		mailMessage = null;

		// check if mail is active? This flag can be set by another plug-in
		if (documentActivity.getItemValueBoolean("keyMailInactive")
				|| "1".equals(documentActivity.getItemValueString("keyMailInactive"))) {
			logger.finest("......keyMailInactive = true - cancel mail message.");
			return documentContext;
		}

		List vectorRecipients = getRecipients(documentContext, documentActivity);
		if (vectorRecipients.isEmpty()) {
			logger.finest("......No Receipients defined for this Activity - cancel mail message.");
			return documentContext;
		}

		try {

			// first initialize mail message object
			initMailMessage();

			if (mailMessage == null) {
				logger.warning(" mailMessage = null");
				return documentContext;
			}

			// set FROM
			mailMessage.setFrom(getInternetAddress(getFrom(documentContext, documentActivity)));

			// set Recipient
			mailMessage.setRecipients(Message.RecipientType.TO, getInternetAddressArray(vectorRecipients));

			// build CC
			mailMessage.setRecipients(Message.RecipientType.CC,
					getInternetAddressArray(getRecipientsCC(documentContext, documentActivity)));

			// build BCC
			mailMessage.setRecipients(Message.RecipientType.BCC,
					getInternetAddressArray(getRecipientsBCC(documentContext, documentActivity)));

			// replay to?
			String sReplyTo = getReplyTo(documentContext, documentActivity);
			if ((sReplyTo != null) && (!sReplyTo.isEmpty())) {
				InternetAddress[] resplysAdrs = new InternetAddress[1];
				resplysAdrs[0] = getInternetAddress(sReplyTo);
				mailMessage.setReplyTo(resplysAdrs);
			}

			// set Subject
			mailMessage.setSubject(getSubject(documentContext, documentActivity), this.getCharSet());

			// set Body
			String aBodyText = getBody(documentContext, documentActivity);

			// set mailbody
			MimeBodyPart messagePart = new MimeBodyPart();
			logger.finest("......ContentType: '" + getContentType() + "'");
			messagePart.setContent(aBodyText, getContentType());
			// append message part
			mimeMultipart.addBodyPart(messagePart);
			// mimeMulitPart object can be extended from subclases

		} catch (Exception e) {
			logger.warning(" run - Warning:" + e.toString());
			e.printStackTrace();
			return documentContext;
		}

		return documentContext;
	}

	/**
	 * Send the mail if the object 'mailMessage' is not null.
	 * 
	 * The method lookups the mail session from the session context.
	 */
	@Override
	public void close(boolean rollbackTransaction) throws PluginException {
		if (!rollbackTransaction && mailSession != null && mailMessage != null) {
			// Send the message
			try {

				// Check if we are running in a Test MODE
				if (getWorkflowService().getPropertyService() != null) {
					String sTestRecipients = (String) getWorkflowService().getPropertyService().getProperties()
							.get("mail.testRecipients");
					// test if TestReceipiens are defined...
					if (sTestRecipients != null && !"".equals(sTestRecipients)) {
						List<String> vRecipients = new Vector<String>();
						// split multivalues
						StringTokenizer st = new StringTokenizer(sTestRecipients, ",", false);
						while (st.hasMoreElements()) {
							vRecipients.add(st.nextToken().trim());
						}

						logger.info("Running in TestMode, forwarding mails to:");
						for (String adr : vRecipients) {
							logger.info("     " + adr);
						}
						try {
							getMailMessage().setRecipients(Message.RecipientType.CC, null);
							getMailMessage().setRecipients(Message.RecipientType.BCC, null);
							getMailMessage().setRecipients(Message.RecipientType.TO,
									getInternetAddressArray(vRecipients));
							// change subject
							String sSubject = getMailMessage().getSubject();
							getMailMessage().setSubject("[TESTMODE] : " + sSubject);

						} catch (MessagingException e) {
							throw new PluginException(MailPlugin.class.getSimpleName(), INVALID_ADDRESS,
									" unable to set mail recipients: ", e);
						}
					}

				}

				logger.finest("......sending message...");

				// if send message fails (e.g. for policy reasons) the process
				// will
				// continue. only a exception is thrown

				// Transport.send(mailMessage);

				// A simple transport.send command did not work if mail host
				// needs
				// a authentification. Therefor we use a manual smtp connection

				Transport trans = mailSession.getTransport("smtp");
				trans.connect(mailSession.getProperty("mail.smtp.user"), mailSession.getProperty("mail.smtp.password"));

				mailMessage.setContent(mimeMultipart, getContentType());

				mailMessage.saveChanges();
				trans.sendMessage(mailMessage, mailMessage.getAllRecipients());
				trans.close();

			} catch (Exception esend) {
				logger.warning("close failed with exception: " + esend.toString());
			}
		}
	}

	/**
	 * Computes the sender name. This method can be overwritten by subclasses.
	 * 
	 * @param documentContext
	 * @param documentActivity
	 * @return String - mail subject
	 */
	public String getFrom(ItemCollection documentContext, ItemCollection documentActivity) {

		String sFrom = null;
		// test if default sender is defined
		if (getWorkflowService().getPropertyService() != null) {
			sFrom = (String) getWorkflowService().getPropertyService().getProperties().get("mail.defaultSender");
		}
		// if no default sender take the current username
		if (sFrom == null || sFrom.isEmpty())
			sFrom = this.getWorkflowService().getUserName();

		logger.finest("......From: " + sFrom);

		return sFrom;
	}

	/**
	 * Computes the replyTo address from current workflow activity. This method
	 * can be overwritten by subclasses.
	 * 
	 * @param documentContext
	 * @param documentActivity
	 * @return String - replyTo address
	 */
	public String getReplyTo(ItemCollection documentContext, ItemCollection documentActivity) {
		String sReplyTo = null;

		// check for ReplyTo...
		if ("1".equals(documentActivity.getItemValueString("keyMailReplyToCurrentUser")))
			sReplyTo = this.getWorkflowService().getUserName();
		else
			sReplyTo = documentActivity.getItemValueString("namMailReplyToUser");

		logger.finest("......ReplyTo=" + sReplyTo);
		return sReplyTo;
	}

	/**
	 * Computes the mail subject from the current workflow activity. This method
	 * can be overwritten by subclasses.
	 * 
	 * @param documentContext
	 * @param documentActivity
	 * @return String - mail subject
	 * @throws PluginException
	 */
	public String getSubject(ItemCollection documentContext, ItemCollection documentActivity) throws PluginException {
		String subject = getWorkflowService().adaptText(documentActivity.getItemValueString("txtMailSubject"), documentContext);
		logger.finest("......Subject: " + subject);

		return subject;
	}

	/**
	 * Computes the mail Recipients from the current workflow activity. This
	 * method can be overwritten by subclasses.
	 * 
	 * @param documentContext
	 * @param documentActivity
	 * @return String list of Recipients
	 */
	@SuppressWarnings("unchecked")
	public List<String> getRecipients(ItemCollection documentContext, ItemCollection documentActivity) {

		// build Recipient from Activity ...
		List<String> vectorRecipients = (List<String>) documentActivity.getItemValue("namMailReceiver");
		if (vectorRecipients == null)
			vectorRecipients = new Vector<String>();

		// read keyMailReceiverFields (multi value)
		// here are the field names defined
		mergeFieldList(documentContext, vectorRecipients, documentActivity.getItemValue("keyMailReceiverFields"));

		// write debug Log
		if (logger.isLoggable(Level.FINE)) {
			logger.finest("......"+vectorRecipients.size() + " Receipients: ");
			for (String rez : vectorRecipients)
				logger.finest("     " + rez);
		}

		return vectorRecipients;
	}

	/**
	 * Computes the mail RecipientsCC from the current workflow activity. This
	 * method can be overwritten by subclasses.
	 * 
	 * @param documentContext
	 * @param documentActivity
	 * @return String list of Recipients
	 */
	@SuppressWarnings("unchecked")
	public List<String> getRecipientsCC(ItemCollection documentContext, ItemCollection documentActivity) {

		// build Recipient Vector from namMailReceiver
		List<String> vectorRecipients = (List<String>) documentActivity.getItemValue("namMailReceiverCC");
		if (vectorRecipients == null)
			vectorRecipients = new Vector<String>();

		// now read keyMailReceiverFieldsCC (multiValue)
		mergeFieldList(documentContext, vectorRecipients, documentActivity.getItemValue("keyMailReceiverFieldsCC"));

		// write debug Log
		if (logger.isLoggable(Level.FINE)) {
			logger.finest("......"+vectorRecipients.size() + " ReceipientsCC: ");
			for (String rez : vectorRecipients)
				logger.finest("     " + rez);
		}
		return vectorRecipients;
	}

	/**
	 * Computes the mail RecipientsBCC from the current workflow activity. This
	 * method can be overwritten by subclasses.
	 * 
	 * @param documentContext
	 * @param documentActivity
	 * @return String list of Recipients
	 */
	@SuppressWarnings("unchecked")
	public List<String> getRecipientsBCC(ItemCollection documentContext, ItemCollection documentActivity) {

		// build Recipient Vector from namMailReceiver
		List<String> vectorRecipients = (List<String>) documentActivity.getItemValue("namMailReceiverBCC");
		if (vectorRecipients == null)
			vectorRecipients = new Vector<String>();

		// now read keyMailReceiverFieldsCC (multiValue)
		mergeFieldList(documentContext, vectorRecipients, documentActivity.getItemValue("keyMailReceiverFieldsBCC"));

		// write debug Log
		if (logger.isLoggable(Level.FINE)) {
			logger.finest("......"+vectorRecipients.size() + " ReceipientsBCC: ");
			for (String rez : vectorRecipients)
				logger.finest("     " + rez);
		}
		return vectorRecipients;
	}

	/**
	 * Computes the mail body from the current workflow event. The method also
	 * updates the internal flag HTMLMail to indicate if the mail is send as
	 * HTML mail.
	 * 
	 * In case the content contains a XSL Template, the template will be
	 * processed with the current document structure.
	 * 
	 * The method can be overwritten by subclasses.
	 * 
	 * @param documentContext
	 * @param documentActivity
	 * @return String - mail subject
	 * @throws PluginException
	 */
	public String getBody(ItemCollection documentContext, ItemCollection documentActivity) throws PluginException {
		// build mail body and replace dynamic values...
		String aBodyText = getWorkflowService().adaptText(documentActivity.getItemValueString("rtfMailBody"), documentContext);

		// Test if mail body contains HTML content and updates the flag
		// 'isHTMLMail'.
		String sTestHTML = aBodyText.trim().toLowerCase();
		if (sTestHTML.startsWith("<!doctype") || sTestHTML.startsWith("<html") || sTestHTML.startsWith("<?xml")) {
			bHTMLMail = true;
		} else
			bHTMLMail = false;

		// xsl transformation...?
		if (sTestHTML.contains("<xsl:stylesheet")) {
			aBodyText = transformXSLBody(documentContext, aBodyText);
		}

		return aBodyText;

	}

	/**
	 * This method performs a XSL transformation based on the current Mail Body
	 * text. The xml source is generated form the current document context.
	 * 
	 * encoding is set to UTF-8
	 * 
	 * @return translated email body
	 * @throws PluginException
	 * 
	 */
	public String transformXSLBody(ItemCollection documentContext, String xslTemplate) throws PluginException {
		String encoding;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		encoding = "UTF-8";

		logger.finest("......transfor mail body based on XSL template....");
		// Transform XML per XSL and generate output
		XMLDocument xml;
		try {
			xml = XMLDocumentAdapter.getDocument(documentContext);
			StringWriter writer = new StringWriter();

			JAXBContext context = JAXBContext.newInstance(XMLDocument.class);
			Marshaller m = context.createMarshaller();
			m.setProperty("jaxb.encoding", encoding);
			m.marshal(xml, writer);

			// create a ByteArray Output Stream
			XSLHandler.transform(writer.toString(), xslTemplate, encoding, outputStream);
			return outputStream.toString(encoding);

		} catch (Exception e) {
			logger.warning("Error processing XSL template!");
			throw new PluginException(MailPlugin.class.getSimpleName(), INVALID_XSL_FORMAT, e.getMessage(), e);
		} finally {
			try {
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * initializes a new mail Message object
	 * 
	 * @throws AddressException
	 * @throws MessagingException
	 */
	public void initMailMessage() throws AddressException, MessagingException {
		logger.finest("......initializeMailMessage...");

		// fetch mail session object....
		initMailSession();

		if (mailSession != null) {
			// log mail Properties ....
			if (logger.isLoggable(Level.FINE)) {
				Properties props = mailSession.getProperties();
				Enumeration<Object> enumer = props.keys();
				while (enumer.hasMoreElements()) {
					String aKey = enumer.nextElement().toString();
					logger.finest("...... ProperyName= " + aKey);
					Object value = props.getProperty(aKey);
					if (value == null)
						logger.finest("...... PropertyValue=null");
					else
						logger.finest("...... PropertyValue= " + props.getProperty(aKey).toString());
				}
			}
			mailMessage = new MimeMessage(mailSession);
			mailMessage.setSentDate(new Date());
			mailMessage.setFrom();
			mimeMultipart = new MimeMultipart();
		}
	}

	/**
	 * This method creates an InternetAddress from a string. If the string has
	 * illegal characters like whitespace the string will be surrounded with "".
	 * 
	 * The method can be overwritten by subclasses to return a different
	 * mail-address name or lookup a mail attribute in a directory.
	 * 
	 * @param aAddr
	 *            string
	 * @return InternetAddress
	 * @throws AddressException
	 */
	public InternetAddress getInternetAddress(String aAddr) throws AddressException {
		InternetAddress inetAddr = null;
		if (aAddr == null) {
			return null;
		}

		try {
			// surround with "" if space
			if (aAddr.indexOf(" ") > -1)
				inetAddr = new InternetAddress("\"" + aAddr + "\"");
			else
				inetAddr = new InternetAddress(aAddr);

		} catch (AddressException ae) {
			// return empty address part
			ae.printStackTrace();
			return null;
		}
		return inetAddr;
	}

	/**
	 * This method transforms a vector of E-Mail addresses into an
	 * InternetAddress Array. Null values will be removed from list
	 * 
	 * @param String
	 *            List of adresses
	 * @return array of InternetAddresses
	 */
	@SuppressWarnings("rawtypes")
	private InternetAddress[] getInternetAddressArray(List aList) {
		// set TO Recipient
		// store valid addresses into atemp vector to avoid null values
		InternetAddress inetAddr = null;
		if (aList == null) {
			return null;
		}

		Vector<InternetAddress> vReceipsTemp = new Vector<InternetAddress>();
		for (int i = 0; i < aList.size(); i++) {
			try {
				inetAddr = getInternetAddress(aList.get(i).toString());
				if (inetAddr != null && !"".equals(inetAddr.getAddress()))
					vReceipsTemp.add(inetAddr);
			} catch (AddressException e) {
				// no todo
			}

		}

		// rebuild new InternetAddress array from TempVector...
		InternetAddress[] receipsAdrs = new InternetAddress[vReceipsTemp.size()];
		for (int i = 0; i < vReceipsTemp.size(); i++) {
			receipsAdrs[i] = (InternetAddress) vReceipsTemp.elementAt(i);
		}
		return receipsAdrs;
	}

	/**
	 * This method initializes a new instance of a MailSession.
	 * 
	 * The MailSession is received from the current jndi context. The Default
	 * name of the Mail Session is 'org.imixs.workflow.jee.mailsession' The name
	 * can be overwritten by the EJB Properties of the workflowmanager
	 * 
	 * @return MailSession object or null if no MailSession is bound to the
	 *         WorkflowContext.
	 */
	public void initMailSession() {

		// check if yet initialized....
		if (mailSession != null || noMailSessionBound) {
			return;
		}

		// Initialize mail session
		InitialContext ic;
		String sJNDINName = "";
		try {
			ic = new InitialContext();
			// add java: prafix if not defined in jndi ref
			if (!sMailSession.startsWith("java:")) {
				sJNDINName = "java:comp/env/" + sMailSession;
			} else {
				sJNDINName = sMailSession;
			}
			logger.finest("...... Lookup MailSession '" + sJNDINName + "' ...");
			mailSession = (Session) ic.lookup(sJNDINName);
			logger.finest("...... Lookup MailSession '" + sJNDINName + "' successful");

		} catch (NamingException e) {
			logger.warning(" Lookup MailSession '" + sJNDINName + "' failed: "+e.getMessage());
			logger.warning(" Unable to send mails! Verify server resources -> mail session.");
			noMailSessionBound = true;
		}

		// if property service is defined we can lookup the default charset
		if (getWorkflowService().getPropertyService() != null) {
			// test for mail.charSet
			String sTestCharSet = (String) getWorkflowService().getPropertyService().getProperties()
					.get("mail.charSet");
			if (sTestCharSet != null && !sTestCharSet.isEmpty())
				setCharSet(sTestCharSet);

		}
	}

	/**
	 * This method returns the mail session object. If no mailSession exists the
	 * method initializes a new instance of a MailSession.
	 * 
	 */
	public Session getMailSession() {
		if (mailSession == null && noMailSessionBound == false) {
			initMailSession();
		}
		return mailSession;
	}

	public Message getMailMessage() {
		return mailMessage;
	}

	public Multipart getMultipart() {
		return mimeMultipart;
	}

	/**
	 * Return true if the mail body contains HTML content.
	 * 
	 * @return
	 */
	public boolean isHTMLMail() {
		return bHTMLMail;
	}

	public String getCharSet() {
		return charSet;
	}

	public void setCharSet(String charSet) {
		this.charSet = charSet;
	}

	/**
	 * This method returns a string representing the mail content type. The
	 * content type depends on the content of the mail body (html or plaintext)
	 * and contains optional the character set.
	 * 
	 * If the mail is a HTML mail then the returned string contains 'text/html'
	 * otherwise it will contain 'text/plain'.
	 * 
	 * The content
	 * 
	 * @return
	 */
	public String getContentType() {
		String sContentType = "";
		if (bHTMLMail) {
			sContentType = CONTENTTYPE_TEXT_HTML;
		} else {
			sContentType = CONTENTTYPE_TEXT_PLAIN;
		}
		if (this.getCharSet() != null && !this.getCharSet().isEmpty()) {
			sContentType = sContentType + "; charset=" + this.getCharSet();
		}
		return sContentType;
	}

}
