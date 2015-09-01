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
import org.imixs.workflow.xml.XMLItemCollectionAdapter;

/**
 * This utility class can be used to convert a old .ixm model into a new imixs
 * bpmn file. The migration tool separates each workflow group into a single
 * bpmn file.
 * 
 * @author rsoika
 *
 */
public class MigrateImixsModelToBPMN {

	private static String sourceModelFile = null;
	private static List<ItemCollection> modelItemCollection = null;
	private static List<SequenceFlow> sequenceFlows=null;
	
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

			modelItemCollection = XMLItemCollectionAdapter
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

			String sModelVersion = profile
					.getItemValueString("txtworkflowmodelid");
			logger.info("Profile found - $modelversion=" + sModelVersion);

			List<String> groups = findGroups();
			for (String group : groups) {
				logger.info("Profile migrate workflow group: " + group);

				// now create a new bpmn file...
				PrintWriter writer;
				try {
					String sFileName = "target/" + group + sModelVersion
							+ ".bpmn";
					writer = new PrintWriter(sFileName, "UTF-8");

					writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
					writer.println("<bpmn2:definitions xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bpmn2=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:ext=\"http://org.eclipse.bpmn2/ext\" xmlns:imixs=\"http://www.imixs.org/bpmn2\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" id=\"Definitions_1\"  targetNamespace=\"http://www.imixs.org/bpmn2\">");

					// write profile
					writer.println("<bpmn2:extensionElements>");
					writer.println(" <imixs:item name=\"txtworkflowmodelversion\" type=\"xs:string\">");
					writer.println(" <imixs:value><![CDATA[" + sModelVersion
							+ "]]></imixs:value>");
					writer.println(" </imixs:item>");
					writer.println("</bpmn2:extensionElements>");

					// write process
					writer.println(" <bpmn2:process id=\""
							+ group.toLowerCase() + "\" name=\"" + group
							+ "\" isExecutable=\"false\">");

					// first we analyze all existing sequence flows
					// and bild a map of sequence flows
					// connection is stored in a map
					sequenceFlows=new ArrayList<SequenceFlow>();
					List<ItemCollection> activities = findActivityEntitiesByGroup(group);
					for (ItemCollection activity : activities) {
						int from=activity.getItemValueInteger("numprocessid");
						int to=activity.getItemValueInteger("numnextprocessid");
						int id=activity.getItemValueInteger("numactivityid");
						//  <bpmn2:sequenceFlow id="SequenceFlow_1" sourceRef="StartEvent_1" targetRef="Task_1"/>
						SequenceFlow sqf =new SequenceFlow(from, to, id); //new SequenceFlow(from,to,id);
						sequenceFlows.add(sqf);
					}
				
					
					
					

					// write tasks.....
					List<ItemCollection> tasks = findProcessEntitiesByGroup(group);
					for (ItemCollection task : tasks) {
						writeTask(writer, task);
					}

					writer.println("</bpmn2:process>");
					writer.println("</bpmn2:definitions>");
					writer.close();
				} catch (FileNotFoundException | UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//

			// Set<String> keys = profile.getAllItems().keySet();
			// for (String key: keys) {
			// logger.info(key);
			// }

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
		List item = entity.getItemValue(fieldName);
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

			writer.println(" <imixs:item name=\"" + fieldName.toLowerCase()
					+ "\" type=\"" + type + "\">");
			for (Object o : item) {
				writer.println(" <imixs:value><![CDATA[" + o.toString()
						+ "]]></imixs:value>");
			}

			writer.println(" </imixs:item>");
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

		// wirte rtfdescription
		if (task.hasItem("rtfdescription")) {
			writer.println("  <bpmn2:documentation id=\"Documentation_"
					+ processID + "\">"
					+ task.getItemValueString("rtfdescription")
					+ "</bpmn2:documentation>");
		}
		writer.println(" </bpmn2:extensionElements>");
		
		
		// finally we need to construct all incomming and outgoing sequenceflows....
		for (SequenceFlow flow: sequenceFlows) {
			// incomming flow?
			if (flow.to==processID) {
				writer.println(" <bpmn2:incoming>SequenceFlow_"+flow.id+"</bpmn2:incoming>");
			}
			if (flow.from==processID) {
				writer.println(" <bpmn2:outgoing>SequenceFlow_"+flow.id+"</bpmn2:outgoing>");
			}
		}
		

		writer.println(" </bpmn2:task>");

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
	 * @param processid
	 * @param group
	 * @return
	 */
	private static ItemCollection findProcessEntityById(int processid) {
		for (ItemCollection entity : modelItemCollection) {

			if ("ProcessEntity".equals(entity.getItemValueString("type"))) {

				if (processid==entity.getItemValueInteger("numprocessid")) {
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

				int processid=entity.getItemValueInteger("numprocessid");
				
				ItemCollection processEntity=findProcessEntityById(processid);
				
				if (group.equals(processEntity.getItemValueString("txtWorkflowGroup"))) {
					result.add(entity);
				}
			}

		}
		
		return result;
	}
}

class SequenceFlow {
	int from,to;
	String id;
	public SequenceFlow(int from,int to,int id) {
		this.from=from;
		this.to=to;
		this.id=""+from + "_"+id;
	}
}
