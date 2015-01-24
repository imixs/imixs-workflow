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

package org.imixs.workflow.jee.faces.workitem;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * The DataController can be used in JSF Applications to manage ItemCollections
 * without any workflow functionality. This bean makes uses of the CRUD
 * operations provided by the Imixs EntityService.
 * 
 * The default type of a entity created with the DataController is 'workitem'.
 * This property can be changed from a client.
 * 
 * The DataController bean is typically used in session scope.
 * 
 * @author rsoika
 * @version 0.0.1
 */
public class DataController implements Serializable {

	private static final long serialVersionUID = 1L;
	ItemCollection workitem = null;
	private String type;

	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;
	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	public DataController() {
		super();
		setType("workitem");
	}

	/**
	 * returns an instance of the EntityService EJB
	 * 
	 * @return
	 */
	public org.imixs.workflow.jee.ejb.EntityService getEntityService() {
		return entityService;
	}

	/**
	 * returns the $uniqueID of the current workitem
	 * 
	 * @return
	 */
	public String getID() {
		return getWorkitem().getItemValueString("$uniqueid");
	}

	/**
	 * set the value for the attribute 'type' of a workitem to be generated or
	 * search by this controller
	 */
	public String getType() {
		return type;
	}

	/**
	 * defines the type attribute of a workitem to be generated or search by
	 * this controller
	 * 
	 * Subclasses may overwrite the type
	 * 
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Returns the current workItem.
	 * 
	 * @return - current workItem or null if not set
	 */
	public ItemCollection getWorkitem() {
		// do not initialize an empty workItem here if null!
		return workitem;
	}

	/**
	 * Updates the current worktItem
	 * 
	 * @param workitem
	 *            - new reference or null to clear the current workItem.
	 */
	public void setWorkitem(ItemCollection workitem) {
		this.workitem = workitem;
	}

	/**
	 * This actionListener method creates an empty workItem with the default
	 * type. PropertyActionListener can be used to initialize the new workItem.
	 * This method should be overwritten to add additional Business logic here.
	 * 
	 */
	public void create(ActionEvent event) {
		ItemCollection newWorkitem = new ItemCollection();
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		String sUser = externalContext.getRemoteUser();
		newWorkitem.replaceItemValue("namCreator", sUser);
		newWorkitem.replaceItemValue("type", getType());
		setWorkitem(newWorkitem);
		logger.fine("ItemCollection created");
	}

	/**
	 * This actionListener method saves the current workItem. Method should be
	 * overwritten to add additional Business logic here.
	 * 
	 * @throws AccessDeniedException
	 *             - if user has insufficient access rights.
	 */
	public void save() throws AccessDeniedException {
		// save workItem ...
		workitem = getEntityService().save(workitem);
		logger.fine("ItemCollection saved");
	}

	/**
	 * This action method saves the current workItem and returns an action
	 * result. The method expects the result action as a parameter.
	 * 
	 * @param action
	 *            - defines the action result
	 * @return action result
	 * @throws AccessDeniedException
	 *             - if user has insufficient access rights.
	 */
	public String save(String action) throws AccessDeniedException {
		save();
		return action;
	}

	/**
	 * Reset current workItem to null
	 */
	public void reset() {
		setWorkitem(null);
	}

	/**
	 * ActionListener to reset the current workItem to null
	 * 
	 * @return
	 */
	public void reset(ActionEvent event) {
		reset();
	}

	/**
	 * ActionListener method to load a workItem from the backend. The method can
	 * be called by dataTables to load a workItem for editing
	 * 
	 * @param uniqueID
	 *            - $uniqueId of the workItem to be loaded
	 */
	public void load(String uniqueID) {
		setWorkitem(getEntityService().load(uniqueID));
		logger.fine("ItemCollection '" + uniqueID + "' loaded");
	}

	/**
	 * Action method to load a workItem from the backend and returns an action
	 * result. The method expects the result action as a parameter. The method
	 * can be called by dataTables to load a workItem for editing
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
	 * This action method removes the current selected workitem from a view. The
	 * Method also deletes also all child workitems recursive
	 * 
	 * @param currentSelection
	 *            - workitem to be deleted
	 * @param action
	 *            - return action
	 * @return action - action event
	 * @throws AccessDeniedException
	 */
	public String delete(String uniqueID, String action)
			throws AccessDeniedException {
		delete(uniqueID);
		return action;

	}

	/**
	 * This actionListener method removes the current selected workitem from a
	 * view. The Method also deletes also all child workitems recursive
	 * 
	 * @param currentSelection
	 *            - workitem to be deleted
	 * @throws AccessDeniedException
	 */
	public void delete(String uniqueID) throws AccessDeniedException {
		// if nothing found - then try to cacth current workitem
		ItemCollection currentSelection = getEntityService().load(uniqueID);
		if (currentSelection != null) {
			deleteChilds(currentSelection);
			entityService.remove(currentSelection);
		}
		setWorkitem(null);
		logger.fine("ItemCollection " + uniqueID + " delted");
	}

	/**
	 * This actionListener method deletes all child workItems of a workItem -
	 * also childs from child workItems will be deleted.
	 * 
	 * @param parent
	 */
	public void deleteChilds(ItemCollection parent) {
		try {
			String id = parent.getItemValueString("$uniqueid");

			String sQuery = null;
			sQuery = "SELECT";
			sQuery += " wi FROM Entity as wi JOIN wi.textItems as t "
					+ "WHERE ";
			sQuery += " t.itemName = '$uniqueidref' and t.itemValue = '" + id
					+ "'";

			Collection<ItemCollection> col = entityService.findAllEntities(
					sQuery, 0, -1);

			for (ItemCollection aworkitem : col) {
				// recursive method call
				deleteChilds(aworkitem);
				// remove workitem
				entityService.remove(aworkitem);
			}
			logger.fine("childs for: " + id + " deleted");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * indicates if a workitem was processed before by the workflowService
	 * 
	 * @return
	 */
	public boolean isNewWorkitem() {
		try {
			return (!getWorkitem().hasItem("$unqiueid"));
		} catch (Exception e) {
			return true;
		}
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
	public void addMessage(String ressourceBundleName, String messageKey,
			Object param) {
		FacesContext context = FacesContext.getCurrentInstance();
		Locale locale = context.getViewRoot().getLocale();

		ResourceBundle rb = ResourceBundle.getBundle(ressourceBundleName,
				locale);
		String msgPattern = rb.getString(messageKey);
		String msg = msgPattern;
		if (param != null) {
			Object[] params = { param };
			msg = MessageFormat.format(msgPattern, params);
		}
		FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
				msg, msg);
		context.addMessage(null, facesMsg);
	}

}
