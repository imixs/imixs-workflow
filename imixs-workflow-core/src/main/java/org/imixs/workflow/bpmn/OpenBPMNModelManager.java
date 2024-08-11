package org.imixs.workflow.bpmn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.RuleEngine;
import org.imixs.workflow.WorkflowKernel;
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
import org.openbpmn.bpmn.navigation.BPMNFlowIterator;
import org.openbpmn.bpmn.navigation.BPMNStartElementIterator;
import org.w3c.dom.Element;

/**
 * This OpenBPMNModelManager implements the Interface ModelManger to handle Open
 * BPMN Models. The implementation is based on the OpenBPMN Meta model.
 * <p>
 * By analyzing the workitem model version the WorkflowKernel determines the
 * corresponding model and get the Tasks and Events from the ModelManager to
 * process the workitem and assign the workitem to the next Task defined by the
 * BPMN Model.
 * 
 */
public class OpenBPMNModelManager implements ModelManager {

    private Logger logger = Logger.getLogger(OpenBPMNModelManager.class.getName());

    // Model store
    private final Map<String, BPMNModel> modelStore = new ConcurrentHashMap<>();

    // cache
    private final Map<String, ItemCollection> bpmnEntityCache = new ConcurrentHashMap<>();
    private final Map<String, BPMNElement> bpmnElementCache = new ConcurrentHashMap<>();

    private RuleEngine ruleEngine = null;

    /**
     * Private constructor to prevent instantiation
     */
    public OpenBPMNModelManager() {
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
    @Override
    public void addModel(BPMNModel model) throws ModelException {
        String version = OpenBPMNUtil.getVersion(model);
        modelStore.put(version, model);
        clearCache();
    }

    /**
     * Removes a BPMNModel form the local model store
     */
    @Override
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
    @Override
    public BPMNModel getModel(String version) throws ModelException {
        return modelStore.get(version);
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
    @Override
    public ItemCollection loadDefinition(ItemCollection workitem) throws ModelException {
        BPMNModel model = findModelByWorkitem(workitem);
        String key = OpenBPMNUtil.getVersion(model);
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
    @Override
    public ItemCollection loadProcess(ItemCollection workitem) throws ModelException {
        BPMNModel model = findModelByWorkitem(workitem);
        String key = OpenBPMNUtil.getVersion(model) + "~" + workitem.getTaskID();
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
    @Override
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
     * 
     * @param workitem
     * @return BPMN Event entity - {@link ItemCollection}
     * @throws ModelException if no event was found
     */
    @Override
    public ItemCollection loadEvent(ItemCollection workitem) throws ModelException {

        BPMNModel model = findModelByWorkitem(workitem);
        logger.info("...loadEvent " + workitem.getTaskID() + "." + workitem.getEventID());
        ItemCollection event = findEventByID(model, workitem.getTaskID(), workitem.getEventID());
        if (event == null) {
            throw new ModelException(ModelException.UNDEFINED_MODEL_ENTRY, "Event " + workitem.getTaskID() + "."
                    + workitem.getEventID() + " not defined in model '" + workitem.getModelVersion() + "'");
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
    @Override
    public ItemCollection nextModelElement(ItemCollection event, ItemCollection workitem)
            throws ModelException {
        long l = System.currentTimeMillis();
        BPMNModel model = findModelByWorkitem(workitem);
        // lookup the current BPMN event element by its ID
        String id = event.getItemValueString("id");
        Event eventElement = (Event) model.findElementNodeById(id);
        String version = OpenBPMNUtil.getVersion(model);
        String key = version + "~" + workitem.getTaskID() + "." + workitem.getEventID();

        // find next task or event.....
        BPMNFlowIterator<BPMNElementNode> elementNavigator;
        try {
            elementNavigator = new BPMNFlowIterator<BPMNElementNode>(
                    eventElement,
                    node -> ((OpenBPMNUtil.isImixsTaskElement(node)) || (OpenBPMNUtil.isImixsEventElement(node))),
                    condition -> evaluateCondition(condition, workitem));

            while (elementNavigator.hasNext()) {
                BPMNElementNode nextElement = elementNavigator.next();
                logger.info("nextModelElement " + key + " took " + (System.currentTimeMillis() - l) + "ms");
                return OpenBPMNEntityBuilder.build(nextElement);

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
    public Set<String> getVersions() {
        return modelStore.keySet();
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
        result = modelStore.get(version);
        if (result != null) {
            return result;
        } else {
            // try to find model by regex...
            List<String> matchingVersions = findVersionsByRegEx(version);
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

            // Still no match, try to find model version by group
            if (!workitem.getWorkflowGroup().isEmpty()) {
                List<String> versions = findVersionsByGroup(workitem.getWorkflowGroup());
                if (!versions.isEmpty()) {
                    String newVersion = versions.get(0);
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
            throw new ModelException(ModelException.UNDEFINED_MODEL_VERSION, "$modelversion " + version + " not found");
        }
    }

    /**
     * This method returns a sorted list of model versions containing the requested
     * workflow group. The result is sorted in reverse order, so the highest version
     * number is the first in the result list.
     * 
     * @param group
     * @return
     */
    public List<String> findVersionsByGroup(String group) {
        boolean debug = logger.isLoggable(Level.FINE);
        List<String> result = new ArrayList<String>();
        if (debug) {
            logger.log(Level.FINEST, "......searching model versions for workflowgroup ''{0}''...", group);
        }
        // try to find matching model version by group
        Collection<BPMNModel> models = modelStore.values();
        for (BPMNModel _model : models) {

            Set<BPMNProcess> processList = _model.getProcesses();
            for (BPMNProcess _process : processList) {
                if (group.equals(_process.getName())) {
                    result.add(OpenBPMNUtil.getVersion(_model));
                }
            }
        }
        // sort result
        Collections.sort(result, Collections.reverseOrder());
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
    public List<String> findVersionsByRegEx(String modelRegex) {
        boolean debug = logger.isLoggable(Level.FINE);
        List<String> result = new ArrayList<String>();
        if (debug) {
            logger.log(Level.FINEST, "......searching model versions for regex ''{0}''...", modelRegex);
        }
        // try to find matching model version by regex
        Collection<BPMNModel> models = modelStore.values();
        for (BPMNModel amodel : models) {
            String _version = OpenBPMNUtil.getVersion(amodel);
            if (Pattern.compile(modelRegex).matcher(_version).find()) {
                result.add(_version);
            }
        }
        // sort result
        Collections.sort(result, Collections.reverseOrder());
        return result;
    }

    /**
     * This method returns a sorted list of all workflow groups contained in a BPMN
     * model
     * 
     * @param group
     * @return
     */
    public Set<String> findAllGroups(BPMNModel _model) {
        Set<String> result = new LinkedHashSet<>();
        Set<BPMNProcess> processList = _model.getProcesses();
        for (BPMNProcess _process : processList) {
            result.add(_process.getName());
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
        String key = OpenBPMNUtil.getVersion(model) + "~" + taskID;
        ItemCollection result = (ItemCollection) bpmnEntityCache.computeIfAbsent(key,
                k -> lookupTaskByID(model, taskID));
        // clone instance to protect for manipulation
        if (result != null) {
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
     * 
     */
    public ItemCollection findEventByID(final BPMNModel model, int taskID, int eventID) {
        String key = OpenBPMNUtil.getVersion(model) + "~" + taskID + "." + eventID;
        ItemCollection result = (ItemCollection) bpmnEntityCache.computeIfAbsent(key,
                k -> lookupEventByID(model, taskID, eventID));
        // clone instance to protect for manipulation
        if (result != null) {
            return (ItemCollection) result.clone();
        }
        return null;
    }

    /**
     * Returns a list of start Tasks of a given Process Group
     * 
     * @param processGroup
     * @return
     * @throws BPMNModelException
     */
    public List<ItemCollection> findStartTasks(final BPMNModel model, String processGroup) throws BPMNModelException {
        List<ItemCollection> result = new ArrayList<>();
        BPMNProcess process = model.findProcessByName(processGroup);
        // test start task....
        BPMNStartElementIterator<Activity> startElements = new BPMNStartElementIterator<>(process,
                node -> (node instanceof Activity));
        while (startElements.hasNext()) {
            result.add(OpenBPMNEntityBuilder.build(startElements.next()));
        }
        return result;

    }

    /**
     * Returns a list of all Events assigned to a Task
     * 
     * @param processGroup
     * @return
     * @throws BPMNModelException
     */
    public List<ItemCollection> findEventsByTask(final BPMNModel model, int taskID) throws BPMNModelException {
        Activity taskElement = lookupTaskElementByID(model, taskID);
        BPMNFlowIterator<BPMNElementNode> elementNavigator = new BPMNFlowIterator<BPMNElementNode>(
                taskElement,
                node -> ((OpenBPMNUtil.isImixsEventElement(node))));
        List<ItemCollection> result = new ArrayList<>();
        while (elementNavigator.hasNext()) {
            result.add(OpenBPMNEntityBuilder.build(elementNavigator.next()));
        }
        return result;
    }

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
     * Reset the internal BPMN Element cache
     */
    private void clearCache() {
        bpmnEntityCache.clear();
        bpmnElementCache.clear();
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
        Set<Element> imixsExtensionElements = OpenBPMNUtil.findAllImixsElements(extensionElement, "item");
        // iterate through set and verify the name attribute
        for (Element extensionItem : imixsExtensionElements) {
            String itemName = extensionItem.getAttribute("name");
            if (itemName != null && !itemName.isEmpty()) {
                // found
                List<?> itemValueList = OpenBPMNUtil.getItemValueList(extensionItem);
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
            String id = activity.getExtensionAttribute(OpenBPMNUtil.getNamespace(), "processid");
            try {
                if (taskID == Long.parseLong(id)) {
                    logger.info("lookupTaskElementByID " + taskID + " took " + (System.currentTimeMillis() - l) + "ms");
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
        String key = OpenBPMNUtil.getVersion(model) + "~" + taskID;
        Activity activity = (Activity) bpmnElementCache.computeIfAbsent(key, k -> lookupTaskElementByID(model, taskID));
        if (activity != null) {
            return OpenBPMNEntityBuilder.build(activity);
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
        String key = OpenBPMNUtil.getVersion(model) + "~" + taskID + "." + eventID;
        Event event = (Event) bpmnElementCache.computeIfAbsent(key,
                k -> lookupEventElementByID(model, taskID, eventID));
        if (event != null) {
            return OpenBPMNEntityBuilder.build(event);
        } else {
            return null;
        }
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
        String version = OpenBPMNUtil.getVersion(model);
        String keyTask = version + "~" + taskID;
        String keyEvent = version + "~" + taskID + "." + eventID;
        Activity task = (Activity) bpmnElementCache.computeIfAbsent(keyTask, k -> lookupTaskElementByID(model, taskID));
        if (task == null) {
            logger.warning("TaskID: " + taskID + " does not exist in model '" + version + "'!");
            return null;
        }
        // find all directly associated Events to the current Task...
        BPMNFlowIterator<Event> eventNavigator = new BPMNFlowIterator<Event>(task,
                node -> (OpenBPMNUtil.isImixsEventElement(node)));

        if (eventNavigator != null) {
            while (eventNavigator.hasNext()) {
                Event event = (Event) eventNavigator.next();
                String id = event.getExtensionAttribute(OpenBPMNUtil.getNamespace(), "activityid");
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

        // now test incoming single catch events (without a source node)
        Set<SequenceFlow> inFlows = task.getIngoingSequenceFlows();
        for (SequenceFlow flow : inFlows) {
            BPMNElement element = flow.getSourceElement();
            if (element != null && element instanceof Event) {
                Event event = (Event) element;
                // no incoming flows
                if (event.getIngoingSequenceFlows().size() == 0) {
                    String id = event.getExtensionAttribute(OpenBPMNUtil.getNamespace(), "activityid");
                    if (id == null || id.isEmpty()) {
                        continue;
                    }
                    try {
                        if (eventID == Long.parseLong(id)) {
                            // cache Event...
                            // bpmnElementCache.put(keyEvent, event);
                            logger.info(
                                    "lookupEventByID " + keyEvent + " took " + (System.currentTimeMillis() - l) + "ms");
                            return event;
                            // return ElementBuilder.buildItemCollectionFromBPMNElement(event);
                        }
                    } catch (NumberFormatException e) {
                        logger.warning(
                                event.getId() + " invalid attribute 'imixs:activityid' = " + id + "  Number expected");
                    }
                }
            }
        }

        return null;
    }

}
