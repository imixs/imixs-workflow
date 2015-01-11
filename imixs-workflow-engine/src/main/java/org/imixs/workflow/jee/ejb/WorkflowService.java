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

package org.imixs.workflow.jee.ejb;

import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.imixs.workflow.ExtendedModel;
import org.imixs.workflow.ExtendedWorkflowContext;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.WorkflowManager;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.jee.jpa.EntityIndex;

/**
 * The WorkflowService is the JEE Implementation for the Imixs Workflow Core
 * API. This interface acts as a service facade and supports basic methods to
 * create, process and access workitems. The Interface extends the core api
 * interface org.imixs.workflow.WorkflowManager with getter methods to fetch
 * collections of workitems.
 * 
 * @author rsoika
 * 
 */

@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
public class WorkflowService implements WorkflowManager,
		ExtendedWorkflowContext, WorkflowServiceRemote {

	// entity properties
	public static final String UNIQUEID = "$uniqueid";
	public static final String UNIQUEIDREF = "$uniqueidref";
	public static final String READACCESS = "$readaccess";
	public static final String WRITEACCESS = "$writeaccess";
	public static final String ISAUTHOR = "$isAuthor";

	// workitem properties
	public static final String WORKITEMID = "$workitemid";
	public static final String PROCESSID = "$processid";
	public static final String MODELVERSION = "$modelversion";
	public static final String ACTIVITYID = "$activityid";

	// view properties
	public static final int SORT_ORDER_CREATED_DESC = 0;
	public static final int SORT_ORDER_CREATED_ASC = 1;
	public static final int SORT_ORDER_MODIFIED_DESC = 2;
	public static final int SORT_ORDER_MODIFIED_ASC = 3;

	private int logLevel = WorkflowKernel.LOG_LEVEL_SEVERE;

	@Inject
	@Any
	private Instance<Plugin> plugins;

	@EJB
	EntityService entityService;

	@EJB
	ModelService modelService;

	@EJB
	ReportService reportService;

	@Resource
	SessionContext ctx;

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	/**
	 * create default index properties
	 * 
	 * @throws AccessDeniedException
	 */
	@PostConstruct
	private void initIndexProperties() throws AccessDeniedException {
		entityService.addIndex("namCreator", EntityIndex.TYP_TEXT);
		entityService.addIndex("txtWorkflowGroup", EntityIndex.TYP_TEXT);
		entityService.addIndex("$ProcessID", EntityIndex.TYP_INT);
		entityService.addIndex("$workitemid", EntityIndex.TYP_TEXT);
		entityService.addIndex("$uniqueidref", EntityIndex.TYP_TEXT);
		entityService.addIndex("txtname", EntityIndex.TYP_TEXT);
		entityService.addIndex("namowner", EntityIndex.TYP_TEXT);
	}

	/**
	 * This method loads a Workitem with the corresponding uniqueid.
	 * 
	 */
	public ItemCollection getWorkItem(String uniqueid) {
		return entityService.load(uniqueid);
	}

	


	/**
	 * Returns a collection of workItems belonging to current user. The method
	 * returns only workitems where the current user has read access.
	 * 
	 * @param startpos
	 *            = optional start position
	 * @param count
	 *            = optional count - default = -1
	 * @param type
	 *            = defines the type property of the workitems to be returnd.
	 *            can be null
	 * @param sortorder
	 *            = defines sortorder (SORT_ORDER_CREATED_DESC = 0
	 *            SORT_ORDER_CREATED_ASC = 1 SORT_ORDER_MODIFIED_DESC = 2
	 *            SORT_ORDER_MODIFIED_ASC = 3)
	 * @return List of workitems
	 * 
	 */
	public List<ItemCollection> getWorkList(int startpos, int count,
			String type, int sortorder) {

		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " wi FROM Entity as wi " + " JOIN wi.textItems as s "
				+ "WHERE ";

		if (type != null && !"".equals(type))
			sQuery += " wi.type='" + type + "' AND ";

		sQuery += " s.itemName = '$workitemid' "
				+ createSortOrderClause(sortorder);
		;
		return entityService.findAllEntities(sQuery, startpos, count);
	}

	/**
	 * Returns the Worklist for the current user
	 */
	public List<ItemCollection> getWorkList() {
		return getWorkList(0, -1, null, 0);
	}

	/**
	 * Returns a collection of workItems belonging to a specified username. The
	 * name is a username or role contained in the $WriteAccess attribute of the
	 * workItem.
	 * 
	 * The method returns only workitems the call has sufficient read access
	 * for.
	 * 
	 * @param name
	 *            = username or role contained in $writeAccess - if null current
	 *            username will be used
	 * @param startpos
	 *            = optional start position
	 * @param count
	 *            = optional count - default = -1
	 * @param type
	 *            = defines the type property of the workitems to be returnd.
	 *            can be null
	 * @param sortorder
	 *            = defines sortorder (SORT_ORDER_CREATED_DESC = 0
	 *            SORT_ORDER_CREATED_ASC = 1 SORT_ORDER_MODIFIED_DESC = 2
	 *            SORT_ORDER_MODIFIED_ASC = 3)
	 * @return List of workitems
	 * 
	 */
	public List<ItemCollection> getWorkListByAuthor(String name, int startpos,
			int count, String type, int sortorder) {

		if (name == null || "".equals(name))
			name = ctx.getCallerPrincipal().getName();

		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " wi FROM Entity as wi " + " JOIN wi.writeAccessList as wa"
				+ " JOIN wi.textItems as s " + "WHERE ";

		if (type != null && !"".equals(type))
			sQuery += " wi.type='" + type + "' AND ";

		sQuery += " wa.value = '" + name + "'"
				+ " AND s.itemName = '$workitemid' "
				+ createSortOrderClause(sortorder);
		return entityService.findAllEntities(sQuery, startpos, count);
	}

	/**
	 * Returns the worklist by author for the current user
	 * 
	 * @param name
	 * @return
	 */
	public List<ItemCollection> getWorkListByAuthor(String name) {
		return getWorkListByAuthor(name, 0, -1, null, 0);
	}

	/**
	 * Returns a collection of workitems created by a specified user
	 * (namCreator). The behaivor is simmilar to the method getWorkList.
	 * 
	 * 
	 * @param name
	 *            = username for property namCreator - if null current username
	 *            will be used
	 * @param startpos
	 *            = optional start position
	 * @param count
	 *            = optional count - default = -1
	 * @param type
	 *            = defines the type property of the workitems to be returnd.
	 *            can be null
	 * @param sortorder
	 *            = defines sortorder (SORT_ORDER_CREATED_DESC = 0
	 *            SORT_ORDER_CREATED_ASC = 1 SORT_ORDER_MODIFIED_DESC = 2
	 *            SORT_ORDER_MODIFIED_ASC = 3)
	 * @return List of workitems
	 * 
	 */
	public List<ItemCollection> getWorkListByCreator(String name, int startpos,
			int count, String type, int sortorder) {

		if (name == null || "".equals(name))
			name = ctx.getCallerPrincipal().getName();

		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " wi FROM Entity as wi"
				+ " JOIN wi.textItems as t JOIN wi.textItems as s " + "WHERE ";
		if (type != null && !"".equals(type))
			sQuery += " wi.type='" + type + "' AND ";

		sQuery += " t.itemName = 'namcreator' and t.itemValue = '" + name + "'"
				+ " AND s.itemName = '$workitemid' "
				+ createSortOrderClause(sortorder);

		return entityService.findAllEntities(sQuery, startpos, count);
	}

	/**
	 * Returns a collection of workitems containing a namOwner property
	 * belonging to a specified username. The namOwner property is typical
	 * controled by the OwnerPlugin
	 * 
	 * @param name
	 *            = username for property namOwner - if null current username
	 *            will be used
	 * @param startpos
	 *            = optional start position
	 * @param count
	 *            = optional count - default = -1
	 * @param type
	 *            = defines the type property of the workitems to be returnd.
	 *            can be null
	 * @param sortorder
	 *            = defines sortorder (SORT_ORDER_CREATED_DESC = 0
	 *            SORT_ORDER_CREATED_ASC = 1 SORT_ORDER_MODIFIED_DESC = 2
	 *            SORT_ORDER_MODIFIED_ASC = 3)
	 * @return List of workitems
	 * 
	 */
	public List<ItemCollection> getWorkListByOwner(String name, int startpos,
			int count, String type, int sortorder) {

		if (name == null || "".equals(name))
			name = ctx.getCallerPrincipal().getName();

		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " wi FROM Entity as wi"
				+ " JOIN wi.textItems as t JOIN wi.textItems as s " + "WHERE ";
		if (type != null && !"".equals(type))
			sQuery += " wi.type='" + type + "' AND ";

		sQuery += " t.itemName = 'namowner' and t.itemValue = '" + name + "'"
				+ " AND s.itemName = '$workitemid' "
				+ createSortOrderClause(sortorder);

		return entityService.findAllEntities(sQuery, startpos, count);
	}

	/**
	 * Returns a collection of workitems where the current user has a
	 * writeAccess. This means the either the username or one of the userroles
	 * is contained in the $writeaccess property
	 * 
	 * 
	 * @param startpos
	 *            = optional start position
	 * @param count
	 *            = optional count - default = -1
	 * @param type
	 *            = defines the type property of the workitems to be returnd.
	 *            can be null
	 * @param sortorder
	 *            = defines sortorder (SORT_ORDER_CREATED_DESC = 0
	 *            SORT_ORDER_CREATED_ASC = 1 SORT_ORDER_MODIFIED_DESC = 2
	 *            SORT_ORDER_MODIFIED_ASC = 3)
	 * @return List of workitems
	 * 
	 */
	public List<ItemCollection> getWorkListByWriteAccess(int startpos,
			int count, String type, int sortorder) {
		StringBuffer nameListBuffer = new StringBuffer();

		String name = ctx.getCallerPrincipal().getName();

		// construct nameList. Begin with empty string '' and username
		nameListBuffer.append("'" + name + "'");
		// now construct role list

		String accessRoles = entityService.getAccessRoles();

		String roleList = "org.imixs.ACCESSLEVEL.READERACCESS,org.imixs.ACCESSLEVEL.AUTHORACCESS,org.imixs.ACCESSLEVEL.EDITORACCESS,"
				+ accessRoles;
		// add each role the user is in to the name list
		StringTokenizer roleListTokens = new StringTokenizer(roleList, ",");
		while (roleListTokens.hasMoreTokens()) {
			String testRole = roleListTokens.nextToken().trim();
			if (!"".equals(testRole) && ctx.isCallerInRole(testRole))
				nameListBuffer.append(",'" + testRole + "'");
		}

		String sQuery = "SELECT wi FROM Entity as wi "
				+ " JOIN wi.writeAccessList wa " + " WHERE ";
		if (type != null && !"".equals(type))
			sQuery += " wi.type='" + type + "' ";

		sQuery += " AND wa.value IN (" + nameListBuffer.toString() + ")"
				+ createSortOrderClause(sortorder);

		return entityService.findAllEntities(sQuery, startpos, count);
	}

	public List<ItemCollection> getWorkListByGroup(String name, int startpos,
			int count, String type, int sortorder) {

		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " wi FROM Entity as wi " + " JOIN wi.textItems as t "
				+ " JOIN wi.textItems as s " + "WHERE ";

		if (type != null && !"".equals(type))
			sQuery += " wi.type='" + type + "' AND ";

		sQuery += " t.itemName = 'txtworkflowgroup' and t.itemValue = '" + name
				+ "'" + " AND s.itemName = '$workitemid' "
				+ createSortOrderClause(sortorder);
		return entityService.findAllEntities(sQuery, startpos, count);
	}

	/**
	 * Returns a collection of workitems belonging to a specified $processID
	 * defined by the workflow model. The behaivor is simmilar to the method
	 * getWorkList.
	 * 
	 * @param aID
	 *            = $ProcessID for the workitems to be returned.
	 * @param startpos
	 *            = optional start position
	 * @param count
	 *            = optional count - default = -1
	 * @param type
	 *            = defines the type property of the workitems to be returnd.
	 *            can be null
	 * @param sortorder
	 *            = defines sortorder (SORT_ORDER_CREATED_DESC = 0
	 *            SORT_ORDER_CREATED_ASC = 1 SORT_ORDER_MODIFIED_DESC = 2
	 *            SORT_ORDER_MODIFIED_ASC = 3)
	 * @return List of workitems
	 * 
	 */
	public List<ItemCollection> getWorkListByProcessID(int aid, int startpos,
			int count, String type, int sortorder) {

		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " wi FROM Entity as wi "
				+ " JOIN wi.integerItems as t JOIN wi.textItems as s "
				+ "WHERE ";

		if (type != null && !"".equals(type))
			sQuery += " wi.type='" + type + "' AND ";

		sQuery += " t.itemName = '$processid' and t.itemValue = '" + aid + "'"
				+ " AND s.itemName = '$workitemid' "
				+ createSortOrderClause(sortorder);

		return entityService.findAllEntities(sQuery, startpos, count);
	}

	/**
	 * Returns a collection of workitems belonging to a specified workitem
	 * identified by the attribute $UniqueIDRef.
	 * 
	 * The behavior of this method is similar to the method getWorkList.
	 * 
	 * @param aref
	 *            A unique reference to another workitem inside a database *
	 * @return List of workitems
	 */
	public List<ItemCollection> getWorkListByRef(String aref) {
		return getWorkListByRef(aref, 0, -1, null, 0);
	}

	/**
	 * Returns a collection of workitems belonging to a specified workitem
	 * identified by the attribute $UniqueIDRef.
	 * 
	 * The behaivor of this Mehtod is simmilar to the method getWorkList.
	 * 
	 * @param aref
	 *            A unique reference to another workitem inside a database *
	 * @param startpos
	 *            = optional start position
	 * @param count
	 *            = optional count - default = -1
	 * @param type
	 *            = defines the type property of the workitems to be returnd.
	 *            can be null
	 * @param sortorder
	 *            = defines sortorder (SORT_ORDER_CREATED_DESC = 0
	 *            SORT_ORDER_CREATED_ASC = 1 SORT_ORDER_MODIFIED_DESC = 2
	 *            SORT_ORDER_MODIFIED_ASC = 3)
	 * @return List of workitems
	 */
	public List<ItemCollection> getWorkListByRef(String aref, int startpos,
			int count, String type, int sortorder) {

		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " wi FROM Entity as wi "
				+ " JOIN wi.textItems as t JOIN wi.textItems as s " + "WHERE ";
		if (type != null && !"".equals(type))
			sQuery += " wi.type='" + type + "' AND ";

		sQuery += " t.itemName = '$uniqueidref' and t.itemValue = '" + aref
				+ "'" + " and s.itemName = '$workitemid' "
				+ createSortOrderClause(sortorder);

		return entityService.findAllEntities(sQuery, startpos, count);
	}

	/**
	 * generates a sort order clause depending on a sororder id
	 * 
	 * @param asortorder
	 * @return
	 */
	private String createSortOrderClause(int asortorder) {
		switch (asortorder) {

		case WorkflowService.SORT_ORDER_CREATED_ASC: {
			return " ORDER BY wi.created asc";
		}
		case WorkflowService.SORT_ORDER_MODIFIED_ASC: {
			return " ORDER BY wi.modified asc";
		}
		case WorkflowService.SORT_ORDER_MODIFIED_DESC: {
			return " ORDER BY wi.modified desc";
		}
		default:
			return " ORDER BY wi.created desc";
		}

	}

	/**
	 * This method processes a workItem by the WorkflowKernel and saves the
	 * workitem after the processing was finished successful. The workitem have
	 * to provide at least the properties '$modelversion', '$processid' and
	 * '$activityid'
	 * 
	 * Before the method starts processing the workitem, the method load the
	 * current instance of the given workitem and compares the property
	 * $processID. If it is not equal the method throws an
	 * ProcessingErrorException.
	 * 
	 * After the workitem was processed successful, the method verifies the
	 * property $workitemList. If this property holds a list of entities these
	 * entities will be saved and the property will be removed automatically.
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
	 */
	@SuppressWarnings("unchecked")
	public ItemCollection processWorkItem(ItemCollection workitem)
			throws AccessDeniedException, ProcessingErrorException,
			PluginException {

		if (workitem == null)
			throw new ProcessingErrorException(
					WorkflowService.class.getSimpleName(),
					ProcessingErrorException.INVALID_WORKITEM,
					"WorkflowService: error - workitem is null");

		// load current instance of this workitem
		ItemCollection currentInstance = this.getWorkItem(workitem
				.getItemValueString(EntityService.UNIQUEID));

		if (currentInstance != null) {
			// test author access
			if (!currentInstance.getItemValueBoolean(ISAUTHOR))
				throw new AccessDeniedException(
						AccessDeniedException.OPERATION_NOTALLOWED,
						"WorkflowService: error - $UnqiueID ("
								+ workitem
										.getItemValueInteger(EntityService.UNIQUEID)
								+ ") no Author Access!");

			// test if $ProcessID matches current instance
			if (currentInstance.getItemValueInteger("$ProcessID") != workitem
					.getItemValueInteger("$ProcessID"))
				throw new ProcessingErrorException(
						WorkflowService.class.getSimpleName(),
						ProcessingErrorException.INVALID_PROCESSID,
						"WorkflowService: error - $ProcesssID ("
								+ workitem.getItemValueInteger("$ProcessID")
								+ ") did not match expected $ProcesssID ("
								+ currentInstance
										.getItemValueInteger("$ProcessID")
								+ ")");
		}

		/*
		 * Fetch the current Profile Entity for this version. The method will
		 * automatically default to the next Version number if the provided
		 * version is no longer provided by the ModelManager
		 */
		String modelversion = workitem.getItemValueString("$modelversion");
		ItemCollection profile = findModelProfile(modelversion);

		WorkflowKernel workflowkernel = new WorkflowKernel(this);

		// register plugins defined in the environment.profile ....
		List<String> vPlugins = profile.getItemValue("txtPlugins");
		for (int i = 0; i < vPlugins.size(); i++) {
			String aPluginClassName = vPlugins.get(i);

			Plugin aPlugin = findPluginByName(aPluginClassName);
			// aPlugin=null;
			if (aPlugin != null) {
				// register injected CDI Plugin
				if (this.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE)
					logger.info("[WorkflowService] register CDI plugin class: "
							+ aPluginClassName + "...");
				workflowkernel.registerPlugin(aPlugin);
			} else {
				// register plugin by class name
				workflowkernel.registerPlugin(aPluginClassName);
			}
		}

		// determine Debuglevel....
		String sDebug = profile.getItemValueString("keyDebugLevel");
		try {
			int idebug = Integer.parseInt(sDebug);
			logLevel = idebug;
		} catch (NumberFormatException e) {
			logLevel = WorkflowKernel.LOG_LEVEL_FINE;
		}

		// test if expected modelVersion matches the profile modelVersion
		if (!modelversion.equals(profile.getItemValueString("$ModelVersion"))) {
			logger.info("WorkflowService: modelversion '"
					+ modelversion
					+ "' no longer provided. Continue processing with modelversion '"
					+ profile.getItemValueString("$ModelVersion") + "'");
			// update Model Version for the WorkItem - this is the version
			// the
			// workitem will be processed now
			modelversion = profile.getItemValueString("$ModelVersion");
			workitem.replaceItemValue("$modelversion", modelversion);
		}

		// identify Caller and update CurrentEditor
		String name;
		name = ctx.getCallerPrincipal().getName();

		// add namCreator if new workitem
		if ("".equals(workitem.getItemValueString("namCreator")))
			workitem.replaceItemValue("namCreator", name);

		// update curreneditor
		workitem.replaceItemValue("namlasteditor",
				workitem.getItemValueString("namcurrenteditor"));
		workitem.replaceItemValue("namcurrenteditor", name);

		// now process the workitem
		workflowkernel.process(workitem);

		if (this.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE)
			logger.info("[WorkflowManager] workitem processed sucessfull");

		return entityService.save(workitem);

	}

	/**
	 * This method returns a n injected Plugin by name or null if not plugin
	 * with the requested class name is injected.
	 * 
	 * @param pluginClassName
	 * @return plugin class or null if not found
	 */
	private Plugin findPluginByName(String pluginClassName) {
		if (pluginClassName == null || pluginClassName.isEmpty())
			return null;

		if (plugins == null || !plugins.iterator().hasNext()) {
			logger.fine("[WorkflowService] no CDI plugins injected");
			return null;
		}
		// iterate over all injected plugins....
		for (Plugin plugin : this.plugins) {
			if (plugin.getClass().getName().equals(pluginClassName)) {
				logger.fine("[WorkflowService] CDI plugin '" + pluginClassName
						+ "' successful injected");
				return plugin;
			}
		}

		return null;
	}

	public void removeWorkItem(ItemCollection aworkitem)
			throws AccessDeniedException {
		entityService.remove(aworkitem);
	}

	/***************************************************************************
	 * Workflow Context
	 */
	public int getLogLevel() {
		return logLevel;
	}

	/**
	 * This Method returns the modelManager Instance. The current ModelVersion
	 * is automatically updated during the Method updateProfileEntity which is
	 * called from the processWorktiem method.
	 * 
	 */
	public Model getModel() {
		return modelService;
	}

	public ExtendedModel getExtendedModel() {
		return modelService;
	}

	public Object getSessionContext() {
		return ctx;
	}

	/**
	 * This method lookups the WorkflowEnvironmentEntity "environment.profile"
	 * for a given $modelVersion. The profile entity is used to determine the
	 * current Plugin definition, FieldMappings and logging information
	 * 
	 * If no WorkflowEnvironmentEntity to the provided ModelVersion is found,
	 * the method fetches automatically the next ModelVersion (in alphabetical
	 * order) provided by the ModelManager. The Version can be different from
	 * the provided version if no corresponding model was found. So the caller
	 * of this method should verfiy the model version of the returned
	 * WorkflowEnvironmentEntity.
	 * 
	 * 
	 * @param modelversion
	 *            - model version to find the profile
	 * @return WorkflowEnvironmentEntity
	 * @throws ProcessingErrorException
	 */
	private ItemCollection findModelProfile(String modelversion) {
		ItemCollection profile = null;

		// if no modelversion is provided default to newest version
		if (modelversion == null || "".equals(modelversion)) {
			try {
				modelversion = modelService.getLatestVersion();
			} catch (ModelException e) {
				throw new ProcessingErrorException(
						WorkflowService.class.getSimpleName(),
						ProcessingErrorException.INVALID_MODELVERSION,
						"WorkflowService: fatal error - no valid model version '"
								+ modelversion
								+ "' found! Verify WorkflowModels.");
			}
		}

		// try to get a Profile matching the provided version.....
		// we lookup for '..>=modelversion' to fetch automatically the next
		// model version if the expected model version is not available
		Collection<ItemCollection> col;
		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " environment FROM Entity AS environment"
				+ " JOIN environment.textItems as n "
				+ " JOIN environment.textItems as v "
				+ " WHERE environment.type = 'WorkflowEnvironmentEntity'"
				+ " AND n.itemName = 'txtname' AND n.itemValue = 'environment.profile'"
				+ " AND v.itemName = '$modelversion' AND v.itemValue >= '"
				+ modelversion + "' ORDER BY v.itemValue";
		col = entityService.findAllEntities(sQuery, 0, 1);
		// if no model for that ModelVersion is found - throw a exception

		if (col.size() == 0) {
			logger.severe("WorkflowService: fatal error - no valid model version '"
					+ modelversion + "' found! Verify WorkflowModels.");
			throw new ProcessingErrorException(
					WorkflowService.class.getSimpleName(),
					ProcessingErrorException.INVALID_MODELVERSION,
					"WorkflowService: fatal error - no valid model version '"
							+ modelversion + "' found! Verify WorkflowModels.");
		}

		profile = col.iterator().next();

		return profile;
	}

	/**
	 * This method returns an instance of the Imixs JEE EntityService used by
	 * the WorkflowManager Implementation. The method can be used to access the
	 * EntityService during a Plugin call.
	 * 
	 * @return EntityService
	 * @throws Exception
	 */
	public EntityService getEntityService() {
		return entityService;
	}

	/**
	 * This method returns an instance of the Imixs JEE ModelService used by the
	 * WorkflowManager Implementation. The method can be used to access the
	 * ModelService during a Plugin call.
	 * 
	 * @return ModelService
	 */
	public ModelService getModelService() {
		return modelService;
	}

	public ReportService getReportService() {
		return reportService;
	}

	/**
	 * Obtain the java.security.Principal that identifies the caller and returns
	 * the name of this principal.
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
	 * This method returns a list of user names, roles and application groups
	 * the caller belongs to.
	 * 
	 * @return
	 */
	public List<String> getUserNameList() {
		return entityService.getUserNameList();
	}
}
