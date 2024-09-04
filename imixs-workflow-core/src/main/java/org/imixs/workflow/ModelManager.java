package org.imixs.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.imixs.workflow.bpmn.BPMNEntityBuilder;
import org.imixs.workflow.bpmn.BPMNLinkedFlowIterator;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNNS;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.elements.Event;
import org.openbpmn.bpmn.elements.Participant;
import org.openbpmn.bpmn.elements.SequenceFlow;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.exceptions.BPMNValidationException;
import org.openbpmn.bpmn.navigation.BPMNEndElementIterator;
import org.openbpmn.bpmn.navigation.BPMNStartElementIterator;
import org.w3c.dom.Element;

/**
 * This {@code ModelManager} provides methods to get model entities from a
 * model instance. The ModelManager is used by the {@link WorkflowKernel} to
 * manage the processing life cycle of a workitem.
 * The implementation is based on the OpenBPMN Meta model.
 * <p>
 * By analyzing the workitem model version the WorkflowKernel determines the
 * corresponding model and get the Tasks and Events from the ModelManager to
 * process the workitem and assign the workitem to the next Task defined by the
 * BPMN Model.
 * 
 */
public class ModelManager {

    private Logger logger = Logger.getLogger(ModelManager.class.getName());

    public final static String TASK_ELEMENT = "task";
    public final static String EVENT_ELEMENT = "intermediateCatchEvent";
    public final static String PARALLELGATEWAY_ELEMENT = "parallelGateway";

    // Model store
    private final Map<String, BPMNModel> modelStore = new ConcurrentHashMap<>();

    // cache
    private final Map<String, ItemCollection> bpmnEntityCache = new ConcurrentHashMap<>();
    private final Map<String, BPMNElement> bpmnElementCache = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> groupCache = new ConcurrentHashMap<>();

    private RuleEngine ruleEngine = null;

    /**
     * Private constructor to prevent instantiation
     */
    public ModelManager() {
        ruleEngine = new RuleEngine();
    }

    /**
     * Returns the internal ModelStore holding all BPMNModels by version.
     * 
     * @return
     */
    public Map<String, BPMNModel> getModelStore() {
        return modelStore;
    }

    /**
     * Adds a new model into the local model store
     */
    public void addModel(BPMNModel model) throws ModelException {
        String version = BPMNUtil.getVersion(model);
        modelStore.put(version, model);
        clearCache();
    }

    /**
     * Removes a BPMNModel form the local model store
     */
    public void removeModel(String version) {
        modelStore.remove(version);
        clearCache();
    }

    /**
     * Returns a BPMNModel by its version from the local model store
     * 
     * @param version - a bpmn model version ($modelVersion)
     * @return a BPMNModel instance.
     */
    public BPMNModel getModel(String version) throws ModelException {
        return modelStore.get(version);
    }

    /**
     * Returns a List with all BPMNModel instances
     * 
     * @return
     */
    public List<BPMNModel> getAllModels() {
        List<BPMNModel> result = new ArrayList<>();
        result.addAll(modelStore.values());
        return result;
    }

    public BPMNModel getModelByWorkitem(ItemCollection workitem) throws ModelException {
        return this.findModelByWorkitem(workitem);
    }

    /**
     * Returns the BPMN Definition entity associated with a given workitem, based on
     * its attribute "$modelVersion". The definition holds the bpmn meta data.
     * <p>
     * The method throws a {@link ModelException} if no Process can be resolved
     * based on the given model information.
     * <p>
     * The method is called by the {@link WorkflowKernel} during the processing
     * life cycle to update the process group information.
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
     * its attributes "$modelVersion", "$taskID". The process holds the name
     * for the attribute $worklfowGroup
     * <p>
     * The taskID has to be unique in a process. The method throws a
     * {@link ModelException} if no Process can be resolved based on the given model
     * information.
     * <p>
     * The method is called by the {@link WorkflowKernel} during the processing
     * life cycle to update the process group information.
     * 
     * @param workitem
     * @return BPMN Event entity - {@link ItemCollection}
     * @throws ModelException if no event was found
     */
    public ItemCollection loadProcess(ItemCollection workitem) throws ModelException {
        BPMNModel model = findModelByWorkitem(workitem);
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
    public ItemCollection loadTask(ItemCollection workitem) throws ModelException {
        BPMNModel model = findModelByWorkitem(workitem);
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
     * A Event is typically connected with a Task by outgoing SequenceFlows.
     * A special case is a Start event followed by one or more Events connected with
     * a Task (Start-Task) element.
     * 
     * @param workitem
     * @return BPMN Event entity - {@link ItemCollection}
     * @throws ModelException if no event was found
     */
    public ItemCollection loadEvent(ItemCollection workitem) throws ModelException {

        BPMNModel model = findModelByWorkitem(workitem);
        logger.info("...loadEvent " + workitem.getTaskID() + "." + workitem.getEventID());
        ItemCollection event = findEventByID(model, workitem.getTaskID(), workitem.getEventID());
        // verify if the event is a valid processing event?
        if (event != null) {
            List<ItemCollection> allowedEvents = findEventsByTask(model, workitem.getTaskID());
            boolean found = false;
            for (ItemCollection allowedEvent : allowedEvents) {
                logger.info("verify event : " + allowedEvent.getItemValueString("id"));
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
                    + workitem.getEventID() + " is not a callable in model '" + workitem.getModelVersion() + "'");
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
    public ItemCollection nextModelElement(ItemCollection event, ItemCollection workitem)
            throws ModelException {
        long l = System.currentTimeMillis();
        BPMNModel model = findModelByWorkitem(workitem);
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
                logger.info("nextModelElement " + key + " took " + (System.currentTimeMillis() - l) + "ms");
                return BPMNEntityBuilder.build(nextElement);

            }
        } catch (BPMNValidationException e) {
            throw new ModelException(ModelException.INVALID_MODEL,
                    "$modelversion " + version + " invalid condition: " + e.getMessage());
        }
        return null;
    }

    /**
     * returns a sorted String list of all stored model versions
     * 
     * @return
     */
    public List<String> getVersions() {
        Set<String> versions = modelStore.keySet();
        // convert to List
        List<String> result = new ArrayList<>();
        result.addAll(versions);
        Collections.sort(result, Collections.reverseOrder());
        return result;
    }

    /**
     * Returns a Model matching the $modelversion of a given workitem. The
     * $modelversion can optional be provided as a regular expression.
     * <p>
     * In case no matching model version exits, the method tries to find the highest
     * Model Version matching the corresponding workflow group.
     * <p>
     * The method throws a ModelException in case the model version did not exits.
     **/
    public BPMNModel findModelByWorkitem(ItemCollection workitem) throws ModelException {
        BPMNModel result = null;
        String version = workitem.getModelVersion();
        // first try a direct fetch....
        if (version != null && !version.isEmpty()) {
            result = modelStore.get(version);
        }
        if (result != null) {
            return result;
        } else {
            // try to find model by regex if version is not empty...
            if (version != null && !version.isEmpty()) {
                Set<String> matchingVersions = findVersionsByRegEx(version);
                for (String matchingVersion : matchingVersions) {
                    result = modelStore.get(matchingVersion);
                    if (result != null) {
                        // match
                        // update $modelVersion
                        logger.info("Update $modelversion by regex " + version + " ▷ " + matchingVersion);
                        workitem.model(matchingVersion);
                        return result;
                    }
                }
            }

            // Still no match, try to find model version by group
            if (!workitem.getWorkflowGroup().isEmpty()) {
                Set<String> versions = findAllVersionsByGroup(workitem.getWorkflowGroup());
                if (!versions.isEmpty()) {
                    String newVersion = versions.iterator().next();
                    if (!newVersion.isEmpty()) {
                        logger.log(Level.WARNING, "Deprecated model version: ''{0}'' -> migrating to ''{1}'',"
                                + "  $workflowgroup: ''{2}'', $uniqueid: {3}",
                                new Object[] { version, newVersion, workitem.getWorkflowGroup(),
                                        workitem.getUniqueID() });
                    }
                    // update $modelVersion
                    workitem.model(newVersion);
                    result = modelStore.get(newVersion);
                    return result;
                }
            }

            // no match!
            throw new ModelException(ModelException.UNDEFINED_MODEL_VERSION,
                    "$modelversion '" + version + "' not found");
        }
    }

    /**
     * This method returns a sorted list of all workflow groups contained in a BPMN
     * model.
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

            Set<BPMNProcess> processList = _model.getProcesses();
            for (BPMNProcess _process : processList) {
                String groupName = _process.getName();
                if (_model.isCollaborationDiagram()) {
                    // collaboration diagram - only add private processes (Pools)
                    if (BPMNTypes.PROCESS_TYPE_PRIVATE.equals(_process.getProcessType())) {
                        // add only private process types
                        result.add(groupName);
                    }
                } else {
                    // if it is not a collaboration diagram we return the name of the first Public
                    // Process
                    if (BPMNTypes.PROCESS_TYPE_PUBLIC.equals(_process.getProcessType())) {
                        result.add(groupName);
                        break;
                    }
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
    public Set<String> findAllGroups() throws ModelException {
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (BPMNModel model : modelStore.values()) {
            result.addAll(findAllGroupsByModel(model));
        }
        return result;
    }

    /**
     * Returns a sorted list of all model versions containing the requested
     * workflow group. The result is sorted in reverse order, so the highest version
     * number is the first in the result list.
     * 
     * @param group - name of the workflow group
     * @return list of matching model versions
     * @throws ModelException
     */
    public Set<String> findAllVersionsByGroup(String group) throws ModelException {
        boolean debug = logger.isLoggable(Level.FINE);
        // Sorted in reverse order
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (debug) {
            logger.log(Level.FINEST, "......searching model versions for workflowgroup ''{0}''...", group);
        }
        // try to find matching model version by group
        Collection<BPMNModel> models = modelStore.values();
        for (BPMNModel _model : models) {
            Set<String> allGroups = findAllGroupsByModel(_model);
            if (allGroups.contains(group)) {
                result.add(BPMNUtil.getVersion(_model));
            }
        }

        return result;
    }

    /**
     * Returns the first matching model version by a given workflow group.
     * <p>
     * If multiple models are containing the same group the latest version will be
     * returned.
     * <p>
     * In case the model is a collaboration diagram, the method compares the given
     * group name only with private process instances (Pools)!
     * 
     * @param group
     * @return
     * @throws ModelException
     */
    public String findVersionByGroup(String group) throws ModelException {
        String result = null;
        Set<String> versions = findAllVersionsByGroup(group);
        if (versions.size() > 0) {
            result = versions.iterator().next();
        }
        return result;
    }

    /**
     * This method returns a sorted list of model versions matching a given regex
     * for a model version. The result is sorted in reverse order, so the highest
     * version number is the first in the result list.
     * 
     * @param group
     * @return
     */
    public Set<String> findVersionsByRegEx(String modelRegex) {
        boolean debug = logger.isLoggable(Level.FINE);
        // List<String> result = new ArrayList<String>();
        // Sorted in reverse order
        Set<String> result = new TreeSet<>(Collections.reverseOrder());
        if (debug) {
            logger.log(Level.FINEST, "......searching model versions for regex ''{0}''...", modelRegex);
        }
        // try to find matching model version by regex
        Collection<BPMNModel> models = modelStore.values();
        for (BPMNModel amodel : models) {
            String _version = BPMNUtil.getVersion(amodel);
            if (Pattern.compile(modelRegex).matcher(_version).find()) {
                result.add(_version);
            }
        }
        return result;
    }

    /**
     * This method finds a Imixs task element by its ID (imixs:activityid).
     * The method returns a cloned Instance of the model entity to avoid
     * manipulation by the client.
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

        // find Process containing matching the process group
        if (model.isCollaborationDiagram()) {
            Set<Participant> poolList = model.getParticipants();
            for (Participant pool : poolList) {
                process = pool.openProcess();
                if (processGroup.equals(process.getName())) {
                    break;
                }
            }
        } else {
            process = model.openDefaultProces();
            if (!processGroup.equals(process.getName())) {
                // no match!
                process = null;
            }
        }

        // test start task....
        BPMNStartElementIterator<Activity> startElements = new BPMNStartElementIterator<>(process,
                node -> (node instanceof Activity));
        while (startElements.hasNext()) {
            result.add(BPMNEntityBuilder.build(startElements.next()));
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
        if (model.isCollaborationDiagram()) {
            Set<Participant> poolList = model.getParticipants();
            for (Participant pool : poolList) {
                process = pool.openProcess();
                if (processGroup.equals(process.getName())) {
                    break;
                }
            }
        } else {
            process = model.openDefaultProces();
            if (!processGroup.equals(process.getName())) {
                // no match!
                process = null;
            }
        }

        // now get the End task ...
        if (process != null) {
            // test End task....
            BPMNEndElementIterator<Activity> endElements = new BPMNEndElementIterator<>(process,
                    node -> (node instanceof Activity));
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
        Activity taskElement = lookupTaskElementByID(model, taskID);
        BPMNLinkedFlowIterator<BPMNElementNode> elementNavigator = new BPMNLinkedFlowIterator<BPMNElementNode>(
                taskElement,
                node -> ((BPMNUtil.isImixsEventElement(node))));
        List<ItemCollection> result = new ArrayList<>();
        while (elementNavigator.hasNext()) {
            result.add(BPMNEntityBuilder.build(elementNavigator.next()));
        }
        // next we also add all initEvent nodes
        List<BPMNElementNode> initEventNodes = findInitEventNodes(taskElement);

        logger.info("Final Result of findInitEventNodes:");
        for (BPMNElementNode e : initEventNodes) {
            logger.info("     " + e.getId());
        }

        for (BPMNElementNode element : initEventNodes) {
            result.add(BPMNEntityBuilder.build(element));
        }
        // result.addAll(initEventNodes);

        logger.info("Final Result of findEventsByTask:");
        for (ItemCollection e : result) {
            logger.info("     " + e.getItemValueString("id"));
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
     * Evaluates a condition
     * 
     * @param expression
     * @param workitem
     * @return
     */
    public boolean evaluateCondition(String expression, ItemCollection workitem) {
        try {
            return ruleEngine.evaluateBooleanExpression(expression, workitem);
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
     * true.
     * These are the only two cases that indicate the Main Version flow!
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
                        // RuleEngine ruleEngine = new RuleEngine();
                        boolean conditionResult = ruleEngine.evaluateBooleanExpression(condition, workitem);
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
     * Reset the internal BPMN Element cache
     */
    private void clearCache() {
        bpmnEntityCache.clear();
        bpmnElementCache.clear();
        groupCache.clear();
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

        logger.info("lookupDefinition " + result.getItemValueString("txtworkflowmodelversion") + " took "
                + (System.currentTimeMillis() - l) + "ms");
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
        long l = System.currentTimeMillis();
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
                            logger.info(
                                    "lookupEventByID " + keyEvent + " took " + (System.currentTimeMillis() - l) + "ms");
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
                    logger.info(
                            "lookupEventByID " + keyEvent + " took " + (System.currentTimeMillis() - l) + "ms");
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
        logger.info("findInitEventNodes for  element " + currentNode.getId() + " type=" + currentNode.getType());
        Set<SequenceFlow> flowSet = currentNode.getIngoingSequenceFlows();
        for (SequenceFlow flow : flowSet) {
            BPMNElementNode element = flow.getSourceElement();
            logger.info("verify element " + element.getId() + " type=" + element.getType());
            if (BPMNUtil.isInitEventNode(element)) {
                // collector.add(BPMNEntityBuilder.build(element));
                collector.add(element);
                // collector.add((Event) element);
            } else if (element != null && BPMNUtil.isImixsEventElement(element)) {
                // is the source an Imixs event node?
                // recursive call....
                List<BPMNElementNode> subResult = findInitEventNodes(element);
                collector.addAll(subResult);
            }
        }

        logger.info("Result of findInitEventNodes:");
        for (BPMNElementNode e : collector) {
            logger.info("     " + e.getId());
        }
        return collector;
    }
}
