package org.imixs.workflow.bpmn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;

/**
 * This utility class can be used to convert a old .ixm model into a new imixs
 * bpmn file. The migration tool separates each workflow group into a single
 * bpmn file.
 * 
 * Arguments:
 * 
 * <code>
 *        # Note: leading slash
 *        /modelfile.ixm
 * </code>
 * 
 * @author rsoika
 *
 */
public class MigrateImixsModelToBPMN {

	private static String sourceModelFile = null;
	private static List<ItemCollection> modelItemCollection = null;
	private static List<SequenceFlow> sequenceFlows = null;
	private static ItemCollection startTask = null;
	private final static Logger logger = Logger
			.getLogger(MigrateImixsModelToBPMN.class.getName());

	public static void main(String[] args) {
		logger.info("Start migration of Imixs Model to BPMN....");
		sourceModelFile = args[0];
 
		readModel();

		buildBPMN();
	}

	private static void readModel() {
		logger.info("read Model file " + sourceModelFile + "...");

		try {
 
			modelItemCollection = XMLDataCollectionAdapter
					.readCollectionFromInputStream(MigrateImixsModelToBPMN.class
							.getClass().getResourceAsStream(sourceModelFile));
			logger.info("Model contains " + modelItemCollection.size()
					+ " elements");
		} catch (JAXBException e) {
			logger.severe("Error reding model file " + e.getMessage());
		} catch (IOException e) {
			logger.severe("Error reding model file " + e.getMessage());
		}

	}

	/**
	 * This method builds a bpmn model form a model item collection
	 */
	private static void buildBPMN() {
		ItemCollection profile = findProfile();
		if (profile != null) {

			List<String> groups = findGroups();
			for (String group : groups) {
				logger.info("Profile migrate workflow group: " + group);

				// find model version
				List<ItemCollection> tasks = findProcessEntitiesByGroup(group);
				startTask = tasks.get(0);
				String sModelVersion = startTask
						.getItemValueString("$modelversion");

				sModelVersion = group.toLowerCase() + "-" + sModelVersion;
				logger.info(" - $modelversion=" + sModelVersion);

				// now create a new bpmn file...
				PrintWriter writer;
				try {
					String sFileName = "target/" + sModelVersion + ".bpmn";
					writer = new PrintWriter(sFileName, "UTF-8");

					writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
					writer.println("<bpmn2:definitions xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bpmn2=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:ext=\"http://org.eclipse.bpmn2/ext\" xmlns:imixs=\"http://www.imixs.org/bpmn2\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" id=\"Definitions_1\"  targetNamespace=\"http://www.imixs.org/bpmn2\">");

					// write profile
					writer.println("<bpmn2:extensionElements>");
					writer.println(" <imixs:item name=\"txtworkflowmodelversion\" type=\"xs:string\">");
					writer.println(" <imixs:value><![CDATA[" + sModelVersion
							+ "]]></imixs:value>");
					writer.println(" </imixs:item>");

					writeItem(writer, profile, "txtfieldmapping");
					writeItem(writer, profile, "txttimefieldmapping");
					writeItem(writer, profile, "txtplugins");

					writer.println("</bpmn2:extensionElements>");

					// write process
					writer.println(" <bpmn2:process id=\""
							+ group.toLowerCase() + "\" name=\"" + group
							+ "\" isExecutable=\"false\">");

					// create start event
					writer.println("<bpmn2:startEvent id=\"StartEvent_1\" name=\"Start\">");
					writer.println("<bpmn2:outgoing>SequenceFlow_0</bpmn2:outgoing>");
					writer.println("</bpmn2:startEvent>");
					writer.println("<bpmn2:sequenceFlow id=\"SequenceFlow_0\" sourceRef=\"StartEvent_1\" targetRef=\"Task_"
							+ startTask.getItemValueInteger("numProcessID")
							+ "\"/>");

					// first we analyze all existing sequence flows
					// and build a map of sequence flows. Each activity has two
					// flow elements
					sequenceFlows = new ArrayList<SequenceFlow>();
					List<String> followUpIds = new ArrayList<String>();
					List<ItemCollection> activities = findActivityEntitiesByGroup(group);
					int flowCount = 1;
					for (ItemCollection activity : activities) {
						boolean isFollowUp = "1".equals(activity
								.getItemValueString("keyfollowup"));

						int processID = activity
								.getItemValueInteger("numprocessid");
						int activityID = activity
								.getItemValueInteger("numactivityid");
						int nextprocessID = activity
								.getItemValueInteger("numnextprocessid");

						// test if follow up.....

						logger.fine("TEST Followup " + processID + "_"
								+ activityID);
						if (!followUpIds.contains(processID + "_" + activityID)) {
							// outgoing...
							String from = "Task_" + processID;
							String to = "IntermediateCatchEvent_" + processID
									+ "-" + activityID;
							String id = "SequenceFlow_" + flowCount;
							sequenceFlows.add(new SequenceFlow(from, to, id));
							// print
							writer.println(" <bpmn2:sequenceFlow id=\"" + id
									+ "\" sourceRef=\"" + from
									+ "\" targetRef=\"" + to + "\"/>");
							flowCount++;
						}
						// incomming only if not the same process and not
						// followup!

						if (processID != nextprocessID || isFollowUp) {

							if (isFollowUp) {
								int nextID = activity
										.getItemValueInteger("numnextid");
								// followup sequence flow
								// normal sequence flow
								String from = "IntermediateCatchEvent_"
										+ processID + "-" + activityID;
								String to = "IntermediateCatchEvent_"
										+ processID + "-" + nextID;
								String id = "SequenceFlow_" + flowCount;
								sequenceFlows
										.add(new SequenceFlow(from, to, id));
								// print
								writer.println(" <bpmn2:sequenceFlow id=\""
										+ id + "\" sourceRef=\"" + from
										+ "\" targetRef=\"" + to + "\"/>");

								followUpIds.add(processID + "_" + nextID);
								logger.fine("ADD follow up " + processID + "_"
										+ nextID);
								flowCount++;

							} else {
								// normal sequence flow
								String from = "IntermediateCatchEvent_"
										+ processID + "-" + activityID;
								String to = "Task_" + nextprocessID;
								String id = "SequenceFlow_" + flowCount;
								sequenceFlows
										.add(new SequenceFlow(from, to, id));
								// print
								writer.println(" <bpmn2:sequenceFlow id=\""
										+ id + "\" sourceRef=\"" + from
										+ "\" targetRef=\"" + to + "\"/>");
								flowCount++;
							}
						}

					}

					// write tasks.....
					tasks = findProcessEntitiesByGroup(group);
					for (ItemCollection task : tasks) {
						writeTask(writer, task);
					}

					// write events.....
					List<ItemCollection> events = findActivityEntitiesByGroup(group);
					for (ItemCollection event : events) {
						writeEvent(writer, event);
					}

					writer.println("</bpmn2:process>");
					writer.println("</bpmn2:definitions>");
					writer.close();
				} catch (FileNotFoundException | UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}

		} else {
			logger.info("No Profile found");
		}

	}

	/**
	 * Writes a singel item into the file
	 * 
	 * <code>
	 *      <imixs:item name="txtworkflowmodelversion" type="xs:string">
		      <imixs:value><![CDATA[1.0.0]]></imixs:value>
		    </imixs:item>
	 * </code>
	 */
	private static void writeItem(PrintWriter writer, ItemCollection entity,
			String fieldName) {
		List<?> item = entity.getItemValue(fieldName);
		if (item.size() > 0) {
			// test type
			Object otest = item.get(0);
			String type = "";
			if (otest instanceof Integer)
				type = "xs:int";
			else if (otest instanceof Boolean)
				type = "xs:boolean";
			else
				type = "xs:string";

			if (!"txtname".equals( fieldName.toLowerCase())) {
				writer.println(" <imixs:item name=\"" + fieldName.toLowerCase()
						+ "\" type=\"" + type + "\">");
				for (Object o : item) {
					writer.println(" <imixs:value><![CDATA[" + o.toString()
							+ "]]></imixs:value>");
				}
				writer.println(" </imixs:item>");
			}

			
		}
	}

	/**
	 * Writes a task element...a singel item into the file
	 * 
	 * <code>
	 *        <bpmn2:task id="Task_1" imixs:processid="1000" name="Task 1">
			      <bpmn2:incoming>SequenceFlow_2</bpmn2:incoming>
			      <bpmn2:outgoing>SequenceFlow_3</bpmn2:outgoing>
			       <bpmn2:extensionElements>
			        <imixs:item name="txtmailsubject" type="xs:string">
			          <imixs:value><![CDATA[Some Message]]></imixs:value>
			        </imixs:item>
			       
			      </bpmn2:extensionElements>
    			</bpmn2:task>
	 * </code>
	 */
	private static void writeTask(PrintWriter writer, ItemCollection task) {
		int processID = task.getItemValueInteger("numprocessid");
		String taskID = "Task_" + processID;

		writer.println(" <bpmn2:task id=\"" + taskID + "\" imixs:processid=\""
				+ processID + "\" name=\"" + task.getItemValueString("txtName")
				+ "\">");

		// now write all elements .....

		writer.println(" <bpmn2:extensionElements>");
		Set<String> keys = task.getAllItems().keySet();
		for (String key : keys) {
			// skip some fields
			if ("numprocessid".equals(key))
				continue;
			if ("$modelversion".equals(key))
				continue;
			if ("$uniqueid".equals(key))
				continue;
			if ("txtworkflowgroup".equals(key))
				continue;
			if ("type".equals(key))
				continue;
			if ("rtfdescription".equals(key))
				continue;

			writeItem(writer, task, key);

		}

		writer.println(" </bpmn2:extensionElements>");

		// write rtfdescription
		if (task.hasItem("rtfdescription")) {
			writer.println("  <bpmn2:documentation id=\"Documentation_"
					+ processID + "\">"
					+ encodeString(task.getItemValueString("rtfdescription"))
					+ "</bpmn2:documentation>");
		}
		// finally we need to construct all incomming and outgoing
		// sequenceflows....
		for (SequenceFlow flow : sequenceFlows) {
			// incomming flow?
			String sid = "Task_" + processID;
			if (sid.equals(flow.to)) {
				writer.println(" <bpmn2:incoming>" + flow.id
						+ "</bpmn2:incoming>");
			}
			if (sid.equals(flow.from)) {
				writer.println(" <bpmn2:outgoing>" + flow.id
						+ "</bpmn2:outgoing>");
			}
		}

		// if start Task connect start event....
		if (startTask.getItemValueInteger("numProcessid")== task.getItemValueInteger("numProcessID")) {
			writer.println("<bpmn2:incoming>SequenceFlow_0</bpmn2:incoming>");
		}

		writer.println(" </bpmn2:task>");

	}

	/**
	 * Writes a task element...a singel item into the file
	 * 
	 * <code>
	 *        <bpmn2:intermediateCatchEvent id="IntermediateCatchEvent_1" imixs:activityid="10" name="submit">
			      <bpmn2:extensionElements>
			        <imixs:item name="keyupdateacl" type="xs:boolean">
			          <imixs:value>true</imixs:value>
			        </imixs:item>
			        <imixs:item name="keyownershipfields" type="xs:string">
			          <imixs:value><![CDATA[namTeam]]></imixs:value>
			          <imixs:value><![CDATA[namManager]]></imixs:value>
			        </imixs:item>
			      </bpmn2:extensionElements>
			      <bpmn2:documentation id="Documentation_1">&lt;b>Submitt&lt;/b> new ticket</bpmn2:documentation>
			      <bpmn2:incoming>SequenceFlow_11</bpmn2:incoming>
			      <bpmn2:outgoing>SequenceFlow_3</bpmn2:outgoing>
			    </bpmn2:intermediateCatchEvent>
	 * </code>
	 */
	private static void writeEvent(PrintWriter writer, ItemCollection activity) {
		int processID = activity.getItemValueInteger("numprocessid");
		int activityID = activity.getItemValueInteger("numactivityid");
		String eventID = "IntermediateCatchEvent_" + processID + "-"
				+ activityID;

		writer.println(" <bpmn2:intermediateCatchEvent id=\"" + eventID
				+ "\" imixs:activityid=\"" + activityID + "\" name=\""
				+ activity.getItemValueString("txtName") + "\">");

		// now write all elements .....

		writer.println(" <bpmn2:extensionElements>");
		Set<String> keys = activity.getAllItems().keySet();
		for (String key : keys) {
			// skip some fields
			if ("numprocessid".equals(key))
				continue;
			if ("numactivityid".equals(key))
				continue;
			if ("numnextprocessid".equals(key))
				continue;
			if ("$modelversion".equals(key))
				continue;
			if ("$uniqueid".equals(key))
				continue;
			if ("txtworkflowgroup".equals(key))
				continue;
			if ("type".equals(key))
				continue;
			if ("rtfdescription".equals(key))
				continue;

			writeItem(writer, activity, key);

		}

		
		// migrate update mode
		if (isACLUpdateMode(activity)) {
			writer.println(" <imixs:item name=\"keyupdateacl\" type=\"xs:boolean\">");
			writer.println(" <imixs:value>true</imixs:value>");
			writer.println("</imixs:item>");
		}
		
		writer.println(" </bpmn2:extensionElements>");

		// write rtfdescription
		if (activity.hasItem("rtfdescription")) {
			writer.println("  <bpmn2:documentation id=\"Documentation_"
					+ processID
					+ "-"
					+ activityID
					+ "\">"
					+ encodeString(activity
							.getItemValueString("rtfdescription"))
					+ "</bpmn2:documentation>");
		}

		// finally we need to construct all incomming and outgoing
		// sequenceflows....
		for (SequenceFlow flow : sequenceFlows) {
			// incomming flow?
			String sid = "IntermediateCatchEvent_" + processID + "-"
					+ activityID;
			if (sid.equals(flow.to)) {
				writer.println(" <bpmn2:incoming>" + flow.id
						+ "</bpmn2:incoming>");
			}
			if (sid.equals(flow.from)) {
				writer.println(" <bpmn2:outgoing>" + flow.id
						+ "</bpmn2:outgoing>");
			}
		}

		writer.println(" </bpmn2:intermediateCatchEvent>");

	}

	private static ItemCollection findProfile() {
		for (ItemCollection entity : modelItemCollection) {

			if ("WorkflowEnvironmentEntity".equals(entity
					.getItemValueString("type"))

					&& ("environment.profile").equals(entity
							.getItemValueString("txtName"))) {
				return entity;
			}

		}
		return null;
	}

	/**
	 * returns a list with all group names
	 * 
	 * @return
	 */
	private static List<String> findGroups() {
		List<String> groups = new ArrayList<String>();
		for (ItemCollection entity : modelItemCollection) {

			if ("ProcessEntity".equals(entity.getItemValueString("type"))) {

				String group = entity.getItemValueString("txtWorkflowGroup");
				if (!groups.contains(group)) {
					// logger.info(group);
					groups.add(group);
				}

			}

		}
		return groups;
	}

	/**
	 * returns a list with all tasks
	 * 
	 * @return
	 */
	private static List<ItemCollection> findProcessEntitiesByGroup(String group) {
		List<ItemCollection> result = new ArrayList<ItemCollection>();
		for (ItemCollection entity : modelItemCollection) {

			if ("ProcessEntity".equals(entity.getItemValueString("type"))) {

				if (group.equals(entity.getItemValueString("txtWorkflowGroup"))) {
					result.add(entity);
				}
			}

		}
		return result;
	}

	/**
	 * returns a specifiy process entity
	 * 
	 * @param processid
	 * @param group
	 * @return
	 */
	private static ItemCollection findProcessEntityById(int processid) {
		for (ItemCollection entity : modelItemCollection) {

			if ("ProcessEntity".equals(entity.getItemValueString("type"))) {

				if (processid == entity.getItemValueInteger("numprocessid")) {
					return entity;
				}
			}

		}
		return null;
	}

	/**
	 * returns a list with all events
	 * 
	 * @return
	 */
	private static List<ItemCollection> findActivityEntitiesByGroup(String group) {
		List<ItemCollection> result = new ArrayList<ItemCollection>();
		for (ItemCollection entity : modelItemCollection) {

			if ("ActivityEntity".equals(entity.getItemValueString("type"))) {

				int processid = entity.getItemValueInteger("numprocessid");

				ItemCollection processEntity = findProcessEntityById(processid);

				if (group.equals(processEntity
						.getItemValueString("txtWorkflowGroup"))) {
					result.add(entity);
				}
			}

		}

		return result;
	}

	/**
	 * replaces < with &lt;
	 * 
	 * @param text
	 */
	private static String encodeString(String text) {
		text = text.replace("<", "&lt;");

		text = text.replace(" & ", " &amp; ");

		return text;

	}
	
	
	/**
	 * Returns true if a old workflow model need to be evaluated
	 * 
	 * @return
	 */
	private static boolean isACLUpdateMode(ItemCollection entity) {
		if (entity.hasItem("keyownershipmode") && "0".equals(entity.getItemValueString("keyownershipmode"))) {
			return true;
		}


		if (entity.hasItem("keyaccessmode") && "0".equals(entity.getItemValueString("keyaccessmode"))) {
			return true;
		}

		

		return false;

	}
}

class SequenceFlow {
	protected String from;
	protected String to;
	protected String id;

	public SequenceFlow(String from, String to, String id) {
		this.from = from;
		this.to = to;
		this.id = id;
	}
}
