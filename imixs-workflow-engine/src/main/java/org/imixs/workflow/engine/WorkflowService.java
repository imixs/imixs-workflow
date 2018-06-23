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

package org.imixs.workflow.engine;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.WorkflowManager;
import org.imixs.workflow.engine.plugins.ResultPlugin;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The WorkflowService is the Java EE Implementation for the Imixs Workflow Core
 * API. This interface acts as a service facade and supports basic methods to
 * create, process and access workitems. The Interface extends the core api
 * interface org.imixs.workflow.WorkflowManager with getter methods to fetch
 * collections of workitems.
 * 
 * @author rsoika
 * 
 */

@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
public class WorkflowService implements WorkflowManager, WorkflowContext {

	// workitem properties
	public static final String UNIQUEIDREF = "$uniqueidref";
	public static final String DEFAULT_TYPE = "workitem";
	
	// view properties
	public static final int SORT_ORDER_CREATED_DESC = 0;
	public static final int SORT_ORDER_CREATED_ASC = 1;
	public static final int SORT_ORDER_MODIFIED_DESC = 2;
	public static final int SORT_ORDER_MODIFIED_ASC = 3;

	public static final String INVALID_ITEMVALUE_FORMAT = "INVALID_ITEMVALUE_FORMAT";
	public static final String INVALID_ITEM_FORMAT = "INVALID_ITEM_FORMAT";

	@Inject
	@Any
	private Instance<Plugin> plugins;

	@EJB
	DocumentService documentService;

	@EJB
	ModelService modelService;

	@EJB
	ReportService reportService;

	@EJB
	PropertyService propertyService;

	@Resource
	SessionContext ctx;

	@Inject
	protected Event<ProcessingEvent> processingEvents;

	@Inject
	protected Event<TextEvent> textEvents;

	private static Logger logger = Logger.getLogger(WorkflowService.class.getName());

	/**
	 * This method loads a Workitem with the corresponding uniqueid.
	 * 
	 */
	public ItemCollection getWorkItem(String uniqueid) {
		return documentService.load(uniqueid);
	}

	/**
	 * Returns a collection of workitems containing a namOwner property belonging to
	 * a specified username. The namOwner property can be controlled by the plug-in
	 * {@code org.imixs.workflow.plugins.OwnerPlugin}
	 * 
	 * @param name
	 *            = username for property namOwner - if null current username will
	 *            be used
	 * @param pageSize
	 *            = optional page count (default 20)
	 * @param pageIndex
	 *            = optional start position
	 * @param type
	 *            = defines the type property of the workitems to be returnd. can be
	 *            null
	 * @param sortBy
	 *            -optional field to sort the result
	 * @param sortReverse
	 *            - optional sort direction
	 * 
	 * @return List of workitems
	 * 
	 */
	public List<ItemCollection> getWorkListByOwner(String name, String type, int pageSize, int pageIndex, String sortBy,
			boolean sortReverse) {

		if (name == null || "".equals(name))
			name = ctx.getCallerPrincipal().getName();

		String searchTerm = "(";
		if (type != null && !"".equals(type)) {
			searchTerm += " type:\"" + type + "\" AND ";
		}
		searchTerm += " namowner:\"" + name + "\" )";
		try {
			return documentService.find(searchTerm, pageSize, pageIndex, sortBy, sortReverse);
		} catch (QueryException e) {
			logger.severe("getWorkListByOwner - invalid param: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Returns a collection of workItems belonging to a specified username. The name
	 * is a username or role contained in the $WriteAccess attribute of the
	 * workItem.
	 * 
	 * The method returns only workitems the call has sufficient read access for.
	 * 
	 * @param name
	 *            = username or role contained in $writeAccess - if null current
	 *            username will be used
	 * @param pageSize
	 *            = optional page count (default 20)
	 * @param pageIndex
	 *            = optional start position
	 * @param type
	 *            = defines the type property of the workitems to be returnd. can be
	 *            null
	 * @param sortBy
	 *            -optional field to sort the result
	 * @param sortReverse
	 *            - optional sort direction
	 * 
	 * @return List of workitems
	 * 
	 */
	public List<ItemCollection> getWorkListByAuthor(String name, String type, int pageSize, int pageIndex,
			String sortBy, boolean sortReverse) {

		if (name == null || "".equals(name))
			name = ctx.getCallerPrincipal().getName();

		String searchTerm = "(";
		if (type != null && !"".equals(type)) {
			searchTerm += " type:\"" + type + "\" AND ";
		}
		searchTerm += " $writeaccess:\"" + name + "\" )";

		try {
			return documentService.find(searchTerm, pageSize, pageIndex, sortBy, sortReverse);
		} catch (QueryException e) {
			logger.severe("getWorkListByAuthor - invalid param: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Returns a collection of workitems created by a specified user (namCreator).
	 * The behaivor is simmilar to the method getWorkList.
	 * 
	 * 
	 * @param name
	 *            = username for property namCreator - if null current username will
	 *            be used
	 * @param pageSize
	 *            = optional page count (default 20)
	 * @param pageIndex
	 *            = optional start position
	 * @param type
	 *            = defines the type property of the workitems to be returnd. can be
	 *            null
	 * @param sortBy
	 *            -optional field to sort the result
	 * @param sortReverse
	 *            - optional sort direction
	 * 
	 * @return List of workitems
	 * 
	 */
	public List<ItemCollection> getWorkListByCreator(String name, String type, int pageSize, int pageIndex,
			String sortBy, boolean sortReverse) {

		if (name == null || "".equals(name))
			name = ctx.getCallerPrincipal().getName();

		String searchTerm = "(";
		if (type != null && !"".equals(type)) {
			searchTerm += " type:\"" + type + "\" AND ";
		}
		searchTerm += " namcreator:\"" + name + "\" )";
		try {
			return documentService.find(searchTerm, pageSize, pageIndex, sortBy, sortReverse);
		} catch (QueryException e) {
			logger.severe("getWorkListByCreator - invalid param: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Returns a collection of workitems where the current user has a writeAccess.
	 * This means the either the username or one of the userroles is contained in
	 * the $writeaccess property
	 * 
	 * 
	 * @param pageSize
	 *            = optional page count (default 20)
	 * @param pageIndex
	 *            = optional start position
	 * @param type
	 *            = defines the type property of the workitems to be returnd. can be
	 *            null
	 * @param sortorder
	 *            = defines sortorder (SORT_ORDER_CREATED_DESC = 0
	 *            SORT_ORDER_CREATED_ASC = 1 SORT_ORDER_MODIFIED_DESC = 2
	 *            SORT_ORDER_MODIFIED_ASC = 3)
	 * @param sortBy
	 *            -optional field to sort the result
	 * @param sortReverse
	 *            - optional sort direction
	 * 
	 * @return List of workitems
	 * 
	 */
	public List<ItemCollection> getWorkListByWriteAccess(String type, int pageSize, int pageIndex, String sortBy,
			boolean sortReverse) {
		StringBuffer nameListBuffer = new StringBuffer();

		String name = ctx.getCallerPrincipal().getName();

		// construct nameList. Begin with empty string '' and username
		nameListBuffer.append("($writeaccess:\"" + name + "\"");
		// now construct role list

		String accessRoles = documentService.getAccessRoles();

		String roleList = "org.imixs.ACCESSLEVEL.READERACCESS,org.imixs.ACCESSLEVEL.AUTHORACCESS,org.imixs.ACCESSLEVEL.EDITORACCESS,"
				+ accessRoles;
		// add each role the user is in to the name list
		StringTokenizer roleListTokens = new StringTokenizer(roleList, ",");
		while (roleListTokens.hasMoreTokens()) {
			String testRole = roleListTokens.nextToken().trim();
			if (!"".equals(testRole) && ctx.isCallerInRole(testRole))
				nameListBuffer.append(" OR $writeaccess:\"" + testRole + "\"");
		}
		nameListBuffer.append(")");

		String searchTerm = "(";
		if (type != null && !"".equals(type)) {
			searchTerm += " type:\"" + type + "\" AND " + nameListBuffer.toString();
		}
		searchTerm += " $writeaccess:\"" + name + "\" )";
		try {
			return documentService.find(searchTerm, pageSize, pageIndex, sortBy, sortReverse);
		} catch (QueryException e) {
			logger.severe("getWorkListByWriteAccess - invalid param: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Returns a list of workitems filtered by the field $workflowgroup
	 * 
	 * the method supports still also the deprecated field "txtworkflowgroup"
	 * 
	 * @param name
	 * @param type
	 * @param pageSize
	 *            = optional page count (default 20)
	 * @param pageIndex
	 *            = optional start position
	 * @param sortBy
	 *            -optional field to sort the result
	 * @param sortReverse
	 *            - optional sort direction
	 * 
	 * @return
	 */

	public List<ItemCollection> getWorkListByGroup(String name, String type, int pageSize, int pageIndex, String sortBy,
			boolean sortReverse) {

		String searchTerm = "(";
		if (type != null && !"".equals(type)) {
			searchTerm += " type:\"" + type + "\" AND ";
		}
		// we support still the deprecated txtworkflowgroup
		searchTerm += " ($workflowgroup:\"" + name + "\" OR txtworkflowgroup:\"" + name + "\") )";
		try {
			return documentService.find(searchTerm, pageSize, pageIndex, sortBy, sortReverse);
		} catch (QueryException e) {
			logger.severe("getWorkListByGroup - invalid param: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Returns a collection of workitems belonging to a specified $taskID defined
	 * by the workflow model. The behaivor is simmilar to the method getWorkList.
	 * 
	 * @param aID
	 *            = $taskID for the workitems to be returned.
	 * @param pageSize
	 *            = optional page count (default 20)
	 * @param pageIndex
	 *            = optional start position
	 * @param type
	 *            = defines the type property of the workitems to be returnd. can be
	 *            null
	 * @param sortBy
	 *            -optional field to sort the result
	 * @param sortReverse
	 *            - optional sort direction
	 * 
	 * @return List of workitems
	 * 
	 */
	public List<ItemCollection> getWorkListByProcessID(int aid, String type, int pageSize, int pageIndex, String sortBy,
			boolean sortReverse) {

		String searchTerm = "(";
		if (type != null && !"".equals(type)) {
			searchTerm += " type:\"" + type + "\" AND ";
		}
		// need to be fixed during slow migration issue #384
		searchTerm += " $processid:\"" + aid + "\" )";
		try {
			return documentService.find(searchTerm, pageSize, pageIndex, sortBy, sortReverse);
		} catch (QueryException e) {
			logger.severe("getWorkListByProcessID - invalid param: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Returns a collection of workitems belonging to a specified workitem
	 * identified by the attribute $UniqueIDRef.
	 * 
	 * The behaivor of this Mehtod is simmilar to the method getWorkList.
	 * 
	 * @param aref
	 *            A unique reference to another workitem inside a database *
	 * @param pageSize
	 *            = optional page count (default 20)
	 * @param pageIndex
	 *            = optional start position
	 * @param type
	 *            = defines the type property of the workitems to be returnd. can be
	 *            null
	 * @param sortBy
	 *            -optional field to sort the result
	 * @param sortReverse
	 *            - optional sort direction
	 * 
	 * @return List of workitems
	 */
	public List<ItemCollection> getWorkListByRef(String aref, String type, int pageSize, int pageIndex, String sortBy,
			boolean sortReverse) {

		String searchTerm = "(";
		if (type != null && !"".equals(type)) {
			searchTerm += " type:\"" + type + "\" AND ";
		}
		searchTerm += " $uniqueidref:\"" + aref + "\" )";
		try {
			return documentService.find(searchTerm, pageSize, pageIndex, sortBy, sortReverse);
		} catch (QueryException e) {
			logger.severe("getWorkListByRef - invalid param: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Returns a collection of all workitems belonging to a specified workitem
	 * identified by the attribute $UniqueIDRef.
	 * 
	 * @return List of workitems
	 */
	public List<ItemCollection> getWorkListByRef(String aref) {
		return getWorkListByRef(aref, null, 0, 0, null, false);
	}

	/**
	 * This returns a list of workflow events assigned to a given workitem. The
	 * method evaluates the events for the current $modelversion and $taskid. The
	 * result list is filtered by the properties 'keypublicresult' and
	 * 'keyRestrictedVisibility'.
	 * 
	 * If the property keyRestrictedVisibility exits the method test if the current
	 * username is listed in one of the namefields.
	 * 
	 * If the current user is in the role 'org.imixs.ACCESSLEVEL.MANAGERACCESS' the
	 * property keyRestrictedVisibility will be ignored.
	 * 
	 * @see imixs-bpmn
	 * @param workitem
	 * @return
	 * @throws ModelException
	 */
	@SuppressWarnings("unchecked")
	public List<ItemCollection> getEvents(ItemCollection workitem) throws ModelException {
		List<ItemCollection> result = new ArrayList<ItemCollection>();
		int processID = workitem.getTaskID();
		// verify if version is valid
		Model model = modelService.getModelByWorkitem(workitem);

		List<ItemCollection> eventList = model.findAllEventsByTask(processID);

		String username = getUserName();
		boolean bManagerAccess = ctx.isCallerInRole(DocumentService.ACCESSLEVEL_MANAGERACCESS);

		// now filter events which are not public (keypublicresult==false) or
		// restricted for current user (keyRestrictedVisibility).
		for (ItemCollection event : eventList) {
			// test keypublicresult==false

			// ad only activities with userControlled != No
			if ("0".equals(event.getItemValueString("keypublicresult"))) {
				continue;
			}

			// test user access level
			List<String> readAccessList = event.getItemValue("$readaccess");
			if (!bManagerAccess && !readAccessList.isEmpty()) {
				/**
				 * check read access for current user
				 */
				boolean accessGranted = false;
				// get user name list
				List<String> auserNameList = getUserNameList();

				// check each read access
				for (String aReadAccess : readAccessList) {
					if (aReadAccess != null && !aReadAccess.isEmpty()) {
						if (auserNameList.indexOf(aReadAccess) > -1) {
							accessGranted = true;
							break;
						}
					}
				}
				if (!accessGranted) {
					// user has no read access!
					continue;
				}
			}

			// test RestrictedVisibility
			List<String> restrictedList = event.getItemValue("keyRestrictedVisibility");
			if (!bManagerAccess && !restrictedList.isEmpty()) {
				// test each item for the current user name...
				List<String> totalNameList = new ArrayList<String>();
				for (String itemName : restrictedList) {
					totalNameList.addAll(workitem.getItemValue(itemName));
				}
				// remove null and empty values....
				totalNameList.removeAll(Collections.singleton(null));
				totalNameList.removeAll(Collections.singleton(""));
				if (!totalNameList.isEmpty() && !totalNameList.contains(username)) {
					// event is not visible for current user!
					continue;
				}
			}
			result.add(event);
		}

		return result;

	}

	/**
	 * This method processes a workItem by the WorkflowKernel and saves the workitem
	 * after the processing was finished successful. The workitem have to provide at
	 * least the properties '$modelversion', '$taskid' and '$eventid'
	 * 
	 * Before the method starts processing the workitem, the method load the current
	 * instance of the given workitem and compares the property $taskID. If it is
	 * not equal the method throws an ProcessingErrorException.
	 * 
	 * After the workitem was processed successful, the method verifies the property
	 * $workitemList. If this property holds a list of entities these entities will
	 * be saved and the property will be removed automatically.
	 * 
	 * 
	 * The method provides a observer pattern for plugins to get called during the
	 * processing phase.
	 * 
	 * @param workitem
	 *            - the workItem to be processed
	 * @return updated version of the processed workItem
	 * @throws AccessDeniedException
	 *             - thrown if the user has insufficient access to update the
	 *             workItem
	 * @throws ProcessingErrorException
	 *             - thrown if the workitem could not be processed by the
	 *             workflowKernel
	 * @throws PluginException
	 *             - thrown if processing by a plugin fails
	 * @throws ModelException
	 */
	@SuppressWarnings("unchecked")
	public ItemCollection processWorkItem(ItemCollection workitem)
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		long lStartTime = System.currentTimeMillis();

		if (workitem == null)
			throw new ProcessingErrorException(WorkflowService.class.getSimpleName(),
					ProcessingErrorException.INVALID_WORKITEM, "WorkflowService: error - workitem is null");

		// fire event
		if (processingEvents != null) {
			processingEvents.fire(new ProcessingEvent(workitem, ProcessingEvent.BEFORE_PROCESS));
		} else {
			logger.warning("CDI Support is missing - ProcessingEvent wil not be fired");
		}
		// load current instance of this workitem
		ItemCollection currentInstance = this.getWorkItem(workitem.getItemValueString(WorkflowKernel.UNIQUEID));

		if (currentInstance != null) {
			// test author access
			if (!currentInstance.getItemValueBoolean(DocumentService.ISAUTHOR))
				throw new AccessDeniedException(AccessDeniedException.OPERATION_NOTALLOWED,
						"WorkflowService: error - $UniqueID (" + workitem.getItemValueInteger(WorkflowKernel.UNIQUEID)
								+ ") no Author Access!");

			// test if $taskID matches current instance
			if (workitem.getTaskID() > 0
					&& currentInstance.getTaskID() != workitem.getTaskID())
				throw new ProcessingErrorException(WorkflowService.class.getSimpleName(),
						ProcessingErrorException.INVALID_PROCESSID,
						"WorkflowService: error - $taskID (" + workitem.getTaskID()
								+ ") did not match expected $ProcesssID ("
								+ currentInstance.getTaskID() + ")");

			// merge workitem into current instance (issue #86)
			// an instance of this WorkItem still exists! so we update the new
			// values....
			// currentInstance.getAllItems().putAll(workitem.getAllItems());
			currentInstance.replaceAllItems(workitem.getAllItems());
			workitem = currentInstance;

		}
		
		// verify type attribute
		if ("".equals(workitem.getType())) {
			workitem.replaceItemValue("type", DEFAULT_TYPE);
		}

		/*
		 * Lookup current processEntity. If not available update model to latest
		 * matching model version
		 */
		Model model = null;
		try {
			model = this.getModelManager().getModelByWorkitem(workitem);
		} catch (ModelException e) {
			throw new ProcessingErrorException(WorkflowService.class.getSimpleName(),
					ProcessingErrorException.INVALID_PROCESSID, e.getMessage(), e);
		}

		// Fetch the current Profile Entity for this version.
		ItemCollection profile = model.getDefinition();
		WorkflowKernel workflowkernel = new WorkflowKernel(this);
		// register plugins defined in the environment.profile ....
		List<String> vPlugins = (List<String>) profile.getItemValue("txtPlugins");
		for (int i = 0; i < vPlugins.size(); i++) {
			String aPluginClassName = vPlugins.get(i);

			Plugin aPlugin = findPluginByName(aPluginClassName);
			// aPlugin=null;
			if (aPlugin != null) {
				// register injected CDI Plugin
				logger.finest("......register CDI plugin class: " + aPluginClassName + "...");
				workflowkernel.registerPlugin(aPlugin);
			} else {
				// register plugin by class name
				workflowkernel.registerPlugin(aPluginClassName);
			}

		}

		// identify Caller and update CurrentEditor
		String nameEditor;
		nameEditor = ctx.getCallerPrincipal().getName();

		// add namCreator if empty
		// migrate $creator
		if (workitem.getItemValueString("$creator").isEmpty() && !workitem.getItemValueString("namCreator").isEmpty()) {
			workitem.replaceItemValue("$creator", workitem.getItemValue("namCreator"));
		}

		if (workitem.getItemValueString("$creator").isEmpty()) {
			workitem.replaceItemValue("$creator", nameEditor);
			// support deprecated fieldname
			workitem.replaceItemValue("namCreator", nameEditor);
		}

		// update namLastEditor only if current editor has changed
		if (!nameEditor.equals(workitem.getItemValueString("$editor"))
				&& !workitem.getItemValueString("$editor").isEmpty()) {

			workitem.replaceItemValue("$lasteditor", workitem.getItemValueString("$editor"));
			// deprecated
			workitem.replaceItemValue("namlasteditor", workitem.getItemValueString("$editor"));
		}

		// update $editor

		workitem.replaceItemValue("$editor", nameEditor);
		// deprecated
		workitem.replaceItemValue("namcurrenteditor", nameEditor);

		// now process the workitem
		try {
			long lKernelTime = System.currentTimeMillis();
			workitem = workflowkernel.process(workitem);
			logger.fine("...WorkflowKernel processing time=" + (System.currentTimeMillis() - lKernelTime) + "ms");
		} catch (PluginException pe) {
			// if a plugin exception occurs we roll back the transaction.
			logger.severe("processing workitem '" + workitem.getItemValueString(WorkflowKernel.UNIQUEID)
					+ " failed, rollback transaction...");
			ctx.setRollbackOnly();
			throw pe;
		}

		// fire event
		if (processingEvents != null) {
			processingEvents.fire(new ProcessingEvent(workitem, ProcessingEvent.AFTER_PROCESS));
		}
		// Now fire also events for all split versions.....
		List<ItemCollection> splitWorkitems = workflowkernel.getSplitWorkitems();
		for (ItemCollection splitWorkitemm : splitWorkitems) {
			// fire event
			if (processingEvents != null) {
				processingEvents.fire(new ProcessingEvent(splitWorkitemm, ProcessingEvent.AFTER_PROCESS));
			}
			documentService.save(splitWorkitemm);
		}

		workitem = documentService.save(workitem);

		logger.fine("...total processing time=" + (System.currentTimeMillis() - lStartTime) + "ms");

		return workitem;
	}

	public void removeWorkItem(ItemCollection aworkitem) throws AccessDeniedException {
		documentService.remove(aworkitem);
	}

	/**
	 * This Method returns the modelManager Instance. The current ModelVersion is
	 * automatically updated during the Method updateProfileEntity which is called
	 * from the processWorktiem method.
	 * 
	 */
	public ModelManager getModelManager() {
		return modelService;
	}

	/**
	 * Returns an instance of the EJB session context.
	 * 
	 * @return
	 */
	public SessionContext getSessionContext() {
		return ctx;
	}

	/**
	 * Returns an instance of the DocumentService EJB.
	 * 
	 * @return
	 */
	public DocumentService getDocumentService() {
		return documentService;
	}

	/**
	 * Returns an instance of the ReportService EJB.
	 * 
	 * @return
	 */
	public ReportService getReportService() {
		return reportService;
	}

	/**
	 * Returns an instance of the PropertyService EJB.
	 * 
	 * @return
	 */
	public PropertyService getPropertyService() {
		return propertyService;
	}

	/**
	 * Obtain the java.security.Principal that identifies the caller and returns the
	 * name of this principal.
	 * 
	 * @return the user name
	 */
	public String getUserName() {
		return ctx.getCallerPrincipal().getName();

	}

	/**
	 * Test if the caller has a given security role.
	 * 
	 * @param rolename
	 * @return true if user is in role
	 */
	public boolean isUserInRole(String rolename) {
		try {
			return ctx.isCallerInRole(rolename);
		} catch (Exception e) {
			// avoid a exception for a role request which is not defined
			return false;
		}
	}

	/**
	 * This method returns a list of user names, roles and application groups the
	 * caller belongs to.
	 * 
	 * @return
	 */
	public List<String> getUserNameList() {
		return documentService.getUserNameList();
	}

	/**
	 * The method adaptText can be called to replace predefined xml tags included in
	 * a text with custom values. The method fires a CDI event to inform
	 * TextAdapterServices to parse and adapt a given text fragment.
	 * 
	 * @param text
	 * @param documentContext
	 * @return
	 * @throws PluginException
	 */
	public String adaptText(String text, ItemCollection documentContext) throws PluginException {
		// fire event
		if (textEvents != null) {
			TextEvent event = new TextEvent(text, documentContext);
			textEvents.fire(event);
			text = event.getText();
		} else {
			logger.warning("CDI Support is missing - TextEvent wil not be fired");
		}
		return text;
	}

	
	/**
	 * The method adaptTextList can be called to replace  a text with custom values. The method fires a CDI event to inform
	 * TextAdapterServices to parse and adapt a given text fragment.
	 * The method expects a textList result.
	 * 
	 * @param text
	 * @param documentContext
	 * @return
	 * @throws PluginException 
	 */
	public List<String> adaptTextList(String text, ItemCollection documentContext) throws PluginException {
		// fire event
		if (textEvents != null) {
			TextEvent event = new TextEvent(text, documentContext);
			textEvents.fire(event);
			return event.getTextList();
		} else {
			logger.warning("CDI Support is missing - TextEvent wil not be fired");
		}
		// no result return default
		List<String> textList=new ArrayList<String>();
		textList.add(text);
		return textList;
	}

	
	
	/**
	 * The method evaluates the WorkflowResult for a given BPMN event and returns a
	 * ItemColleciton containing all item definitions. Each item definition of a
	 * WorkflowResult contains a name and a optional list of additional attributes.
	 * The method generates a item for each content element and attribute value.
	 * <br>
	 * e.g. <item name="comment" ignore="true">text</item> <br>
	 * will result in the attributes 'comment' with value 'text' and
	 * 'comment.ignore' with the value 'true'
	 * 
	 * Also embedded itemVaues can be resolved (resolveItemValues=true):
	 * 
	 * <code>
	 * 		<somedata>ABC<itemvalue>$uniqueid</itemvalue></somedata>
	 * </code>
	 * 
	 * This example will result in a new item 'somedata' with the $uniqueid prefixed
	 * with 'ABC'
	 * 
	 * @see http://ganeshtiwaridotcomdotnp.blogspot.de/2011/12/htmlxml-tag-
	 *      parsing-using-regex-in-java.html
	 * @param event
	 * @param documentContext
	 * @param resolveItemValues
	 *            - if true, itemValue tags will be resolved.
	 * @return
	 * @throws PluginException
	 */
	public ItemCollection evalWorkflowResult(ItemCollection event, ItemCollection documentContext,
			boolean resolveItemValues) throws PluginException {
		boolean invalidPattern = true;

		ItemCollection result = new ItemCollection();
		String workflowResult = event.getItemValueString("txtActivityResult");
		if (workflowResult.trim().isEmpty()) {
			return null;
		}
		// replace dynamic values?
		if (resolveItemValues) {
			workflowResult = adaptText(workflowResult, documentContext);
		}
		// Extract all <item> tags with attributes using regex (including empty item
		// tags)
		// The XMLParser class is not suited in this scenario.
		Pattern pattern = Pattern.compile("<item(.*?)>(.*?)</item>|<item(.*?)./>", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(workflowResult);
		while (matcher.find()) {
			invalidPattern = false;
			// we expect up to 3 different result groups

			// group 0 contains complete item string
			String attributes = matcher.group(1);
			String content = matcher.group(2);

			// test if empty tag (group 1 and 2 empty)
			if (attributes == null || content == null) {
				attributes = matcher.group(3);
			}

			if (content == null) {
				content = "";
			}

			// now extract the attributes to verify the item name..
			if (attributes != null && !attributes.isEmpty()) {
				// parse attributes...
				String spattern = "(\\S+)=[\"']?((?:.(?![\"']?\\s+(?:\\S+)=|[>\"']))+.)[\"']?";
				Pattern attributePattern = Pattern.compile(spattern);
				Matcher attributeMatcher = attributePattern.matcher(attributes);
				Map<String, String> attrMap = new HashMap<String, String>();
				while (attributeMatcher.find()) {
					String attrName = attributeMatcher.group(1); // name
					String attrValue = attributeMatcher.group(2); // value
					attrMap.put(attrName, attrValue);
				}

				String itemName = attrMap.get("name");
				if (itemName == null) {
					throw new PluginException(ResultPlugin.class.getSimpleName(), INVALID_ITEM_FORMAT,
							"<item> tag contains no name attribute.");
				}

				if (itemName.startsWith("$")) {
					throw new PluginException(ResultPlugin.class.getSimpleName(), INVALID_ITEM_FORMAT,
							"<item> tag contains invalid name attribute '" + itemName + "'.");
				}

				// now add optional attributes if available
				for (String attrName : attrMap.keySet()) {
					// we need to skip the 'name' attribute
					if (!"name".equals(attrName)) {
						result.appendItemValue(itemName + "." + attrName, attrMap.get(attrName));
					}
				}

				// test if the type attribute was provided to convert content?
				String sType = result.getItemValueString(itemName + ".type");
				String sFormat = result.getItemValueString(itemName + ".format");
				if (!sType.isEmpty()) {
					// convert content type
					if ("boolean".equalsIgnoreCase(sType)) {
						result.appendItemValue(itemName, Boolean.valueOf(content));
					} else if ("integer".equalsIgnoreCase(sType)) {
						result.appendItemValue(itemName, Integer.valueOf(content));
					} else if ("double".equalsIgnoreCase(sType)) {
						result.appendItemValue(itemName, Double.valueOf(content));
					} else if ("date".equalsIgnoreCase(sType)) {
						if (content == null || content.isEmpty()) {
							// no value available - no op!
							logger.finer("......can not convert empty string into date object");
						} else {
							// convert content value to date object
							try {
								logger.finer("......convert string into date object");
								Date dateResult = null;
								if (sFormat == null || sFormat.isEmpty()) {
									// use standard formate short/short
									dateResult = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).parse(content);
								} else {
									// use given formatter (see: TextItemValueAdapter)
									DateFormat dateFormat = new SimpleDateFormat(sFormat);
									dateResult = dateFormat.parse(content);
								}
								result.appendItemValue(itemName, dateResult);
							} catch (ParseException e) {
								logger.finer("failed to convert string into date object: " + e.getMessage());
							}
						}

					} else
						// no type conversion
						result.appendItemValue(itemName, content);
				} else {
					// no type definition
					result.appendItemValue(itemName, content);
				}

			} else {
				throw new PluginException(ResultPlugin.class.getSimpleName(), INVALID_ITEM_FORMAT,
						"<item> tag contains no name attribute.");

			}

		}

		// test for general invalid format
		if (invalidPattern) {
			throw new PluginException(ResultPlugin.class.getSimpleName(), INVALID_ITEM_FORMAT,
					"invalid <item> tag format in workflowResult: " + workflowResult
							+ "  , expected format is <item name=\"...\">...</item> ");
		}
		return result;
	}

	/**
	 * The method evaluates the WorkflowResult of a BPMN event and resolves embedded
	 * ItemValues.
	 * 
	 * * <code>
	 * 		<somedata>ABC<itemvalue>$uniqueid</itemvalue></somedata>
	 * </code>
	 * 
	 * This example will result in a new item 'somedata' with the $uniqueid prafixed
	 * with 'ABC'
	 * 
	 * @see evalWorkflowResult(ItemCollection activityEntity, ItemCollection
	 *      documentContext,boolean resolveItemValues)
	 * @param activityEntity
	 * @param documentContext
	 * @return
	 * @throws PluginException
	 */
	public ItemCollection evalWorkflowResult(ItemCollection activityEntity, ItemCollection documentContext)
			throws PluginException {
		return evalWorkflowResult(activityEntity, documentContext, true);
	}

	/**
	 * This method evaluates the next task based on a Model Event element. If the
	 * event did not point to a new task, the current task will be returned.
	 * 
	 * The method supports the 'conditional-events' and 'split-events'.
	 * 
	 * A conditional-event contains the attribute 'keyExclusiveConditions' defining
	 * conditional targets (tasks) or adds conditional follow up events
	 * 
	 * A split-event contains the attribute 'keySplitConditions' defining the target
	 * for the current master version (condition evaluates to 'true')
	 * 
	 * @return Task entity
	 * @throws PluginException
	 * @throws ModelException
	 */
	public ItemCollection evalNextTask(ItemCollection documentContext, ItemCollection event)
			throws PluginException, ModelException {
		WorkflowKernel workflowkernel = new WorkflowKernel(this);
		return workflowkernel.findNextTask(documentContext, event);
	}

	/**
	 * This method returns a n injected Plugin by name or null if not plugin with
	 * the requested class name is injected.
	 * 
	 * @param pluginClassName
	 * @return plugin class or null if not found
	 */
	private Plugin findPluginByName(String pluginClassName) {
		if (pluginClassName == null || pluginClassName.isEmpty())
			return null;

		if (plugins == null || !plugins.iterator().hasNext()) {
			logger.finest("......no CDI plugins injected");
			return null;
		}
		// iterate over all injected plugins....
		for (Plugin plugin : this.plugins) {
			if (plugin.getClass().getName().equals(pluginClassName)) {
				logger.finest("......CDI plugin '" + pluginClassName + "' successful injected");
				return plugin;
			}
		}

		return null;
	}

}
