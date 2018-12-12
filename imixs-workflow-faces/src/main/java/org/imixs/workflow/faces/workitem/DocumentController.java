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
 * The DocumentController is a @ConversationScoped CDI bean to control the life
 * cycle of a ItemCollection in an JSF application without any workflow
 * functionality. The bean can be used in single page applications, as well for
 * complex page flows. The controller is easy to use and supports bookmarkable
 * URLs.
 * <p>
 * The DocumentController fires CDI events from the type WorkflowEvent. A CDI
 * bean can observe these events to participate in the processing life cycle.
 * <p>
 * To load a document the methods load(id) and onLoad() can be used. The method
 * load expects the uniqueId of a document to be loaded. The onLoad() method
 * extracts the uniqueid from the query parameter 'id'. This is the recommended
 * way to support bookmarkable URLs. To load a document the onLoad method can be
 * triggered by an jsf viewAction placed in the header of a JSF page:
 * 
 * <pre>
 * {@code
    <f:metadata>
      <f:viewAction action="... documentController.onLoad()" />
    </f:metadata> }
 * </pre>
 * <p>
 * A bookmarkable URL looks like this:
 * <p>
 * {@code /myForm.xthml?id=[UNIQUEID] }
 * <p>
 * In combination with the viewAction the DocumentController is automatically
 * initialized.
 * <p>
 * After a document is loaded, a new conversation is started and the CDI event
 * WorkflowEvent.DOCUMENT_CHANGED is fired.
 * <p>
 * After a document was saved, the conversation is automatically closed. Stale
 * conversations will automatically timeout with the default session timeout.
 * <p>
 * After each call of the method save the Post-Redirect-Get is initialized with
 * the default URL from the start of the conversation. This guarantees
 * bookmakrable URLs.
 * <p>
 * Within a JSF form, the items of a document can be accessed by the getter
 * method getDocument().
 * 
 * <pre>
 *   #{documentController.document.item['$workflowstatus']}
 * </pre>
 * 
 * <p>
 * The default type of a entity created with the DataController is 'workitem'.
 * This property can be changed from a client.
 * 
 * 
 * 
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
	protected Event<WorkflowEvent> events;

	@Inject
	Conversation conversation;

	ItemCollection document = null;

	@EJB
	DocumentService documentService;

	String defaultActionResult;

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
		events.fire(new WorkflowEvent(document, WorkflowEvent.DOCUMENT_CREATED));
		logger.finest("......ItemCollection created");
	}

	/**
	 * This method saves the current document.
	 * <p>
	 * The method fires the WorkflowEvents WORKITEM_BEFORE_SAVE and
	 * WORKITEM_AFTER_SAVE.
	 * 
	 * 
	 * @throws AccessDeniedException - if user has insufficient access rights.
	 */
	public void save() throws AccessDeniedException {

		// save workItem ...
		events.fire(new WorkflowEvent(document, WorkflowEvent.DOCUMENT_BEFORE_SAVE));
		document = getDocumentService().save(document);
		events.fire(new WorkflowEvent(document, WorkflowEvent.DOCUMENT_AFTER_SAVE));

		// close conversation
		stopConversation();

		logger.finest("......ItemCollection saved");
	}

	/**
	 * Reset current document
	 */
	public void reset() {
		document = new ItemCollection();
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
			events.fire(new WorkflowEvent(getDocument(), WorkflowEvent.DOCUMENT_BEFORE_DELETE));
			documentService.remove(_workitem);
			events.fire(new WorkflowEvent(getDocument(), WorkflowEvent.DOCUMENT_AFTER_DELETE));
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

		logger.info("......sinnlos");
	}

	/**
	 * This method extracts a $uniqueid from the query param 'id' and loads the
	 * workitem. After the workitm was loaded, a new conversation is started.
	 * <p>
	 * The method is not running during a JSF Postback of in case of a JSF
	 * validation error.
	 */
	// https://stackoverflow.com/questions/6377798/what-can-fmetadata-fviewparam-and-fviewaction-be-used-for
	public void onLoad() {
		logger.info("...onload...");
		FacesContext facesContext = FacesContext.getCurrentInstance();
		if (!facesContext.isPostback() && !facesContext.isValidationFailed()) {
			// ...
			FacesContext fc = FacesContext.getCurrentInstance();
			Map<String, String> paramMap = fc.getExternalContext().getRequestParameterMap();
			// try to extract tjhe uniqueid form the query string...

			String uniqueid = paramMap.get("id");
			if (uniqueid == null || uniqueid.isEmpty()) {
				// alternative 'workitem=...'
				uniqueid = paramMap.get("workitem");
			}

			setDefaultActionResult(facesContext.getViewRoot().getViewId());

			load(uniqueid);
		}
	}

	/**
	 * Loads a workitem by a given $uniqueid and starts a new conversaton. The
	 * conversaion will be ended after the workitem was processed or after the
	 * MaxInactiveInterval from the session.
	 * 
	 * @param uniqueid
	 */
	public void load(String uniqueid) {
		if (uniqueid != null && !uniqueid.isEmpty()) {
			logger.info("...load uniqueid=" + uniqueid);
			document = documentService.load(uniqueid);
			if (document == null) {
				document = new ItemCollection();
			}
			startConversation();
			// fire event
			events.fire(new WorkflowEvent(document, WorkflowEvent.DOCUMENT_CHANGED));
		}
	}

	public String getDefaultActionResult() {
		if (defaultActionResult == null) {
			defaultActionResult = "";
		}
		return defaultActionResult;
	}

	public void setDefaultActionResult(String defaultActionResult) {
		this.defaultActionResult = defaultActionResult;
	}

	/**
	 * Starts a new conversation
	 */
	public void startConversation() {
		if (conversation.isTransient()) {
			conversation.setTimeout(
					((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
							.getSession().getMaxInactiveInterval() * 1000);
			conversation.begin();
			logger.info("......start new conversation, id=" + conversation.getId());
		}
	}

	/**
	 * Stops the current conversation
	 */
	public void stopConversation() {
		if (!conversation.isTransient()) {
			logger.info("......stopping conversation, id=" + conversation.getId());
			conversation.end();
		}
	}

}
