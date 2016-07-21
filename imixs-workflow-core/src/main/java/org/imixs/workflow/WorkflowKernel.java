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

package org.imixs.workflow;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;

/**
 * The Workflowkernel is the core component of this Framework to control the
 * processing of a workitem. A <code>Workflowmanager</code> loads an instance of
 * a Workflowkernel and hand over a <code>Model</code> and register
 * <code>Plugins</code> for processing one or many workitems.
 * 
 * @author Ralph Soika
 * @version 1.1
 * @see org.imixs.workflow.WorkflowManager
 */

public class WorkflowKernel {

	public static final String UNDEFINED_PROCESSID = "UNDEFINED_PROCESSID";
	public static final String UNDEFINED_ACTIVITYID = "UNDEFINED_ACTIVITYID";
	public static final String UNDEFINED_WORKITEM = "UNDEFINED_WORKITEM";
	public static final String UNDEFINED_PLUGIN_ERROR = "UNDEFINED_PLUGIN_ERROR";
	public static final String ACTIVITY_NOT_FOUND = "ACTIVITY_NOT_FOUND";
	public static final String MODEL_ERROR = "MODEL_ERROR";
	public static final String PLUGIN_NOT_CREATEABLE = "PLUGIN_NOT_CREATEABLE";
	public static final String PLUGIN_NOT_REGISTERED = "PLUGIN_NOT_REGISTERED";

	public static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

	public static final String UNIQUEID = "$uniqueid";
	public static final String MODELVERSION = "$modelversion";
	public static final String PROCESSID = "$processid";
	public static final String ACTIVITYID = "$activityid";
	public static final String ACTIVITYIDLIST = "$activityidlist";
	public static final String TYPE = "type";

	/** Plugin objects **/
	private Vector<Plugin> vectorPlugins = null;
	private WorkflowContext ctx = null;
	private ItemCollection documentContext = null;
	private ItemCollection documentActivity = null;
	private Vector<String> vectorEdgeHistory = new Vector<String>();

	private static Logger logger = Logger.getLogger(WorkflowKernel.class.getName());

	/**
	 * Constructor initialize the contextObject and plugin vectors
	 */
	public WorkflowKernel(WorkflowContext actx) {
		ctx = actx;
		vectorPlugins = new Vector<Plugin>();

	}

	/**
	 * This method registers a new plugin class. The method throws a
	 * PluginException if the class can not be registered.
	 * 
	 * @param pluginClass
	 * @throws PluginException
	 */
	public void registerPlugin(Plugin plugin) throws PluginException {
		plugin.init(ctx);
		vectorPlugins.add(plugin);
	}

	/**
	 * This method registers a new plugin based on class name. The plugin will
	 * be instantiated by its name. The method throws a PluginException if the
	 * plugin class can not be created.
	 * 
	 * @param pluginClass
	 * @throws PluginException
	 */
	public void registerPlugin(String pluginClass) throws PluginException {

		if ((pluginClass != null) && (!"".equals(pluginClass))) {
			if (logger.isLoggable(Level.FINE))
				logger.info("[WorkflowKernel] register plugin class: " + pluginClass + "...");

			Class<?> clazz = null;
			try {
				clazz = Class.forName(pluginClass);
				Plugin plugin = (Plugin) clazz.newInstance();
				registerPlugin(plugin);

			} catch (ClassNotFoundException e) {
				throw new PluginException(WorkflowKernel.class.getSimpleName(), PLUGIN_NOT_CREATEABLE,
						"[WorkflowKernel] Could not register plugin: " + pluginClass + " reason: " + e.toString(), e);
			} catch (InstantiationException e) {
				throw new PluginException(WorkflowKernel.class.getSimpleName(), PLUGIN_NOT_CREATEABLE,
						"[WorkflowKernel] Could not register plugin: " + pluginClass + " reason: " + e.toString(), e);
			} catch (IllegalAccessException e) {
				throw new PluginException(WorkflowKernel.class.getSimpleName(), PLUGIN_NOT_CREATEABLE,
						"[WorkflowKernel] Could not register plugin: " + pluginClass + " reason: " + e.toString(), e);
			}

		}

	}

	/**
	 * This method removes a registered plugin based on its class name.
	 * 
	 * @param pluginClass
	 * @throws PluginException
	 *             if plugin not registered
	 */
	public void unregisterPlugin(String pluginClass) throws PluginException {
		logger.fine("[WorkflowKernel] unregisterPlugin " + pluginClass);
		for (Plugin plugin : vectorPlugins) {
			if (plugin.getClass().getName().equals(pluginClass)) {
				vectorPlugins.remove(plugin);
				return;
			}
		}

		// throw PluginExeption
		throw new PluginException(WorkflowKernel.class.getSimpleName(), PLUGIN_NOT_REGISTERED,
				"[WorkflowKernel] Could not unregister plugin: " + pluginClass + " reason: ");
	}

	/**
	 * This method removes all registered plugins
	 * 
	 * @param pluginClass
	 */
	public void unregisterAllPlugins() {
		logger.fine("[WorkflowKernel] unregisterAllPlugins");
		vectorPlugins = new Vector<Plugin>();
	}

	/**
	 * Processes a workitem. The Workitem have at least provide the properties
	 * PROCESSID and ACTIVITYID
	 * 
	 * @param workitem
	 * @throws PluginException
	 *             ,ModelException
	 */
	public void process(ItemCollection workitem) throws PluginException {

		vectorEdgeHistory = new Vector<String>();
		documentContext = workitem;

		// check document context
		if (workitem == null)
			throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), UNDEFINED_WORKITEM,
					"[WorkflowKernel] processing error: workitem is null");

		// check processID
		if (workitem.getItemValueInteger(PROCESSID) <= 0)
			throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), UNDEFINED_PROCESSID,
					"[WorkflowKernel] processing error: $processid undefined ("
							+ workitem.getItemValueInteger(PROCESSID) + ")");

		// check activityid

		if (workitem.getItemValueInteger(ACTIVITYID) <= 0)
			throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), UNDEFINED_ACTIVITYID,
					"[WorkflowKernel] processing error: $activityid undefined ("
							+ workitem.getItemValueInteger(ACTIVITYID) + ")");

		// Check if $UniqueID is available
		if ("".equals(documentContext.getItemValueString(UNIQUEID))) {
			// generating a new one
			documentContext.replaceItemValue(UNIQUEID, generateUniqueID());
		}

		// Check if $WorkItemID is available
		if ("".equals(documentContext.getItemValueString("$WorkItemID"))) {
			documentContext.replaceItemValue("$WorkItemID", generateUniqueID());
		}

		// now process all activites defined by the model
		while (hasMoreActivities())
			processActivity();

	}

	/**
	 * This method controls the Process-Chain The method returns true if a) a
	 * valid $activityid exists b) the attribute $activityidlist has more valid
	 * ActivityIDs
	 * 
	 **/
	private boolean hasMoreActivities() {
		int integerID;

		// is $activityid provided?
		integerID = documentContext.getItemValueInteger(ACTIVITYID);

		if ((integerID > 0))
			return true;
		else {
			// no - test for property $ActivityIDList
			List<?> vActivityList = documentContext.getItemValue(ACTIVITYIDLIST);

			// remove 0 values if contained!
			while (vActivityList.indexOf(Integer.valueOf(0)) > -1) {
				vActivityList.remove(vActivityList.indexOf(Integer.valueOf(0)));
			}

			// test if an id is found....
			if (vActivityList.size() > 0) {
				// yes - load next ID from activityID List
				int iNextID = 0;
				Object oA = vActivityList.get(0);
				if (oA instanceof Integer)
					iNextID = ((Integer) oA).intValue();
				if (oA instanceof Double)
					iNextID = ((Double) oA).intValue();

				if (iNextID > 0) {
					// load activity
					logger.info("[WorkflowKernel] processing=" + documentContext.getItemValueString(UNIQUEID)
							+ " -> loading next activityID = " + iNextID);
					vActivityList.remove(0);
					// update document context
					documentContext.replaceItemValue(ACTIVITYID, Integer.valueOf(iNextID));
					documentContext.replaceItemValue(ACTIVITYIDLIST, vActivityList);
					return true;
				}
			}
			// no more Activities defined
			return false;
		}

	}

	/**
	 * This method creates a unique key which can be used as a primary key. The
	 * method is used by the 'checkWorkItemID()' method and the checkUniqueID()
	 * method.
	 * <p>
	 * The uniqueID consists of two parts containing a random unique char
	 * sequence
	 * 
	 * @return
	 */
	public static String generateUniqueID() {
		String sIDPart1 = Long.toHexString(System.currentTimeMillis());
		double d = Math.random() * 900000000;
		int i = new Double(d).intValue();
		String sIDPart2 = Integer.toHexString(i);
		String id = sIDPart1 + "-" + sIDPart2;

		return id;
	}

	/**
	 * This method process a activity instance by loading and running all
	 * plugins.
	 * 
	 * If an FollowUp Activity is defined (keyFollowUp="1" &
	 * numNextActivityID>0) it will be attached at the $ActiviyIDList.
	 * 
	 * @throws ProcessingErrorException
	 */
	private void processActivity() throws PluginException {

		// log the general processing message
		String msg = "[WorkflowKernel] processing=" + documentContext.getItemValueString(UNIQUEID) + ", MODELVERSION="
				+ documentContext.getItemValueString(MODELVERSION) + ", $processid="
				+ documentContext.getItemValueInteger(PROCESSID) + ", $activityid="
				+ documentContext.getItemValueInteger(ACTIVITYID);

		if (ctx == null) {
			logger.warning("[WorkflowKernel] no WorkflowContext defined!");
		}
		logger.info(msg);

		// load activity...
		loadEvent();

		// run plugins - PluginExceptions will bubble up....
		int iStatus = runPlugins();
		closePlugins(iStatus);

		if (iStatus == Plugin.PLUGIN_ERROR) {
			throw new PluginException(WorkflowKernel.class.getSimpleName(), UNDEFINED_PLUGIN_ERROR,
					"[WorkflowKernel] Error in Plugin detected.");
		}

		writeLog();

		// put current edge in history
		vectorEdgeHistory.addElement(documentActivity.getItemValueInteger("numprocessid") + "."
				+ documentActivity.getItemValueInteger("numactivityid"));

		/*** get Next Task **/
		int iNewProcessID = documentActivity.getItemValueInteger("numnextprocessid");
		if (logger.isLoggable(Level.FINE))
			logger.info("[WorkflowKernel] next $processid=" + iNewProcessID + "");

		// NextProcessID will only be set if NextTask>0
		if (iNewProcessID > 0) {
			documentContext.replaceItemValue(PROCESSID, Integer.valueOf(iNewProcessID));
		}

		// clear ActivityID and create new workflowActivity Instance
		documentContext.replaceItemValue(ACTIVITYID, Integer.valueOf(0));

		// FollowUp Activity ?
		String sFollowUp = documentActivity.getItemValueString("keyFollowUp");
		int iNextActivityID = documentActivity.getItemValueInteger("numNextActivityID");
		if ("1".equals(sFollowUp) && iNextActivityID > 0) {
			this.appendActivityID(iNextActivityID);
		}

	}

	/**
	 * This method adds a new ActivityID into the current activityList
	 * ($ActivityIDList) The activity list may not contain 0 values.
	 * 
	 */
	@SuppressWarnings("unchecked")
	private void appendActivityID(int aID) {

		// check if activityidlist is available
		List<Integer> vActivityList = (List<Integer>) documentContext.getItemValue(ACTIVITYIDLIST);
		if (vActivityList == null)
			vActivityList = new Vector<Integer>();
		// clear list?
		if ((vActivityList.size() == 1) && ("".equals(vActivityList.get(0).toString())))
			vActivityList = new Vector<Integer>();

		vActivityList.add(Integer.valueOf(aID));

		// remove 0 values if contained!
		while (vActivityList.indexOf(Integer.valueOf(0)) > -1) {
			vActivityList.remove(vActivityList.indexOf(Integer.valueOf(0)));
		}

		documentContext.replaceItemValue(ACTIVITYIDLIST, vActivityList);
		if (logger.isLoggable(Level.FINE))
			logger.info("[WorkflowKernel]  append new Activity ID=" + aID);

	}

	/**
	 * This method is responsible for the internal workflow log. The attribute
	 * txtworkflowactivitylog logs the transition from one process to another.
	 * 
	 * Format:
	 * 
	 * <code>
	    timestamp|model-version|1000.10|1000|userid|
		timestamp|model-version|1000.20|1010|userid|
		timestamp|model-version|1010.10|1010|userid|comment
	 * </code>
	 * 
	 * The userid and comment are optional information generated by Plugins. If
	 * a property 'txtworkflowactivitylogComment' exits the value will be
	 * appended
	 * 
	 */
	private void writeLog() {

		StringBuffer sLogEntry = new StringBuffer();
		// 22.9.2004 13:50:41|modelversion|1000.90|1000|

		sLogEntry.append(new SimpleDateFormat(ISO8601_FORMAT).format(new Date()));

		sLogEntry.append("|");
		sLogEntry.append(documentContext.getItemValueString(MODELVERSION));

		sLogEntry.append("|");
		sLogEntry.append(documentActivity.getItemValueInteger("numprocessid") + "."
				+ documentActivity.getItemValueInteger("numactivityid"));

		sLogEntry.append("|");
		sLogEntry.append(documentActivity.getItemValueInteger("numnextprocessid"));
		sLogEntry.append("|");

		// check for optional log comment
		String sLogComment = documentContext.getItemValueString("txtworkflowactivitylogComment");
		if (!sLogComment.isEmpty())
			sLogEntry.append(sLogComment);

		@SuppressWarnings("unchecked")
		List<String> vLog = (List<String>) documentContext.getItemValue("txtworkflowactivitylog");
		if (vLog == null)
			vLog = new Vector<String>();

		vLog.add(sLogEntry.toString());
		documentContext.replaceItemValue("txtworkflowactivitylog", vLog);
		documentContext.replaceItemValue("numlastactivityid",
				Integer.valueOf(documentActivity.getItemValueInteger("numactivityid")));

	}

	/**
	 * This Method loads the current event from the provided Model
	 * 
	 * The method also verifies the activity to be valid
	 * 
	 * @throws ModelException
	 * @throws ProcessingErrorException
	 */
	private void loadEvent() {

		int aProcessID = documentContext.getItemValueInteger(PROCESSID);
		int aActivityID = documentContext.getItemValueInteger(ACTIVITYID);

		// determine model version
		String version = documentContext.getItemValueString(MODELVERSION);

		try {
			Model model = ctx.getModelManager().getModel(version);
			documentActivity = model.getEvent(aProcessID, aActivityID);
		} catch (ModelException e) {
			throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), ACTIVITY_NOT_FOUND,
					"[WorkflowKernel] model entry " + aProcessID + "." + aActivityID + " not found for model version '"+version +"'");
		}

		if (documentActivity == null)
			throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), ACTIVITY_NOT_FOUND,
					"[WorkflowKernel] model entry " + aProcessID + "." + aActivityID + " not found for model version '"+version +"'");

		if (logger.isLoggable(Level.FINE))
			logger.info("[WorkflowKernel] WorkflowActivity: " + aProcessID + "." + aActivityID + " loaded successful");

		// Check for loop in edge history
		if (vectorEdgeHistory != null) {
			if (vectorEdgeHistory.indexOf((aProcessID + "." + aActivityID)) != -1)
				throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), MODEL_ERROR,
						"[WorkflowKernel] loop detected " + aProcessID + "." + aActivityID + ","
								+ vectorEdgeHistory.toString());
		}

	}

	/**
	 * This method runs all registered plugins until the run method of a plugin
	 * breaks with an error In this case the method stops.
	 * 
	 * @throws PluginException
	 */
	private int runPlugins() throws PluginException {
		int iStatus;
		String sPluginName = null;
		List<String> localPluginLog = new Vector<String>();

		try {
			for (Plugin plugin : vectorPlugins) {

				sPluginName = plugin.getClass().getName();
				if (logger.isLoggable(Level.FINE))
					logger.info("[WorkflowKernel] running Plugin: " + sPluginName + "...");

				iStatus = plugin.run(documentContext, documentActivity);

				// write PluginLog
				String sLog = new SimpleDateFormat(ISO8601_FORMAT).format(new Date());
				sLog = sLog + " " + plugin.getClass().getName() + "=" + iStatus;

				localPluginLog.add(sLog);

				if (iStatus == Plugin.PLUGIN_ERROR) {
					// log error....
					logger.severe("[WorkflowKernel] Error processing Plugin: " + sPluginName);
					logger.severe("[WorkflowKernel] Plugin-Log: ");
					for (String sLogEntry : localPluginLog)
						logger.severe("[WorkflowKernel]   " + sLogEntry);

					return Plugin.PLUGIN_ERROR;
				}
			}
			return Plugin.PLUGIN_OK;

		} catch (PluginException e) {
			// log plugin stack!....
			logger.severe("[WorkflowKernel] Plugin-Stack: ");
			for (String sLogEntry : localPluginLog)
				logger.severe("[WorkflowKernel]   " + sLogEntry);
			// re throw the PluginException !
			throw e;
		}

	}

	private void closePlugins(int astatus) throws PluginException {
		for (int i = 0; i < vectorPlugins.size(); i++) {
			Plugin plugin = (Plugin) vectorPlugins.elementAt(i);
			if (logger.isLoggable(Level.FINE))
				logger.info("[WorkflowKernel] closing Plugin: " + plugin.getClass().getName() + "...");
			plugin.close(astatus);
		}
	}

}
