package org.imixs.workflow.bpmn;

import java.util.ArrayList;
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
	ItemCollection currentEntity = null;
	String bpmnID = null;

	BPMNModel model = null;

	Map<String, ItemCollection> processCache = null;
	Map<String, ItemCollection> activityCache = null;
	// Map<String, ItemCollection> nodeCache = null;
	Map<String, SequenceFlow> sequenceCache = null;
	List<String> loopFlowCache = null;

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

			currentEntity.replaceItemValue("type", "environment.profile");

		}

		// bpmn2:task - identify a Imixs Workflow Taks element
		if (qName.equalsIgnoreCase("bpmn2:task")) {

			bTask = true;

			// imixs Task element?
			String value = attributes.getValue("imixs:processid");
			if (value == null) {
				return;
			}
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

			bEvent = true;

			// imixs Task element?
			String value = attributes.getValue("imixs:activityid");
			if (value == null) {
				return;
			}
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

			String currentItemName = attributes.getValue("name");
			String currentItemType = attributes.getValue("type");

			currentEntity.replaceItemValue(currentItemName, "");
		}

		if (qName.equalsIgnoreCase("bpmn2:extensionElements")) {

			bExtensionElements = true;
		}

	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		System.out.println("End Element :" + qName);
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
	}

	@Override
	public void characters(char ch[], int start, int length)
			throws SAXException {

		if (bDefinitions) {
			System.out
					.println("Definitions : " + new String(ch, start, length));
			bDefinitions = false;
		}

	}

	/**
	 * This method builds the model from the information parsed by the handler.
	 * First all task elements were adds as process entities into the model. In
	 * the second step the method adds the Activity elements to the assigned
	 * Task.
	 * 
	 * Finally we look for activities with no incoming SequenceFlow
	 * 
	 * @throws ModelException
	 */
	public BPMNModel buildModel() throws ModelException {

		model = new BPMNModel();
		// add all Imixs tasks into the model
		for (ItemCollection task : processCache.values()) {
			model.addProcessEntity(task);
		}

		// first of all we find all Source Imxis Task Elements for each
		// collected Imixs Event
		for (String eventID : activityCache.keySet()) {
			// get the event...
			ItemCollection event = activityCache.get(eventID);
			// ...and look for all incoming connections (normally this would be
			// only one!)
			List<SequenceFlow> iFlows = findIncomingFlows(eventID);

			if (iFlows != null && iFlows.size() > 0) {
				// next we search for the source Task element if one can be
				// found
				for (SequenceFlow aFlow : iFlows) {
					loopFlowCache = new ArrayList<String>();
					ItemCollection task = findImixsSourceTask(aFlow);
					if (task != null) {

						// we found the task so we can add the event into the
						// model
						event.replaceItemValue("numProcessID",
								task.getItemValue("numProcessID"));

						// test if we can identify the target task
						task = findImixsTargetTask(aFlow);
						if (task != null) {
							event.replaceItemValue("numNextProcessID",
									task.getItemValue("numProcessID"));
						}

						model.addActivityEntity(event);

						// now we need to copy the event because we need to
						// reuse it
						// for another task...
						event = new ItemCollection(event);
					}

				}
			}

			else {
				// we have no incomming flows! 
				// check outgoing
				iFlows = findOutgoingFlows(eventID);

				if (iFlows != null && iFlows.size() > 1) {
					// invalid model!!
					throw new ModelException(ModelException.INVALID_MODEL,
							"Imixs BPMN Event has more than one target Flows!");
				}
				
				if (iFlows.size() ==0) {
					logger.warning("Imixs BPMN Event '"+eventID + "' has more than no target Flow!");
					continue;
				}

				// this case can happen if the event is not changing the
				// state
				loopFlowCache = new ArrayList<String>();
				ItemCollection task = findImixsTargetTask(iFlows.get(0));
				if (task != null) {
					event.replaceItemValue("numProcessID",
							task.getItemValue("numProcessID"));
					event.replaceItemValue("numNextProcessID",
							task.getItemValue("numProcessID"));
					model.addActivityEntity(event);

				}
			}

		}

		return model;

	}

	/**
	 * This method searches a Imixs Task Element connected to the given
	 * SequenceFlow element. If the Sequence Flow is not connected to a Imixs
	 * Task element the method returns null.
	 * 
	 * 
	 * @return the Imixs Task element or null if no Task Element was found.
	 * @return
	 */
	private ItemCollection findImixsSourceTask(SequenceFlow flow) {

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

		// no Imixs task found so we are trying to look for the next incoming
		// flow elements.
		List<SequenceFlow> refList = findIncomingFlows(flow.source);
		for (SequenceFlow aflow : refList) {
			return (findImixsSourceTask(aflow));
		}
		return null;
	}

	/**
	 * This method searches a Imixs Task Element targeted from the given
	 * SequenceFlow element. If the Sequence Flow is not connected to a Imixs
	 * Task element the method returns null.
	 * 
	 * 
	 * @return the Imixs Task element or null if no Task Element was found.
	 * @return
	 */
	private ItemCollection findImixsTargetTask(SequenceFlow flow) {

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

		// test if the source is a Imixs task
		ItemCollection imixstask = processCache.get(flow.target);
		if (imixstask != null) {
			return imixstask;
		}

		// no Imixs task found so we are trying to look for the next incoming
		// flow elements.
		List<SequenceFlow> refList = findOutgoingFlows(flow.target);
		for (SequenceFlow aflow : refList) {
			return (findImixsTargetTask(aflow));
		}
		return null;
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
}
