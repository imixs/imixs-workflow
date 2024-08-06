package org.imixs.workflow.bpmn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.ModelException;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNNS;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.elements.Event;
import org.openbpmn.bpmn.elements.SequenceFlow;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNValidationException;
import org.openbpmn.bpmn.navigation.BPMNFlowIterator;
import org.openbpmn.bpmn.navigation.BPMNFlowNavigator;
import org.w3c.dom.Element;

/**
 * This OpenBPMNModelManager is a static ModelManger to handle Open BPMN Models.
 * The implementation is based on the OpenBPMN Meta model.
 * 
 */
public class OpenBPMNModelManager implements ModelManager {

    private static Logger logger = Logger.getLogger(OpenBPMNModelManager.class.getName());

    // Model store
    private static final Map<String, BPMNModel> modelStore = new ConcurrentHashMap<>();

    // cache
    private static final Map<String, ItemCollection> bpmnEntityCache = new ConcurrentHashMap<>();
    private static final Map<String, BPMNElement> bpmnElementCache = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation
     */
    public OpenBPMNModelManager() {
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
     * Returns a BPMNModel by its version from the local model store
     */
    @Override
    public BPMNModel getModel(String version) throws ModelException {
        return modelStore.get(version);
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
    @Override
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

    @Override
    public ItemCollection loadEvent(ItemCollection workitem) throws ModelException {

        BPMNModel model = findModelByWorkitem(workitem);
        ItemCollection event = findEventByID(model, workitem.getTaskID(), workitem.getEventID());
        if (event == null) {
            throw new ModelException(ModelException.UNDEFINED_MODEL_ENTRY, "Event " + workitem.getTaskID() + "."
                    + workitem.getEventID() + " not defined in model '" + workitem.getModelVersion() + "'");
        }
        return event;
    }

    @Override
    public ItemCollection nextModelElement(ItemCollection workitem) throws ModelException {
        long l = System.currentTimeMillis();
        // load current event
        loadEvent(workitem);
        // fetch BPMN element
        BPMNModel model = findModelByWorkitem(workitem);
        String version = OpenBPMNUtil.getVersion(model);
        String key = version + "~" + workitem.getTaskID() + "." + workitem.getEventID();
        Event currentEventElement = (Event) bpmnElementCache.get(key);

        // find next task or event.....
        BPMNFlowNavigator<BPMNElementNode> elementNavigator;
        try {
            elementNavigator = new BPMNFlowNavigator<BPMNElementNode>(
                    currentEventElement,
                    n -> ((n instanceof Event) || (n instanceof Activity)));
        } catch (BPMNValidationException e) {
            throw new ModelException(ModelException.INVALID_MODEL, "Unable to resolve next ModelElement in ' "
                    + version + "' : " + e.getMessage());
        }

        while (elementNavigator.hasNext()) {
            BPMNElementNode nextElement = elementNavigator.next();
            // check if Element is a Imixs Task or Event
            if (OpenBPMNUtil.isImixsTaskElement(nextElement)
                    || OpenBPMNUtil.isImixsEventElement(nextElement)) {

                logger.info("nextModelElement " + key + " took " + (System.currentTimeMillis() - l) + "ms");
                return ElementBuilder.buildItemCollectionFromBPMNElement(nextElement);
            }
        }
        return null;
    }

    /**
     * Reset the internal BPMN Element cache
     */
    private static void clearCache() {
        bpmnEntityCache.clear();
        bpmnElementCache.clear();
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
    public static ItemCollection findTaskByID(final BPMNModel model, int taskID) {
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
    public static ItemCollection findEventByID(final BPMNModel model, int taskID, int eventID) {
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
     * This method returns an ItemCollection holding the BPMNModel Definition
     * Attributes including the Imixs Extension attributes.
     * <p>
     * The method returns a cloned Instance of the model entity to avoid
     * manipulation by the client.
     * 
     * @param model
     * @return
     */
    public static ItemCollection findDefinition(final BPMNModel model) {
        String key = OpenBPMNUtil.getVersion(model);
        ItemCollection result = (ItemCollection) bpmnEntityCache.computeIfAbsent(key, k -> lookupDefinition(model));
        // clone instance to protect for manipulation
        if (result != null) {
            return (ItemCollection) result.clone();
        }
        return null;
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
    private static ItemCollection lookupDefinition(final BPMNModel model) {
        long l = System.currentTimeMillis();
        ItemCollection result = new ItemCollection();
        Element definition = model.getDefinitions();

        result.setItemValue("id", definition.getAttribute("id"));
        result.setItemValue("exporter", definition.getAttribute("exporter"));
        result.setItemValue("exporterVersion", definition.getAttribute("exporterVersion"));
        result.setItemValue("targetNamespace", definition.getAttribute("targetNamespace"));

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
    private static Activity lookupTaskElementByID(final BPMNModel model, int taskID) {
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
    private static ItemCollection lookupTaskByID(final BPMNModel model, int taskID) {
        String key = OpenBPMNUtil.getVersion(model) + "~" + taskID;
        Activity activity = (Activity) bpmnElementCache.computeIfAbsent(key, k -> lookupTaskElementByID(model, taskID));
        if (activity != null) {
            return ElementBuilder.buildItemCollectionFromBPMNElement(activity);
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
    private static ItemCollection lookupEventByID(final BPMNModel model, int taskID, int eventID) {
        long l = System.currentTimeMillis();
        String keyTask = OpenBPMNUtil.getVersion(model) + "~" + taskID;
        String keyEvent = OpenBPMNUtil.getVersion(model) + "~" + taskID + "." + eventID;
        Activity task = (Activity) bpmnElementCache.computeIfAbsent(keyTask, k -> lookupTaskElementByID(model, taskID));
        // Activity task = lookupTaskByID(model, taskID);
        if (task == null) {
            logger.warning("TaskID: " + taskID + " does not exist!");
            return null;
        }
        // find all associated Events...
        BPMNFlowIterator<Event> eventNavigator = new BPMNFlowIterator<Event>(task,
                n -> n instanceof Event);

        if (eventNavigator != null) {
            while (eventNavigator.hasNext()) {
                Event event = (Event) eventNavigator.next();
                String id = event.getExtensionAttribute(OpenBPMNUtil.getNamespace(), "activityid");
                if (id == null || id.isEmpty()) {
                    continue;
                }
                try {
                    if (eventID == Long.parseLong(id)) {
                        // cache Event...
                        bpmnElementCache.put(keyEvent, event);
                        logger.info("lookupEventByID " + keyEvent + " took " + (System.currentTimeMillis() - l) + "ms");
                        return ElementBuilder.buildItemCollectionFromBPMNElement(event);
                    }
                } catch (NumberFormatException e) {
                    logger.warning(
                            event.getId() + " invalid attribute 'imixs:activityid' = " + id + "  Number expected");
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
                            bpmnElementCache.put(keyEvent, event);
                            logger.info(
                                    "lookupEventByID " + keyEvent + " took " + (System.currentTimeMillis() - l) + "ms");
                            return ElementBuilder.buildItemCollectionFromBPMNElement(event);
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
