package org.imixs.workflow.bpmn;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNNS;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.Event;
import org.openbpmn.bpmn.elements.SequenceFlow;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.bpmn.navigation.BPMNFlowIterator;
import org.w3c.dom.Element;

/**
 * This helper class provides methods to access extension tags within a
 * Open-BPMN Model
 * 
 * 
 * Example:
 * 
 * <pre>{@code  
 * <bpmn2:task id="Task_2" imixs:processid="1900" name="Approve">
      <bpmn2:extensionElements>
        <imixs:item name="user.name" type="xs:string">John</imixs:item>
        ....
      </bpmn2:extensionElements>
        }</pre>
 * 
 */
public class OpenBPMNManager {

    private static Logger logger = Logger.getLogger(OpenBPMNManager.class.getName());

    // cache
    private static final Map<String, ItemCollection> itemColCache = new ConcurrentHashMap<>();
    private static final Map<String, BPMNElement> bpmnCache = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation
     */
    private OpenBPMNManager() {
    }

    /**
     * Reset the internal BPMN Element cache
     */
    public static void clearCache() {
        itemColCache.clear();
        bpmnCache.clear();
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
        ItemCollection result = (ItemCollection) itemColCache.computeIfAbsent(key, k -> lookupTaskByID(model, taskID));
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
        ItemCollection result = (ItemCollection) itemColCache.computeIfAbsent(key,
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
        ItemCollection result = (ItemCollection) itemColCache.computeIfAbsent(key, k -> lookupDefinition(model));
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
                    logger.info("fineEventByID took " + (System.currentTimeMillis() - l) + "ms");
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
        Activity activity = (Activity) bpmnCache.computeIfAbsent(key, k -> lookupTaskElementByID(model, taskID));
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
        String key = OpenBPMNUtil.getVersion(model) + "~" + taskID;
        Activity task = (Activity) bpmnCache.computeIfAbsent(key, k -> lookupTaskElementByID(model, taskID));
        // Activity task = lookupTaskByID(model, taskID);
        if (task == null) {
            logger.warning("TaskID: " + taskID + " does not exist!");
            return null;
        }
        long l = System.currentTimeMillis();
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
                        logger.info("fineEventByID took " + (System.currentTimeMillis() - l) + "ms");
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
                            logger.info("fineEventByID took " + (System.currentTimeMillis() - l) + "ms");
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
