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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.imixs.workflow.bpmn.BPMNEntityBuilder;
import org.imixs.workflow.bpmn.BPMNLinkedFlowIterator;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNNS;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.elements.Event;
import org.openbpmn.bpmn.elements.SequenceFlow;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.exceptions.BPMNValidationException;
import org.openbpmn.bpmn.navigation.BPMNEndElementIterator;
import org.openbpmn.bpmn.navigation.BPMNStartElementIterator;
import org.w3c.dom.Element;

/**
 * This {@code ModelManager} provides methods to get thread save instances of
 * {@code org.openbpmn.bpmn.BPMNModel} and provides convenience methods to
 * resolve model flow details.
 * <p>
 * The ModelManager is used by the {@link WorkflowKernel} to manage the
 * processing life cycle of a workitem.
 * <p>
 * A client can use the {@linke WorkflowContext} to resolve a valid model
 * version and fetch a thread save instance of a model. The model version is
 * defined in the item `$modelversion` of a workitem. This may also be a regular
 * expression. Also a valid model version can be resolved by the WorkflowContext
 * based on the $workflowGroup.
 * <p>
 * The ModelManager provides methods to get instances of ItemCollection for
 * Tasks and Events and other common BPMNElements.
 * 
 */
public class ModelManager {

    private Logger logger = Logger.getLogger(ModelManager.class.getName());

    public final static String TASK_ELEMENT = "task";
    public final static String EVENT_ELEMENT = "intermediateCatchEvent";
    public final static String PARALLELGATEWAY_ELEMENT = "parallelGateway";

    // cache
    private final Map<String, ItemCollection> bpmnEntityCache = new ConcurrentHashMap<>();
    private final Map<String, BPMNElement> bpmnElementCache = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> groupCache = new ConcurrentHashMap<>();

    private WorkflowContext workflowContext = null;
    private RuleEngine ruleEngine = null;

    // BPMNModel store
    private final ConcurrentHashMap<String, BPMNModel> modelStore = new ConcurrentHashMap<>();

    /**
     * Constructor initializes the RuleEngine.
     * 
     */
    public ModelManager(WorkflowContext workflowContext) {
        this.workflowContext = workflowContext;
    }

    /**
     * This method returns a thread save instance of a BPMNModel based on the given
     * workitem meta data. The method lookups the matching model version by the
     * {@link WorkflowContext}
     * 
     * @param workitem
     * @return
     * @throws ModelException
     */
    public BPMNModel getModelByWorkitem(ItemCollection workitem) throws ModelException {
        // resolve version
        String version = workflowContext.findModelVersionByWorkitem(workitem);
        return getModel(version);
    }

    /**
     * This method return a thread save instance of a BPMN Model. If the model does
     * not yet exist in the ModelManager a new model instance is fetched and cached
     * in the local model store.
     * 
     * @param version
     * @return
     * @throws ModelException
     */
    public BPMNModel getModel(String version) throws ModelException {
        BPMNModel model = modelStore.get(version);

        if (model == null) {
            // fetch thread save copy of a new BPMNModel instance
            model = workflowContext.fetchModel(version);
            modelStore.put(version, model);
        }
        return model;
    }

    /**
     * Returns an instance of the Imixs RuleEngine. The method is using a lazy
     * loading mechanism.
     * 
     * @return
     */
    private RuleEngine getRuleEngine() {
        if (ruleEngine == null) {
            ruleEngine = new RuleEngine();
        }
        return ruleEngine;
    }

    /**
     * Returns the BPMN Definition entity associated with a given workitem, based on
     * its attribute "$modelVersion". The definition holds the bpmn meta data.
     * <p>
     * The method throws a {@link ModelException} if no Process can be resolved
     * based on the given model information.
     * <p>
     * The method is called by the {@link WorkflowKernel} during the processing life
     * cycle to update the process group information.
     * 
     * @param workitem
     * @return BPMN Event entity - {@link ItemCollection}
     * @throws ModelException if no event was found
     */
    public ItemCollection loadDefinition(BPMNModel model) throws ModelException {
        String key = BPMNUtil.getVersion(model);
        ItemCollection result = (ItemCollection) bpmnEntityCache.computeIfAbsent(key, k -> lookupDefinition(model));
        // clone instance to protect for manipulation
        if (result != null) {
            return (ItemCollection) result.clone();
        }
        return null;
    }

    /**
     * Returns the BPMN Process entity associated with a given workitem, based on
     * its attributes "$modelVersion", "$taskID". The process holds the name for the
     * attribute $worklfowGroup
     * <p>
     * The taskID has to be unique in a process. The method throws a
     * {@link ModelException} if no Process can be resolved based on the given model
     * information.
     * <p>
     * The method is called by the {@link WorkflowKernel} during the processing life
     * cycle to update the process group information.
     * 
     * @param workitem
     * @return BPMN Event entity - {@link ItemCollection}
     * @throws ModelException if no event was found
     */
    public ItemCollection loadProcess(ItemCollection workitem, BPMNModel model) throws ModelException {

        String key = BPMNUtil.getVersion(model) + "~" + workitem.getTaskID();
        Activity task = (Activity) bpmnElementCache.computeIfAbsent(key,
                k -> lookupTaskElementByID(model, workitem.getTaskID()));
        BPMNProcess process = task.getBpmnProcess();
        ItemCollection result = new ItemCollection();
        result.setItemValue("id", process.getId());
        if (process.hasAttribute("name")) {
            result.setItemValue("name", process.getAttribute("name"));
        }

        return result;
    }

    /**
     * Returns the BPMN Task entity associated with a given workitem, based on its
     * attributes "$modelVersion" and "$taskID".
     * <p>
     * The method throws a {@link ModelException} if no Task can be resolved based
     * on the given model information.
     * <p>
     * The method is called by the {@link WorkflowKernel} during the the processing
     * life cycle.
     * 
     * @param workitem
     * @return BPMN Event entity - {@link ItemCollection}
     * @throws ModelException if no event was found
     */
    public ItemCollection loadTask(ItemCollection workitem, BPMNModel model) throws ModelException {

        ItemCollection task = findTaskByID(model, workitem.getTaskID());
        if (task == null) {
            throw new ModelException(ModelException.UNDEFINED_MODEL_ENTRY,
                    "task " + workitem.getTaskID() + " not defined in model '" + workitem.getModelVersion() + "'");
        }
        return task;
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

        // logger.info("...loadEvent " + workitem.getTaskID() + "." +
        // workitem.getEventID());
        ItemCollection event = findEventByID(model, workitem.getTaskID(), workitem.getEventID());
        // verify if the event is a valid processing event?
        if (event != null) {
            List<ItemCollection> allowedEvents = findEventsByTask(model, workitem.getTaskID());
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
     * Finds the next BPMN Element associated with a given event element. The
     * returned BPMN Element must either be an Activity (Task) element, an
     * Intermediate Catch Event (follow-up-event) or a End Event. The method must
     * not return any other BPMN elements (e.g. Gateways, Intermediate Throw
     * Events).
     * <p>
     * The method throws a {@link ModelException} if no Element can be resolved
     * based on the given model information.
     * 
     * @param event    - current event
     * @param workitem - current Workitem
     * @return a BPMN Element entity - {@link ItemCollection}
     * @throws ModelException          - if no valid element was found
     * @throws BPMNValidationException
     */
    public ItemCollection nextModelElement(BPMNModel model, ItemCollection event, ItemCollection workitem)
            throws ModelException {
        long l = System.currentTimeMillis();
        // lookup the current BPMN event element by its ID
        String id = event.getItemValueString("id");
        Event eventElement = (Event) model.findElementNodeById(id);
        String version = BPMNUtil.getVersion(model);
        String key = version + "~" + workitem.getTaskID() + "." + workitem.getEventID();

        // find next task or event.....
        BPMNLinkedFlowIterator<BPMNElementNode> elementNavigator;
        try {
            elementNavigator = new BPMNLinkedFlowIterator<BPMNElementNode>(
                    eventElement,
                    node -> ((BPMNUtil.isImixsTaskElement(node))
                            || (BPMNUtil.isImixsEventElement(node))
                            || (BPMNUtil.isParallelGatewayElement(node))),
                    condition -> evaluateCondition(condition, workitem));

            while (elementNavigator.hasNext()) {
                BPMNElementNode nextElement = elementNavigator.next();

                // if the navigator has still more elements it is an ambiguous sequence flow
                if (elementNavigator.hasNext()) {
                    throw new ModelException(ModelException.INVALID_MODEL,
                            "$modelversion " + version + " ambiguous sequence flow: " + workitem.getTaskID() + "."
                                    + workitem.getEventID());
                }
                // logger.info("nextModelElement " + key + " took " +
                // (System.currentTimeMillis() - l) + "ms");
                return BPMNEntityBuilder.build(nextElement);

            }
        } catch (BPMNValidationException e) {
            throw new ModelException(ModelException.INVALID_MODEL,
                    "$modelversion " + version + " invalid condition: " + e.getMessage());
        }
        return null;
    }

    /**
     * This method returns a sorted list of all workflow groups contained in an
     * Imixs BPMN model.
     * <p>
     * In case the model is a collaboration diagram, the method returns only group
     * names from private process instances (Pools)!
     * 
     * @param _model - BPMN model instance
     * @return list of workflow groups
     * @throws ModelException if model is undefined
     */
    public Set<String> findAllGroupsByModel(BPMNModel _model) throws ModelException {
        Set<String> result = null;

        if (_model == null) {
            throw new ModelException(ModelException.INVALID_MODEL,
                    "model is null!");
        }
        // test cache
        String version = BPMNUtil.getVersion(_model);
        result = groupCache.get(version);
        if (result == null) {
            result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

            // find Process containing matching the process group
            for (BPMNProcess _process : _model.getProcesses()) {
                if (isImixsProcess(_process)) {
                    String groupName = _process.getName();
                    result.add(groupName);
                }
            }

            if (result.size() == 0) {
                logger.warning("Model " + BPMNUtil.getVersion(_model)
                        + " does not contain valid process elements! Please check your model file!");
            }
            // finally cache the new group set
            groupCache.put(version, result);
        }

        // Create an immutable set from the sorted set
        return Set.copyOf(result);
    }

    /**
     * This method returns a sorted list of all unique workflow groups contained in
     * the model store.
     * <p>
     * In case the model is a collaboration diagram, the method returns only group
     * names from private process instances (Pools)!
     * 
     * @param group
     * @return
     * @throws ModelException
     */
    // public Set<String> findAllGroups() throws ModelException {
    // Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    // for (BPMNModel model : modelStore.values()) {
    // result.addAll(findAllGroupsByModel(model));
    // }
    // return result;
    // }

    /**
     * This method finds a Imixs task element by its ID (imixs:activityid). The
     * method returns a cloned Instance of the model entity to avoid manipulation by
     * the client.
     * <p>
     * The method implements an effectively generic, thread-safe way of a
     * compute-once, cache-for-later pattern.
     * 
     * @param model
     * @param taskID
     * @return
     */
    public ItemCollection findTaskByID(final BPMNModel model, int taskID) {
        String key = BPMNUtil.getVersion(model) + "~" + taskID;
        // Avoid recursive call chains and do not use computeIfAbsent here!
        if (bpmnEntityCache.containsKey(key)) {
            return (ItemCollection) bpmnEntityCache.get(key).clone();
        }
        ItemCollection result = lookupTaskByID(model, taskID);
        if (result != null) {
            bpmnEntityCache.put(key, result);
            return (ItemCollection) result.clone();
        }
        return null;
    }

    /**
     * This method finds a Imixs Event element by its ID (imixs:activityid)
     * associated with a given Task. The method returns a cloned Instance of the
     * model entity to avoid manipulation by the client.
     * <p>
     * The method implements an effectively generic, thread-safe way of a
     * compute-once, cache-for-later pattern.
     * 
     */
    public ItemCollection findEventByID(final BPMNModel model, int taskID, int eventID) {
        String key = BPMNUtil.getVersion(model) + "~" + taskID + "." + eventID;
        // Avoid recursive call chains and do not use computeIfAbsent here!
        if (bpmnEntityCache.containsKey(key)) {
            return (ItemCollection) bpmnEntityCache.get(key).clone();
        }
        ItemCollection result = lookupEventByID(model, taskID, eventID);
        if (result != null) {
            bpmnEntityCache.put(key, result);
            return (ItemCollection) result.clone();
        }
        return null;
    }

    /**
     * Returns a list of all Tasks of a given Process Group
     * <p>
     * In case of a collaboration diagram only Pool names are compared. The default
     * process (Public Process) will be ignored if it does not contain at least one
     * start task.
     * 
     * @param model        - the BPMN model
     * @param processGroup - the name of the process group to match
     * @return list of all Task entities or null if not process with the given name
     *         was found.
     * @throws BPMNModelException
     */
    public List<ItemCollection> findTasks(final BPMNModel model, String processGroup) throws ModelException {
        List<ItemCollection> result = new ArrayList<>();
        BPMNProcess process = null;

        if (model == null) {
            throw new ModelException(ModelException.INVALID_MODEL,
                    "model is null!");
        }
        if (processGroup == null || processGroup.isEmpty()) {
            logger.warning("findEndTasks processGroup is empty!");
            return result;
        }

        // find Process containing matching the process group
        Set<BPMNProcess> processList = model.getProcesses();
        for (BPMNProcess _process : processList) {
            if (isImixsProcess(_process)) {
                if (processGroup.equals(_process.getName())) {
                    process = _process;
                    break;
                }
            }
        }

        // test Imixs tasks....
        if (process != null) {
            Set<Activity> activities = process.getActivities();
            for (Activity activity : activities) {
                if (BPMNUtil.isImixsTaskElement(activity)) {
                    result.add(BPMNEntityBuilder.build(activity));
                }
            }
            // sort result by taskID
            Collections.sort(result, new ItemCollectionComparator(BPMNUtil.TASK_ITEM_TASKID, true));
        }
        return result;
    }

    /**
     * Returns a list of all executable Imixs Processes in a BPMN Model.
     * 
     * @param _model
     * @return List of BPMNProcess that contain at least one Imixs Task Element
     */
    public List<BPMNProcess> findAllImixsProcesses(BPMNModel _model) {
        List<BPMNProcess> result = new ArrayList<>();
        // find Process containing matching the process group
        for (BPMNProcess _process : _model.getProcesses()) {
            try {
                if (isImixsProcess(_process)) {
                    result.add(_process);
                }
            } catch (ModelException e) {
                // invalid process
            }
        }
        return result;
    }

    /**
     * This method returns true if the geiven process contains at least one Imixs
     * Task Element. Only in this case the process is executable at all.
     * 
     * @param _process
     * @return
     * @throws ModelException
     * 
     */
    public boolean isImixsProcess(BPMNProcess _process) throws ModelException {
        if (_process != null) {
            try {
                _process.init();
                BPMNStartElementIterator<Activity> startElements = new BPMNStartElementIterator<>(_process,
                        node -> (BPMNUtil.isImixsTaskElement(node)));
                return startElements.hasNext();
            } catch (BPMNModelException e) {
                throw new ModelException(ModelException.INVALID_MODEL,
                        "Invalid process model: " + e.getMessage(), e);
            }
        }
        return false;
    }

    /**
     * Returns a list of start Tasks of a given Process Group
     * <p>
     * In case of a collaboration diagram only Pool names are compared. The default
     * process (Public Process) will be ignored.
     * 
     * @param model        - the BPMN model
     * @param processGroup - the name of the process group to match
     * @return list of all Start Task entities or null if not process with the given
     *         name was found.
     * @throws BPMNModelException
     */
    public List<ItemCollection> findStartTasks(final BPMNModel model, String processGroup) throws ModelException {
        List<ItemCollection> result = new ArrayList<>();
        BPMNProcess process = null;

        if (model == null) {
            throw new ModelException(ModelException.INVALID_MODEL,
                    "model is null!");
        }
        if (processGroup == null || processGroup.isEmpty()) {
            logger.warning("findEndTasks processGroup is empty!");
            return result;
        }

        // find Process containing matching the process group name
        Set<BPMNProcess> processList = model.getProcesses();
        for (BPMNProcess _process : processList) {
            if (isImixsProcess(_process)) {
                logger.fine("process name=" + _process.getName());
                if (processGroup.equals(_process.getName())) {
                    process = _process;
                    break;
                }
            }
        }

        // test start task....
        if (process != null) {
            BPMNStartElementIterator<Activity> startElements = new BPMNStartElementIterator<>(process,
                    node -> (BPMNUtil.isImixsTaskElement(node)));
            while (startElements.hasNext()) {
                result.add(BPMNEntityBuilder.build(startElements.next()));
            }
        }

        return result;
    }

    /**
     * Returns a list of End Tasks of a given Process Group
     * <p>
     * In case of a collaboration diagram only Pool names are compared. The default
     * process (Public Process) will be ignored.
     * 
     * @param model        - the BPMN model
     * @param processGroup - the name of the process group to match
     * @return list of all End Task entities or null if not process with the given
     *         name was found.
     * @throws BPMNModelException
     */
    public List<ItemCollection> findEndTasks(final BPMNModel model, String processGroup) throws ModelException {
        List<ItemCollection> result = new ArrayList<>();
        BPMNProcess process = null;

        if (model == null) {
            throw new ModelException(ModelException.INVALID_MODEL,
                    "model is null!");
        }
        if (processGroup == null || processGroup.isEmpty()) {
            logger.warning("findEndTasks processGroup is empty!");
            return result;
        }

        // find Process containing matching the process group
        Set<BPMNProcess> processList = model.getProcesses();
        for (BPMNProcess _process : processList) {
            if (isImixsProcess(_process)) {
                if (processGroup.equals(_process.getName())) {
                    process = _process;
                    break;
                }
            }
        }

        // now get the End task ...
        if (process != null) {
            // test End task....
            BPMNEndElementIterator<Activity> endElements = new BPMNEndElementIterator<>(process,
                    node -> (BPMNUtil.isImixsTaskElement(node)));
            while (endElements.hasNext()) {
                result.add(BPMNEntityBuilder.build(endElements.next()));
            }
        }

        return result;
    }

    /**
     * Returns a list of all Events assigned to a Task. The event can be either
     * connected by an outgoing sequence flow or by an ingoing sequence flow
     * (init-event-node).
     * 
     * @param model
     * @param taskID
     * @return list of all events assigned to the task as an processing event.
     * 
     */
    public List<ItemCollection> findEventsByTask(final BPMNModel model, int taskID) {
        List<ItemCollection> result = new ArrayList<>();
        Activity taskElement = lookupTaskElementByID(model, taskID);
        if (taskElement != null) {
            BPMNLinkedFlowIterator<BPMNElementNode> elementNavigator = new BPMNLinkedFlowIterator<BPMNElementNode>(
                    taskElement,
                    node -> ((BPMNUtil.isImixsEventElement(node))));

            while (elementNavigator.hasNext()) {
                result.add(BPMNEntityBuilder.build(elementNavigator.next()));
            }
            // next we also add all initEvent nodes
            List<BPMNElementNode> initEventNodes = findInitEventNodes(taskElement);
            for (BPMNElementNode element : initEventNodes) {
                result.add(BPMNEntityBuilder.build(element));
            }
        }
        return result;
    }

    /**
     * Returns the content of a BPMN DataObject, part of a Task or Event element.
     * <p>
     * DataObjects can be associated in a BPMN Diagram with a Task or an Event
     * element.
     * 
     * @param bpmnElement - bpmn element, either a Task or a Event
     * @param name        - name of the dataobject
     * @return
     */
    @SuppressWarnings("unchecked")
    public String findDataObject(ItemCollection bpmnElement, String name) {

        List<List<String>> dataObjects = bpmnElement.getItemValue("dataObjects");

        if (dataObjects != null && dataObjects.size() > 0) {
            for (List<String> dataObject : dataObjects) {
                String key = dataObject.get(0);
                if (name.equals(key)) {
                    return dataObject.get(1);
                }
            }
        }
        // not found!
        return null;

    }

    /**
     * Evaluates a BPMN sequence flow condition with extensible observer support.
     * 
     * 
     * @param expression the condition expression from a BPMN SequenceFlow. Can be
     *                   JavaScript code or a prompt for an observer.
     * @param workitem   the workflow item providing context data for condition
     *                   evaluation
     * @return true if the condition evaluates to true, false otherwise
     */
    public boolean evaluateCondition(String expression, ItemCollection workitem) {

        // delegate expression to the runtime
        String finalExpression = this.workflowContext.evalConditionalExpression(expression, workitem);
        try {
            return getRuleEngine().evaluateBooleanExpression(finalExpression, workitem);
        } catch (PluginException e) {
            e.printStackTrace();
            logger.severe("Failed to evaluate Condition: " + e.getMessage());
        }
        return false;
    }

    /**
     * This method evaluates the outgoing sequenceFlows of a ParallelGateway.
     * <p>
     * The method returns true if the given targetNode is connected with a
     * TASK_ELEMENT or with a EVENT_ELEMENT by a conditional flow that evaluates to
     * true. These are the only two cases that indicate the Main Version flow!
     * 
     * @param gatewayNode - ParallelGateway Node
     * @param targetNode  - Target Node (either a Task or a Event)
     * @param workitem    - current workitem
     * @return true if targetNode defines the main flow
     */
    public boolean isMainParallelGatewayFlow(BPMNElementNode gatewayNode, BPMNElementNode targetNode,
            ItemCollection workitem) {

        if (ModelManager.TASK_ELEMENT.equals(targetNode.getType())) {
            return true;
        }
        // find SequenceFlow
        Set<SequenceFlow> outFlows = gatewayNode.getOutgoingSequenceFlows();
        for (SequenceFlow outFlow : outFlows) {
            if (outFlow.getTargetElement().getId().equals(targetNode.getId())) {
                String condition = outFlow.getConditionExpression();
                if (condition != null && !condition.isEmpty()) {
                    try {
                        boolean conditionResult = getRuleEngine().evaluateBooleanExpression(condition, workitem);
                        if (conditionResult == true) {
                            return true;
                        }
                    } catch (PluginException e) {
                        logger.severe("Failed to evaluate Condition of SplitEvent '" + gatewayNode.getId() + "': "
                                + e.getMessage());
                    }
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * This method lookups the definitions element and returns a ItemCollection
     * holding all attributes including the Imixs Extension attributes.
     * 
     * BPMN default attributes: id, exporter, exporterVersion, targetNamespace
     * 
     * @param model
     * @param taskID
     * @return
     */
    private ItemCollection lookupDefinition(final BPMNModel model) {
        long l = System.currentTimeMillis();
        ItemCollection result = new ItemCollection();
        Element definition = model.getDefinitions();

        result.setItemValue("id", definition.getAttribute("id"));
        result.setItemValue("exporter", definition.getAttribute("exporter"));
        result.setItemValue("exporterVersion", definition.getAttribute("exporterVersion"));
        result.setItemValue("targetNamespace", definition.getAttribute("targetNamespace"));
        result.setItemValue("name", "environment.profile");
        result.setItemValue("txtname", "environment.profile");
        result.setItemValue("type", "WorkflowEnvironmentEntity");

        Element extensionElement = model.findChildNodeByName(definition,
                BPMNNS.BPMN2, "extensionElements");
        Set<Element> imixsExtensionElements = BPMNUtil.findAllImixsElements(extensionElement, "item");
        // iterate through set and verify the name attribute
        for (Element extensionItem : imixsExtensionElements) {
            String itemName = extensionItem.getAttribute("name");
            if (itemName != null && !itemName.isEmpty()) {
                // found
                List<?> itemValueList = BPMNUtil.getItemValueList(extensionItem);
                result.setItemValue(itemName, itemValueList);
            }
        }
        // convert model version new item name
        result.setItemValue(WorkflowKernel.MODELVERSION, result.getItemValueString("txtworkflowmodelversion"));

        // logger.info("lookupDefinition " +
        // result.getItemValueString("txtworkflowmodelversion") + " took "
        // + (System.currentTimeMillis() - l) + "ms");
        return result;
    }

    /**
     * This method lookups an Imixs BPMN Task element by a given TaskID.
     * 
     * @param model
     * @param taskID
     * @return
     */
    private Activity lookupTaskElementByID(final BPMNModel model, int taskID) {
        if (model == null) {
            return null;
        }
        Set<Activity> activities = model.findAllActivities();
        // filter the imixs activity with the corresponding id
        for (Activity activity : activities) {
            // imixs:processid="10"
            String id = activity.getExtensionAttribute(BPMNUtil.getNamespace(), "processid");
            try {
                if (taskID == Long.parseLong(id)) {
                    return activity;
                }
            } catch (NumberFormatException e) {
                logger.warning(activity.getId() + " invalid attribute 'imixs:processid' = " + id + "  Number expected");
            }
        }
        return null;
    }

    /**
     * This method lookups an Imixs BPMN Task element by a given TaskID represented
     * in an ItemCollection.
     * 
     * @param model
     * @param taskID
     * @return
     */
    private ItemCollection lookupTaskByID(final BPMNModel model, int taskID) {
        String key = BPMNUtil.getVersion(model) + "~" + taskID;
        Activity activity = (Activity) bpmnElementCache.computeIfAbsent(key, k -> lookupTaskElementByID(model, taskID));
        if (activity != null) {
            return BPMNEntityBuilder.build(activity);
        } else {
            return null;
        }
    }

    /**
     * This method finds a Imixs event element by its ID (imixs:activityid)
     * associated with a given Task
     * 
     * @param model
     * @param taskID
     * @param eventID
     * @return
     */
    private ItemCollection lookupEventByID(final BPMNModel model, int taskID, int eventID) {
        Event event = null;
        String key = BPMNUtil.getVersion(model) + "~" + taskID + "." + eventID;
        // Avoid recursive call chains and do not use computeIfAbsent here!
        if (bpmnElementCache.containsKey(key)) {
            event = (Event) bpmnElementCache.get(key);
            return BPMNEntityBuilder.build(event);
        }
        event = lookupEventElementByID(model, taskID, eventID);
        if (event != null) {
            bpmnElementCache.put(key, event);
            return BPMNEntityBuilder.build(event);
        }
        return null;
    }

    /**
     * This method finds a Imixs event element by its ID (imixs:activityid)
     * associated with a given Task
     * 
     * This includes all follow up events.
     * 
     * @param model
     * @param taskID
     * @param eventID
     * @return
     */
    private Event lookupEventElementByID(final BPMNModel model, int taskID, int eventID) {
        long l = System.currentTimeMillis();
        String version = BPMNUtil.getVersion(model);
        String keyTask = version + "~" + taskID;
        String keyEvent = version + "~" + taskID + "." + eventID;
        Activity task = (Activity) bpmnElementCache.computeIfAbsent(keyTask, k -> lookupTaskElementByID(model, taskID));
        if (task == null) {
            logger.warning("TaskID: " + taskID + " does not exist in model '" + version + "'!");
            return null;
        }
        // find all directly associated Events to the current Task...
        BPMNLinkedFlowIterator<Event> eventNavigator = new BPMNLinkedFlowIterator<Event>(task,
                node -> (BPMNUtil.isImixsEventElement(node)));

        if (eventNavigator != null) {
            while (eventNavigator.hasNext()) {
                Event event = (Event) eventNavigator.next();
                String id = event.getExtensionAttribute(BPMNUtil.getNamespace(), "activityid");
                if (id != null && !id.isEmpty()) {
                    try {
                        if (eventID == Long.parseLong(id)) {
                            // logger.info(
                            // "lookupEventByID " + keyEvent + " took " + (System.currentTimeMillis() - l) +
                            // "ms");
                            return event;
                        }
                    } catch (NumberFormatException e) {
                        logger.warning(
                                event.getId() + " invalid attribute 'imixs:activityid' = " + id + "  Number expected");
                    }
                }
            }
        }

        // not yet found, collect all incoming events...
        // List<Event> allIncomingEvents = new ArrayList<>();
        // BPMNUtil.findAllIncomingEventNodes(task, allIncomingEvents);

        List<BPMNElementNode> allIncomingEvents = findInitEventNodes(task);

        // for (Event inEvent : allIncomingEvents) {
        for (BPMNElementNode inEvent : allIncomingEvents) {
            // Event inEvent = (Event)
            // model.findElementNodeById(initEventItemcol.getItemValueString("id"));
            String id = inEvent.getExtensionAttribute(BPMNUtil.getNamespace(), "activityid");
            if (id == null || id.isEmpty()) {
                continue; // no match...
            }
            try {
                if (eventID == Long.parseLong(id)) {
                    // match!
                    // logger.info(
                    // "lookupEventByID " + keyEvent + " took " + (System.currentTimeMillis() - l) +
                    // "ms");
                    return (Event) inEvent;
                }
            } catch (NumberFormatException e) {
                logger.warning(
                        inEvent.getId() + " invalid attribute 'imixs:activityid' = " + id + "  Number expected");
            }
        }
        // not found!
        return null;
    }

    /**
     * Iterates tough all ingoing sequence flows and tests if the source element is
     * a so called Init-Event. An Init-Event is an Imixs Event with no incoming
     * nodes or with one incoming node that comes direct from a Start event.
     * <p>
     * If a source element is an Event and has a predecessor event the method calls
     * itself recursive.
     * 
     * @param currentNode
     */
    private List<BPMNElementNode> findInitEventNodes(BPMNElementNode currentNode) {
        List<BPMNElementNode> collector = new ArrayList<>();
        // logger.info("findInitEventNodes for element " + currentNode.getId() + "
        // type=" + currentNode.getType());
        Set<SequenceFlow> flowSet = currentNode.getIngoingSequenceFlows();
        for (SequenceFlow flow : flowSet) {
            BPMNElementNode element = flow.getSourceElement();
            // logger.info("verify element " + element.getId() + " type=" +
            // element.getType());
            if (BPMNUtil.isInitEventNode(element)) {
                collector.add(element);
            } else if (element != null && BPMNUtil.isImixsEventElement(element)) {
                // is the source an Imixs event node?
                // recursive call....
                List<BPMNElementNode> subResult = findInitEventNodes(element);
                collector.addAll(subResult);
            }
        }
        return collector;
    }
}
