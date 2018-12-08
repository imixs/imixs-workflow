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
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

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
 * @version 2.0
 */
public class DocumentController implements Serializable {

	private static final long serialVersionUID = 1L;

	@EJB
	DocumentService documentService;
	private static Logger logger = Logger.getLogger(DocumentController.class.getName());

	public DocumentController() {
		super();
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
	 * This method creates an empty workItem with the default type property and
	 * the property '$Creator' holding the current RemoteUser This method
	 * should be overwritten to add additional Business logic here.
	 * 
	 */
	public ItemCollection create() {
		
		ItemCollection workitem=new ItemCollection();
		// initialize new ItemCollection
	
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		String sUser = externalContext.getRemoteUser();
		workitem.replaceItemValue("$Creator", sUser);
		workitem.replaceItemValue("namCreator", sUser); // backward compatibility
		logger.finest("......ItemCollection created");
		
		return workitem;
	}

	/**
	 * This actionListener method creates an empty workItem with the default
	 * type property and the property 'namCreator' holding the current
	 * RemoteUser This method should be overwritten to add additional Business
	 * logic here.
	 * 
	 */
	public void create(ActionEvent event) {
		create();
	}

	/**
	 * This method saves the current workItem. This method can be overwritten to
	 * add additional Business logic.
	 * 
	 * @throws AccessDeniedException
	 *             - if user has insufficient access rights.
	 */
	public void save(ItemCollection workitem) throws AccessDeniedException {
		// save workItem ...
		workitem = getDocumentService().save(workitem);
		logger.finest("......ItemCollection saved");
	}

	


	/**
	 * This method loads the current workItem from the DocumentService.
	 * 
	 * @param uniqueID
	 *            - $uniqueId of the workItem to be loaded
	 */
	public ItemCollection  load(String uniqueID) {
		ItemCollection workitem= getDocumentService().load(uniqueID);
		if (workitem != null) {
			logger.finest("......workitem '" + uniqueID + "' loaded");
		} else {
			logger.finest("......workitem '" + uniqueID + "' not found (null)");
		}
		return workitem;
	}

	/**
	 * This Action method loads the current workItem from the DocumentService and
	 * returns an action result. The method expects the result action as a
	 * parameter. The method can be called by dataTables to load a workItem for
	 * editing
	 * 
	 * @param uniqueID
	 *            - $uniqueId of the workItem to be loaded
	 * @param action
	 *            - return action
	 * @return action event
	 */
	public String load(String uniqueID, String action) {
		load(uniqueID);
		return action;
	}

	/**
	 * This action method deletes a workitem. The Method also deletes also all
	 * child workitems recursive
	 * 
	 * @param currentSelection
	 *            - workitem to be deleted
	 * @throws AccessDeniedException
	 */
	public void delete(String uniqueID) throws AccessDeniedException {
		ItemCollection _workitem = getDocumentService().load(uniqueID);
		if (_workitem != null) {
			documentService.remove(_workitem);
			
			logger.fine("workitem " + uniqueID + " deleted");
		} else {
			logger.fine("workitem '" + uniqueID + "' not found (null)");
		}
	}

	/**
	 * This action method deletes a workitem and returns an action result. The
	 * method expects the result action as a parameter.
	 * 
	 * @param currentSelection
	 *            - workitem to be deleted
	 * @param action
	 *            - return action
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
	public boolean isNewWorkitem(ItemCollection workitem) {
		if (workitem==null) {
			return false;
		}
		Date created = workitem.getItemValueDate("$created");
		Date modified = workitem.getItemValueDate("$modified");
		//return (modified == null || created == null || modified.compareTo(created) == 0);
		return (modified == null || created == null);
	}

	/**
	 * This method can be used to add a Error Messege to the Application Context
	 * during an actionListener Call. Typical this method is used in the
	 * doProcessWrktiem method to display a processing exception to the user.
	 * The method expects the Ressoruce bundle name and the message key inside
	 * the bundle.
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

}
