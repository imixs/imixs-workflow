/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.bpmn.BPMNEntityBuilder;
import org.imixs.workflow.bpmn.BPMNLinkedFlowIterator;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.util.XMLParser;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;

/**
 * The WorkflowKernel is the core component to process a workitem based on its
 * associated BPMN Task ($taskId) and Event ($eventId) elements.
 * <p>
 * The WorkflowKernel operates on a Open BPMN model instance to navigate through
 * a BPMN 2.0 model. The model instance is identified by the attribute
 * $modelversion. The WorkflowKernel expects a {@link WorkflowContext} to access
 * the {@link ModelManager} and the runtime environment.
 * <p>
 * An implementation of the {@link WorkflowManager} typical creates an instance
 * of a WorkflowKernel and register {@link Plugin} and {@link Adapter} classes
 * to be executed during the processing life cycle of one or many workItems.
 * 
 * @author Ralph Soika
 * @version 2.0
 * @see org.imixs.workflow.WorkflowContext
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

    public static final String ADAPTER_ERROR_CONTEXT = "adapter.error_context";
    public static final String ADAPTER_ERROR_CODE = "adapter.error_code";
    public static final String ADAPTER_ERROR_PARAMS = "adapter.error_params";
    public static final String ADAPTER_ERROR_MESSAGE = "adapter.error_message";

    public static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    public static final String UNIQUEID = "$uniqueid";
    public static final String UNIQUEIDSOURCE = "$uniqueidsource";
    public static final String UNIQUEIDVERSIONS = "$uniqueidversions";
    public static final String WORKITEMID = "$workitemid";
    public static final String MODELVERSION = "$modelversion";
    public static final String TRANSACTIONID = "$transactionid";

    @Deprecated
    public static final String PROCESSID = "$processid";

    public static final String TASKID = "$taskid";
    public static final String EVENTID = "$eventid";

    public static final String INTERMEDIATE_EVENTID = "$intermediateEvent";
    public static final String INTERMEDIATE_EVENT_ELEMENTID = "$intermediateEvent.elementId";

    public static final String WORKFLOWGROUP = "$workflowgroup";
    public static final String WORKFLOWSTATUS = "$workflowstatus";
    public static final String ISVERSION = "$isversion";
    public static final String LASTTASK = "$lasttask";
    public static final String LASTEVENT = "$lastevent";
    public static final String LASTEVENTDATE = "$lasteventdate";
    public static final String CREATOR = "$creator";
    public static final String EDITOR = "$editor";
    public static final String LASTEDITOR = "$lasteditor";

    public static final String EVENTLOG = "$eventlog";
    public static final String EVENTLOG_COMMENT = "$eventlogcomment";

    public static final String CREATED = "$created";
    public static final String MODIFIED = "$modified";

    public static final String TYPE = "type";

    private List<Plugin> pluginRegistry = null;
    private Map<String, Adapter> adapterRegistry = null;

    private ModelManager modelManager = null;
    private WorkflowContext context = null;

    private List<ItemCollection> splitWorkitems = null;

    private static final Logger logger = Logger.getLogger(WorkflowKernel.class.getName());

    /**
     * Constructor initialize a ModelManger instance. The ModelManager instance can
     * load models form the WorkflowContext.
     */
    public WorkflowKernel(final WorkflowContext context) {
        // check workflow context
        if (context == null) {
            throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), MISSING_WORKFLOWCONTEXT,
                    "WorkflowKernel can not be initialized: context is null!");
        }

        this.context = context;
        modelManager = new ModelManager(context);
        pluginRegistry = new ArrayList<Plugin>();
        adapterRegistry = new HashMap<String, Adapter>();
        splitWorkitems = new ArrayList<ItemCollection>();
    }

    public ModelManager getModelManager() {
        return modelManager;
    }

    /**
     * This method generates an immutable universally unique identifier (UUID). A
     * UUID represents a 128-bit value.
     * 
     * @see https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html
     * 
     * @return UUID
     */
    public static String generateUniqueID() {
        String id = UUID.randomUUID().toString();
        return id;
    }

    /**
     * This method generates an secure 8 byte random secure id. The ID is returned
     * as a hex decimal value.
     * 
     * @return transactionID
     */
    public static String generateTransactionID() {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[8];
        random.nextBytes(bytes);
        return new BigInteger(1, bytes).toString(16);
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

        // Test if plugin was already registered.
        if (pluginRegistryContains(plugin)) {
            throw new PluginException(WorkflowKernel.class.getSimpleName(), PLUGIN_ERROR,
                    "plugin: " + plugin.getClass().getName() + " is already registered!");
        }

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
                    logger.log(Level.WARNING, "Plugin ''{0}'' depends on unregistered Plugin class ''{1}''",
                            new Object[] { plugin.getClass().getName(), dependency });
                }
            }
        }
        plugin.init(context);
        pluginRegistry.add(plugin);
    }

    /**
     * Returns true if the plugin with the classname is already registered.
     * 
     * @param nameToCheck
     * @return
     */
    private boolean pluginRegistryContains(final Plugin pluginToCheck) {
        if (pluginRegistry != null) {
            for (Plugin plugin : pluginRegistry) {
                if (plugin.getClass().getName().equals(pluginToCheck.getClass().getName())) {
                    return true; // Plugin with the specified name found
                }
            }
        }
        return false; // Plugin with the specified name not found
    }

    /**
     * This method registers a new adapter class.
     * 
     * @param adapterClass
     */
    public void registerAdapter(final Adapter adapter) {
        adapterRegistry.put(adapter.getClass().getName(), adapter);
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
                logger.log(Level.FINEST, "......register plugin class: {0}...", pluginClass);

            Class<?> clazz = null;
            try {
                clazz = Class.forName(pluginClass);
                Plugin plugin = (Plugin) clazz.getDeclaredConstructor().newInstance();
                registerPlugin(plugin);
            } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
                    | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new PluginException(WorkflowKernel.class.getSimpleName(), PLUGIN_NOT_CREATEABLE,
                        "unable to register plugin: " + pluginClass + " - reason: " + e.toString(), e);
            }
        }
    }

    /**
     * This method removes a registered plugin based on its class name.
     * 
     * @param pluginClass
     * @throws PluginException if plugin not registered
     */
    public void unregisterPlugin(final String pluginClass) throws PluginException {
        boolean debug = logger.isLoggable(Level.FINE);
        if (debug) {
            logger.log(Level.FINEST, "......unregisterPlugin {0}", pluginClass);
        }
        boolean found = false;
        // unregister all plugins matching the name
        if (pluginRegistry != null) {
            Iterator<Plugin> iterator = pluginRegistry.iterator();
            while (iterator.hasNext()) {
                Plugin plugin = iterator.next();
                if (plugin.getClass().getName().equals(pluginClass)) {
                    iterator.remove(); // Safely remove the plugin
                    found = true;
                }
            }
        }

        if (!found) {
            // throw PluginExeption
            throw new PluginException(WorkflowKernel.class.getSimpleName(), PLUGIN_NOT_REGISTERED,
                    "unable to unregister plugin: " + pluginClass + " - reason: ");
        }
    }

    /**
     * This method removes all registered plugins
     * 
     * @param pluginClass
     */
    public void unregisterAllPlugins() {
        boolean debug = logger.isLoggable(Level.FINE);
        if (debug) {
            logger.finest("......unregisterAllPlugins...");
        }
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
     * Returns a registry containing all registered Adapter instances.
     * 
     * @return
     */
    public Map<String, Adapter> getAdapterRegistry() {
        return adapterRegistry;
    }

    /**
     * This method processes a workitem (process instance) based on the current
     * model definition. A workitem must at least provide the properties
     * <code>$ModelVersion</code>, <code>$TaskID</code> and <code>$EventID</code>.
     * <p>
     * During the processing life-cycle more than one event can be processed. This
     * depends on the model definition which is controlled by the
     * {@code ModelManager}. For example an event can be followed by another event
     * in the process flow. Also conditional events can have different outcomes
     * depending on the data of a workitem.
     * <p>
     * The method executes all plugin and adapter classes and returns an updated
     * instance of the workitem. The method did not persist the process instance.
     * The persistence mechanism is covered by the {@code WorkflowService} witch is
     * not part of this core project.
     * 
     * @param workitem the process instance to be processed.
     * @return updated workitem
     * @throws PluginException,ModelException
     */
    public ItemCollection process(ItemCollection workitem) throws PluginException, ModelException {
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

        // Check if $UniqueID is available
        if ("".equals(workitem.getItemValueString(UNIQUEID))) {
            // generating a new one
            workitem.replaceItemValue(UNIQUEID, generateUniqueID());
        }

        // Generate a $TransactionID
        workitem.replaceItemValue(TRANSACTIONID, generateTransactionID());

        // store last $lastTask
        workitem.replaceItemValue("$lastTask", workitem.getTaskID());

        // Check if $WorkItemID is available
        if ("".equals(workitem.getItemValueString(WorkflowKernel.WORKITEMID))) {
            workitem.replaceItemValue(WorkflowKernel.WORKITEMID, generateUniqueID());
        }

        // clear all existing adapter errors..
        workitem.removeItem(ADAPTER_ERROR_CONTEXT);
        workitem.removeItem(ADAPTER_ERROR_CODE);
        workitem.removeItem(ADAPTER_ERROR_PARAMS);
        workitem.removeItem(ADAPTER_ERROR_MESSAGE);

        // fetch Model instance
        BPMNModel model = modelManager.getModelByWorkitem(workitem);

        // Iterate through all events in the process flow
        splitWorkitems = new ArrayList<ItemCollection>();
        List<String> loopDetector = new ArrayList<String>();
        ItemCollection event = this.loadEvent(workitem, model);
        while (event != null) {
            String id = event.getItemValueString("id");
            if (loopDetector.contains(id)) {
                throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), MODEL_ERROR,
                        "Event loop detected " + workitem.getTaskID() + "." + workitem.getEventID() + " event " + id
                                + " was called twice in one processing life cycle. Check your model!");
            }
            event = processEvent(workitem, event, model);
            // verify if the model version has changed during the last processing cycle!
            if (!workitem.getModelVersion().equals(BPMNUtil.getVersion(model))) {
                // update model instance...
                // logger.finest("Update Model Instance to: " + workitem.getModelVersion());

                logger.log(Level.INFO, "\u2699 update model: ''{0}'' ▶ ''{1}'',"
                        + "  $workflowgroup: ''{2}'', $uniqueid: {3}",
                        new Object[] { BPMNUtil.getVersion(model), workitem.getModelVersion(),
                                workitem.getWorkflowGroup(),
                                workitem.getUniqueID() });

                model = modelManager.getModelByWorkitem(workitem);
            }

            loopDetector.add(id);
        }

        return workitem;
    }

    /**
     * Returns the BPMN Event entity associated with a given workitem, based on its
     * attributes "$modelVersion", "$taskID" and "$eventID".
     * <p>
     * The method throws a {@link ModelException} if no Event can be resolved based
     * on the given model information.
     * <p>
     * The method is called by the {@link WorkflowKernel} to start the processing
     * life cycle.
     * <p>
     * A Event is typically connected with a Task by outgoing SequenceFlows. A
     * special case is a Start event followed by one or more Events connected with a
     * Task (Start-Task) element.
     * 
     * @param workitem
     * @return BPMN Event entity - {@link ItemCollection}
     * @throws ModelException if no event was found
     */
    public ItemCollection loadEvent(ItemCollection workitem, BPMNModel model) throws ModelException {

        logger.fine("...loadEvent " + workitem.getTaskID() + "." + workitem.getEventID());
        ItemCollection event = modelManager.findEventByID(model, workitem.getTaskID(), workitem.getEventID());
        // verify if the event is a valid processing event?
        if (event != null) {
            List<ItemCollection> allowedEvents = modelManager.findEventsByTask(model, workitem.getTaskID());
            boolean found = false;
            for (ItemCollection allowedEvent : allowedEvents) {
                // logger.info("verify event : " + allowedEvent.getItemValueString("id"));
                if (allowedEvent.getItemValueString("id").equals(event.getItemValueString("id"))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                event = null;
            }
        }
        // If we still did not find the event we throw a ModelException....
        if (event == null) {
            throw new ModelException(ModelException.UNDEFINED_MODEL_ENTRY, "Event " + workitem.getTaskID() + "."
                    + workitem.getEventID() + " is not callable in model '" + workitem.getModelVersion() + "'");
        }
        return event;
    }

    /**
     * This method processes a single event on a given process instance and executes
     * all assigned MicroKernels. After the execution was completed, the method
     * loads the next BPMN element in the process flow. If the next BPMN element is
     * again an event, the method returns the next event. In all other cases the
     * method returns null to signal that the processing life cycle can be
     * terminated.
     * 
     * @param workitem
     * @param event
     * @return the next event in the process flow or null if the next element is a
     *         Task
     * @throws ModelException
     * @throws PluginException
     */
    private ItemCollection processEvent(ItemCollection workitem, ItemCollection event, BPMNModel model)
            throws ModelException, PluginException {

        // Update the intermediate processing status
        updateIntermediateEvent(workitem, event);
        // set $lastEventDate
        workitem.replaceItemValue(LASTEVENTDATE, new Date());
        // Execute Plugins and Adapters....
        workitem = executeMicroKernels(workitem, event);

        // test if a new model version was assigned by the last event
        if (updateModelVersionByEvent(workitem, event)) {
            logger.log(Level.FINE, "\u2699 set new model : {0} ({1})",
                    new Object[] { workitem.getItemValueString(UNIQUEID),
                            workitem.getItemValueString(MODELVERSION) });
            // load new model instance...
            model = modelManager.getModelByWorkitem(workitem);
            // write event log
            logEvent(workitem.getTaskID(), workitem.getEventID(), workitem.getTaskID(), workitem);
            event = this.loadEvent(workitem, model);
            workitem.event(event.getItemValueInteger(BPMNUtil.EVENT_ITEM_EVENTID));
            return event;
        } else {
            // evaluate next BPMN Element.....
            ItemCollection nextElement = this.modelManager.nextModelElement(model, event, workitem);
            if (nextElement == null || !nextElement.hasItem("type")) {
                throw new ModelException(ModelException.INVALID_MODEL_ENTRY,
                        "No valid Target Element found - BPMN Event Element must be followed by a Task or another Event! Verify Sequence Flows and Conditions!");
            }

            // ==> bpmn2:parallelGateway
            if (ModelManager.PARALLELGATEWAY_ELEMENT.equals(nextElement.getType())) {
                nextElement = handleParallelGateWay(model, workitem, nextElement, true);
            }

            // ==> bpmn2:intermediateCatchEvent
            if (ModelManager.EVENT_ELEMENT.equals(nextElement.getType())) {
                // load next event
                logEvent(workitem.getTaskID(), workitem.getEventID(), workitem.getTaskID(), workitem);
                event = nextElement;
                workitem.event(event.getItemValueInteger(BPMNUtil.EVENT_ITEM_EVENTID));

                // return the next event element to be processed....
                return event;
            }

            // == bpm2:task
            if (ModelManager.TASK_ELEMENT.equals(nextElement.getType())) {
                // update status and terminate processing life cycle
                logEvent(workitem.getTaskID(), workitem.getEventID(),
                        nextElement.getItemValueInteger(BPMNUtil.TASK_ITEM_TASKID), workitem);
                // Update status - Issue #722
                updateWorkflowStatus(workitem, nextElement, model);
                // terminate processing life cycle
                workitem.event(0);
                event = null;
                return null;
            }
        }

        return null;

    }

    /**
     * This helper method resolves a ParallelGateway Situation.
     * 
     * The method verifies all outgoing flows and creates a new Split-WorkItem for
     * each following Event. At least on Task element is expected. This is the final
     * status of the main workItem.
     * 
     * 
     * @param model
     * @param workitem
     * @param parallelGateway
     * @param processEvents   - if true the process events of a split workitem will
     *                        be processed.
     * @return
     * @throws ModelException
     * @throws PluginException
     */
    private ItemCollection handleParallelGateWay(BPMNModel model, ItemCollection workitem,
            ItemCollection parallelGateway, boolean processEvents) throws ModelException, PluginException {
        ItemCollection result = null;

        // verify if we have a parallelgateway
        if (!ModelManager.PARALLELGATEWAY_ELEMENT.equals(parallelGateway.getType())) {
            throw new ModelException(ModelException.INVALID_MODEL_ENTRY,
                    "BPMN Model Parallel Gateway expected!");
        }

        // We need the follow Up Task and Event Nodes now to create the split Events
        BPMNElementNode gatewayNode = model.findElementNodeById(parallelGateway.getItemValueString("id"));
        BPMNLinkedFlowIterator<BPMNElementNode> splitElementNavigator = new BPMNLinkedFlowIterator<BPMNElementNode>(
                gatewayNode,
                node -> ((BPMNUtil.isImixsTaskElement(node)) || (BPMNUtil.isImixsEventElement(node))));
        // now iterate all targets....
        boolean foundMainTask = false;
        while (splitElementNavigator.hasNext()) {
            BPMNElementNode nextSplitNode = splitElementNavigator.next();
            ItemCollection splitItemCol = BPMNEntityBuilder.build(nextSplitNode);
            // Test if the flow is a the Main SequenceFlow of the ParallelGateway.
            boolean bMainFlow = this.modelManager.isMainParallelGatewayFlow(gatewayNode, nextSplitNode,
                    workitem);
            if (bMainFlow) {
                if (foundMainTask == true) {
                    throw new ModelException(ModelException.INVALID_MODEL_ENTRY,
                            "BPMN Model Error: Parallel Gateway: " + gatewayNode.getId()
                                    + " - only one outcome can be directly linked to a task element! Missing Event element.");
                }
                foundMainTask = true;
                result = splitItemCol;
            } else {
                // clone current instance to a new version...
                ItemCollection cloned = createVersion(workitem);
                // set new event
                cloned.setEventID(splitItemCol.getItemValueInteger(BPMNUtil.EVENT_ITEM_EVENTID));
                // add temporary attribute $isversion...
                cloned.replaceItemValue(ISVERSION, true);

                ItemCollection splitEvent = splitItemCol;
                // only process events in the normal process cycle
                if (processEvents) {
                    while (splitEvent != null) {
                        splitEvent = this.processEvent(cloned, splitEvent, model);
                    }
                }

                // remove temporary attribute $isversion...
                cloned.removeItem(ISVERSION);
                // add to cache...
                splitWorkitems.add(cloned);
                continue;
            }
        }
        // if we did not have found a SplitEvent we throw a Model Exception!
        if (foundMainTask == false) {
            throw new ModelException(ModelException.INVALID_MODEL_ENTRY,
                    "BPMN Model Error: Parallel Gateway: " + gatewayNode.getId()
                            + " - At least one outcome must be connected directly to a Task Element or evaluate to 'true'!");
        }
        // continue with normal flow
        return result;
    }

    /**
     * Evaluates the next task BPMN element for a process instance (workitem) based
     * on the current model definition. A Workitem must at least provide the
     * properties <code>$TaskID</code> and a <code>$EventID</code> or
     * <code>$intermediateEventID</code>.
     * <p>
     * During the evaluation life-cycle more than one event can be evaluated. This
     * depends on the model definition which can define follow-up-events,
     * split-events and conditional events.
     * <p>
     * The method did not persist the process instance or execute any plugins or
     * adapter classes.
     * 
     * @param workitem the process instance to be evaluated.
     * @return the BPMN task element followed by the given execution flow
     * @throws PluginException,ModelException
     */
    public ItemCollection eval(final ItemCollection _workitem) throws PluginException,
            ModelException {
        // check document context
        if (_workitem == null)
            throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(),
                    UNDEFINED_WORKITEM,
                    "eval error: workitem is null");

        // clone the workitem to avoid pollution of the origin workitem
        ItemCollection workitem = (ItemCollection) _workitem.clone();
        // fetch Model instance
        BPMNModel model = modelManager.getModelByWorkitem(workitem);

        // check $TaskID
        if (workitem.getTaskID() <= 0)
            throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(),
                    UNDEFINED_PROCESSID,
                    "processing error: $taskID undefined (" + workitem.getTaskID() + ")");

        // check $eventId
        if (workitem.getEventID() <= 0)
            throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(),
                    UNDEFINED_ACTIVITYID,
                    "processing error: $eventID undefined (" + workitem.getEventID() + ")");

        // now evaluate all events defined by the model

        List<String> loopDetector = new ArrayList<String>();

        // In case we have an Intermediate Event we load the next event in the
        // sequenceFlow.
        // This logic is only valid for the eval method. In case of a normal processing
        // life cycle an intermediate event is not allowed!
        ItemCollection event = null;
        String intermediateEventElementID = workitem.getItemValueString(WorkflowKernel.INTERMEDIATE_EVENT_ELEMENTID);
        int intermediateEvent = workitem.getItemValueInteger(WorkflowKernel.INTERMEDIATE_EVENTID);
        if (intermediateEvent > 0) {
            BPMNElementNode intermediateEventElement = model.findElementNodeById(intermediateEventElementID);
            event = BPMNEntityBuilder.build(intermediateEventElement);
        } else {
            event = this.loadEvent(workitem, model);
        }

        while (event != null) {
            String id = event.getItemValueString("id");
            if (loopDetector.contains(id)) {
                throw new ProcessingErrorException(WorkflowKernel.class.getSimpleName(), MODEL_ERROR,
                        "Event loop detected " + workitem.getTaskID() + "." + workitem.getEventID() + " event " + id
                                + " was called twice in one processing life cycle. Check your model!");
            }
            loopDetector.add(id);

            // test if a new model version was assigned by the last event
            if (updateModelVersionByEvent(workitem, event)) {
                // load new model instance
                model = modelManager.getModelByWorkitem(workitem);
                event = this.loadEvent(workitem, model);
                workitem.event(event.getItemValueInteger("numactivityid"));
            } else {
                // evaluate next BPMN Element.....
                ItemCollection nextElement = this.modelManager.nextModelElement(model, event, workitem);
                if (nextElement != null && !nextElement.hasItem("type")) {
                    throw new ModelException(ModelException.INVALID_MODEL_ENTRY,
                            "BPMN Element Entity must provide the item 'type'!");
                }

                // ==> bpmn2:parallelGateway
                if (ModelManager.PARALLELGATEWAY_ELEMENT.equals(nextElement.getType())) {
                    nextElement = handleParallelGateWay(model, workitem, nextElement, false);
                }

                // ==> bpmn2:intermediateCatchEvent
                if (ModelManager.EVENT_ELEMENT.equals(nextElement.getType())) {
                    // load next event and continue processing live-cycle
                    event = nextElement;
                    workitem.event(event.getItemValueInteger("numactivityid"));
                } else {
                    // terminate processing life cycle
                    workitem.event(0);
                    return nextElement;
                }
            }
        }

        // evaluation failed!
        return null;
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
     * If the current workflow result of an Event defines a new model tag, this
     * method updates the $modelversion, $taskID and $eventID and returns true. If
     * no <model> tag was found, the method returns false.
     * 
     * <pre>
     * {@code
     * <model>     
     *    <version>sub-model-1.0.0</version>
     *    <task>1000</task>
     *    <event>10</event>
     * </model>
     * }
     * </pre>
     * 
     * The task tag within the model configuration is optional.
     * <p>
     * The method is called during the processing live-cycle of the workflowKernel.
     * 
     * @param workitem
     * @param event
     * @param log      - indicates if the procedure should be logged into the server
     *                 log
     * @return true if the model version was updated by this method
     * @throws ModelException
     * @throws PluginException
     * 
     **/
    private boolean updateModelVersionByEvent(final ItemCollection workitem, final ItemCollection event)
            throws ModelException, PluginException {

        // test if a <model> tag is defined
        String eventResult = event.getItemValueString("txtActivityResult");
        List<String> modelTags = XMLParser.findNoEmptyXMLTags(eventResult, "model");

        if (modelTags == null || modelTags.size() == 0) {
            // no model tag found
            return false;
        }

        // extract the model tag information - version and event are mandatory
        ItemCollection modelData;
        modelData = XMLParser.parseTag(modelTags.get(0), "model");

        String version = modelData.getItemValueString("version");
        int iNextEvent = modelData.getItemValueInteger("event");
        int iTask = modelData.getItemValueInteger("task");
        if (version.trim().isEmpty() || iNextEvent <= 0) {
            String sErrorMessage = "Invalid model tag in event " +
                    +event.getItemValueInteger(BPMNUtil.EVENT_ITEM_EVENTID) + " (" + event.getModelVersion();
            logger.warning(sErrorMessage);
            throw new ModelException(ModelException.INVALID_MODEL, sErrorMessage);
        }
        // apply new model version and event id
        workitem.model(version).event(iNextEvent);

        if (iTask > 0) {
            // optional - test if we can load the target task...
            workitem.task(iTask);
            // fetch Model instance
            BPMNModel model = modelManager.getModelByWorkitem(workitem);
            ItemCollection itemColNextTask = this.modelManager.loadTask(workitem, model);
            if (itemColNextTask != null) {
                updateWorkflowStatus(workitem, itemColNextTask, model);
            }
        }
        return true;

    }

    /**
     * This method executes all registered adapter and plug-in classes defined by a
     * single event on a workflow instance.
     * <p>
     * In case of an AdapterException, the exception data will be wrapped into items
     * with the prefix 'adapter.'
     * 
     * @throws PluginException,ModelException
     */
    private ItemCollection executeMicroKernels(final ItemCollection workitem, final ItemCollection event)
            throws PluginException, ModelException {
        ItemCollection documentResult = workitem;
        // log the general processing message
        String msg = "⚙ processing: " + workitem.getItemValueString(UNIQUEID) + " ("
                + workitem.getItemValueString(MODELVERSION) + " ▷ " + workitem.getTaskID() + "."
                + workitem.getEventID() + ")";

        logger.info(msg);
        // execute SignalAdapters
        executeSignalAdapters(documentResult, event);
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
        // execute GenericAdapters
        executeGenericAdapters(documentResult, event);

        return documentResult;
    }

    /**
     * Helper method to update the items $taskid, $worklfowstatus, $workflowgroup
     * and type. The given Element must be a Task. Otherwise the method throws a
     * ModelException.
     * 
     * @throws ModelException
     */
    private void updateWorkflowStatus(ItemCollection workitem, ItemCollection itemColNextTask, BPMNModel model)
            throws ModelException {
        boolean debug = logger.isLoggable(Level.FINE);
        if (!ModelManager.TASK_ELEMENT.equals(itemColNextTask.getType())) {
            throw new ModelException(ModelException.INVALID_MODEL,
                    "Invalid Model Element - BPMN Task Element was expected to update the current workflow status.");
        }

        // remove optional intermediate event status
        workitem.removeItem("$intermediateEvent");
        workitem.removeItem("$intermediateEventElementID");

        ItemCollection process = this.modelManager.loadProcess(workitem, model);
        // Update the attributes $taskID and $WorkflowStatus
        workitem.task(itemColNextTask.getItemValueInteger(BPMNUtil.TASK_ITEM_TASKID));
        if (debug) {
            logger.log(Level.FINEST, "......new $taskID={0}", workitem.getTaskID());
        }
        workitem.replaceItemValue(WORKFLOWSTATUS, itemColNextTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));
        workitem.replaceItemValue(WORKFLOWGROUP, process.getItemValueString("name"));
        if (debug) {
            logger.log(Level.FINEST, "......new $workflowStatus={0}",
                    workitem.getItemValueString(WORKFLOWSTATUS));
        }
        // update deprecated attributes txtworkflowStatus and txtworkflowGroup
        workitem.replaceItemValue("txtworkflowStatus", workitem.getItemValueString(WORKFLOWSTATUS));
        workitem.replaceItemValue("txtworkflowGroup", workitem.getItemValueString(WORKFLOWGROUP));

        // update the type attribute if defined.
        // the type attribute can only be overwritten by a plug-in if the type is not
        // defined by the task!
        String sType = itemColNextTask.getItemValueString(BPMNUtil.TASK_ITEM_APPLICATION_TYPE);
        if (!"".equals(sType)) {
            workitem.replaceItemValue(TYPE, sType);
        }
    }

    /**
     * Helper method to update the item <code>$intermediateEvent</code> and
     * <code>$intermediateEvent.elementId</code>. The given Element must be a Event.
     * Otherwise the method throws a ModelException.
     * <p>
     * This method is called by the method processEvent() and indicates that we are
     * currently in an active processing life cycle. This status is evaluated by the
     * eval() method to avoid invalid calls of loadEvent().
     * 
     * @throws ModelException
     */
    private void updateIntermediateEvent(ItemCollection workitem, ItemCollection itemColNextEvent)
            throws ModelException {
        if (!ModelManager.EVENT_ELEMENT.equals(itemColNextEvent.getType())) {
            throw new ModelException(ModelException.INVALID_MODEL,
                    "Invalid Model Element - BPMN Event Element was expected to update the intermediate event status.");
        }
        workitem.setItemValue(INTERMEDIATE_EVENTID, itemColNextEvent.getItemValueInteger(BPMNUtil.EVENT_ITEM_EVENTID));
        workitem.setItemValue(INTERMEDIATE_EVENT_ELEMENTID, itemColNextEvent.getItemValueString("id"));
    }

    /**
     * This method executes all SignalAdapters associated with the model.
     * <p>
     * A StaticAdaper should not be associated with a BPMN Signal Event.
     * 
     * @param documentResult
     * @param event
     * @throws PluginException
     * @throws ModelException
     */
    private void executeSignalAdapters(ItemCollection documentResult, ItemCollection event)
            throws PluginException, ModelException {
        boolean debug = logger.isLoggable(Level.FINE);
        if (debug) {
            logger.finest("......executing SignalAdapters...");
        }
        // execute adapters if adapter class is defined....
        String adapterClass = event.getItemValueString("adapter.id");
        if (!adapterClass.isEmpty() && adapterClass.matches("^(?:\\w+|\\w+\\.\\w+)+$")) {
            Adapter adapter = adapterRegistry.get(adapterClass);
            if (adapter != null) {

                if (adapter instanceof GenericAdapter) {
                    logger.log(Level.WARNING, "...GenericAdapter ''{0}'' should not be associated with a Signal Event."
                            + " Adapter will not be executed.", adapterClass);
                    // ...stop execution as the GenericAdapter was already executed by the method
                    // executeGenericAdapters...
                } else {
                    // execute only instance of signal Adapters...
                    if (adapter instanceof SignalAdapter) {
                        executeAdaper(adapter, documentResult, event);
                    } else {
                        throw new PluginException(WorkflowKernel.class.getSimpleName(), PLUGIN_ERROR,
                                "Abstract Adapter '" + adapterClass
                                        + "' can not be executed - use SignalAdapter or GenericAdapter instead!");

                    }
                }
            } else {
                throw new ModelException(ModelException.INVALID_MODEL,
                        "...Adapter '" + adapterClass + "' not registered - verify model!");
            }
        }
    }

    /**
     * This method executes all StaticAdapters. StaticAdapters are executed before
     * the SignalAdapters
     * 
     * @param documentResult
     * @param event
     * @throws PluginException
     */
    private void executeGenericAdapters(ItemCollection documentResult, ItemCollection event) throws PluginException {
        boolean debug = logger.isLoggable(Level.FINE);
        if (debug) {
            logger.finest("......executing GenericAdapters...");
        }
        // execute all GenericAdapters
        Collection<Adapter> adapters = adapterRegistry.values();
        for (Adapter adapter : adapters) {
            // test if Adapter is static
            if (adapter instanceof GenericAdapter) {
                // execute...
                executeAdaper(adapter, documentResult, event);
            }
        }
    }

    /**
     * This method executes an instance of Adapter and logs adapter error messages.
     * <p>
     * In case of an AdapterException, the exception data will be wrapped into items
     * with the prefix 'adapter.'
     * 
     * @param adapter
     * @param workitem
     * @param event
     * @throws PluginException
     */
    @SuppressWarnings("unchecked")
    private void executeAdaper(Adapter adapter, ItemCollection workitem, ItemCollection event) throws PluginException {
        boolean debug = logger.isLoggable(Level.FINE);
        // execute...
        try {
            workitem = adapter.execute(workitem, event);
        } catch (AdapterException e) {
            logger.log(Level.WARNING, "...execution of adapter " + adapter.getClass().getSimpleName() + " failed: {0}",
                    e.getMessage());
            // update workitem with adapter exception....
            workitem.appendItemValue(ADAPTER_ERROR_CONTEXT, e.getErrorContext());
            workitem.appendItemValue(ADAPTER_ERROR_CODE, e.getErrorCode());
            workitem.appendItemValue(ADAPTER_ERROR_PARAMS, e.getErrorParameters());
            workitem.appendItemValue(ADAPTER_ERROR_MESSAGE, e.getMessage());

            // revert order of error list
            List<Object> valueList = workitem.getItemValue(ADAPTER_ERROR_CONTEXT);
            Collections.reverse(valueList);
            workitem.setItemValue(ADAPTER_ERROR_CONTEXT, valueList);
            valueList = workitem.getItemValue(ADAPTER_ERROR_CODE);
            Collections.reverse(valueList);
            workitem.setItemValue(ADAPTER_ERROR_CODE, valueList);
            valueList = workitem.getItemValue(ADAPTER_ERROR_PARAMS);
            Collections.reverse(valueList);
            workitem.setItemValue(ADAPTER_ERROR_PARAMS, valueList);
            valueList = workitem.getItemValue(ADAPTER_ERROR_MESSAGE);
            Collections.reverse(valueList);
            workitem.setItemValue(ADAPTER_ERROR_MESSAGE, valueList);

            if (debug) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method creates a new instance of a sourceWorkitem. The method did not
     * save the workitem!.
     * <p>
     * The new property $UniqueIDSource will be added to the new version, which
     * points to the $uniqueID of the sourceWorkitem. In addition the item
     * $created.version marks the point of time.
     * <p>
     * The new property $UniqueIDVersions will be added to the sourceWorkItem which
     * points to the id of the new version.
     * 
     * @param sourceItemCollection the ItemCollection which should be versioned
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
        itemColNewVersion.replaceItemValue(CREATED + ".version", sourceItemCollection.getItemValueDate(LASTEVENTDATE));

        // remove $UniqueIDVersions
        itemColNewVersion.removeItem(UNIQUEIDVERSIONS);

        // append the version uniqueid to the source ItemCollection
        sourceItemCollection.appendItemValue(UNIQUEIDVERSIONS, itemColNewVersion.getUniqueID());

        return itemColNewVersion;

    }

    /**
     * This method is responsible for the internal workflow log. The attribute
     * $eventlog logs the transition from one process to another.
     * <p>
     * Format:
     * <p>
     * <code>
     * timestamp+timezone|model-version|sourcetask|eventid|targettask|actor|comment
     * </code>
     * <p>
     * Example: <code>
     * 2024-08-27T12:04:20.469+02:00|requirement-1.0.0|2000|10|3000|admin@foo.com|approved
     * </code>
     * <p>
     * The comment and actor are optional information. The actor is read from the
     * field '$editor'. The comment is read from the field '$eventlogComment'.
     */
    /**
     * This method is responsible for the internal workflow log.
     */
    @SuppressWarnings("unchecked")
    private ItemCollection logEvent(int taskID, int eventID, int targetTaskID, final ItemCollection workitem) {
        ItemCollection documentResult = workitem;

        // Migration check for deprecated log entries.
        migrateDeprecatedEventLogFormat(workitem);

        List<String> existingLogEntries = (List<String>) workitem.getItemValue(EVENTLOG);

        // Create new log entry...
        StringBuilder logEntry = new StringBuilder();
        LocalDateTime dateTime = LocalDateTime.now();
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.systemDefault());

        logEntry.append(zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .append("|")
                .append(workitem.getItemValueString(MODELVERSION))
                .append("|")
                .append(taskID)
                .append("|")
                .append(eventID)
                .append("|")
                .append(targetTaskID)
                .append("|")
                .append(workitem.getItemValueString("$editor"))
                .append("|");

        // Append optional comment
        String logComment = workitem.getItemValueString(EVENTLOG_COMMENT);
        if (!logComment.isEmpty()) {
            logEntry.append(logComment);
        }

        // Update the log entries
        existingLogEntries.add(logEntry.toString());
        documentResult.replaceItemValue(EVENTLOG, existingLogEntries);
        documentResult.replaceItemValue(LASTEVENT, eventID);

        return documentResult;
    }

    /**
     * Migration of deprecated $eventLog.
     * 
     * This method checks if the eventlog needs to be migrated and performs
     * migration if necessary. The old format will be stored in the field
     * '$eventlogdeprecated'.
     * 
     * @param workitem - the workitem to be migrated
     */
    @SuppressWarnings("unchecked")
    private void migrateDeprecatedEventLogFormat(ItemCollection workitem) {
        if (workitem.hasItem("$eventlogdeprecated")) {
            // already migrated
            return;
        }
        List<String> logEntries = (List<String>) workitem.getItemValue(EVENTLOG);

        if (logEntries.isEmpty()) {
            // no entries available
            return;
        }
        // Test if migration is necessary
        String firstEntry = logEntries.get(0);
        String[] parts = firstEntry.split("\\|");

        // Old format has 4 or 5 parts (with optional comment)
        // and contains a dot in the third part (task.event)
        if (parts.length <= 5 && parts.length >= 4
                && parts[2].contains(".")) {
            // backup old format
            workitem.setItemValue("$eventlogdeprecated", logEntries);

            // Create new list with migrated entries
            List<String> migratedEntries = new ArrayList<>();
            for (String oldEntry : logEntries) {
                String migratedEntry = migrateDeprecatedLogEntry(oldEntry);
                migratedEntries.add(migratedEntry);
            }
            // Update eventlog with migrated entries
            workitem.replaceItemValue(EVENTLOG, migratedEntries);
        }
    }

    /**
     * Converts a single log entry from old to new format Old:
     * timestamp|model-version|1000.10|1000|comment New:
     * timestamp+timezone|model-version|sourcetask|eventid|targettask|actor|comment
     */
    private String migrateDeprecatedLogEntry(String oldEntry) {
        String[] parts = oldEntry.split("\\|");
        StringBuilder newEntry = new StringBuilder();

        // Convert timestamp to timezone format
        try {
            LocalDateTime oldDateTime = LocalDateTime.parse(parts[0]);
            ZonedDateTime zonedDateTime = oldDateTime.atZone(ZoneId.systemDefault());
            newEntry.append(zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        } catch (Exception e) {
            // If parsing fails, keep original timestamp
            newEntry.append(parts[0]);
        }

        newEntry.append("|")
                .append(parts[1]) // model-version
                .append("|");

        // Split task.event into separate fields
        String[] taskEvent = parts[2].split("\\.");
        newEntry.append(taskEvent[0]) // sourcetask
                .append("|")
                .append(taskEvent[1]) // eventid
                .append("|")
                .append(parts[3]) // targettask
                .append("||"); // empty actor field

        // Append comment if exists
        if (parts.length > 4 && !parts[4].isEmpty()) {
            newEntry.append(parts[4]);
        }

        return newEntry.toString();
    }

    /**
     * This method runs all registered plugins until the run method of a plugin
     * breaks with an error In this case the method stops.
     * 
     * @throws PluginException
     */
    private ItemCollection runPlugins(final ItemCollection documentContext, final ItemCollection event)
            throws PluginException {
        boolean debug = logger.isLoggable(Level.FINE);
        ItemCollection documentResult = documentContext;
        String sPluginName = null;
        List<String> localPluginLog = new Vector<String>();

        try {
            for (Plugin plugin : pluginRegistry) {

                sPluginName = plugin.getClass().getName();
                if (debug) {
                    logger.log(Level.FINEST, "......running Plugin: {0}...", sPluginName);
                }
                long lPluginTime = System.currentTimeMillis();
                documentResult = plugin.run(documentResult, event);
                if (debug) {
                    logger.log(Level.FINE, "...Plugin ''{0}'' processing time={1}ms",
                            new Object[] { sPluginName, System.currentTimeMillis() - lPluginTime });
                }
                if (documentResult == null) {
                    logger.log(Level.SEVERE, "[runPlugins] PLUGIN_ERROR: {0}", sPluginName);
                    for (String sLogEntry : localPluginLog)
                        logger.log(Level.SEVERE, "[runPlugins]   {0}", sLogEntry);

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
            logger.log(Level.SEVERE, "Plugin-Error at {0}: {1} ({2})",
                    new Object[] { e.getErrorContext(), e.getErrorCode(), e.getMessage() });
            if (debug) {
                logger.severe("Last Plugins run successfull:");
                for (String sLogEntry : localPluginLog)
                    logger.log(Level.SEVERE, "   ...{0}", sLogEntry);
            }
            throw e;
        }

    }

    private void closePlugins(boolean rollbackTransaction) throws PluginException {
        for (int i = 0; i < pluginRegistry.size(); i++) {
            Plugin plugin = (Plugin) pluginRegistry.get(i);
            if (logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST, "closing Plugin: {0}...", plugin.getClass().getName());
            plugin.close(rollbackTransaction);
        }
    }

}
