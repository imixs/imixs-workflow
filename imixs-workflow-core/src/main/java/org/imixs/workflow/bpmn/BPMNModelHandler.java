package org.imixs.workflow.bpmn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.ModelException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The Imixs BPMNDefaultHandler is used to extract the Imixs Task and Event
 * Elements of a Imixs BPMN model.
 * 
 * A BPMN file can either be a simple diagram with one process or a
 * collaboration diagram with a bpmn2:collaboration definition. For
 * collaboration diagrams the currentWorkflowGroup is read from the
 * bpmn2:collaboration element. For a simple BPMN diagram type the
 * currentWorkflowGroup is read from the bpmn2:process element.
 * 
 * #issue 113: The parser connects pairs of catch and throw link events with a
 * virtual SequenceFlow to support the same behavior as if the link events were
 * connected directly.
 * 
 * @author rsoika
 *
 */
public class BPMNModelHandler extends DefaultHandler {

	private static Logger logger = Logger.getLogger(BPMNModelHandler.class.getName());

	private boolean bDefinitions = false;
	private boolean bMessage = false;
	private boolean bAnnotation = false;
	private boolean bDataObject = false;
	private boolean bSignal = false;
	private boolean bExtensionElements = false;
	private boolean bImixsTask = false;
	private boolean bImixsEvent = false;
	private boolean bThrowEvent = false;
	private boolean bCatchEvent = false;
	private boolean bLinkThrowEvent = false;
	private boolean bLinkCatchEvent = false;
	private boolean bItemValue = false;
	private boolean bdocumentation = false;
	private boolean bSequenceFlow = false;

	private boolean bconditionExpression = false;

	private ItemCollection currentEntity = null;
	private String currentItemName = null;
	private String currentItemType = null;
	private String currentWorkflowGroup = null;
	private String currentMessageName = null;
	private String currentAnnotationName = null;
	private String currentDataObjectName = null;
	private String currentDataObjectID = null;
	private String currentSignalID = null;
	private String currentSignalName = null;
	private String currentSignalRefID = null;
	private String currentLinkName = null;

	private String bpmnID = null;
	private StringBuilder characterStream = null;

	private BPMNModel model = null;

	private Map<String, ItemCollection> taskCache = null;
	private Map<String, ItemCollection> eventCache = null;

	private Map<String, String> linkThrowEventCache = null;
	private Map<String, String> linkCatchEventCache = null;

	private Map<String, SequenceFlow> sequenceCache = null;
	private Map<String, SequenceFlow> associationCache = null;
	private Map<String, String> messageCache = null;
	private Map<String, String> annotationCache = null;
	private Map<String, List<String>> dataObjectCache = null;
	private Map<String, String> signalCache = null;

	private Map<String, String> conditionCache = null;

	private List<String> startEvents = null;
	private List<String> endEvents = null;
	private List<String> conditionalGatewayCache = null;
	private List<String> parallelGatewayCache = null;

	private ItemCollection definition = null;

	private List<String> ignoreItemList = null;

	public BPMNModelHandler() {
		super();
		model = new BPMNModel();
		// initalize cache objects
		taskCache = new HashMap<String, ItemCollection>();
		eventCache = new HashMap<String, ItemCollection>();
		messageCache = new HashMap<String, String>();
		annotationCache = new HashMap<String, String>();
		dataObjectCache = new HashMap<String, List<String>>();
		signalCache = new HashMap<String, String>();
		conditionCache = new HashMap<String, String>();

		linkThrowEventCache = new HashMap<String, String>();
		linkCatchEventCache = new HashMap<String, String>();

		conditionalGatewayCache = new ArrayList<String>();
		parallelGatewayCache = new ArrayList<String>();

		startEvents = new ArrayList<String>();
		endEvents = new ArrayList<String>();

		sequenceCache = new HashMap<String, SequenceFlow>();
		associationCache = new HashMap<String, SequenceFlow>();

		// define items to be ignored for import
		String[] array = { "txtname", "txtworkflowgroup", "numprocessid", "numactivityid", "type" };
		ignoreItemList = new ArrayList<String>(Arrays.asList(array));

	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		logger.finest("......Start Element :" + qName);

		// bpmn2:definitions
		if (qName.equalsIgnoreCase("bpmn2:definitions")) {
			bDefinitions = true;
			currentEntity = new ItemCollection();
			// initialize profile entity...
			currentEntity.replaceItemValue("type", "WorkflowEnvironmentEntity");
			currentEntity.replaceItemValue("txtname", "environment.profile");

		}

		// bpmn2:process - parse workflowGroup
		if (qName.equalsIgnoreCase("bpmn2:process")) {
			if (bDefinitions && currentEntity != null) {
				definition = currentEntity;
				bDefinitions = false;

			}
			currentWorkflowGroup = attributes.getValue("name");
			if (currentWorkflowGroup == null || currentWorkflowGroup.isEmpty()) {
				logger.warning("No process name defined!");
				currentWorkflowGroup = "Default";
			}
		}

		// bpmn2:startEvent
		if (qName.equalsIgnoreCase("bpmn2:startEvent")) {
			startEvents.add(attributes.getValue("id"));
		}

		// bpmn2:endEvent
		if (qName.equalsIgnoreCase("bpmn2:endEvent")) {
			endEvents.add(attributes.getValue("id"));
		}

		// bpmn2:task - identify a Imixs Workflow Taks element
		if (qName.equalsIgnoreCase("bpmn2:task")) {

			// imixs Task element?
			String value = attributes.getValue("imixs:processid");
			if (value == null) {
				return;
			}

			bImixsTask = true;
			int currentID = Integer.parseInt(value);
			currentEntity = new ItemCollection();
			bpmnID = attributes.getValue("id");
			currentEntity.replaceItemValue("type", "ProcessEntity");
			currentEntity.replaceItemValue("txtname", attributes.getValue("name"));
			currentEntity.replaceItemValue("txtworkflowgroup", currentWorkflowGroup);
			currentEntity.replaceItemValue("numprocessid", currentID);
		}

		// bpmn2:intermediateCatchEvent - identify link events...
		if (qName.equalsIgnoreCase("bpmn2:intermediateThrowEvent") && attributes.getValue("imixs:activityid") == null) {
			bThrowEvent = true;
			currentLinkName = attributes.getValue("name");
			bpmnID = attributes.getValue("id");
			return;
		}
		if (bThrowEvent && qName.equalsIgnoreCase("bpmn2:linkEventDefinition")) {
			bLinkThrowEvent = true;
			bThrowEvent = false;
			return;
		}
		// bpmn2:intermediateCatchEvent - identify link events...
		if (qName.equalsIgnoreCase("bpmn2:intermediateCatchEvent") && attributes.getValue("imixs:activityid") == null) {
			bCatchEvent = true;
			currentLinkName = attributes.getValue("name");
			bpmnID = attributes.getValue("id");
			return;
		}
		if (bCatchEvent && qName.equalsIgnoreCase("bpmn2:linkEventDefinition")) {
			bLinkCatchEvent = true;
			bCatchEvent = false;
			return;
		}

		// bpmn2:intermediateCatchEvent - identify a Imixs Workflow Event
		// element
		if (qName.equalsIgnoreCase("bpmn2:intermediateCatchEvent")
				|| qName.equalsIgnoreCase("bpmn2:intermediateThrowEvent")) {

			// imixs Event element?
			String value = attributes.getValue("imixs:activityid");
			if (value == null) {
				return;
			}

			bImixsEvent = true;
			int currentID = Integer.parseInt(value);
			currentEntity = new ItemCollection();
			bpmnID = attributes.getValue("id");
			currentEntity.replaceItemValue("type", "ActivityEntity");
			currentEntity.replaceItemValue("txtname", attributes.getValue("name"));
			currentEntity.replaceItemValue("numactivityid", currentID);
			currentSignalRefID = null;
		}

		// bpmn2:sequenceFlow - cache all sequenceFlows...
		if (qName.equalsIgnoreCase("bpmn2:sequenceFlow")) {
			bpmnID = attributes.getValue("id");
			bSequenceFlow = true;
			String source = attributes.getValue("sourceRef");
			String target = attributes.getValue("targetRef");
			sequenceCache.put(bpmnID, new SequenceFlow(source, target));
		}

		// bpmn2:association - cache all association...
		if (qName.equalsIgnoreCase("bpmn2:association")) {
			bpmnID = attributes.getValue("id");
			String source = attributes.getValue("sourceRef");
			String target = attributes.getValue("targetRef");
			associationCache.put(bpmnID, new SequenceFlow(source, target));
		}

		/*
		 * parse a imixs:item
		 */
		if (qName.equalsIgnoreCase("imixs:item")) {
			// check attributes
			currentItemName = attributes.getValue("name");
			currentItemType = attributes.getValue("type");
		}

		/*
		 * parse a imixs:value
		 */
		if (qName.equalsIgnoreCase("imixs:value")) {
			bItemValue = true;
			characterStream = new StringBuilder();
		}

		if (qName.equalsIgnoreCase("bpmn2:extensionElements")) {
			bExtensionElements = true;
		}

		if (qName.equalsIgnoreCase("bpmn2:documentation")) {
			bdocumentation = true;
			characterStream = new StringBuilder();
		}

		if (qName.equalsIgnoreCase("bpmn2:message")) {
			bMessage = true;
			currentMessageName = attributes.getValue("name");
		}

		// parallel gateway
		if (qName.equalsIgnoreCase("bpmn2:exclusiveGateway") || qName.equalsIgnoreCase("bpmn2:inclusiveGateway")
				|| qName.equalsIgnoreCase("bpmn2:eventBasedGateway")) {
			// Put conditional Gateway ID into the gateway cache...
			conditionalGatewayCache.add(attributes.getValue("id"));
		}

		// conditional gateway
		if (qName.equalsIgnoreCase("bpmn2:parallelGateway")) {
			// Put parallel Gateway ID into the gateway cache...
			parallelGatewayCache.add(attributes.getValue("id"));
		}

		// test for conditional Expression...
		if (qName.equalsIgnoreCase("bpmn2:conditionExpression")) {
			bconditionExpression = true;
			characterStream = new StringBuilder();
		}

		if (qName.equalsIgnoreCase("bpmn2:textAnnotation")) {
			bAnnotation = true;
			currentAnnotationName = attributes.getValue("id");
		}

		if (qName.equalsIgnoreCase("bpmn2:dataObject")) {
			bDataObject = true;
			currentDataObjectID = attributes.getValue("id");
			currentDataObjectName = attributes.getValue("name");
		}

		if (qName.equalsIgnoreCase("bpmn2:signal")) {
			bSignal = true;
			currentSignalID = attributes.getValue("id");
			currentSignalName = attributes.getValue("name");
		}

		if (qName.equalsIgnoreCase("bpmn2:signalEventDefinition")) {
			currentSignalRefID = attributes.getValue("signalRef");
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {

		// end of bpmn2:process
		if (qName.equalsIgnoreCase("bpmn2:process")) {
			if (currentWorkflowGroup != null) {
				currentWorkflowGroup = null;
			}
		}

		// end of bpmn2:task -
		if (bImixsTask && qName.equalsIgnoreCase("bpmn2:task")) {
			bImixsTask = false;

			// adapt deprecated proptery format
			adaptDeprecatedTaskProperties(currentEntity);

			taskCache.put(bpmnID, currentEntity);
		}

		if (qName.equalsIgnoreCase("bpmn2:extensionElements")) {
			bExtensionElements = false;
		}

		// end of bpmn2:intermediateCatchEvent -
		if (bImixsEvent && (qName.equalsIgnoreCase("bpmn2:intermediateCatchEvent")
				|| qName.equalsIgnoreCase("bpmn2:intermediateThrowEvent"))) {
			bImixsEvent = false;

			// adapt deprecated proptery format
			adaptDeprecatedEventProperties(currentEntity);

			// adapter ?
			if (currentSignalRefID != null && !currentSignalRefID.isEmpty()) {
				String signalName = signalCache.get(currentSignalRefID);
				if (signalName != null && !signalName.isEmpty()) {
					currentEntity.setItemValue("adapter.id", signalName);
				} else {
					logger.warning("Event " + currentEntity.getItemValueInteger("id") + " Signal Ref " + signalName
							+ " is not defined!");
				}
			}

			// we need to cache the activities because the sequence flows must be
			// analyzed later
			eventCache.put(bpmnID, currentEntity);
		}

		/*
		 * End of a imixs:value
		 */
		if (qName.equalsIgnoreCase("imixs:value")) {
			if (bExtensionElements && bItemValue && currentEntity != null && characterStream != null) {

				String svalue = characterStream.toString();
				List valueList = currentEntity.getItemValue(currentItemName);

				if ("xs:boolean".equals(currentItemType.toLowerCase())) {
					valueList.add(Boolean.valueOf(svalue));
				} else if ("xs:integer".equals(currentItemType.toLowerCase())) {
					valueList.add(Integer.valueOf(svalue));
				} else {
					valueList.add(svalue);
				}

				// item will only be added if it is not listed in the ignoreItem
				// List!
				if (!ignoreItemList.contains(currentItemName)) {
					currentEntity.replaceItemValue(currentItemName, valueList);
				}
			}
			bItemValue = false;
			characterStream = null;
		}

		if (qName.equalsIgnoreCase("bpmn2:documentation")) {
			if (currentEntity != null) {
				currentEntity.replaceItemValue("rtfdescription", characterStream.toString());
			}

			// bpmn2:message?
			if (bMessage) {
				// cache the message...
				messageCache.put(currentMessageName, characterStream.toString());
				bMessage = false;
			}

			// bpmn2:annotation?
			if (bAnnotation) {
				// cache the annotation
				annotationCache.put(currentAnnotationName, characterStream.toString());
				bAnnotation = false;
			}

			// bpmn2:dataObject?
			if (bDataObject) {
				// cache the dataObject
				List<String> dataobject = new ArrayList<String>();
				dataobject.add(currentDataObjectName);
				dataobject.add(characterStream.toString());
				dataObjectCache.put(currentDataObjectID, dataobject);
				bDataObject = false;
			}

			characterStream = null;
			bdocumentation = false;
		}

		// bpmn2:signal?
		if (bSignal) {
			// cache the Signal
			signalCache.put(currentSignalID, currentSignalName);
			bSignal = false;
		}

		// end of bpmn2:intermediateThrowEvent -
		if (bLinkThrowEvent && !bLinkCatchEvent && (qName.equalsIgnoreCase("bpmn2:linkEventDefinition"))) {
			bLinkThrowEvent = false;
			// we need to cache the link name
			linkThrowEventCache.put(bpmnID, currentLinkName);
		}

		// end of bpmn2:intermediateCatchEvent -
		if (bLinkCatchEvent && !bLinkThrowEvent && (qName.equalsIgnoreCase("bpmn2:linkEventDefinition"))) {
			bLinkCatchEvent = false;
			// we need to cache the link name
			linkCatchEventCache.put(currentLinkName, bpmnID);
		}

		// test conditional sequence flow...
		if (bSequenceFlow && bconditionExpression && qName.equalsIgnoreCase("bpmn2:conditionExpression")) {
			String svalue = characterStream.toString();
			logger.finest("......conditional SequenceFlow:" + bpmnID + "=" + svalue);
			bconditionExpression = false;
			conditionCache.put(bpmnID, svalue);
		}

	}

	// @SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void characters(char ch[], int start, int length) throws SAXException {

		if (bdocumentation || bconditionExpression) {
			characterStream = characterStream.append(new String(ch, start, length));
		}

		/*
		 * parse a imixs:value
		 */
		if (bExtensionElements && bItemValue && currentEntity != null) {
			characterStream = characterStream.append(new String(ch, start, length));
		}

	}

	/**
	 * This method builds the model from the information parsed by the handler.
	 * First all task elements were adds as unique process entities into the model.
	 * In the second step the method adds the Activity elements to the assigned
	 * Task. We look also for activities with no incoming SequenceFlow.
	 * 
	 * The builder verifies the ProcessIDs for each task element to guaranty that
	 * the numProcessID is unique
	 * 
	 * The build connects pairs of Catch and Throw LinkEvents with a virtual
	 * SequenceFlow to support the same behavior as if those elements where
	 * connected directly in the model.
	 * 
	 * The method tests the model for bpmn2:message elements and replace links in
	 * Activity elements attribute 'rtfMailBody'
	 * 
	 * @throws ModelException
	 */
	public BPMNModel buildModel() throws ModelException {

		String modelVersion = definition.getItemValueString("txtworkflowmodelversion");
		definition.replaceItemValue("$modelversion", modelVersion);
		model = new BPMNModel();

		model.setDefinition(definition);

		// create virtual sequence Flows for LinkEvents if available
		for (String sourceID : linkThrowEventCache.keySet()) {
			String linkName = linkThrowEventCache.get(sourceID);
			// test if we found a matching target CatchEvent
			String targetID = linkCatchEventCache.get(linkName);
			if (targetID != null) {
				// build a virtual sequenceFlow...
				sequenceCache.put(WorkflowKernel.generateUniqueID(), new SequenceFlow(sourceID, targetID));
			}
		}

		// add all Imixs tasks into the model and validate the processids
		List<Integer> processIDList = new ArrayList<Integer>();
		for (String key : taskCache.keySet()) {
			ItemCollection task = taskCache.get(key);
			// check if numProcessID is unique...
			int pId = task.getItemValueInteger("numProcessID");
			if (processIDList.contains(pId)) {
				// we need a new pid!
				pId = processIDList.get(processIDList.size() - 1);
				pId = pId + 100;
				logger.warning("Task " + task.getItemValueInteger("numProcessID") + " ("
						+ task.getItemValueString("txtname") + ") is not unique, assigning new ProcessID " + pId
						+ ". Please verify the XML content.");
				task.replaceItemValue("numProcessID", pId);
				// update task in cache
				taskCache.put(key, task);
			}

			// update modelversion...
			task.replaceItemValue("$modelVersion", modelVersion);

			// look for optional annotations....
			// annotation text will be added to the task if the task has yet no
			// documentation
			String annotationText = getAnnotationForElement(key);
			if (annotationText != null && task.getItemValueString("rtfdescription").isEmpty()) {
				// we take the annotation as the new documentation
				task.replaceItemValue("rtfdescription", annotationText);
			}

			// look for optional dataObjects...
			List<List<String>> dataObjectList = getDataObjectsForElement(key);
			if (dataObjectList != null) {
				// we take the annotation as the new documentation
				task.replaceItemValue("dataObjects", dataObjectList);
			}

			if (isStartTask(key)) {
				task.setItemValue("startTask", true);
			}
			if (isEndTask(key)) {
				task.setItemValue("endTask", true);
			}

			model.addTask(task);

			// add id and resort
			processIDList.add(pId);
			Collections.sort(processIDList);
		}

		// Iterate over all Imixs Event IDs and add them to the corresponding
		// Imixs Task Elements
		for (String eventID : eventCache.keySet()) {
			List<ItemCollection> sourceTaskList = findSourceTasks(eventID);
			for (ItemCollection sourceTask : sourceTaskList) {
				addImixsEvent(eventID, sourceTask);
			}
		}

		return model;

	}

	/**
	 * This method returns the documentation of connected objects (e.g. annotations
	 * or dataObjects) to a given Element
	 * 
	 * @param elementID
	 *            - BPMN element linked with an annotation
	 * @return - the documentation text or null if no annotation was linked
	 **/
	private String getAnnotationForElement(String elementID) {
		StringBuilder builder = new StringBuilder();
		// check all annotations....
		for (Map.Entry<String, String> entry : annotationCache.entrySet()) {
			String id = entry.getKey();
			String annotation = entry.getValue();
			if (annotation == null || annotation.trim().isEmpty()) {
				continue;
			}
			// test if the elementID is connected to this annotation....
			List<SequenceFlow> resultList = findIncomingAssociations(elementID);
			for (SequenceFlow flow : resultList) {
				if (flow.source.equals(id)) {
					builder.append(annotation);
				}
			}
		}

		if (builder.length() > 0) {
			return builder.toString();
		} else {
			return null;
		}

	}

	/**
	 * This method returns the documentations of connected dataObjects to a given
	 * Element
	 * 
	 * @param elementID
	 *            - BPMN element linked with an annotation
	 * @return - a list of arrays containing the dataObjectID and the documentation
	 **/
	private List<List<String>> getDataObjectsForElement(String elementID) {
		List<List<String>> result = new ArrayList<List<String>>();

		// check all annotations....
		for (Map.Entry<String, List<String>> entry : dataObjectCache.entrySet()) {
			String id = entry.getKey();
			List<String> dataobject = entry.getValue();
			if (dataobject == null || dataobject.size() == 0) {
				continue;
			}
			// test if the elementID is connected to this annotation....
			List<SequenceFlow> resultList = findIncomingAssociations(elementID);
			for (SequenceFlow flow : resultList) {
				if (flow.source.equals(id)) {
					result.add(dataobject);
				}
			}
		}

		if (result.size() > 0) {
			return result;
		} else {
			return null;
		}

	}

	// check if this task is connected to a start event....
	private boolean isStartTask(String taskID) {
		List<SequenceFlow> inFlows = findIncomingFlows(taskID);
		if (inFlows != null && inFlows.size() > 0) {
			for (SequenceFlow aFlow : inFlows) {
				String id = new ElementResolver().findStartEvent(aFlow, true);
				if (id != null) {
					return true;
				}
			}
		}
		return false;
	}

	// check if this task is connected to a start event....
	private boolean isStartEvent(String eventID) {
		List<SequenceFlow> inFlows = findIncomingFlows(eventID);
		if (inFlows != null && inFlows.size() > 0) {
			for (SequenceFlow aFlow : inFlows) {
				String id = new ElementResolver().findStartEvent(aFlow, false);
				if (id != null) {
					return true;
				}
			}
		}
		return false;
	}

	// check if this task is connected to an end event....
	private boolean isEndTask(String taskID) {
		List<SequenceFlow> outFlows = findOutgoingFlows(taskID);
		if (outFlows != null && outFlows.size() > 0) {
			for (SequenceFlow aFlow : outFlows) {
				String id = new ElementResolver().findEndEvent(aFlow);
				if (id != null) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * This method returns all SourceTask Elements connected to a given eventID. The
	 * method takes care about loop events and follow up events. Later ones are
	 * handled by the method addImixsEvent(). For that reason, the result of this
	 * method can be also an empty list.
	 * 
	 * An event can be a shared event so it is possible that more than one source
	 * tasks are found
	 * 
	 * @param eventID
	 * @throws ModelException
	 */
	private List<ItemCollection> findSourceTasks(String eventID) throws ModelException {
		List<ItemCollection> result = new ArrayList<ItemCollection>();
		boolean isFollowUp = false;

		// first we lookup all possible incoming flows to identify direct source
		// tasks
		List<SequenceFlow> inFlows = findIncomingFlows(eventID);

		if (inFlows != null && inFlows.size() > 0) {
			for (SequenceFlow aFlow : inFlows) {
				List<ItemCollection> sourceTaskList = new ArrayList<ItemCollection>();
				sourceTaskList = new ElementResolver().findAllImixsSourceTasks(aFlow, sourceTaskList);
				if (sourceTaskList.size() > 0) {
					result.addAll(sourceTaskList);
				} else {
					// we found no source task. Test if the incoming flow is a
					// event. Than we can ignore this flow! (follow up event)
					ItemCollection sourceEvent = new ElementResolver().findImixsSourceEvent(aFlow);
					if (sourceEvent != null) {
						isFollowUp = true;
						// ignore
						continue;
					} else {
						// now as we found no task or event we check if we got a
						// start event!
						if (startEvents.contains(aFlow.source)) {
							// all possible target Tasks are the source tasks
							// for this event!
							List<SequenceFlow> outFlows = findOutgoingFlows(eventID);
							List<String> targetTaskList = new ArrayList<String>();

							for (SequenceFlow outgoingFlow : outFlows) {
								targetTaskList = new ElementResolver().findAllImixsTargetTaskIDs(outgoingFlow,
										targetTaskList);
							}
							// here we return all possible target tasks
							for (String targetID : targetTaskList) {
								result.add(taskCache.get(targetID));
							}
						}
					}
				}
			}

			// finish?
			if (result.size() > 0) {
				// we do not test for loop events
				return result;
			}
		}

		// possible a loop event, if this is no followUp.
		// so we test the target task...

		if (!isFollowUp) {

			List<SequenceFlow> outFlows = findOutgoingFlows(eventID);
			if (outFlows != null && outFlows.size() != 1) {
				// invalid model!!
				throw new ModelException(ModelException.INVALID_MODEL,
						"Imixs BPMN Event '" + eventID + "' has none or more than one targets!");
			}

			// test target element....
			// issue #211 - verify all outFlows!
			// SequenceFlow outgoingFlow = outFlows.get(0);

			for (SequenceFlow outgoingFlow : outFlows) {
				ItemCollection targetTask = new ElementResolver().findImixsTargetTask(outgoingFlow);
				if (targetTask != null) {
					result.add(targetTask);
				}
			}
		}

		logger.finest(
				"......Imixs BPMN Event '" + eventID + "' is directly assigend to " + result.size() + " task elements");
		return result;
	}

	/**
	 * This method computes the target for an event and adds the event to a source
	 * task. The method call recursive if the target is a followUp Event.
	 * 
	 * If a event has no target the method throws an exception
	 * 
	 * If a event has more than one targets (task or event elements) then the event
	 * is handled as a loop event.
	 * 
	 * If a event is already assigned to the sourceTask, the method returns without
	 * adding the event.
	 * 
	 * @param sourceTask
	 * @param event
	 * @throws ModelException
	 */
	private void addImixsEvent(String eventID, ItemCollection sourceTask) throws ModelException {

		ItemCollection event = eventCache.get(eventID);
		// test event for null
		if (event == null) {
			// invalid model (should not happen)
			throw new ModelException(ModelException.INVALID_MODEL, "Imixs BPMN Event '" + eventID + "' unknown!");
		}
		// clone event
		event = new ItemCollection(event);
		String eventName = event.getItemValueString("txtname");

		// test sourceTask for null
		if (sourceTask == null) {
			// invalid model!!
			throw new ModelException(ModelException.INVALID_MODEL,
					"Imixs BPMN Event '" + eventName + "' has no source task!");
		}

		// if the event is already assigned to the sourceTask, then we can
		// skip, because there is no need to duplicate an event!
		try {
			if (model.getEvent(sourceTask.getItemValueInteger("numProcessID"),
					event.getItemValueInteger("numactivityid")) != null) {
				logger.finest("......Imixs BPMN Event '" + eventName + "' is already assigned tosource task!");
				return;
			}
		} catch (ModelException me1) {
			// ok we need to add the event....
		}

		logger.finest("......adding event '" + eventName + "'");

		List<SequenceFlow> outFlows = findOutgoingFlows(eventID);
		if (outFlows == null || outFlows.size() == 0) {
			// invalid model!!
			throw new ModelException(ModelException.INVALID_MODEL,
					"Imixs BPMN Event '" + eventName + "' has no target!");
		}

		// test if the element has multiple targets. In this case the event is
		// handled as a loop event
		List<String> targetList = new ArrayList<String>();
		for (SequenceFlow outgoingFlow : outFlows) {
			targetList = new ElementResolver().findAllImixsTargetIDs(outgoingFlow, targetList);
		}
		if (targetList.size() > 1) {
			// we have a multi event which need to be handled like a loop event
			event.removeItem("keyFollowUp");
			event.replaceItemValue("numNextProcessID", sourceTask.getItemValue("numProcessID"));

			// test if this is a conditional event - search for conditional gateways...
			List<SequenceFlow> outgoingList = this.findOutgoingFlows(eventID);
			if (outgoingList != null && outgoingList.size() > 0) {
				Map<String, String> conditions = new HashMap<String, String>();
				for (SequenceFlow flow : outgoingList) {
					// lookup for a exclusive gateway....
					String exclusiveGatewayID = new ElementResolver().findExclusiveGateway(flow);
					if (exclusiveGatewayID != null) {

						String conditionalGatewayID = exclusiveGatewayID; // flow.target;
						// get all outgoing flows from this gateway
						List<SequenceFlow> conditionalFlows = this.findOutgoingFlows(conditionalGatewayID);
						for (SequenceFlow condFlow : conditionalFlows) {
							ItemCollection targetTask = new ElementResolver().findImixsTargetTask(condFlow);
							// build the condition
							if (targetTask != null) {
								String sExpression = findConditionBySquenceFlow(condFlow);
								if (sExpression != null && !sExpression.trim().isEmpty()) {
									logger.finest("......add condition: "
											+ targetTask.getItemValueInteger("numProcessid") + "=" + sExpression);
									conditions.put("task=" + targetTask.getItemValueInteger("numProcessid"),
											sExpression);
								}
							} else {
								// test for an event....
								String targetEventID = new ElementResolver().findImixsTargetEventID(condFlow);
								ItemCollection targetEvent = eventCache.get(targetEventID);
								if (targetEvent != null) {
									String sExpression = findConditionBySquenceFlow(condFlow);
									if (sExpression != null && !sExpression.trim().isEmpty()) {
										logger.finest("......add condition: "
												+ targetEvent.getItemValueInteger("numActivityid") + "=" + sExpression);
										conditions.put("event=" + targetEvent.getItemValueInteger("numActivityid"),
												sExpression);
									}
								}

							}
						}

					}

				}
				// add the attribute 'keyExclusiveConditions' if available...
				if (!conditions.isEmpty()) {
					event.replaceItemValue("keyExclusiveConditions", conditions);
				}

			}

			// test if this is a split event - search for parallel gateway...
			outgoingList = this.findOutgoingFlows(eventID);
			if (outgoingList != null && outgoingList.size() > 0) {
				Map<String, String> conditions = new HashMap<String, String>();
				for (SequenceFlow flow : outgoingList) {
					if (parallelGatewayCache.contains(flow.target)) {

						String parallelGatewayID = flow.target;
						// get all outgoing flows from this gateway
						List<SequenceFlow> parallelFlows = this.findOutgoingFlows(parallelGatewayID);
						for (SequenceFlow parallelFlow : parallelFlows) {
							ItemCollection targetTask = new ElementResolver().findImixsTargetTask(parallelFlow);
							// build the condition
							if (targetTask != null) {
								String sExpression = findConditionBySquenceFlow(parallelFlow);
								if (sExpression != null && !sExpression.trim().isEmpty()) {
									logger.finest("......add condition: "
											+ targetTask.getItemValueInteger("numProcessid") + "=" + sExpression);
									conditions.put("task=" + targetTask.getItemValueInteger("numProcessid"),
											sExpression);
								}
							} else {
								// test for an event....
								String targetEventID = new ElementResolver().findImixsTargetEventID(parallelFlow);
								ItemCollection targetEvent = eventCache.get(targetEventID);
								if (targetEvent != null) {
									String sExpression = findConditionBySquenceFlow(parallelFlow);
									if (sExpression != null && !sExpression.trim().isEmpty()) {
										logger.finest("......add condition: "
												+ targetEvent.getItemValueInteger("numActivityid") + "=" + sExpression);
										conditions.put("event=" + targetEvent.getItemValueInteger("numActivityid"),
												sExpression);
									}
								}
							}
						}

					}

				}
				// add the attribute 'keySplitConditions' if available...
				if (!conditions.isEmpty()) {
					event.replaceItemValue("keySplitConditions", conditions);
				}

			}

			// here we need to check if one of the targets is an event - this
			// need to be handled in a recursive call
			for (String elementID : targetList) {
				// test if the target is a Imixs Event
				ItemCollection imixsElement = eventCache.get(elementID);
				if (imixsElement != null) {
					// recursive call!
					addImixsEvent(elementID, sourceTask);
				}
			}

		} else {
			// normal case - the event has one outgoing target and we test the
			// target element now

			// test target element....
			// SequenceFlow outgoingFlow = outFlows.get(0);
			String followUpEventID = null;
			for (SequenceFlow outgoingFlow : outFlows) {
				// is this Event connected to a followUp Activity?
				followUpEventID = new ElementResolver().findImixsTargetEventID(outgoingFlow);
				if (followUpEventID != null) {
					break;
				}
			}

			if (followUpEventID != null) {
				// recursive call!
				addImixsEvent(followUpEventID, sourceTask);
				ItemCollection followUpEvent = eventCache.get(followUpEventID);
				event.replaceItemValue("keyFollowUp", "1");
				event.replaceItemValue("numNextActivityID", followUpEvent.getItemValue("numactivityid"));

			} else {
				// test if we found a target task.
				ItemCollection targetTask = null;
				for (SequenceFlow outgoingFlow : outFlows) {
					// invalid model if more than one target tasks!!
					if (targetTask != null)
						throw new ModelException(ModelException.INVALID_MODEL,
								"Imixs BPMN Event '" + eventName + "' has more than one target task element!");
					targetTask = new ElementResolver().findImixsTargetTask(outgoingFlow);
					if (targetTask != null) {
						event.removeItem("keyFollowUp");
						event.replaceItemValue("numNextProcessID", targetTask.getItemValue("numProcessID"));
						break;
					}
				}
				if (targetTask == null) {
					// invalid model!! - no target task
					throw new ModelException(ModelException.INVALID_MODEL,
							"Imixs BPMN Event '" + eventName + "' has no target task element!");
				}
			}
		}
		// source found
		event.replaceItemValue("numProcessID", sourceTask.getItemValue("numProcessID"));
		event.replaceItemValue("$modelVersion", sourceTask.getModelVersion());
		replaceMessageTags(event);

		// look for optional dataObjects...
		List<List<String>> dataObjectList = getDataObjectsForElement(eventID);
		if (dataObjectList != null) {
			// we take the annotation as the new documentation
			event.replaceItemValue("dataObjects", dataObjectList);
		}

		if (isStartEvent(eventID)) {
			event.setItemValue("startEvent", true);
		}
		
		model.addEvent(verifyActiviytIdForEvent(event));
	}

	/**
	 * This helper method verifies if the activity of the event is still unique for
	 * the task element. If not the method computes a new one and updates the event
	 * 
	 * @param event
	 * @param task
	 * @return
	 */
	private ItemCollection verifyActiviytIdForEvent(ItemCollection event) {
		// ItemCollection event = activityCache.get(eventID);
		int processid = event.getItemValueInteger("numprocessid");
		int activityid = event.getItemValueInteger("numactivityid");

		List<ItemCollection> assignedActivities = model.findAllEventsByTask(processid);
		int bestID = -1;
		for (ItemCollection aactivity : assignedActivities) {
			int aid = aactivity.getItemValueInteger("numactivityid");
			if (aid >= bestID) {
				bestID = aid + 10;
			}
			if (aid == activityid) {
				// problem!
				String name = event.getItemValueString("txtname");
				logger.warning("ActivityID " + name + " ID=" + activityid + " is not unique for task " + processid);
				activityid = -1;
			}
		}

		// suggest new activityid?
		if (activityid <= 0) {
			// replace id
			logger.warning("new ActivityID suggested for task " + processid + "=" + bestID);
			event.replaceItemValue("numactivityid", bestID);

			// processCache.put(eventID, event);
		} else {
			// no changes needed!
		}

		return event;
	}

	/**
	 * This method returns all incoming sequence flows for a given element ID
	 * 
	 * @param elementID
	 * @return
	 */
	private List<SequenceFlow> findIncomingFlows(String elementID) {

		List<SequenceFlow> result = new ArrayList<BPMNModelHandler.SequenceFlow>();
		for (String aFlowID : sequenceCache.keySet()) {
			SequenceFlow aFlow = sequenceCache.get(aFlowID);
			if (aFlow.target.equals(elementID)) {
				result.add(aFlow);
			}
		}

		return result;
	}

	/**
	 * This method returns all outgoing sequence flows for a given element ID
	 * 
	 * @param elementID
	 * @return
	 */
	private List<SequenceFlow> findOutgoingFlows(String elementID) {
		List<SequenceFlow> result = new ArrayList<BPMNModelHandler.SequenceFlow>();
		for (String aFlowID : sequenceCache.keySet()) {
			SequenceFlow aFlow = sequenceCache.get(aFlowID);
			if (aFlow.source.equals(elementID)) {
				result.add(aFlow);
			}
		}
		return result;
	}

	/**
	 * This method returns all incoming Associations flows for a given element ID
	 * 
	 * @param elementID
	 * @return
	 */
	private List<SequenceFlow> findIncomingAssociations(String elementID) {

		List<SequenceFlow> result = new ArrayList<BPMNModelHandler.SequenceFlow>();
		for (String aFlowID : associationCache.keySet()) {
			SequenceFlow aFlow = associationCache.get(aFlowID);
			if (aFlow.target.equals(elementID)) {
				result.add(aFlow);
			}
		}

		return result;
	}

	/**
	 * This method returns all outgoing Associations flows for a given element ID
	 * 
	 * @param elementID
	 * @return
	 */
	@SuppressWarnings("unused")
	private List<SequenceFlow> findOutgoingAssociations(String elementID) {
		List<SequenceFlow> result = new ArrayList<BPMNModelHandler.SequenceFlow>();
		for (String aFlowID : associationCache.keySet()) {
			SequenceFlow aFlow = associationCache.get(aFlowID);
			if (aFlow.source.equals(elementID)) {
				result.add(aFlow);
			}
		}
		return result;
	}

	/**
	 * This method returns an optional condition for a given sequenceFlow object.
	 * The method iterates the conditionCache to lookup the condition
	 * 
	 * @param flow
	 * @return the condition if available or null
	 */
	private String findConditionBySquenceFlow(SequenceFlow flow) {
		if (conditionCache == null) {
			return null;
		}
		// first we need do figure out the squenceFlowID for the flow object
		String sequenceID = null;
		for (Map.Entry<String, SequenceFlow> entry : sequenceCache.entrySet()) {
			String key = entry.getKey();
			SequenceFlow value = entry.getValue();
			if (value == flow) {
				sequenceID = key;
				break;
			}
		}
		return conditionCache.get(sequenceID);
	}

	/**
	 * This method parses an event for the text fragment
	 * <bpmn2:message>...</bpmn2:message> and replaces the tag with the
	 * corresponding message if available
	 * 
	 * @param itemcol
	 */
	private void replaceMessageTags(ItemCollection itemcol) {

		String[] fieldList = { "rtfmailbody", "txtmailsubject" };
		for (String field : fieldList) {

			String value = itemcol.getItemValueString(field);
			int parsingPos = 0;
			boolean bNewValue = false;
			while (value.indexOf("<bpmn2:message>", parsingPos) > -1) {

				int istart = value.indexOf("<bpmn2:message>", parsingPos);
				int iend = value.indexOf("</bpmn2:message>", parsingPos);
				if (istart > -1 && iend > -1 && iend > istart) {
					String messageName = value.substring(istart + 15, iend);
					String message = messageCache.get(messageName);
					if (message != null) {
						value = value.substring(0, istart) + message + value.substring(iend + 16);
						bNewValue = true;
					}
				}

				parsingPos = parsingPos + 15;
			}

			if (bNewValue) {
				itemcol.replaceItemValue(field, value);
			}

		}

	}

	/**
	 * This is a helper method to adapt the old property names into the new. The
	 * method also works the other way around so that new imixs-workflow can handle
	 * old bpmn files too.
	 * 
	 * @param currentEntity2
	 */
	private void adaptDeprecatedTaskProperties(ItemCollection taskEntity) {

		adaptDeprecatedItem(taskEntity, BPMNModel.TASK_ITEM_NAME, "txtname");
		adaptDeprecatedItem(taskEntity, BPMNModel.TASK_ITEM_DOCUMENTATION, "rtfdescription");

		adaptDeprecatedItem(taskEntity, BPMNModel.TASK_ITEM_WORKFLOW_SUMMARY, "txtworkflowsummary");
		adaptDeprecatedItem(taskEntity, BPMNModel.TASK_ITEM_WORKFLOW_ABSTRACT, "txtworkflowabstract");

		adaptDeprecatedItem(taskEntity, BPMNModel.TASK_ITEM_APPLICATION_EDITOR, "txteditorid");
		adaptDeprecatedItem(taskEntity, BPMNModel.TASK_ITEM_APPLICATION_ICON, "txtimageurl");
		adaptDeprecatedItem(taskEntity, BPMNModel.TASK_ITEM_APPLICATION_TYPE, "txttype");

		adaptDeprecatedItem(taskEntity, BPMNModel.TASK_ITEM_ACL_OWNER_LIST, "namownershipnames");
		adaptDeprecatedItem(taskEntity, BPMNModel.TASK_ITEM_ACL_OWNER_LIST_MAPPING, "keyownershipfields");
		adaptDeprecatedItem(taskEntity, BPMNModel.TASK_ITEM_ACL_READACCESS_LIST, "namaddreadaccess");
		adaptDeprecatedItem(taskEntity, BPMNModel.TASK_ITEM_ACL_READACCESS_LIST_MAPPING, "keyaddreadfields");
		adaptDeprecatedItem(taskEntity, BPMNModel.TASK_ITEM_ACL_WRITEACCESS_LIST, "namaddwriteaccess");
		adaptDeprecatedItem(taskEntity, BPMNModel.TASK_ITEM_ACL_WRITEACCESS_LIST_MAPPING, "keyaddwritefields");
		adaptDeprecatedItem(taskEntity, BPMNModel.TASK_ITEM_ACL_UPDATE, "keyupdateacl");

	}

	/**
	 * This is a helper method to adapt the old property names into the new. The
	 * method also works the other way around so that new imixs-workflow can handle
	 * old bpmn files too.
	 * 
	 * @param currentEntity2
	 */
	private void adaptDeprecatedEventProperties(ItemCollection eventEntity) {

		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_NAME, "txtname");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_DOCUMENTATION, "rtfdescription");

		// migrate keypublicresult
		if (!eventEntity.hasItem("keypublicresult")) {
			if (!eventEntity.hasItem(BPMNModel.EVENT_ITEM_WORKFLOW_PUBLIC)) {
				eventEntity.setItemValue(BPMNModel.EVENT_ITEM_WORKFLOW_PUBLIC, true);
			} else {
				if (!eventEntity.hasItem(BPMNModel.EVENT_ITEM_WORKFLOW_PUBLIC)) {
					eventEntity.setItemValue(BPMNModel.EVENT_ITEM_WORKFLOW_PUBLIC,
							!"0".equals(eventEntity.getItemValueString("keypublicresult")));
				}
			}
		} else {
			if (!eventEntity.hasItem(BPMNModel.EVENT_ITEM_WORKFLOW_PUBLIC)) {
				eventEntity.setItemValue(BPMNModel.EVENT_ITEM_WORKFLOW_PUBLIC,
						!"0".equals(eventEntity.getItemValueString("keypublicresult")));
			}
		}
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_WORKFLOW_PUBLIC_ACTORS, "keyrestrictedvisibility");

		// acl
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_ACL_OWNER_LIST, "namownershipnames");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_ACL_OWNER_LIST_MAPPING, "keyownershipfields");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_ACL_READACCESS_LIST, "namaddreadaccess");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_ACL_READACCESS_LIST_MAPPING, "keyaddreadfields");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_ACL_WRITEACCESS_LIST, "namaddwriteaccess");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_ACL_WRITEACCESS_LIST_MAPPING, "keyaddwritefields");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_ACL_UPDATE, "keyupdateacl");

		// workflow
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_WORKFLOW_RESULT, "txtactivityresult");

		// history
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_HISTORY_MESSAGE, "rtfresultlog");

		// mail
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_MAIL_SUBJECT, "txtmailsubject");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_MAIL_BODY, "rtfmailbody");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_MAIL_TO_LIST, "nammailreceiver");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_MAIL_TO_LIST_MAPPING, "keymailreceiverfields");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_MAIL_CC_LIST, "nammailreceivercc");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_MAIL_CC_LIST_MAPPING, "keymailreceiverfieldscc");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_MAIL_BCC_LIST, "nammailreceiverbcc");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_MAIL_BCC_LIST_MAPPING, "keymailreceiverfieldsbcc");

		// rule
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_RULE_ENGINE, "txtbusinessruleengine");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_RULE_DEFINITION, "txtbusinessrule");

		// report
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_REPORT_NAME, "txtreportname");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_REPORT_PATH, "txtreportfilepath");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_REPORT_OPTIONS, "txtreportparams");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_REPORT_TARGET, "txtreporttarget");

		// version
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_VERSION_MODE, "keyversion");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_VERSION_EVENT, "numversionactivityid");

		// timer
		if (!eventEntity.hasItem(BPMNModel.EVENT_ITEM_TIMER_ACTIVE)) {
			eventEntity.setItemValue(BPMNModel.EVENT_ITEM_TIMER_ACTIVE,
					new Boolean("1".equals(eventEntity.getItemValueString("keyscheduledactivity"))));
		}
		if (!eventEntity.hasItem("keyscheduledactivity")) {
			if (eventEntity.getItemValueBoolean(BPMNModel.EVENT_ITEM_TIMER_ACTIVE)) {
				eventEntity.setItemValue("keyscheduledactivity", "1");
			} else {
				eventEntity.setItemValue("keyscheduledactivity", "0");
			}
		}
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_TIMER_SELECTION, "txtscheduledview");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_TIMER_DELAY, "numactivitydelay");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_TIMER_DELAY_UNIT, "keyactivitydelayunit");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_TIMER_DELAY_BASE, "keyscheduledbaseobject");
		adaptDeprecatedItem(eventEntity, BPMNModel.EVENT_ITEM_TIMER_DELAY_BASE_PROPERTY, "keytimecomparefield");

	}

	/**
	 * Helper method to adopt a old name into a new one
	 * 
	 * @param taskEntity
	 * @param newItemName
	 * @param oldItemName
	 */
	private void adaptDeprecatedItem(ItemCollection taskEntity, String newItemName, String oldItemName) {

		// test if old name is provided with a value...
		if (taskEntity.getItemValueString(newItemName).isEmpty()
				&& !taskEntity.getItemValueString(oldItemName).isEmpty()) {
			taskEntity.replaceItemValue(newItemName, taskEntity.getItemValue(oldItemName));
		}

		// now we support backward compatibility and add the old name if missing
		if (taskEntity.getItemValueString(oldItemName).isEmpty()) {
			taskEntity.replaceItemValue(oldItemName, taskEntity.getItemValue(newItemName));
		}

	}

	class SequenceFlow {
		private String target = null;
		private String source = null;

		public SequenceFlow(String source, String target) {
			this.target = target;
			this.source = source;
		}

	}

	/**
	 * This helper class provides methods to resolve the connected Imixs elements to
	 * a flow element. The constructor is used to initialize a loopDetection cache
	 * 
	 * @author rsoika
	 *
	 */
	class ElementResolver {
		private List<String> loopFlowCache = null;

		public ElementResolver() {
			// initalize loop dedection
			loopFlowCache = new ArrayList<String>();
		}

		/**
		 * This method searches a Imixs Task Element connected to the given SequenceFlow
		 * element. If the Sequence Flow is not connected to a Imixs Task element the
		 * method returns null.
		 * 
		 * 
		 * @return the Imixs Task element or null if no Task Element was found.
		 * @return
		 */
		public List<ItemCollection> findAllImixsSourceTasks(SequenceFlow flow, List<ItemCollection> sourceList) {

			if (flow.source == null) {
				return sourceList;
			}

			// detect loops...
			if (loopFlowCache.contains(flow.source)) {
				// loop!
				return sourceList;
			} else {
				loopFlowCache.add(flow.source);
			}

			// test if the source is a Imixs task
			ItemCollection imixstask = taskCache.get(flow.source);
			if (imixstask != null) {
				sourceList.add(imixstask);
				return sourceList;
			}

			// test if the source is a Imixs Event - than we are in a follow up
			// event!
			ItemCollection imixsevent = eventCache.get(flow.source);
			if (imixsevent != null) {
				// event is connected to a event - so we are in a follow up
				// event!
				return sourceList;
			}

			// no Imixs task found so we are trying to look for the next
			// incoming
			// flow elements.
			List<SequenceFlow> refList = findIncomingFlows(flow.source);
			for (SequenceFlow aflow : refList) {
				sourceList = findAllImixsSourceTasks(aflow, sourceList);
			}
			return sourceList;
		}

		/**
		 * This method searches a Imixs Event Element connected to the given
		 * SequenceFlow element. If the Sequence Flow is not connected to a Imixs Event
		 * element the method returns null.
		 * 
		 * 
		 * @return the Imixs event element or null if no event Element was found.
		 * @return
		 */
		public ItemCollection findImixsSourceEvent(SequenceFlow flow) {

			if (flow.source == null) {
				return null;
			}

			// detect loops...
			if (loopFlowCache.contains(flow.source)) {
				// loop!
				return null;
			} else {
				loopFlowCache.add(flow.source);
			}

			// test if the source is a Imixs Event - than we are in a follow up
			// event!
			ItemCollection imixsevent = eventCache.get(flow.source);
			if (imixsevent != null) {
				return imixsevent;
			}

			// test if the source is a Imixs task
			ItemCollection imixstask = taskCache.get(flow.source);
			if (imixstask != null) {
				// event is connected to a task - so we are not in a follow up
				// event!
				return null;
			}

			// no Imixs task found so we are trying to look for the next
			// incoming
			// flow elements.
			List<SequenceFlow> refList = findIncomingFlows(flow.source);
			for (SequenceFlow aflow : refList) {
				return (findImixsSourceEvent(aflow));
			}
			return null;
		}

		/**
		 * This method searches a BPMN2:Start Event Element connected to the given
		 * SequenceFlow element. If the Sequence Flow is not connected to a BPMN2:Start
		 * Event element the method returns null.
		 * 
		 * @return the id of the start event or null if no event Element was found.
		 * @return
		 */
		public String findStartEvent(SequenceFlow flow, boolean forTask) {

			if (flow.source == null) {
				return null;
			}

			// detect loops...
			if (loopFlowCache.contains(flow.source)) {
				// loop!
				return null;
			} else {
				loopFlowCache.add(flow.source);
			}

			// test if the source is a Imixs task
			if (forTask) {
				ItemCollection imixstask = taskCache.get(flow.source);
				if (imixstask != null) {
					// event is connected to a task - so this is not a start Task
					return null;
				}
			} else {
				// we check for a start event...
				ItemCollection imixsevent = eventCache.get(flow.source);
				if (imixsevent != null) {
					// event is connected to a task - so this is not a start Task
					return null;
				}
			}

			// test if the source is a Imixs Event - than we are in a follow up
			// event!
			int pos = startEvents.indexOf(flow.source);
			if (pos > -1) {
				return startEvents.get(pos);
			}

			// no start event found so we are trying to look for the next
			// incoming flow elements.
			List<SequenceFlow> refList = findIncomingFlows(flow.source);
			for (SequenceFlow aflow : refList) {
				return (findStartEvent(aflow, forTask));
			}
			return null;
		}

		/**
		 * This method searches a BPMN2:End Event Element connected to the given
		 * SequenceFlow element. If the Sequence Flow is not connected to a BPMN2:Event
		 * Event element the method returns null.
		 * 
		 * @return the id of the end event or null if no event Element was found.
		 * @return
		 */
		public String findEndEvent(SequenceFlow flow) {

			if (flow.target == null) {
				return null;
			}

			// detect loops...
			if (loopFlowCache.contains(flow.target)) {
				// loop!
				return null;
			} else {
				loopFlowCache.add(flow.target);
			}

			// test if the source is a Imixs task
			ItemCollection imixstask = taskCache.get(flow.target);
			if (imixstask != null) {
				// event is connected to a task - so this is not a end Task
				return null;
			}

			// test if the source is a Imixs Event - than we are in a follow up
			// event!
			int pos = endEvents.indexOf(flow.target);
			if (pos > -1) {
				return endEvents.get(pos);
			}

			// no end task found so we are trying to look for the next
			// incoming flow elements.
			List<SequenceFlow> refList = findIncomingFlows(flow.target);
			for (SequenceFlow aflow : refList) {
				return (findEndEvent(aflow));
			}
			return null;
		}

		/**
		 * This method searches a Imixs Task Element targeted from the given
		 * SequenceFlow element. If the Sequence Flow is not connected to a Imixs Task
		 * element the method returns null.
		 * 
		 * If the target is a Event (FollowUp Event) the method returns null.
		 * 
		 * @return the Imixs Task element or null if no Task Element was found.
		 */
		public ItemCollection findImixsTargetTask(SequenceFlow flow) {
			if (flow.source == null) {
				return null;
			}
			// detect loops...
			if (loopFlowCache.contains(flow.target)) {
				// loop!
				return null;
			} else {
				loopFlowCache.add(flow.target);
			}

			// test if the target is a Imixs task
			ItemCollection imixstask = taskCache.get(flow.target);
			if (imixstask != null) {
				return imixstask;
			}

			// test if the target is a Imixs event - return null if yes!
			ItemCollection imixsevent = eventCache.get(flow.target);
			if (imixsevent != null) {
				return null;
			}

			// no Imixs task or event found so we are trying to look for the
			// next outgoing flow elements. (issue #211)
			List<SequenceFlow> refList = findOutgoingFlows(flow.target);
			for (SequenceFlow aflow : refList) {
				// recursive call....
				ItemCollection aResult = findImixsTargetTask(aflow);
				if (aResult != null) {
					// we got the task!
					return aResult;
				}
			}
			return null;
		}

		/**
		 * This method searches a Conditional Gateway targeted from the given
		 * SequenceFlow element. If no conditional gateway was found the method returns
		 * null.
		 * 
		 * @return id of the conditional gateway if found.
		 */
		public String findExclusiveGateway(SequenceFlow flow) {
			if (flow.source == null) {
				return null;
			}
			// detect loops...
			if (loopFlowCache.contains(flow.target)) {
				// loop!
				return null;
			} else {
				loopFlowCache.add(flow.target);
			}

			String id = flow.target;
			for (String condID : conditionalGatewayCache) {
				if (id.equals(condID)) {
					return condID;
				}
			}

			// no Imixs task or event found so we are trying to look for the
			// next outgoing flow elements. (issue #211)
			List<SequenceFlow> refList = findOutgoingFlows(flow.target);
			for (SequenceFlow aflow : refList) {
				// recursive call....
				String aResult = findExclusiveGateway(aflow);
				if (aResult != null) {
					// we got the task!
					return aResult;
				}
			}
			return null;
		}

		/**
		 * This method searches the id for a Imixs follow-Up activity. This is the case
		 * if the target is another Imixs Event element. The method returns the id of
		 * the followup event
		 * 
		 * @return the ID of the Imixs Event element or null if no Event Element was
		 *         found.
		 * @return
		 */
		public String findImixsTargetEventID(SequenceFlow flow) {

			if (flow.source == null) {
				return null;
			}
			// detect loops...
			if (loopFlowCache.contains(flow.target)) {
				// loop!
				return null;
			} else {
				loopFlowCache.add(flow.target);
			}

			// test if the target is a Imixs task
			ItemCollection imixsElement = taskCache.get(flow.target);
			if (imixsElement != null) {
				// stop here!
				return null;
			}

			// test if the target is a Imixs Event
			imixsElement = eventCache.get(flow.target);
			if (imixsElement != null) {
				return flow.target;
			}

			// no Imixs task or event found so we are trying to look for the
			// next incoming flow elements.
			List<SequenceFlow> refList = findOutgoingFlows(flow.target);
			for (SequenceFlow aflow : refList) {
				return (findImixsTargetEventID(aflow));
			}
			return null;
		}

		/**
		 * This method searches for all target events or task for a outgoing sequence
		 * flow. The method returns a List of possible target elemetns.
		 * 
		 * 
		 * @return the ID of the Imixs Event element or null if no Event Element was
		 *         found.
		 * @return
		 */
		public List<String> findAllImixsTargetIDs(SequenceFlow flow, List<String> targetList) {

			if (targetList == null) {
				targetList = new ArrayList<String>();
			}

			if (flow.source == null) {
				return targetList;
			}
			// detect loops...
			if (loopFlowCache.contains(flow.target)) {
				// loop!
				return targetList;
			} else {
				loopFlowCache.add(flow.target);
			}

			// test if the target is a Imixs task
			ItemCollection imixsElement = taskCache.get(flow.target);
			if (imixsElement != null) {
				targetList.add(flow.target);
				// stop here!
				return targetList;
			}

			// test if the target is a Imixs Event
			imixsElement = eventCache.get(flow.target);
			if (imixsElement != null) {
				targetList.add(flow.target);
				// stop here!
				return targetList;
			}

			// no Imixs task or event found so we are trying to look for the
			// next outgoing flow elements.
			List<SequenceFlow> refList = findOutgoingFlows(flow.target);
			for (SequenceFlow aflow : refList) {
				targetList = findAllImixsTargetIDs(aflow, targetList);
			}
			return targetList;
		}

		/**
		 * This method searches for all target tasks for a outgoing sequence flow. The
		 * method returns a List of possible imixs task elements.
		 * 
		 * 
		 * @return the ID of the Imixs Event element or null if no Event Element was
		 *         found.
		 * @return
		 */
		public List<String> findAllImixsTargetTaskIDs(SequenceFlow flow, List<String> targetList) {

			if (targetList == null) {
				targetList = new ArrayList<String>();
			}

			if (flow.source == null) {
				return targetList;
			}
			// detect loops...
			if (loopFlowCache.contains(flow.target)) {
				// loop!
				return targetList;
			} else {
				loopFlowCache.add(flow.target);
			}

			// test if the target is a Imixs task
			ItemCollection imixsElement = taskCache.get(flow.target);
			if (imixsElement != null) {
				targetList.add(flow.target);
				// stop here!
				return targetList;
			}

			// no Imixs task or event found so we are trying to look for the
			// next outgoing flow elements.
			List<SequenceFlow> refList = findOutgoingFlows(flow.target);
			for (SequenceFlow aflow : refList) {
				targetList = findAllImixsTargetIDs(aflow, targetList);
			}
			return targetList;
		}

	}

}
