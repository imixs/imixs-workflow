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
import java.util.Map;
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

	public static final String MISSING_WORKFLOWCONTEXT = "MISSING_WORKFLOWCONTEXT";
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
	public static final String UNIQUEIDSOURCE = "$uniqueidsource";
	public static final String UNIQUEIDVERSIONS = "$uniqueidversions";
	public static final String WORKITEMID = "$workitemid";
	public static final String MODELVERSION = "$modelversion";

	@Deprecated
	public static final String PROCESSID = "$processid";

	public static final String TASKID = "$taskid";
	public static final String EVENTID = "$eventid";

	public static final String ACTIVITYIDLIST = "$activityidlist";
	public static final String WORKFLOWGROUP = "$workflowgroup";
	public static final String WORKFLOWSTATUS = "$workflowstatus";
	public static final String ISVERSION = "$isversion";
	public static final String LASTTASK = "$lasttask";
	public static final String LASTEVENT = "$lastevent";
	public static final String LASTEVENTDATE = "$lasteventdate";
	public static final String CREATOR = "$creator";
	public static final String EDITOR = "$editor";
	public static final String LASTEDITOR = "$lasteditor";
	

	public static final String TYPE = "type";

	public static final int MAXIMUM_ACTIVITYLOGENTRIES = 30;

	/** Plugin objects **/
	private List<Plugin> pluginRegistry = null;
	private WorkflowContext ctx = null;
	private Vector<String> vectorEdgeHistory = new Vector<String>();
	private List<ItemCollection> splitWorkitems = null;
	private RuleEngine ruleEngine = null;

	private static Logger logger = Logger.getLogger(WorkflowKernel.class.getName());

	/**
	 * Constructor initialize the contextObject and plugin vectors
	 */
	public WorkflowKernel(final WorkflowContext actx) {
		// check workflow context
		if (actx == null) {
			throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), MISSING_WORKFLOWCONTEXT,
					"WorkflowKernel can not be initialized: workitemContext is null!");
		}

		ctx = actx;
		pluginRegistry = new ArrayList<Plugin>();
		splitWorkitems = new ArrayList<ItemCollection>();
		ruleEngine = new RuleEngine();
	}

	/**
	 * This method generates an immutable universally unique identifier (UUID). A
	 * UUID represents a 128-bit value.
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
	 * This method registers a new plugin class. The method throws a PluginException
	 * if the class can not be registered.
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
				for (Plugin regiseredPlugin : pluginRegistry) {
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
		pluginRegistry.add(plugin);
	}

	/**
	 * This method registers a new plugin based on class name. The plugin will be
	 * instantiated by its name. The method throws a PluginException if the plugin
	 * class can not be created.
	 * 
	 * @param pluginClass
	 * @throws PluginException
	 */
	public void registerPlugin(final String pluginClass) throws PluginException {

		if ((pluginClass != null) && (!"".equals(pluginClass))) {
			if (logger.isLoggable(Level.FINEST))
				logger.finest("......register plugin class: " + pluginClass + "...");

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
		logger.finest("......unregisterPlugin " + pluginClass);
		for (Plugin plugin : pluginRegistry) {
			if (plugin.getClass().getName().equals(pluginClass)) {
				pluginRegistry.remove(plugin);
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
		logger.finest("......unregisterAllPlugins...");
		pluginRegistry = new ArrayList<Plugin>();
	}

	/**
	 * Returns a registry containing all registered plugin instances.
	 * 
	 * @return
	 */
	public List<Plugin> getPluginRegistry() {
		return pluginRegistry;
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

		// check $TaskID
		if (workitem.getTaskID() <= 0)
			throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), UNDEFINED_PROCESSID,
					"processing error: $taskID undefined (" + workitem.getTaskID() + ")");

		
		// check $eventId
		if (workitem.getEventID() <= 0)
			throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), UNDEFINED_ACTIVITYID,
					"processing error: $eventID undefined (" + workitem.getEventID() + ")");

		
		ItemCollection documentResult = new ItemCollection(workitem);
		vectorEdgeHistory = new Vector<String>();

		
		// Check if $UniqueID is available
		if ("".equals(workitem.getItemValueString(UNIQUEID))) {
			// generating a new one
			documentResult.replaceItemValue(UNIQUEID, generateUniqueID());
		}

		// store last $lastTask
		documentResult.replaceItemValue("$lastTask", workitem.getTaskID());

		// Check if $WorkItemID is available
		if ("".equals(workitem.getItemValueString(WorkflowKernel.WORKITEMID))) {
			documentResult.replaceItemValue(WorkflowKernel.WORKITEMID, generateUniqueID());
		}

		// now process all events defined by the model
		while (documentResult.getEventID() > 0) {
			// set $lastEventDate
			documentResult.replaceItemValue("$lastEventDate", new Date());
			// load event...
			ItemCollection event = loadEvent(documentResult);
			documentResult = processEvent(documentResult, event);
			documentResult = updateEventList(documentResult);
		}


		return documentResult;
	}

	/**
	 * This method computes the next task based on a Model Event element. If the
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
	 * @throws ModelException
	 * @throws PluginException
	 */
	public ItemCollection findNextTask(ItemCollection documentContext, ItemCollection event)
			throws ModelException, PluginException {
	
		ItemCollection itemColNextTask = null;
	
		int iNewProcessID = event.getItemValueInteger("numnextprocessid");
		logger.finest("......next $taskID=" + iNewProcessID + "");
	
		// test if we have an conditional exclusive Task exits...
		itemColNextTask = findConditionalExclusiveTask(event, documentContext);
		if (itemColNextTask != null) {
			return itemColNextTask;
		}
	
		itemColNextTask = findConditionalSplitTask(event, documentContext);
		if (itemColNextTask != null) {
			return itemColNextTask;
		}
	
		// default behavior
		if (iNewProcessID > 0) {
			itemColNextTask = this.ctx.getModelManager().getModel(documentContext.getModelVersion())
					.getTask(iNewProcessID);
		} else {
			// get current task...
			itemColNextTask = this.ctx.getModelManager().getModel(documentContext.getItemValueString(MODELVERSION))
					.getTask(documentContext.getTaskID());
		}
		return itemColNextTask;
	}

	/**
	 * This method returns new SplitWorkitems evaluated during the last processing
	 * life-cycle.
	 * 
	 * @return
	 */
	public List<ItemCollection> getSplitWorkitems() {
		return splitWorkitems;
	}

	/**
	 * This method controls the Event-Chain. If the attribute $activityidlist has
	 * more valid ActivityIDs the next activiytID will be loaded into $activity.
	 * 
	 **/
	private ItemCollection updateEventList(final ItemCollection documentContext) {
		ItemCollection documentResult = documentContext;

		// is $eventid already provided?
		if ((documentContext.getEventID() <= 0)) {
			// no $eventID provided, so we test for property $ActivityIDList
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
					logger.finest("......processing=" + documentContext.getItemValueString(UNIQUEID)
							+ " -> loading next activityID = " + iNextID);
					vActivityList.remove(0);
					// update document context
					documentResult.setEventID(Integer.valueOf(iNextID));
					documentResult.replaceItemValue(ACTIVITYIDLIST, vActivityList);
				}
			}
		}
		return documentResult;
	}

	/**
	 * This method process an event by running all registered plug-ins.
	 * 
	 * Before a plug-in is called the method updates the 'type' attribute defined by
	 * the next Task.
	 * 
	 * After all plug-ins are processed, the attributes $taskID, $workflowstatus
	 * and $workflowgroup are updated.
	 * 
	 * If an FollowUp Activity is defined (keyFollowUp="1" & numNextActivityID>0)
	 * the next event will be attached to the $ActiviyIDList.
	 * 
	 * @throws PluginException,ModelException
	 */
	private ItemCollection processEvent(final ItemCollection documentContext, final ItemCollection event)
			throws PluginException, ModelException {
		ItemCollection documentResult = documentContext;
		// log the general processing message
		String msg = "processing=" + documentContext.getItemValueString(UNIQUEID) + ", MODELVERSION="
				+ documentContext.getItemValueString(MODELVERSION) + ", $taskID="
				+ documentContext.getTaskID() + ", $eventID="
				+ documentContext.getEventID();

		if (ctx == null) {
			logger.warning("no WorkflowContext defined!");
		}
		logger.info(msg);

		// compute next task..
		ItemCollection itemColNextTask = findNextTask(documentContext, event);

		// update the type attribute if defined.
		// the type attribute can be overwritten by a plug-in
		String sType = itemColNextTask.getItemValueString("txttype");
		if (sType != null && !"".equals(sType)) {
			documentResult.replaceItemValue(TYPE, sType);
		}

		// update the $workflowGroup
		documentResult.replaceItemValue(WORKFLOWGROUP, itemColNextTask.getItemValueString("txtworkflowgroup"));
		logger.finest("......new $workflowGroup=" + documentResult.getItemValueString(WORKFLOWGROUP));

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
		// write event log
		documentResult = logEvent(documentResult, event);

		// put current edge in history
		vectorEdgeHistory.addElement(
				event.getItemValueInteger("numprocessid") + "." + event.getItemValueInteger("numactivityid"));

		// evaluate a split-event and create new versions of the current process
		// instance.
		evaluateSplitEvent(event, documentResult);

		// Update the attributes $taskID and $WorkflowStatus
		documentResult.setTaskID(Integer.valueOf(itemColNextTask.getItemValueInteger("numprocessid")));
		logger.finest("......new $taskID=" + documentResult.getTaskID());
		documentResult.replaceItemValue(WORKFLOWSTATUS, itemColNextTask.getItemValueString("txtname"));
		logger.finest("......new $workflowStatus=" + documentResult.getItemValueString(WORKFLOWSTATUS));
		// update deprecated attributes txtworkflowStatus and txtworkflowGroup
		documentResult.replaceItemValue("txtworkflowStatus", documentResult.getItemValueString(WORKFLOWSTATUS));
		documentResult.replaceItemValue("txtworkflowGroup", documentResult.getItemValueString(WORKFLOWGROUP));

		// clear ActivityID
		documentResult.setEventID(Integer.valueOf(0));

		// test if a FollowUp event is defined...
		String sFollowUp = event.getItemValueString("keyFollowUp");
		int iNextActivityID = event.getItemValueInteger("numNextActivityID");
		if ("1".equals(sFollowUp) && iNextActivityID > 0) {
			documentResult = appendActivityID(documentResult, iNextActivityID);
		}

		return documentResult;
	}

	/**
	 * This method returns the first conditional Task or Event of a given Event
	 * object. The method evaluates conditional expressions to 'true'. If no
	 * conditional expression exists or no expression evaluates to true the the
	 * method returns null
	 * 
	 * @param conditions
	 * @param documentContext
	 * @return conditional Task or Event object or null if no condition exits.
	 * @throws PluginException
	 * @throws ModelException
	 */
	@SuppressWarnings("unchecked")
	private ItemCollection findConditionalExclusiveTask(ItemCollection event, ItemCollection documentContext)
			throws PluginException, ModelException {

		Map<String, String> conditions = null;
		// test if we have an exclusive condition
		if (event.hasItem("keyExclusiveConditions")) {
			// get first element
			conditions = (Map<String, String>) event.getItemValue("keyExclusiveConditions").get(0);

			if (conditions != null && conditions.size() > 0) {

				// evaluate all conditions and return the fist match...
				for (Map.Entry<String, String> entry : conditions.entrySet()) {
					String key = entry.getKey();
					String expression = entry.getValue();
					if (key.startsWith("task=")) {
						int taskID = Integer.parseInt(key.substring(5));
						boolean bmatch = ruleEngine.evaluateBooleanExpression(expression, documentContext);
						if (bmatch) {
							logger.finest("......matching conditional event: " + expression);
							ItemCollection conditionslTask = this.ctx.getModelManager()
									.getModel(documentContext.getModelVersion()).getTask(taskID);
							if (conditionslTask != null) {
								return conditionslTask;
							}
						}
					}

					if (key.startsWith("event=")) {
						int eventID = Integer.parseInt(key.substring(6));
						boolean bmatch = ruleEngine.evaluateBooleanExpression(expression, documentContext);
						if (bmatch) {
							logger.finest("......matching conditional event: " + expression);
							// we update the documentContext....
							ItemCollection itemColEvent = this.ctx.getModelManager()
									.getModel(documentContext.getModelVersion())
									.getEvent(documentContext.getTaskID(), eventID);
							if (itemColEvent != null) {
								// create follow up event....

								event.replaceItemValue("keyFollowUp", "1");
								event.replaceItemValue("numNextActivityID", eventID);

								// get current task...
								ItemCollection itemColNextTask = this.ctx.getModelManager()
										.getModel(documentContext.getItemValueString(MODELVERSION))
										.getTask(documentContext.getTaskID());

								return itemColNextTask;
							}
						}
					}
				}
				logger.finest("......conditional event: no matching condition found.");
			}
		}
		return null;
	}

	/**
	 * This method returns the first conditional Split Task or Event of a given
	 * Event object. The method evaluates conditional expressions to 'true'. If no
	 * conditional expression exists or no expression evaluates to true the the
	 * method returns null
	 * 
	 * @param conditions
	 * @param documentContext
	 * @return conditional Task or Event object or null if no condition exits.
	 * @throws PluginException
	 * @throws ModelException
	 */
	@SuppressWarnings("unchecked")
	private ItemCollection findConditionalSplitTask(ItemCollection event, ItemCollection documentContext)
			throws PluginException, ModelException {

		// test if we have an split event
		Map<String, String> conditions = null;
		// test if we have an split event
		if (event.hasItem("keySplitConditions")) {
			// get first element
			conditions = (Map<String, String>) event.getItemValue("keySplitConditions").get(0);

			if (conditions != null) {

				// evaluate all conditions and return the fist match evaluating to true (this is
				// the flow for the master version)...
				for (Map.Entry<String, String> entry : conditions.entrySet()) {
					String key = entry.getKey();
					String expression = entry.getValue();
					if (key.startsWith("task=")) {
						int taskID = Integer.parseInt(key.substring(5));
						boolean bmatch = ruleEngine.evaluateBooleanExpression(expression, documentContext);
						if (bmatch) {
							logger.finest("......matching split Task found: " + expression);
							ItemCollection itemColNextTask = this.ctx.getModelManager()
									.getModel(documentContext.getModelVersion()).getTask(taskID);
							if (itemColNextTask != null) {
								// Conditional Target Task evaluated to 'true' was found!
								return itemColNextTask;
							}
						}
					}

					if (key.startsWith("event=")) {
						int eventID = Integer.parseInt(key.substring(6));
						boolean bmatch = ruleEngine.evaluateBooleanExpression(expression, documentContext);
						if (bmatch) {
							logger.finest("......matching split Event found: " + expression);
							// we update the documentContext....
							ItemCollection itemColEvent = this.ctx.getModelManager()
									.getModel(documentContext.getModelVersion())
									.getEvent(documentContext.getTaskID(), eventID);
							if (itemColEvent != null) {
								// create follow up event....
								event.replaceItemValue("keyFollowUp", "1");
								event.replaceItemValue("numNextActivityID", eventID);
								// get current task...
								ItemCollection itemColNextTask = this.ctx.getModelManager()
										.getModel(documentContext.getItemValueString(MODELVERSION))
										.getTask(documentContext.getTaskID());
								return itemColNextTask;
							}
						}
					}
				}
				// we found not condition evaluated to 'true', so the workitem will not leave
				// the current task.
				logger.finest("......split event: no matching condition, current Task will not change.");
			}
		}

		return null;
	}

	/**
	 * This method evaluates conditional split expressions to 'false'. For each
	 * condition a new process instance will be created and processed by the
	 * expected follow-up event. The expression evaluated to 'false' MUST be
	 * followed by an Event. If not, a ModelExcption is thrown to indicate that the
	 * new version can not be processed correctly!
	 * 
	 * @param conditions
	 * @param documentContext
	 * @return conditional Task or Event object or null if no condition exits.
	 * @throws PluginException
	 * @throws ModelException
	 */
	@SuppressWarnings("unchecked")
	private void evaluateSplitEvent(ItemCollection event, ItemCollection documentContext)
			throws PluginException, ModelException {

		// test if we have an split event
		Map<String, String> conditions = null;
		// test if we have an split event
		if (event.hasItem("keySplitConditions")) {
			// get first element
			conditions = (Map<String, String>) event.getItemValue("keySplitConditions").get(0);

			if (conditions != null) {

				// evaluate all conditions and return the fist match evaluating to true (this is
				// the flow for the master version)...
				for (Map.Entry<String, String> entry : conditions.entrySet()) {
					String key = entry.getKey();
					String expression = entry.getValue();
					if (key.startsWith("task=")) {
						// if a task evaluated to false, the model is invalid.
						boolean bmatch = ruleEngine.evaluateBooleanExpression(expression, documentContext);
						if (!bmatch) {
							String sErrorMessage = "Outcome of Split-Event " + event.getItemValueInteger("numProcessid")
									+ "." + +event.getItemValueInteger("numActivityid") + " (" + event.getModelVersion()
									+ ") evaluate to false must not be connected to a task. ";

							logger.warning(sErrorMessage + " Condition = " + expression);
							throw new ModelException(ModelException.INVALID_MODEL, sErrorMessage);
						}
					}

					if (key.startsWith("event=")) {
						int eventID = Integer.parseInt(key.substring(6));
						boolean bmatch = ruleEngine.evaluateBooleanExpression(expression, documentContext);
						if (!bmatch) {
							logger.finest("......matching conditional event: " + expression);
							// we update the documentContext....
							ItemCollection itemColEvent = this.ctx.getModelManager()
									.getModel(documentContext.getModelVersion())
									.getEvent(documentContext.getTaskID(), eventID);
							if (itemColEvent != null) {
								// get current task...
								ItemCollection itemColNextTask = this.ctx.getModelManager()
										.getModel(documentContext.getItemValueString(MODELVERSION))
										.getTask(documentContext.getTaskID());

								// clone current instance to a new version...
								ItemCollection cloned = createVersion(documentContext);
								logger.finest("......created new version=" + cloned.getUniqueID());
								// set new $taskID
								cloned.setTaskID(Integer.valueOf(itemColNextTask.getItemValueInteger("numprocessid")));
								cloned.setEventID(eventID);
								// add temporary attribute $isversion...
								cloned.replaceItemValue(ISVERSION, true);
								cloned = this.process(cloned);
								// remove temporary attribute $isversion...
								cloned.removeItem(ISVERSION);
								// add to cache...
								splitWorkitems.add(cloned);

							}
						}
					}
				}
				logger.finest("......split event: no matching condition");
			}
		}

	}

	/**
	 * This method creates a new instance of a sourceWorkitem. The method did not
	 * save the workitem!.
	 * <p>
	 * The new property $UniqueIDSource will be added to the new version, which
	 * points to the $uniqueID of the sourceWorkitem.
	 * <p>
	 * The new property $UniqueIDVersions will be added to the sourceWorktiem which
	 * points to the id of the new version.
	 * 
	 * @param sourceItemCollection
	 *            the ItemCollection which should be versioned
	 * @return new version of the source ItemCollection
	 * 
	 * @throws PluginException
	 * @throws Exception
	 */
	private ItemCollection createVersion(ItemCollection sourceItemCollection) throws PluginException {
		
		// clone the source workitem with its '$workitemid'
		ItemCollection itemColNewVersion = (ItemCollection) sourceItemCollection.clone();
		String id = sourceItemCollection.getUniqueID();

		// create a new $Uniqueid to force the generation of a new Entity Instance.
		itemColNewVersion.replaceItemValue(UNIQUEID, WorkflowKernel.generateUniqueID());

		// update $unqiueIDSource 
		itemColNewVersion.replaceItemValue(UNIQUEIDSOURCE, id);

		// remove $UniqueIDVersions
		itemColNewVersion.removeItem(UNIQUEIDVERSIONS);

		// append the version uniqueid to the source ItemCollection
		sourceItemCollection.appendItemValue(UNIQUEIDVERSIONS, itemColNewVersion.getUniqueID());

		return itemColNewVersion;

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
		logger.finest("......append new Activity ID=" + aID);

		return documentResult;
	}

	/**
	 * This method is responsible for the internal workflow log. The attribute
	 * $eventlog logs the transition from one process to another.
	 * 
	 * Format:
	 * 
	 * <code>
	    timestamp|model-version|1000.10|1000|
		timestamp|model-version|1000.20|1010|
		timestamp|model-version|1010.10|1010|comment
	 * </code>
	 * 
	 * The comment is an optional information generated by Plugins. If a
	 * property 'txtworkflowactivitylogComment' exits the value will be appended.
	 * 
	 * The method restrict the maximum count of entries to avoid a overflow. (issue
	 * #179)
	 * 
	 */
	@SuppressWarnings("unchecked")
	private ItemCollection logEvent(final ItemCollection documentContext, final ItemCollection event) {

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

		// support deprecated field txtworkflowactivitylog
		List<String> logEntries=null;
		if (!documentContext.hasItem("$eventlog"))
			logEntries = (List<String>) documentContext.getItemValue("txtworkflowactivitylog"); // deprecated
		else 
			logEntries = (List<String>) documentContext.getItemValue("$eventlog");
		logEntries.add(sLogEntry.toString());

		// test if the log has exceeded the maximum count of entries
		while (logEntries.size() > MAXIMUM_ACTIVITYLOGENTRIES) {
			logger.finest(
					"......maximum activity log entries=" + MAXIMUM_ACTIVITYLOGENTRIES + " exceeded, remove first entry...");
			logEntries.remove(0);
		}

		
		documentResult.replaceItemValue("$eventlog", logEntries);

		documentResult.replaceItemValue("$lastEvent", Integer.valueOf(event.getItemValueInteger("numactivityid")));
		// deprecated
		documentResult.replaceItemValue("numlastactivityid",Integer.valueOf(event.getItemValueInteger("numactivityid")));

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
		int aActivityID = documentContext.getEventID();

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

		logger.finest(".......event: " + aProcessID + "." + aActivityID + " loaded");

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
			for (Plugin plugin : pluginRegistry) {

				sPluginName = plugin.getClass().getName();
				logger.finest("......running Plugin: " + sPluginName + "...");

				long lPluginTime=System.currentTimeMillis();
				documentResult = plugin.run(documentResult, event);
				logger.fine("...Plugin '" + sPluginName + "' processing time=" + (System.currentTimeMillis() - lPluginTime) + "ms");
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
			logger.severe(
					"Plugin-Error at " + e.getErrorContext() + ": " + e.getErrorCode() + " (" + e.getMessage() + ")");
			if (logger.isLoggable(Level.FINE)) {
				logger.severe("Last Plugins run successfull:");
				for (String sLogEntry : localPluginLog)
					logger.severe("   ..." + sLogEntry);
			}
			throw e;
		}

	}

	private void closePlugins(boolean rollbackTransaction) throws PluginException {
		for (int i = 0; i < pluginRegistry.size(); i++) {
			Plugin plugin = (Plugin) pluginRegistry.get(i);
			if (logger.isLoggable(Level.FINEST))
				logger.finest("closing Plugin: " + plugin.getClass().getName() + "...");
			plugin.close(rollbackTransaction);
		}
	}

}
