package org.imixs.workflow.bpmn;

import java.util.HashMap;
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
public class BPMNDefaultHandler extends DefaultHandler {

	private static Logger logger = Logger.getLogger(BPMNDefaultHandler.class
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
	Map<String, SequenceFlow> sequenceCache = null;

	public BPMNDefaultHandler() {
		super();
		model = new BPMNModel();
		// initalize cache objects
		processCache = new HashMap<String, ItemCollection>();
		activityCache = new HashMap<String, ItemCollection>();
		sequenceCache = new HashMap<String, SequenceFlow>();
	}

	public BPMNModel getModel() {

		reduceModel();

		return model;
	}

	/**
	 * This method reduces the model information parsed by the handler and adds
	 * the Activity elements into the BPMNModel instance
	 */
	private void reduceModel() {

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
			try {
				processCache.put(bpmnID, currentEntity);
				model.addProcessEntity(currentEntity);
			} catch (ModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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

	class SequenceFlow {
		String target = null;
		String source = null;

		public SequenceFlow(String source, String target) {
			this.target = target;
			this.source = source;
		}

	}

}
