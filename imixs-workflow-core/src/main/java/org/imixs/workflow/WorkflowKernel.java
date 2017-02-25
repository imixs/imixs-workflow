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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
	public static final String PLUGIN_ERROR = "PLUGIN_ERROR";

	public static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

	public static final String UNIQUEID = "$uniqueid";
	public static final String MODELVERSION = "$modelversion";
	public static final String PROCESSID = "$processid";
	public static final String ACTIVITYID = "$activityid";
	public static final String ACTIVITYIDLIST = "$activityidlist";
	public static final String WORKFLOWGROUP = "$workflowgroup";
	public static final String WORKFLOWSTATUS = "$workflowstatus";
	
	public static final String TYPE = "type";

	public static final int MAXIMUM_ACTIVITYLOGENTRIES = 30;

	/** Plugin objects **/
	private List<Plugin> pluginList = null;
	private WorkflowContext ctx = null;
	private Vector<String> vectorEdgeHistory = new Vector<String>();

	private static Logger logger = Logger.getLogger(WorkflowKernel.class.getName());

	/**
	 * Constructor initialize the contextObject and plugin vectors
	 */
	public WorkflowKernel(final WorkflowContext actx) {
		ctx = actx;
		pluginList = new ArrayList<Plugin>();

	}

	/**
	 * This method generates an immutable universally unique identifier (UUID).
	 * A UUID represents a 128-bit value.
	 * 
	 * @see https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html
	 * 
	 * @return
	 */
	public static String generateUniqueID() {
		String id = UUID.randomUUID().toString();
		return id;
	}

	/**
	 * This method registers a new plugin class. The method throws a
	 * PluginException if the class can not be registered.
	 * 
	 * If the new Plugin implements the PluginDependency interface, the method
	 * validates dependencies.
	 * 
	 * @param pluginClass
	 * @throws PluginException
	 */
	public void registerPlugin(final Plugin plugin) throws PluginException {
		// validate dependencies
		if (plugin instanceof PluginDependency) {
			List<String> dependencies = ((PluginDependency) plugin).dependsOn();
			for (String dependency : dependencies) {
				boolean found = false;
				for (Plugin regiseredPlugin : pluginList) {
					if (regiseredPlugin.getClass().getName().equals(dependency)) {
						found = true;
						break;
					}
				}
				if (!found) {
					logger.warning("Plugin '" + plugin.getClass().getName() + "' depends on unregistered Plugin class '"
							+ dependency + "'");
				}
			}
		}
		plugin.init(ctx);
		pluginList.add(plugin);
	}

	/**
	 * This method registers a new plugin based on class name. The plugin will
	 * be instantiated by its name. The method throws a PluginException if the
	 * plugin class can not be created.
	 * 
	 * @param pluginClass
	 * @throws PluginException
	 */
	public void registerPlugin(final String pluginClass) throws PluginException {

		if ((pluginClass != null) && (!"".equals(pluginClass))) {
			if (logger.isLoggable(Level.FINE))
				logger.info("register plugin class: " + pluginClass + "...");

			Class<?> clazz = null;
			try {
				clazz = Class.forName(pluginClass);
				Plugin plugin = (Plugin) clazz.newInstance();
				registerPlugin(plugin);

			} catch (ClassNotFoundException e) {
				throw new PluginException(WorkflowKernel.class.getSimpleName(), PLUGIN_NOT_CREATEABLE,
						"unable to register plugin: " + pluginClass + " - reason: " + e.toString(), e);
			} catch (InstantiationException e) {
				throw new PluginException(WorkflowKernel.class.getSimpleName(), PLUGIN_NOT_CREATEABLE,
						"unable to register plugin: " + pluginClass + " - reason: " + e.toString(), e);
			} catch (IllegalAccessException e) {
				throw new PluginException(WorkflowKernel.class.getSimpleName(), PLUGIN_NOT_CREATEABLE,
						"unable to register plugin: " + pluginClass + " - reason: " + e.toString(), e);
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
	public void unregisterPlugin(final String pluginClass) throws PluginException {
		logger.fine("unregisterPlugin " + pluginClass);
		for (Plugin plugin : pluginList) {
			if (plugin.getClass().getName().equals(pluginClass)) {
				pluginList.remove(plugin);
				return;
			}
		}

		// throw PluginExeption
		throw new PluginException(WorkflowKernel.class.getSimpleName(), PLUGIN_NOT_REGISTERED,
				"unable to unregister plugin: " + pluginClass + " - reason: ");
	}

	/**
	 * This method removes all registered plugins
	 * 
	 * @param pluginClass
	 */
	public void unregisterAllPlugins() {
		logger.fine("unregisterAllPlugins");
		pluginList = new ArrayList<Plugin>();
	}

	/**
	 * Processes a workitem. The Workitem have at least provide the properties
	 * PROCESSID and ACTIVITYID
	 * 
	 * @param workitem
	 *            to be processed.
	 * @return updated workitem
	 * @throws PluginException,ModelException
	 */
	public ItemCollection process(final ItemCollection workitem) throws PluginException, ModelException {

		// check document context
		if (workitem == null)
			throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), UNDEFINED_WORKITEM,
					"processing error: workitem is null");

		ItemCollection documentResult = new ItemCollection(workitem);
		vectorEdgeHistory = new Vector<String>();

		// check $processID
		if (workitem.getItemValueInteger(PROCESSID) <= 0)
			throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), UNDEFINED_PROCESSID,
					"processing error: $processid undefined (" + workitem.getItemValueInteger(PROCESSID) + ")");

		// check $activityid
		if (workitem.getItemValueInteger(ACTIVITYID) <= 0)
			throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), UNDEFINED_ACTIVITYID,
					"processing error: $activityid undefined (" + workitem.getItemValueInteger(ACTIVITYID) + ")");

		// Check if $UniqueID is available
		if ("".equals(workitem.getItemValueString(UNIQUEID))) {
			// generating a new one
			documentResult.replaceItemValue(UNIQUEID, generateUniqueID());
		}

		// store last $lastTask
		documentResult.replaceItemValue("$lastTask", workitem.getProcessID());

		// Check if $WorkItemID is available
		if ("".equals(workitem.getItemValueString("$WorkItemID"))) {
			documentResult.replaceItemValue("$WorkItemID", generateUniqueID());
		}

		// now process all events defined by the model
		while (documentResult.getItemValueInteger(ACTIVITYID) > 0) {
			// load event...
			ItemCollection event = loadEvent(documentResult);
			documentResult = processActivity(documentResult, event);
			documentResult = updateActivityList(documentResult);
		}

		// set $lastProcessingDate
		documentResult.replaceItemValue("$lastProcessingDate", new Date());

		return documentResult;
	}

	/**
	 * This method controls the Evnet-Chain. If the attribute $activityidlist
	 * has more valid ActivityIDs the next activiytID will be loaded into
	 * $activity.
	 * 
	 **/
	private ItemCollection updateActivityList(final ItemCollection documentContext) {
		ItemCollection documentResult = documentContext;

		// is $activityid already provided?
		if ((documentContext.getItemValueInteger(ACTIVITYID) <= 0)) {
			// no $activityID provided, so we test for property $ActivityIDList
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
					logger.info("processing=" + documentContext.getItemValueString(UNIQUEID)
							+ " -> loading next activityID = " + iNextID);
					vActivityList.remove(0);
					// update document context
					documentResult.replaceItemValue(ACTIVITYID, Integer.valueOf(iNextID));
					documentResult.replaceItemValue(ACTIVITYIDLIST, vActivityList);
				}
			}
		}
		return documentResult;
	}

	/**
	 * This method process an event by running all registered plug-ins.
	 * 
	 * Before a plug-in is called the method updates the 'type' attribute
	 * defined by the next Task.
	 * 
	 * After all plug-ins are processed, the attributes $processid,
	 * $workflowstatus and $workflowgroup are updated.
	 * 
	 * If an FollowUp Activity is defined (keyFollowUp="1" &
	 * numNextActivityID>0) the next event will be attached to the
	 * $ActiviyIDList.
	 * 
	 * @throws PluginException,ModelException
	 */
	private ItemCollection processActivity(final ItemCollection documentContext, final ItemCollection event)
			throws PluginException, ModelException {
		ItemCollection documentResult = documentContext;
		// log the general processing message
		String msg = "processing=" + documentContext.getItemValueString(UNIQUEID) + ", MODELVERSION="
				+ documentContext.getItemValueString(MODELVERSION) + ", $processid="
				+ documentContext.getItemValueInteger(PROCESSID) + ", $activityid="
				+ documentContext.getItemValueInteger(ACTIVITYID);

		if (ctx == null) {
			logger.warning("no WorkflowContext defined!");
		}
		logger.info(msg);

		// compute next task..
		ItemCollection itemColNextTask = getNextTask(documentContext, event);

		// update the type attribute if defined.
		// the type attribute can be overwritten by a plug-in
		String sType = itemColNextTask.getItemValueString("txttype");
		if (sType != null && !"".equals(sType)) {
			documentResult.replaceItemValue(TYPE, sType);
		}
		
		// update the $workflowGroup
		documentResult.replaceItemValue(WORKFLOWGROUP, itemColNextTask.getItemValueString("txtworkflowgroup"));
		logger.fine("new $workflowGroup=" + documentResult.getItemValueString(WORKFLOWGROUP));


		// execute plugins - PluginExceptions will bubble up....
		try {
			documentResult = runPlugins(documentResult, event);
		} catch (PluginException pe) {
			// close plugins
			closePlugins(true);
			// throw exeption
			throw pe;
		}
		// Successful close plugins
		closePlugins(false);
		// write execution log
		documentResult = writeLog(documentResult, event);

		// put current edge in history
		vectorEdgeHistory.addElement(
				event.getItemValueInteger("numprocessid") + "." + event.getItemValueInteger("numactivityid"));

		// Update the attributes $ProcessID and $WorkflowStatus
		documentResult.replaceItemValue(PROCESSID,
				Integer.valueOf(itemColNextTask.getItemValueInteger("numprocessid")));
		logger.fine("new $processid=" + documentResult.getProcessID());
		documentResult.replaceItemValue(WORKFLOWSTATUS, itemColNextTask.getItemValueString("txtname"));
		logger.fine("new $workflowStatus=" + documentResult.getItemValueString(WORKFLOWSTATUS));
		// update deprecated attributes txtworkflowStatus and txtworkflowGroup
		documentResult.replaceItemValue("txtworkflowStatus", documentResult.getItemValueString(WORKFLOWSTATUS));
		documentResult.replaceItemValue("txtworkflowGroup", documentResult.getItemValueString(WORKFLOWGROUP));

		// clear ActivityID
		documentResult.replaceItemValue(ACTIVITYID, Integer.valueOf(0));

		// test if a FollowUp event is defined...
		String sFollowUp = event.getItemValueString("keyFollowUp");
		int iNextActivityID = event.getItemValueInteger("numNextActivityID");
		if ("1".equals(sFollowUp) && iNextActivityID > 0) {
			documentResult = appendActivityID(documentResult, iNextActivityID);
		}

		return documentResult;
	}

	/**
	 * This method computes the next task based on a Model Event element. If the
	 * event did not point to a new task, the current task will be returned.
	 * 
	 * @return Task entity
	 * @throws ModelException
	 */
	private ItemCollection getNextTask(ItemCollection documentContext, ItemCollection event) throws ModelException {
		int iNewProcessID = event.getItemValueInteger("numnextprocessid");
		logger.fine("next $processid=" + iNewProcessID + "");

		// NextProcessID will only be set if NextTask>0
		ItemCollection itemColNextTask = null;
		if (iNewProcessID > 0) {
			itemColNextTask = this.ctx.getModelManager().getModel(documentContext.getModelVersion())
					.getTask(iNewProcessID);
		} else {
			// get current task...
			itemColNextTask = this.ctx.getModelManager().getModel(documentContext.getItemValueString(MODELVERSION))
					.getTask(documentContext.getProcessID());
		}
		return itemColNextTask;
	}

	/**
	 * This method adds a new ActivityID into the current activityList
	 * ($ActivityIDList) The activity list may not contain 0 values.
	 * 
	 */
	@SuppressWarnings("unchecked")
	private ItemCollection appendActivityID(final ItemCollection documentContext, final int aID) {

		ItemCollection documentResult = documentContext;
		// check if activityidlist is available
		List<Integer> vActivityList = (List<Integer>) documentContext.getItemValue(ACTIVITYIDLIST);
		// clear list?
		if ((vActivityList.size() == 1) && ("".equals(vActivityList.get(0).toString())))
			vActivityList = new Vector<Integer>();

		vActivityList.add(Integer.valueOf(aID));

		// remove 0 values if contained!
		while (vActivityList.indexOf(Integer.valueOf(0)) > -1) {
			vActivityList.remove(vActivityList.indexOf(Integer.valueOf(0)));
		}

		documentResult.replaceItemValue(ACTIVITYIDLIST, vActivityList);
		if (logger.isLoggable(Level.FINE))
			logger.info("  append new Activity ID=" + aID);

		return documentResult;
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
	 * appended.
	 * 
	 * The method restrict the maximum count of entries to avoid a overflow.
	 * (issue #179)
	 * 
	 */
	private ItemCollection writeLog(final ItemCollection documentContext, final ItemCollection event) {

		ItemCollection documentResult = documentContext;
		StringBuffer sLogEntry = new StringBuffer();
		// 22.9.2004 13:50:41|modelversion|1000.90|1000|

		sLogEntry.append(new SimpleDateFormat(ISO8601_FORMAT).format(new Date()));

		sLogEntry.append("|");
		sLogEntry.append(documentContext.getItemValueString(MODELVERSION));

		sLogEntry.append("|");
		sLogEntry.append(event.getItemValueInteger("numprocessid") + "." + event.getItemValueInteger("numactivityid"));

		sLogEntry.append("|");
		sLogEntry.append(event.getItemValueInteger("numnextprocessid"));
		sLogEntry.append("|");

		// check for optional log comment
		String sLogComment = documentContext.getItemValueString("txtworkflowactivitylogComment");
		if (!sLogComment.isEmpty())
			sLogEntry.append(sLogComment);

		@SuppressWarnings("unchecked")
		List<String> logEntries = (List<String>) documentContext.getItemValue("txtworkflowactivitylog");
		logEntries.add(sLogEntry.toString());

		// test if the log has exceeded the maximum count of entries
		while (logEntries.size() > MAXIMUM_ACTIVITYLOGENTRIES) {
			logger.fine(
					"Maximum activity log entries=" + MAXIMUM_ACTIVITYLOGENTRIES + " exceeded, remove first entry...");
			logEntries.remove(0);
		}

		documentResult.replaceItemValue("txtworkflowactivitylog", logEntries);

		documentResult.replaceItemValue("$lastEvent", Integer.valueOf(event.getItemValueInteger("numactivityid")));

		// deprecated
		documentResult.replaceItemValue("numlastactivityid",
				Integer.valueOf(event.getItemValueInteger("numactivityid")));

		return documentResult;
	}

	/**
	 * This Method loads the current event from the provided Model
	 * 
	 * The method also verifies the activity to be valid
	 * 
	 * @return workflow event object.
	 */
	private ItemCollection loadEvent(final ItemCollection documentContext) {
		ItemCollection event = null;
		int aProcessID = documentContext.getItemValueInteger(PROCESSID);
		int aActivityID = documentContext.getItemValueInteger(ACTIVITYID);

		// determine model version
		String version = documentContext.getItemValueString(MODELVERSION);

		try {
			Model model = ctx.getModelManager().getModelByWorkitem(documentContext);
			event = model.getEvent(aProcessID, aActivityID);
		} catch (ModelException e) {
			throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), MODEL_ERROR, e.getMessage());
		}

		if (event == null)
			throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), ACTIVITY_NOT_FOUND,
					"[loadEvent] model entry " + aProcessID + "." + aActivityID + " not found for model version '"
							+ version + "'");

		if (logger.isLoggable(Level.FINE))
			logger.info("[loadEvent] WorkflowActivity: " + aProcessID + "." + aActivityID + " loaded successful");

		// Check for loop in edge history
		if (vectorEdgeHistory != null) {
			if (vectorEdgeHistory.indexOf((aProcessID + "." + aActivityID)) != -1)
				throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), MODEL_ERROR,
						"[loadEvent] loop detected " + aProcessID + "." + aActivityID + ","
								+ vectorEdgeHistory.toString());
		}

		return event;

	}

	/**
	 * This method runs all registered plugins until the run method of a plugin
	 * breaks with an error In this case the method stops.
	 * 
	 * @throws PluginException
	 */
	private ItemCollection runPlugins(final ItemCollection documentContext, final ItemCollection event)
			throws PluginException {
		ItemCollection documentResult = documentContext;
		String sPluginName = null;
		List<String> localPluginLog = new Vector<String>();

		try {
			for (Plugin plugin : pluginList) {

				sPluginName = plugin.getClass().getName();
				if (logger.isLoggable(Level.FINE))
					logger.info("running Plugin: " + sPluginName + "...");

				documentResult = plugin.run(documentResult, event);
				if (documentResult == null) {
					logger.severe("[runPlugins] PLUGIN_ERROR: " + sPluginName);
					for (String sLogEntry : localPluginLog)
						logger.severe("[runPlugins]   " + sLogEntry);

					throw new PluginException(WorkflowKernel.class.getSimpleName(), PLUGIN_ERROR,
							"plugin: " + sPluginName + " returned null");
				}
				// write PluginLog
				String sLog = new SimpleDateFormat(ISO8601_FORMAT).format(new Date());
				sLog = sLog + " " + plugin.getClass().getName();

				localPluginLog.add(sLog);

			}
			return documentResult;

		} catch (PluginException e) {
			// log plugin stack!....
			logger.severe("Plugin-Error in Plugin - " + e.getErrorContext());
			logger.severe("Last Plugins run successfull:");
			for (String sLogEntry : localPluginLog)
				logger.severe("   ..." + sLogEntry);
			throw e;
		}

	}

	private void closePlugins(boolean rollbackTransaction) throws PluginException {
		for (int i = 0; i < pluginList.size(); i++) {
			Plugin plugin = (Plugin) pluginList.get(i);
			if (logger.isLoggable(Level.FINE))
				logger.info("closing Plugin: " + plugin.getClass().getName() + "...");
			plugin.close(rollbackTransaction);
		}
	}

}
