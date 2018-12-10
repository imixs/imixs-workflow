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

package org.imixs.workflow.faces.workitem;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.event.Event;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * The DataController can be used in JSF Applications to manage ItemCollections
 * without any workflow functionality. This bean makes uses of the CRUD
 * operations provided by the Imixs DocumentService.
 * 
 * The default type of a entity created with the DataController is 'workitem'.
 * This property can be changed from a client.
 * 
 * The DataController bean is typically used in session scope.
 * 
 * @author rsoika
 * @version 0.0.1
 */
@Named
@ConversationScoped
public class DocumentController implements Serializable {

	private static final long serialVersionUID = 1L;
	private String defaultType;
	private static Logger logger = Logger.getLogger(DocumentController.class.getName());

	@Inject
	protected Event<DocumentEvent> events;

	@Inject
	Conversation conversation;

	ItemCollection document = null;

	@EJB
	DocumentService documentService;

	public DocumentController() {
		super();
		setDefaultType("workitem");
	}

	/**
	 * This method returns the Default 'type' attribute of the local workitem.
	 */
	public String getDefaultType() {
		return defaultType;
	}

	/**
	 * This method set the default 'type' attribute of the local workitem.
	 * 
	 * Subclasses may overwrite the type
	 * 
	 * @param type
	 */
	public void setDefaultType(String type) {
		this.defaultType = type;
	}

	/**
	 * returns an instance of the DocumentService EJB
	 * 
	 * @return
	 */
	public DocumentService getDocumentService() {
		return documentService;
	}

	/**
	 * Returns the current workItem. If no workitem is defined the method
	 * Instantiates a empty ItemCollection.
	 * 
	 * @return - current workItem or null if not set
	 */
	public ItemCollection getDocument() {
		// do initialize an empty workItem here if null
		if (document == null) {
			document = new ItemCollection();
			document.replaceItemValue("type", getDefaultType());
			setDocument(document);
		}
		return document;
	}

	/**
	 * Set the current worktItem
	 * 
	 * @param workitem - new reference or null to clear the current workItem.
	 */
	public void setDocument(ItemCollection workitem) {
		this.document = workitem;
	}

	/**
	 * This method creates an empty workItem with the default type property and the
	 * property '$Creator' holding the current RemoteUser This method should be
	 * overwritten to add additional Business logic here.
	 * 
	 */
	public void create() {
		reset();
		// initialize new ItemCollection
		getDocument();
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		String sUser = externalContext.getRemoteUser();
		document.replaceItemValue("$Creator", sUser);
		document.replaceItemValue("namCreator", sUser); // backward compatibility
		events.fire(new DocumentEvent(document, DocumentEvent.DOCUMENT_CREATED));
		logger.finest("......ItemCollection created");
	}

	/**
	 * This method saves the current document. This method can be overwritten to add
	 * additional Business logic.
	 * 
	 * @throws AccessDeniedException - if user has insufficient access rights.
	 */
	public void save() throws AccessDeniedException {
		// save workItem ...
		events.fire(new DocumentEvent(getDocument(), DocumentEvent.DOCUMENT_BEFORE_SAVE));

		document = getDocumentService().save(document);
		events.fire(new DocumentEvent(getDocument(), DocumentEvent.DOCUMENT_AFTER_SAVE));

		// close conversation
		if (!conversation.isTransient()) {
			logger.fine("close conversation, id=" + conversation.getId());
			conversation.end();
		}

		logger.finest("......ItemCollection saved");
	}

	/**
	 * Reset current document
	 */
	public void reset() {
		document = new ItemCollection();
	}

	/**
	 * This method loads a document by id from the DocumentService.
	 * 
	 * @param uniqueID - $uniqueId of the document to be loaded
	 */
	public void load(String uniqueID) {

		events.fire(new DocumentEvent(getDocument(), DocumentEvent.DOCUMENT_BEFORE_DELETE));
		if (document != null) {
			logger.finest("......workitem '" + uniqueID + "' loaded");
		} else {
			logger.finest("......workitem '" + uniqueID + "' not found (null)");
		}
		events.fire(new DocumentEvent(getDocument(), DocumentEvent.DOCUMENT_AFTER_DELETE));
		events.fire(new DocumentEvent(document, DocumentEvent.DOCUMENT_CHANGED));
	}

	/**
	 * This action method deletes a workitem. The Method also deletes also all child
	 * workitems recursive
	 * 
	 * @param currentSelection - workitem to be deleted
	 * @throws AccessDeniedException
	 */
	public void delete(String uniqueID) throws AccessDeniedException {
		ItemCollection _workitem = getDocumentService().load(uniqueID);
		if (_workitem != null) {
			documentService.remove(_workitem);
			setDocument(null);
			logger.fine("workitem " + uniqueID + " deleted");
		} else {
			logger.fine("workitem '" + uniqueID + "' not found (null)");
		}
	}

	/**
	 * This action method deletes a workitem and returns an action result. The
	 * method expects the result action as a parameter.
	 * 
	 * @param currentSelection - workitem to be deleted
	 * @param action           - return action
	 * @return action - action event
	 * @throws AccessDeniedException
	 */
	public String delete(String uniqueID, String action) throws AccessDeniedException {
		delete(uniqueID);
		return action;

	}

	/**
	 * Returns true if the current entity was never saved before by the
	 * DocumentService. This is indicated by the time difference of $modified and
	 * $created.
	 * 
	 * @return
	 */
	public boolean isNewWorkitem() {
		Date created = getDocument().getItemValueDate("$created");
		Date modified = getDocument().getItemValueDate("$modified");
		// return (modified == null || created == null || modified.compareTo(created) ==
		// 0);
		return (modified == null || created == null);
	}

	/**
	 * This method can be used to add a Error Messege to the Application Context
	 * during an actionListener Call. Typical this method is used in the
	 * doProcessWrktiem method to display a processing exception to the user. The
	 * method expects the Ressoruce bundle name and the message key inside the
	 * bundle.
	 * 
	 * @param ressourceBundleName
	 * @param messageKey
	 * @param param
	 */
	public void addMessage(String ressourceBundleName, String messageKey, Object param) {
		FacesContext context = FacesContext.getCurrentInstance();
		Locale locale = context.getViewRoot().getLocale();

		ResourceBundle rb = ResourceBundle.getBundle(ressourceBundleName, locale);
		String msgPattern = rb.getString(messageKey);
		String msg = msgPattern;
		if (param != null) {
			Object[] params = { param };
			msg = MessageFormat.format(msgPattern, params);
		}
		FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg);
		context.addMessage(null, facesMsg);
	}

	/**
	 * Starts a new conversation
	 */
	@PostConstruct
	private void init() {
		loadDocument();
		if (conversation.isTransient()) {
			conversation.setTimeout(
					((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
							.getSession().getMaxInactiveInterval() * 1000);
			conversation.begin();
			logger.finest("......start new conversation, id=" + conversation.getId());
		}
	}

	/**
	 * Loads a workitem based on the query params 'id' or 'workitem'
	 */
	private void loadDocument() {
		FacesContext fc = FacesContext.getCurrentInstance();
		Map<String, String> paramMap = fc.getExternalContext().getRequestParameterMap();
		// try to extract tjhe uniqueid form the query string...

		String uniqueid = paramMap.get("id");

		document = documentService.load(uniqueid);
		if (document == null) {
			reset();
		}

		events.fire(new DocumentEvent(document, DocumentEvent.DOCUMENT_CHANGED));

	}
}
