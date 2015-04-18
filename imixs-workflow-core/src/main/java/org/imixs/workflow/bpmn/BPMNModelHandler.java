package org.imixs.workflow.bpmn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The Imixs BPMNDefaultHandler is used to extract the Imixs Task and Event
 * Elements of a Imixs BPMN model.
 * 
 * @author rsoika
 *
 */
public class BPMNModelHandler extends DefaultHandler {

	private static Logger logger = Logger.getLogger(BPMNModelHandler.class
			.getName());

	boolean bDefinitions = false;
	boolean bExtensionElements = false;
	boolean bImixsProperty = false;
	boolean bTask = false;
	boolean bEvent = false;
	boolean bItemValue = false;
	ItemCollection currentEntity = null;
	String currentItemName = null;
	String currentItemType = null;
	String bpmnID = null;

	BPMNModel model = null;

	Map<String, ItemCollection> processCache = null;
	Map<String, ItemCollection> activityCache = null;
	Map<String, SequenceFlow> sequenceCache = null;
	ItemCollection profileEnvironment=null;
	
	public BPMNModelHandler() {
		super();
		model = new BPMNModel();
		// initalize cache objects
		processCache = new HashMap<String, ItemCollection>();
		activityCache = new HashMap<String, ItemCollection>();

		// nodeCache = new HashMap<String, ItemCollection>();
		sequenceCache = new HashMap<String, SequenceFlow>();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		logger.finest("Start Element :" + qName);

		// bpmn2:definitions
		if (qName.equalsIgnoreCase("bpmn2:definitions")) {
			bDefinitions = true;
			currentEntity = new ItemCollection();
			// initialize profile entity...
			currentEntity.replaceItemValue("type", "WorkflowEnvironmentEntity");
			currentEntity.replaceItemValue("txtname", "environment.profile");

		}

		// bpmn2:process - close definitions?
		if (qName.equalsIgnoreCase("bpmn2:process")) {
			if (bDefinitions && currentEntity != null) {
				profileEnvironment=currentEntity;
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

			bTask = true;
			int currentID = Integer.parseInt(value);
			currentEntity = new ItemCollection();
			bpmnID = attributes.getValue("id");
			String currentItemName = attributes.getValue("name");
			currentEntity.replaceItemValue("type", "processentity");
			currentEntity.replaceItemValue("txtname", currentItemName);
			currentEntity.replaceItemValue("numprocessid", currentID);
		}

		// bpmn2:intermediateCatchEvent - identify a Imixs Workflow Event
		// element
		if (qName.equalsIgnoreCase("bpmn2:intermediateCatchEvent")) {

			// imixs Task element?
			String value = attributes.getValue("imixs:activityid");
			if (value == null) {
				return;
			}

			bEvent = true;
			int currentID = Integer.parseInt(value);
			currentEntity = new ItemCollection();
			bpmnID = attributes.getValue("id");
			String currentItemName = attributes.getValue("name");
			currentEntity.replaceItemValue("type", "activityentity");
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

		/*
		 * parse a imixs:item
		 */
		if (qName.equalsIgnoreCase("imixs:item")) {
			bExtensionElements = true;
			// check attributes
			currentItemName = attributes.getValue("name");
			currentItemType = attributes.getValue("type");
		}

		/*
		 * parse a imixs:value
		 */
		if (qName.equalsIgnoreCase("imixs:value")) {
			bItemValue = true;
		}

		if (qName.equalsIgnoreCase("bpmn2:extensionElements")) {
			bExtensionElements = true;
		}

	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		// end of bpmn2:task -
		if (bTask && qName.equalsIgnoreCase("bpmn2:task")) {
			bTask = false;
			processCache.put(bpmnID, currentEntity);
		}

		// end of bpmn2:intermediateCatchEvent -
		if (bEvent && qName.equalsIgnoreCase("bpmn2:intermediateCatchEvent")) {
			bEvent = false;

			// we need to cache the activities because the sequenceflows must be
			// analysed later
			activityCache.put(bpmnID, currentEntity);
		}

		/*
		 * parse a imixs:value
		 */
		if (qName.equalsIgnoreCase("imixs:value")) {
			bItemValue = false;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void characters(char ch[], int start, int length)
			throws SAXException {

		/*
		 * parse a imixs:value
		 */
		if (bItemValue && currentEntity != null) {
			String svalue = new String(ch, start, length);

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

	}

	/**
	 * This method builds the model from the information parsed by the handler.
	 * First all task elements were adds as process entities into the model. In
	 * the second step the method adds the Activity elements to the assigned
	 * Task.
	 * 
	 * Finally we look for activities with no incoming SequenceFlow.
	 * 
	 * In addition the builder verifies the ProcessIDs for each task element to
	 * guaranty that the numProcessID is unique
	 * 
	 * @throws ModelException
	 */
	public BPMNModel buildModel() throws ModelException {

		model = new BPMNModel();
		
		model.setProfile(profileEnvironment);

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

			model.addProcessEntity(task);

			// add id and resort
			processIDList.add(pId);
			Collections.sort(processIDList);
		}

		// first of all we find all Source Imxis Task Elements for each
		// collected Imixs Event
		for (String eventID : activityCache.keySet()) {
			// get the event...
			ItemCollection event = activityCache.get(eventID);

			// ...and look for all incoming connections (normally this would be
			// only one!)
			List<SequenceFlow> inFlows = findIncomingFlows(eventID);

			if (inFlows != null && inFlows.size() > 0) {
				// next we search for the source Task element if one can be
				// found
				for (SequenceFlow aFlow : inFlows) {

					ItemCollection task = new TaskResolver()
							.findImixsSourceTask(aFlow);
					if (task != null) {

						// happen if the event is not changing the state
						List<SequenceFlow> outFlows = findOutgoingFlows(eventID);

						if (outFlows != null && outFlows.size() > 1) {
							// invalid model!!
							throw new ModelException(
									ModelException.INVALID_MODEL,
									"Imixs BPMN Event has more than one target Flows!");
						}

						if (outFlows.size() == 0) {
							logger.warning("Imixs BPMN Event '" + eventID
									+ "' has no target Flow!");
							continue;
						}

						// we found the task so we can add the event into
						// the model
						event.replaceItemValue("numProcessID",
								task.getItemValue("numProcessID"));

						// is this Event is connected to a followUp Activity!!
						task = new TaskResolver().findImixsTargetEvent(outFlows
								.get(0));
						if (task != null) {
							event.replaceItemValue("keyFollowUp", "1");
							event.replaceItemValue("numNextActivityID",
									task.getItemValue("numactivityid"));
						} else {
							// test if we can identify the target task
							task = new TaskResolver()
									.findImixsTargetTask(outFlows.get(0));
							if (task != null) {
								event.removeItem("keyFollowUp");
								event.replaceItemValue("numNextProcessID",
										task.getItemValue("numProcessID"));
							}

						}

						// it can happen that the numactivtyid is not unique for
						// that task - we verify this first
						model.addActivityEntity(verifyActiviytIdForEvent(event));

						// now we need to copy the event because we need to
						// reuse it
						// for another task...
						event = new ItemCollection(event);
					}

				}
			}

			else {
				// we have no incoming flows! check outgoing. This case can
				// happen if the event is not changing the state
				List<SequenceFlow> outFlows = findOutgoingFlows(eventID);

				if (outFlows != null && outFlows.size() > 1) {
					// invalid model!!
					throw new ModelException(ModelException.INVALID_MODEL,
							"Imixs BPMN Event has more than one target Flows!");
				}

				if (outFlows.size() == 0) {
					logger.warning("Imixs BPMN Event '" + eventID
							+ "' has no target Flow!");
					continue;
				}

				// is this Event is connected to a followUp Activity!!
				ItemCollection task = new TaskResolver()
						.findImixsTargetEvent(outFlows.get(0));
				if (task != null) {
					event.replaceItemValue("keyFollowUp", "1");
					event.replaceItemValue("numNextActivityID",
							task.getItemValue("numactivityid"));
				} else {
					// check for the target task..

					task = new TaskResolver().findImixsTargetTask(outFlows
							.get(0));
					if (task != null) {

						event.replaceItemValue("numProcessID",
								task.getItemValue("numProcessID"));
						event.replaceItemValue("numNextProcessID",
								task.getItemValue("numProcessID"));

						// it can happen that the numactivtyid is not unique for
						// that task - we verify this first
						model.addActivityEntity(verifyActiviytIdForEvent(event));
					} else {
						logger.warning("Inconsistant model state! - check BPMN event '"
								+ eventID + "'");
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

		Collection<ItemCollection> assignedActivities = model
				.getActivityEntityList(processid);
		int bestID = -1;
		for (ItemCollection aactivity : assignedActivities) {
			int aid = aactivity.getItemValueInteger("numactivityid");
			if (aid >= bestID) {
				bestID = aid + 10;
			}
			if (aid == activityid) {
				// problem!
				String name = event.getItemValueString("txtname");
				logger.warning("ActivityID " + name + " ID=" + activityid
						+ " is not unique for task " + processid);
				activityid = -1;
			}
		}

		// suggest new activityid?
		if (activityid <= 0) {
			// replace id
			logger.warning("new ActivityID suggested for task " + processid
					+ "=" + bestID);
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

	class SequenceFlow {
		String target = null;
		String source = null;

		public SequenceFlow(String source, String target) {
			this.target = target;
			this.source = source;
		}

	}

	/**
	 * This helper class provides methods to resolve the conntect Imixs task
	 * elements to a flow element. The constructor is used to initialize a
	 * loopDetection cache
	 * 
	 * @author rsoika
	 *
	 */
	class TaskResolver {
		List<String> loopFlowCache = null;

		public TaskResolver() {
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
