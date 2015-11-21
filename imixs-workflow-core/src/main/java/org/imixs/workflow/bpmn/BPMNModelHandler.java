package org.imixs.workflow.bpmn;

import java.util.ArrayList;
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

	boolean bDefinitions = false;
	boolean bMessage = false;
	boolean bExtensionElements = false;
	boolean bImixsProperty = false;
	boolean bImixsTask = false;
	boolean bImixsEvent = false;
	boolean bThrowEvent = false;
	boolean bCatchEvent = false;
	boolean bLinkThrowEvent = false;
	boolean bLinkCatchEvent = false;
	boolean bItemValue = false;
	boolean bdocumentation = false;
	ItemCollection currentEntity = null;
	String currentItemName = null;
	String currentItemType = null;
	String currentWorkflowGroup = null;
	String currentMessageName = null;
	String currentLinkName = null;
	String bpmnID = null;
	StringBuilder characterStream = null;

	BPMNModel model = null;

	Map<String, ItemCollection> processCache = null;
	Map<String, ItemCollection> activityCache = null;

	Map<String, String> linkThrowEventCache = null;
	Map<String, String> linkCatchEventCache = null;

	Map<String, SequenceFlow> sequenceCache = null;
	Map<String, String> messageCache = null;
	ItemCollection profileEnvironment = null;

	public BPMNModelHandler() {
		super();
		model = new BPMNModel();
		// initalize cache objects
		processCache = new HashMap<String, ItemCollection>();
		activityCache = new HashMap<String, ItemCollection>();
		messageCache = new HashMap<String, String>();

		linkThrowEventCache = new HashMap<String, String>();
		linkCatchEventCache = new HashMap<String, String>();

		// nodeCache = new HashMap<String, ItemCollection>();
		sequenceCache = new HashMap<String, SequenceFlow>();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		logger.finest("Start Element :" + qName);

		// bpmn2:definitions
		if (qName.equalsIgnoreCase("bpmn2:definitions")) {
			bDefinitions = true;
			currentEntity = new ItemCollection();
			// initialize profile entity...
			currentEntity.replaceItemValue("type", "WorkflowEnvironmentEntity");
			currentEntity.replaceItemValue("txtname", "environment.profile");

		}

		// bpmn2:collaboration
		if (qName.equalsIgnoreCase("bpmn2:collaboration")) {
			if (bDefinitions && currentEntity != null) {
				profileEnvironment = currentEntity;
				currentWorkflowGroup = attributes.getValue("name");
				if (currentWorkflowGroup == null || currentWorkflowGroup.isEmpty()) {
					logger.warning("No process name defined!");
					currentWorkflowGroup = "Default";
				}
				bDefinitions = false;
			}
		}

		// bpmn2:process - start definitions? parse workflowGroup is not a
		// collaboration diagram
		if (qName.equalsIgnoreCase("bpmn2:process")) {
			if (bDefinitions && currentEntity != null && currentWorkflowGroup == null) {
				profileEnvironment = currentEntity;
				currentWorkflowGroup = attributes.getValue("name");
				if (currentWorkflowGroup == null || currentWorkflowGroup.isEmpty()) {
					logger.warning("No process name defined!");
					currentWorkflowGroup = "Default";
				}
				bDefinitions = false;
			}
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
			String currentItemName = attributes.getValue("name");
			currentEntity.replaceItemValue("type", "ProcessEntity");
			currentEntity.replaceItemValue("txtname", currentItemName);
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
			String currentItemName = attributes.getValue("name");
			currentEntity.replaceItemValue("type", "ActivityEntity");
			currentEntity.replaceItemValue("txtname", currentItemName);
			currentEntity.replaceItemValue("numactivityid", currentID);
		}

		// bpmn2:sequenceFlow - cache all sequenceFlows...
		if (qName.equalsIgnoreCase("bpmn2:sequenceFlow")) {
			bpmnID = attributes.getValue("id");
			String source = attributes.getValue("sourceRef");
			String target = attributes.getValue("targetRef");
			sequenceCache.put(bpmnID, new SequenceFlow(source, target));
		}

		// bpmn2:messageFlow - cache all messageFlow...
		if (qName.equalsIgnoreCase("bpmn2:messageFlow")) {
			bpmnID = attributes.getValue("id");
			String source = attributes.getValue("sourceRef");
			String target = attributes.getValue("targetRef");
			sequenceCache.put(bpmnID, new SequenceFlow(source, target));
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

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {

		// end of bpmn2:task -
		if (bImixsTask && qName.equalsIgnoreCase("bpmn2:task")) {
			bImixsTask = false;
			processCache.put(bpmnID, currentEntity);
		}

		if (qName.equalsIgnoreCase("bpmn2:extensionElements")) {
			bExtensionElements = false;
		}

		// end of bpmn2:intermediateCatchEvent -
		if (bImixsEvent && (qName.equalsIgnoreCase("bpmn2:intermediateCatchEvent")
				|| qName.equalsIgnoreCase("bpmn2:intermediateThrowEvent"))) {
			bImixsEvent = false;
			// we need to cache the activities because the sequenceflows must be
			// analysed later
			activityCache.put(bpmnID, currentEntity);
		}

		/*
		 * End of a imixs:value
		 */
		if (qName.equalsIgnoreCase("imixs:value")) {
			if (bExtensionElements && bItemValue && currentEntity != null && characterStream != null) {

				String svalue = characterStream.toString();
				List valueList = currentEntity.getItemValue(currentItemName);

				if ("xs:boolean".equals(currentItemType.toLowerCase())) {
					valueList.add(new Boolean(svalue));
				} else if ("xs:integer".equals(currentItemType.toLowerCase())) {
					valueList.add(new Integer(svalue));
				} else {
					valueList.add(svalue);
				}
				currentEntity.replaceItemValue(currentItemName, valueList);
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
				messageCache.put(currentMessageName, characterStream.toString());
				bMessage = false;
			}
			characterStream = null;
			bdocumentation = false;
		}

		// end of bpmn2:intermediateThrowEvent -
		if (bLinkThrowEvent && (qName.equalsIgnoreCase("bpmn2:linkEventDefinition"))) {
			bLinkThrowEvent = false;
			// we need to cache the link name
			linkThrowEventCache.put(bpmnID, currentLinkName);
		}

		// end of bpmn2:intermediateCatchEvent -
		if (bLinkCatchEvent && (qName.equalsIgnoreCase("bpmn2:linkEventDefinition"))) {
			bLinkCatchEvent = false;
			// we need to cache the link name
			linkCatchEventCache.put(currentLinkName, bpmnID);
		}

	}

	// @SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void characters(char ch[], int start, int length) throws SAXException {

		if (bdocumentation) {
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
	 * First all task elements were adds as process entities into the model. In
	 * the second step the method adds the Activity elements to the assigned
	 * Task. We look also for activities with no incoming SequenceFlow.
	 * 
	 * The builder verifies the ProcessIDs for each task element to guaranty
	 * that the numProcessID is unique
	 * 
	 * The build connects pairs of Catch and Throw LinkEvents with a virtual
	 * SequenceFlow to support the same behavior as if those elements where
	 * connected directly in the model.
	 * 
	 * The method tests the model for bpmn2:message elements and replace links
	 * in Activity elements attribute 'rtfMailBody'
	 * 
	 * @throws ModelException
	 */
	public BPMNModel buildModel() throws ModelException {

		String modelVersion = profileEnvironment.getItemValueString("txtworkflowmodelversion");
		profileEnvironment.replaceItemValue("$modelversion", modelVersion);
		model = new BPMNModel();

		model.setProfile(profileEnvironment);
		
		
		// create virtual sequence Flows for LinkEvents if available
		for (String sourceID : linkThrowEventCache.keySet()) {
			String linkName  = linkThrowEventCache.get(sourceID);
			// test if we found a matching target CatchEvent
			String targetID= linkCatchEventCache.get(linkName);
			if (targetID!=null) {
				// build a virtual sequenceFlow...
				sequenceCache.put(WorkflowKernel.generateUniqueID(), new SequenceFlow(sourceID, targetID));
			}
		}

		// add all Imixs tasks into the model and validate the processids
		List<Integer> processIDList = new ArrayList<Integer>();
		for (String key : processCache.keySet()) {
			ItemCollection task = processCache.get(key);
			// check if numProcessID is unique...
			int pId = task.getItemValueInteger("numProcessID");
			if (processIDList.contains(pId)) {
				// we need a new pid!
				pId = processIDList.get(processIDList.size() - 1);
				pId = pId + 100;
				task.replaceItemValue("numProcessID", pId);
				// update task in cache
				processCache.put(key, task);
			}

			// update modelversion...
			task.replaceItemValue("$modelVersion", modelVersion);
			model.addProcessEntity(task);

			// add id and resort
			processIDList.add(pId);
			Collections.sort(processIDList);
		}

		// first of all we find all Source Imixs Task Elements for each
		// collected Imixs Event
		for (String eventID : activityCache.keySet()) {
			// get the event...
			ItemCollection event = activityCache.get(eventID);

			// ...and look for all incoming connections (normally this would be
			// only one!)
			List<SequenceFlow> inFlows = findIncomingFlows(eventID);

			boolean taskFound = false;

			if (inFlows != null && inFlows.size() > 0) {
				// next we search for the source Task element if one can be
				// found
				for (SequenceFlow aFlow : inFlows) {

					ItemCollection sourceTask = new ElementResolver().findImixsSourceTask(aFlow);
					if (sourceTask != null) {

						// we found the task so we can add the event into
						// the model
						event.replaceItemValue("numProcessID", sourceTask.getItemValue("numProcessID"));

						// check outgoing flows
						List<SequenceFlow> outFlows = findOutgoingFlows(eventID);

						if (outFlows != null && outFlows.size() > 1) {
							// invalid model!!
							throw new ModelException(ModelException.INVALID_MODEL,
									"Imixs BPMN Event has more than one target Flows!");
						}

						// test target element....
						if (outFlows.size() > 0) {
							SequenceFlow outgoingFlow = outFlows.get(0);
							// is this Event is connected to a followUp
							// Activity!!
							ItemCollection followUpEvent = new ElementResolver().findImixsTargetEvent(outgoingFlow);
							if (followUpEvent != null) {
								event.replaceItemValue("keyFollowUp", "1");
								event.replaceItemValue("numNextActivityID",
										followUpEvent.getItemValue("numactivityid"));
								taskFound = true;
							} else {
								// test if we can identify the target task
								ItemCollection targetTask = new ElementResolver().findImixsTargetTask(outgoingFlow);
								if (targetTask != null) {
									event.removeItem("keyFollowUp");
									event.replaceItemValue("numNextProcessID", targetTask.getItemValue("numProcessID"));
									taskFound = true;
								} else {
									// event has no targets - so the source task is the
									// target task!
									event.removeItem("keyFollowUp");
									event.replaceItemValue("numNextProcessID", sourceTask.getItemValue("numProcessID"));
									taskFound = true;
								}

							}
						} else {
							// event has no targets - so the source task is the
							// target task!
							event.removeItem("keyFollowUp");
							event.replaceItemValue("numNextProcessID", sourceTask.getItemValue("numProcessID"));
							taskFound = true;
						}

						// it can happen that the numactivtyid is not unique for
						// that task - we verify this first
						if (taskFound) {
							event.replaceItemValue("$modelVersion", modelVersion);

							replaceMessageTags(event);
							model.addActivityEntity(verifyActiviytIdForEvent(event));
							// taskFound = true;
						}
					}

				}
			}

			// if we found still not task element continue with outgoing
			// flows....
			if (taskFound == false) {
				// we have no incoming flows! check outgoing. This case can
				// happen if the event is not changing the state or the event is
				// connected with a start event
				List<SequenceFlow> outFlows = findOutgoingFlows(eventID);

				if (outFlows != null && outFlows.size() > 1) {
					// invalid model!!
					throw new ModelException(ModelException.INVALID_MODEL,
							"Imixs BPMN Event has more than one target Flows!");
				}

				if (outFlows.size() == 0) {
					logger.warning("Imixs BPMN Event '" + eventID + "' has no target Flow!");
					continue;
				}

				SequenceFlow outgoingFlow = outFlows.get(0);
				// is this Event is connected to a followUp Activity!!
				ItemCollection followUpEvent = new ElementResolver().findImixsTargetEvent(outgoingFlow);
				if (followUpEvent != null) {
					event.replaceItemValue("keyFollowUp", "1");
					event.replaceItemValue("numNextActivityID", followUpEvent.getItemValue("numactivityid"));
				} else {
					// check for the target task..

					ItemCollection targetTask = new ElementResolver().findImixsTargetTask(outgoingFlow);
					if (targetTask != null) {

						event.replaceItemValue("numProcessID", targetTask.getItemValue("numProcessID"));
						event.replaceItemValue("numNextProcessID", targetTask.getItemValue("numProcessID"));

						// it can happen that the numactivtyid is not unique for
						// that task - we verify this first
						event.replaceItemValue("$modelVersion", modelVersion);
						model.addActivityEntity(verifyActiviytIdForEvent(event));
					} else {
						logger.warning("Inconsistant model state! - check BPMN event '" + eventID + "'");
					}
				}
			}

		}

		return model;

	}

	/**
	 * This helper method verifies if the activity of the event is still unique
	 * for the task element. If not the method computes a new one and updates
	 * the event
	 * 
	 * @param event
	 * @param task
	 * @return
	 */
	private ItemCollection verifyActiviytIdForEvent(ItemCollection event) {
		// ItemCollection event = activityCache.get(eventID);
		int processid = event.getItemValueInteger("numprocessid");
		int activityid = event.getItemValueInteger("numactivityid");

		List<ItemCollection> assignedActivities = model.getActivityEntityList(processid,
				event.getItemValueString(WorkflowKernel.MODELVERSION));
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

	class SequenceFlow {
		String target = null;
		String source = null;

		public SequenceFlow(String source, String target) {
			this.target = target;
			this.source = source;
		}

	}

	/**
	 * This helper class provides methods to resolve the connected Imixs
	 * elements to a flow element. The constructor is used to initialize a
	 * loopDetection cache
	 * 
	 * @author rsoika
	 *
	 */
	class ElementResolver {
		List<String> loopFlowCache = null;

		public ElementResolver() {
			// initalize loop dedection
			loopFlowCache = new ArrayList<String>();
		}

		/**
		 * This method searches a Imixs Task Element connected to the given
		 * SequenceFlow element. If the Sequence Flow is not connected to a
		 * Imixs Task element the method returns null.
		 * 
		 * 
		 * @return the Imixs Task element or null if no Task Element was found.
		 * @return
		 */
		public ItemCollection findImixsSourceTask(SequenceFlow flow) {

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
			ItemCollection imixstask = processCache.get(flow.source);
			if (imixstask != null) {
				return imixstask;
			}

			// no Imixs task found so we are trying to look for the next
			// incoming
			// flow elements.
			List<SequenceFlow> refList = findIncomingFlows(flow.source);
			for (SequenceFlow aflow : refList) {
				return (findImixsSourceTask(aflow));
			}
			return null;
		}

		/**
		 * This method searches a Imixs Task Element targeted from the given
		 * SequenceFlow element. If the Sequence Flow is not connected to a
		 * Imixs Task element the method returns null.
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
			ItemCollection imixstask = processCache.get(flow.target);
			if (imixstask != null) {
				return imixstask;
			}

			// no Imixs task or event found so we are trying to look for the
			// next incoming flow elements.
			List<SequenceFlow> refList = findOutgoingFlows(flow.target);
			for (SequenceFlow aflow : refList) {
				return (findImixsTargetTask(aflow));
			}
			return null;
		}

		/**
		 * This method searches a Imixs follow-Up activity. This is when the
		 * target is another Imixs Event element. In this case we return the
		 * event
		 * 
		 * @return the Imixs Event element or null if no Event Element was
		 *         found.
		 * @return
		 */
		public ItemCollection findImixsTargetEvent(SequenceFlow flow) {

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
			ItemCollection imixstask = processCache.get(flow.target);
			if (imixstask != null) {
				// stopp here!
				return null;
			}

			// test if the target is a Imixs Event
			imixstask = activityCache.get(flow.target);
			if (imixstask != null) {
				return imixstask;
			}

			// no Imixs task or event found so we are trying to look for the
			// next incoming flow elements.
			List<SequenceFlow> refList = findOutgoingFlows(flow.target);
			for (SequenceFlow aflow : refList) {
				return (findImixsTargetEvent(aflow));
			}
			return null;
		}

		

	}

}
