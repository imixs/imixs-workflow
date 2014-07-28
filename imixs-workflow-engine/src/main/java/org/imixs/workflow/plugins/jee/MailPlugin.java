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

package org.imixs.workflow.plugins.jee;

import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
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

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This plugin supports a Mail interface to send mail like defined in the model
 * using the Mail tab in an activity Entity. This plugin uses the JEE Mail
 * Interface Currentyl no HTML Mail is supported
 * 
 * @author Ralph Soika
 * 
 */
public class MailPlugin extends AbstractPlugin {

	// Mail objects
	Session mailSession;
	MimeMessage mailMessage = null;
	Multipart mimeMultipart = null;
	boolean isHTMLMail = false;

	static final String CONTENTTYPE_TEXT_PLAIN = "text/plain";
	static final String CONTENTTYPE_TEXT_HTML = "text/html";
	String charSet = "ISO-8859-1";
	boolean noMailSessionBound = false;

	@Resource(name = "IMIXS_MAIL_SESSION")
	private String sMailSession = "org.imixs.workflow.mail";

	private static Logger logger = Logger.getLogger(MailPlugin.class.getName());

	/**
	
	 */
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);
	}

	/**
	 * The run method first verifies if a message should be send. If not the
	 * method terminates immediately. Otherwise the method constructs a
	 * mailMessage object.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public int run(ItemCollection documentContext,
			ItemCollection documentActivity) throws PluginException {

		String sFrom;
		String sReplyTo = "";
		InternetAddress[] recipientsTo, recipientsCC;

		try {

			// check if mail is active?
			if ("1".equals(documentActivity
					.getItemValueString("keyMailInactive")))
				return Plugin.PLUGIN_OK;

			// check if recipients are defined
			String snamMailReceifer = documentActivity
					.getItemValueString("namMailReceiver");
			logger.finest("[MailPlugin] namMailReceiver=" + snamMailReceifer);

			String skeyMailReceiverFields = documentActivity
					.getItemValueString("keyMailReceiverFields");
			logger.finest("[MailPlugin] keyMailReceiverFields="
					+ skeyMailReceiverFields);

			// check if recipients are defined....
			if (((snamMailReceifer != null) && (!"".equals(documentActivity
					.getItemValueString("namMailReceiver"))))
					|| ((skeyMailReceiverFields != null) && (!""
							.equals(documentActivity
									.getItemValueString("keyMailReceiverFields"))))) {

				sFrom = getUserName();
				logger.finest("[MailPlugin] userName (from) = " + sFrom);

				// check if sender is defined....
				if ((sFrom == null) || ("".equals(sFrom)))
					return Plugin.PLUGIN_OK;

				// first initialize mail message object
				initMailMessage();

				// check for ReplyTo...
				if ("1".equals(documentActivity
						.getItemValueString("keyMailReplyToCurrentUser")))
					sReplyTo = getUserName();
				else
					sReplyTo = documentActivity
							.getItemValueString("namMailReplyToUser");

				logger.finest("[MailPlugin] ReplyTo=" + sReplyTo);

				// sender = CurrentUser?
				mailMessage.setFrom(getInternetAddress(sFrom));

				// replay to?
				if ((sReplyTo != null) && (!"".equals(sReplyTo))) {
					InternetAddress[] resplysAdrs = new InternetAddress[1];
					resplysAdrs[0] = getInternetAddress(sReplyTo);
					mailMessage.setReplyTo(resplysAdrs);
				}

				// build Recipient from Vector namMailReceiver
				List vectorRecipients = documentActivity
						.getItemValue("namMailReceiver");
				if (vectorRecipients == null)
					vectorRecipients = new Vector();

				// read keyMailReceiverFields (mulit value)
				// here are the field names defined
				mergeMappedFieldValues(documentContext, vectorRecipients,
						documentActivity.getItemValue("keyMailReceiverFields"));

				// cancel send mail if no receipiens defined
				if (vectorRecipients.size() == 0) {
					mailMessage = null;
					return Plugin.PLUGIN_OK;
				}

				/*
				 * In the following code the vector with email addresses will be
				 * transformed into a InternetAddress array. Therefore the
				 * helper getInternetAddress() method will be called which can
				 * be over written by a subclass. The Method call
				 * getInternetAddressArray ensures additional that no 'null'
				 * values will be stored into the array as this would throw a
				 * exception into the setReceipients call of the mailMessage
				 * object
				 */

				// set TO Recipient
				recipientsTo = getInternetAddressArray(vectorRecipients);
				mailMessage.setRecipients(Message.RecipientType.TO,
						recipientsTo);

				// build Recipient Vector from namMailReceiver
				vectorRecipients = documentActivity
						.getItemValue("namMailReceiverCC");
				if (vectorRecipients == null)
					vectorRecipients = new Vector();

				// now read keyMailReceiverFieldsCC (multi value)
				mergeMappedFieldValues(documentContext, vectorRecipients,
						documentActivity
								.getItemValue("keyMailReceiverFieldsCC"));

				// set CC Recipients
				// build array...
				recipientsCC = getInternetAddressArray(vectorRecipients);
				mailMessage.setRecipients(Message.RecipientType.CC,
						recipientsCC);

				// set Subject with the defined text CharSet
				mailMessage.setSubject(
						replaceDynamicValues(documentActivity
								.getItemValueString("txtMailSubject"),
								documentContext), this.getCharSet());

				// build mail body...
				String aBodyText = documentActivity
						.getItemValueString("rtfMailBody");
				isHTMLMail = false;
				if (aBodyText != null) {
					// set mailbody
					MimeBodyPart messagePart = new MimeBodyPart();
					aBodyText = replaceDynamicValues(aBodyText, documentContext);
					// test if content ist html mail...
					String sTestHTML = aBodyText.trim().toLowerCase();
					if (sTestHTML.startsWith("<!doctype")
							|| sTestHTML.startsWith("<html")
							|| sTestHTML.startsWith("<?xml")) {
						isHTMLMail = true;
					} else {
						isHTMLMail = false;
					}
					logger.fine("[MailPlugin] creating mail body type: '"
							+ getContentType() + "'");
					messagePart.setContent(aBodyText, getContentType());

					// append message part
					mimeMultipart.addBodyPart(messagePart);
					// mimeMulitPart object can be extended from subclases
				}

				// write debug Log
				if ((recipientsTo.length > 0)
						&& ctx.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE) {
					logger.info("[MailPlugin] Creating new PlainText mail....");
					logger.info("[MailPlugin] From: " + sFrom);
					logger.info("[MailPlugin] To (" + recipientsTo.length
							+ " Receipients):");
					if (recipientsTo.length > 0) {
						for (int j = 0; j < recipientsTo.length; j++)
							logger.info("[MailPlugin]     "
									+ recipientsTo[j].getAddress());
					} else
						logger.info("[MailPlugin] no receipients defined");

					if (recipientsCC.length > 0) {
						logger.info("[MailPlugin] CopyTo ("
								+ recipientsCC.length + " Receipients):");
						for (int j = 0; j < recipientsCC.length; j++)
							logger.info("[MailPlugin]     "
									+ recipientsCC[j].getAddress());
					} else
						logger.info("[MailPlugin] no CC defined");
				}

			} else {
				if (ctx.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE)
					logger.info("[MailPlugin] No Receipients defined for this Activity...");

			}

		} catch (Exception e) {
			logger.warning("[MailPlugin] run - Warning:" + e.toString());
			e.printStackTrace();
			return Plugin.PLUGIN_WARNING;
		}

		return Plugin.PLUGIN_OK;
	}

	/**
	 * Send the mail if the object 'mailMessage' is not null.
	 * 
	 * The method lookups the mail session from the session context.
	 */
	public void close(int status) throws PluginException {
		if (status == Plugin.PLUGIN_OK && mailSession != null
				&& mailMessage != null) {
			// Send the message
			try {
				if (ctx.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE)
					logger.info("[MailPlugin] SendMessage now...");

				// if send message fails (e.g. for policy reasons) the process
				// will
				// continue. only a exception is thrown

				// Transport.send(mailMessage);

				// A simple transport.send command did not work if mail host
				// needs
				// a authentification. Therefor we use a manual smtp connection

				Transport trans = mailSession.getTransport("smtp");
				trans.connect(mailSession.getProperty("mail.smtp.user"),
						mailSession.getProperty("mail.smtp.password"));

				if (this.isHTMLMail()) {
					mailMessage.setContent(mimeMultipart, getContentType());
				} else {
					mailMessage.setContent(mimeMultipart, getContentType());
				}

				mailMessage.saveChanges();
				trans.sendMessage(mailMessage, mailMessage.getAllRecipients());
				trans.close();

			} catch (Exception esend) {
				logger.warning("[MailPlugin] close - Warning:"
						+ esend.toString());
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
		logger.finest("[MailPlugin] initializeMailMessage...");

		// fetch mail session object....
		initMailSession();

		if (mailSession != null) {
			// log mail Properties ....
			if (logger.isLoggable(Level.FINE)) {
				Properties props = mailSession.getProperties();
				Enumeration<Object> enumer = props.keys();
				while (enumer.hasMoreElements()) {
					String aKey = enumer.nextElement().toString();
					logger.fine("[MailPlugin]  ProperyName= " + aKey);
					Object value = props.getProperty(aKey);
					if (value == null)
						logger.fine("[MailPlugin]  PropertyValue=null");
					else
						logger.fine("[MailPlugin]  PropertyValue= "
								+ props.getProperty(aKey).toString());
				}
			}
			mailMessage = new MimeMessage(mailSession);
			mailMessage.setSentDate(new Date());
			mailMessage.setFrom();
			mimeMultipart = new MimeMultipart();
		}
	}

	/**
	 * this helper method creates an internet address from a string if the
	 * string has illegal characters like whitespace the string will be
	 * surrounded with "". If you subclass this MailPlugin Class you can
	 * overwrite this method to return a different mail-address name or lookup a
	 * mail attribute in a directory like a ldap directory.
	 * 
	 * @param aAddr
	 * @return
	 * @throws AddressException
	 */
	public InternetAddress getInternetAddress(String aAddr)
			throws AddressException {

		InternetAddress inetAddr = null;

		if (aAddr == null)
			return null;

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
	 * this method transforms a vector of emails into a InternetAddress Array.
	 * Null values will be removed from list
	 * 
	 * @param aList
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private InternetAddress[] getInternetAddressArray(List aList) {
		// set TO Recipient
		// store valid addresses into atemp vector to avoid null values
		InternetAddress inetAddr = null;
		if (aList == null)
			return null;

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
		try {
			ic = new InitialContext();

			String snName = "";
			snName = "java:comp/env/mail/" + sMailSession;
			mailSession = (Session) ic.lookup(snName);
			logger.finest("[MailPlugin] MailSession '" + sMailSession
					+ "' found");

		} catch (NamingException e) {
			logger.warning("[MailPlugin] Unable to send mails! Verify server resources -> mail session.");
			logger.warning("[MailPlugin] Mail Session for jndi name:'mail/"
					+ sMailSession + "' missing.");
			logger.warning("[MailPlugin] ErrorMessage: " + e.getMessage());
			noMailSessionBound = true;
		}

		// if property service is defined we can lookup the default charset
		if (this.propertyService != null) {
			// // test for mail.htmlCharSet
			// String sTestCharSet = (String)
			// propertyService.getProperties().get(
			// "mail.htmlCharSet");
			// if (sTestCharSet != null && !sTestCharSet.isEmpty())
			// setHtmlCharSet(sTestCharSet);
			//
			// // test for mail.textCharSet
			// sTestCharSet = (String) propertyService.getProperties().get(
			// "mail.textCharSet");
			// if (sTestCharSet != null && !sTestCharSet.isEmpty())
			// setTextCharSet(sTestCharSet);

			// test for mail.charSet
			String sTestCharSet = (String) propertyService.getProperties().get(
					"mail.charSet");
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

	public boolean isHTMLMail() {
		return isHTMLMail;
	}

	public String getCharSet() {
		return charSet;
	}

	public void setCharSet(String charSet) {
		this.charSet = charSet;
	}

	/**
	 * This method returns a string representing the general mail content type.
	 * The content type depends on the content of the mail body (html or
	 * plaintext) and the character set.
	 * 
	 * @return
	 */
	public String getContentType() {
		String sContentType = "";
		if (isHTMLMail) {
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
