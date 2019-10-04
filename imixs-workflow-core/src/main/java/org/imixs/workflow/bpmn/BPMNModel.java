package org.imixs.workflow.bpmn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.Model;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.ModelException;

/**
 * The BPMNModel implements the Imixs Model Interface. The class is used by the
 * class BPMNModelHandler.
 * 
 * @see BPMNModelHandler
 * @author rsoika
 * 
 */
public class BPMNModel implements Model {

	public final static String TASK_ITEM_NAME = "name";
	public final static String TASK_ITEM_DOCUMENTATION = "documentation";
	public final static String TASK_ITEM_WORKFLOW_SUMMARY = "workflow.summary";
	public final static String TASK_ITEM_WORKFLOW_ABSTRACT = "workflow.abstract";
	public final static String TASK_ITEM_APPLICATION_EDITOR = "application.editor";
	public final static String TASK_ITEM_APPLICATION_ICON = "application.icon";
	public final static String TASK_ITEM_APPLICATION_TYPE = "application.type";
	public final static String TASK_ITEM_ACL_OWNER_LIST = "acl.owner_list";
	public final static String TASK_ITEM_ACL_OWNER_LIST_MAPPING = "acl.owner_list_mapping";
	public final static String TASK_ITEM_ACL_READACCESS_LIST = "acl.readaccess_list";
	public final static String TASK_ITEM_ACL_READACCESS_LIST_MAPPING = "acl.readaccess_list_mapping";
	public final static String TASK_ITEM_ACL_WRITEACCESS_LIST = "acl.writeaccess_list";
	public final static String TASK_ITEM_ACL_WRITEACCESS_LIST_MAPPING = "acl.writeaccess_list_mapping";
	public final static String TASK_ITEM_ACL_UPDATE = "acl.update";

	public final static String EVENT_ITEM_NAME = "name";
	public final static String EVENT_ITEM_DOCUMENTATION = "documentation";
	public final static String EVENT_ITEM_ACL_OWNER_LIST = "acl.owner_list";
	public final static String EVENT_ITEM_ACL_OWNER_LIST_MAPPING = "acl.owner_list_mapping";
	public final static String EVENT_ITEM_ACL_READACCESS_LIST = "acl.readaccess_list";
	public final static String EVENT_ITEM_ACL_READACCESS_LIST_MAPPING = "acl.readaccess_list_mapping";
	public final static String EVENT_ITEM_ACL_WRITEACCESS_LIST = "acl.writeaccess_list";
	public final static String EVENT_ITEM_ACL_WRITEACCESS_LIST_MAPPING = "acl.writeaccess_list_mapping";
	public final static String EVENT_ITEM_ACL_UPDATE = "acl.update";

	public final static String EVENT_ITEM_WORKFLOW_RESULT = "workflow.result";
	public final static String EVENT_ITEM_WORKFLOW_PUBLIC = "workflow.public";
	public final static String EVENT_ITEM_WORKFLOW_PUBLIC_ACTORS = "workflow.public_actors";
	public final static String EVENT_ITEM_READACCESS = "$readaccess";
	public final static String EVENT_ITEM_HISTORY_MESSAGE = "history.message";
	public final static String EVENT_ITEM_MAIL_SUBJECT = "mail.subject";
	public final static String EVENT_ITEM_MAIL_BODY = "mail.body";
	public final static String EVENT_ITEM_MAIL_TO_LIST = "mail.to_list";
	public final static String EVENT_ITEM_MAIL_TO_LIST_MAPPING = "mail.to_list_mapping";
	public final static String EVENT_ITEM_MAIL_CC_LIST = "mail.cc_list";
	public final static String EVENT_ITEM_MAIL_CC_LIST_MAPPING = "mail.cc_list_mapping";
	public final static String EVENT_ITEM_MAIL_BCC_LIST = "mail.bcc_list";
	public final static String EVENT_ITEM_MAIL_BCC_LIST_MAPPING = "mail.bcc_list_mapping";
	public final static String EVENT_ITEM_RULE_ENGINE = "rule.engine";
	public final static String EVENT_ITEM_RULE_DEFINITION = "rule.definition";

	public final static String EVENT_ITEM_REPORT_NAME = "report.name";
	public final static String EVENT_ITEM_REPORT_PATH = "report.path";
	public final static String EVENT_ITEM_REPORT_OPTIONS = "report.options";
	public final static String EVENT_ITEM_REPORT_TARGET = "report.target";
	public final static String EVENT_ITEM_VERSION_MODE = "version.mode";
	public final static String EVENT_ITEM_VERSION_EVENT = "version.event";

	public final static String EVENT_ITEM_TIMER_ACTIVE = "timer.active";
	public final static String EVENT_ITEM_TIMER_SELECTION = "timer.selection";
	public final static String EVENT_ITEM_TIMER_DELAY = "timer.delay";
	public final static String EVENT_ITEM_TIMER_DELAY_UNIT = "timer.delay_unit";
	public final static String EVENT_ITEM_TIMER_DELAY_BASE = "timer.delay_base";
	public final static String EVENT_ITEM_TIMER_DELAY_BASE_PROPERTY = "timer.delay_base_property";

	private Map<Integer, ItemCollection> taskList = null;
	private Map<Integer, List<ItemCollection>> eventList = null;
	private List<String> workflowGroups = null;
	private ItemCollection definition = null;
	private byte[] rawData = null;
	private static Logger logger = Logger.getLogger(BPMNModel.class.getName());

	public BPMNModel() {
		taskList = new TreeMap<Integer, ItemCollection>();
		eventList = new TreeMap<Integer, List<ItemCollection>>();
		workflowGroups = new ArrayList<String>();
	}

	/**
	 * Returns the raw data of the BPMN file
	 * 
	 * @return
	 */
	public byte[] getRawData() {
		return rawData;
	}

	/**
	 * Set the raw data of the bpmn source file
	 * 
	 * @param rawData
	 */
	public void setRawData(byte[] data) {
		this.rawData = data;
	}

	@Override
	public String getVersion() {
		if (definition != null) {
			return definition.getModelVersion();
		}
		return null;
	}

	/**
	 * Returns the model profile entity
	 * 
	 * @return
	 */
	public ItemCollection getDefinition() {
		return new ItemCollection(definition);
	}

	/**
	 * This method returns all Tasks coming from a Start event
	 * 
	 * @return
	 */
	public List<ItemCollection> getStartTasks() {

		Collection<ItemCollection> allTasks = taskList.values();

		List<ItemCollection> result = allTasks.stream() // convert list to stream
				.filter(task -> task.getItemValueBoolean("startTask")) // we care only for startTasks
				.collect(Collectors.toList()); // collect the output and convert streams to a List
		return result;
	}

	/**
	 * This method returns all Tasks followed by a End event
	 * 
	 * @return
	 */
	public List<ItemCollection> getEndTasks() {

		Collection<ItemCollection> allTasks = taskList.values();

		List<ItemCollection> result = allTasks.stream() // convert list to stream
				.filter(task -> task.getItemValueBoolean("endTask")) // we care only for endTasks
				.collect(Collectors.toList()); // collect the output and convert streams to a List
		return result;
	}

	/**
	 * This method returns start Events for a given Start Task.
	 * <p>
	 * If the task is not a start task, the method returns null!
	 * <p>
	 * If one of the events is connected to the BPMN:startEvent then the method
	 * returns this event only!
	 * <p>
	 * In case of none event is connected to the BPMN:startEvent then the method
	 * returns all events which are not follow up events
	 * 
	 * @return
	 */
	public List<ItemCollection> getStartEvents(int taskID) {

		ItemCollection task = taskList.get(taskID);
		if (task == null || !task.getItemValueBoolean("startTask")) {
			// not a start task!
			return null;
		}

		// check the events...
		List<ItemCollection> eventsOfTask = eventList.get(taskID);

		// 1st test if we have true startEvents
		List<ItemCollection> result = eventsOfTask.stream() // convert list to stream
				.filter(event -> event.getItemValueBoolean("startEvent")) // we care only for startEvent
				.collect(Collectors.toList()); // collect the output and convert streams to a List
		if (result != null && result.size() > 0) {
			// yes there are true start events!
			return result;
		}

		// we have no true start event, so lets return all what is not a follow up
		result = eventsOfTask.stream() // convert list to stream
				.filter(event -> !"1".equals(event.getItemValueString("keyFollowUp"))) // we care only for startEvent
				.collect(Collectors.toList()); // collect the output and convert streams to a List
		return result;

	}

	@Override
	public ItemCollection getTask(int taskid) throws ModelException {
		ItemCollection task = taskList.get(taskid);
		if (task != null) {
			return new ItemCollection(task);
		} else {
			throw new ModelException(ModelException.UNDEFINED_MODEL_ENTRY,
					"BPMN Task " + taskid + " not defined by version '" + this.getVersion() + "'");
		}
	}

	@Override
	public ItemCollection getEvent(int processid, int activityid) throws ModelException {
		List<ItemCollection> activities = findAllEventsByTask(processid);
		for (ItemCollection aactivity : activities) {
			if (activityid == aactivity.getItemValueInteger("numactivityid")) {
				return new ItemCollection(aactivity);
			}
		}
		// not found!
		throw new ModelException(ModelException.UNDEFINED_MODEL_ENTRY,
				"BPMN Event " + processid + "." + activityid + " not defined by version '" + this.getVersion() + "'");
	}

	public List<String> getGroups() {
		return new ArrayList<>(workflowGroups);
	}

	/**
	 * Returns a list of all tasks. The result set is sorted by taskID.
	 * 
	 * The list is a clone of the internal map values!
	 * 
	 * @return list of tasks
	 */
	@Override
	public List<ItemCollection> findAllTasks() {
		List<ItemCollection> _tasks = new ArrayList<ItemCollection>(taskList.values());
		// clone task list
		ArrayList<ItemCollection> result = new ArrayList<ItemCollection>();
		for (ItemCollection _task : _tasks) {
			result.add(new ItemCollection(_task));
		}
		return result;

	}

	/**
	 * Returns a list of all events for a given taskID. The result set is sorted by
	 * event id (numactivityID)
	 * 
	 * @return list of tasks
	 */
	@Override
	public List<ItemCollection> findAllEventsByTask(int processid) {
		List<ItemCollection> _events = eventList.get(processid);
		if (_events == null) {
			return new ArrayList<ItemCollection>();
		}
		// clone event list
		ArrayList<ItemCollection> result = new ArrayList<ItemCollection>();
		for (ItemCollection _event : _events) {
			result.add(new ItemCollection(_event));
		}
		return result;
	}

	/***
	 * Returns a list of tasks filtered by the workflow group (txtWorkflowGroup).
	 * The result set is sorted by taskID.
	 */
	@Override
	public List<ItemCollection> findTasksByGroup(String group) {
		List<ItemCollection> result = new ArrayList<ItemCollection>();
		if (group != null && !group.isEmpty()) {
			List<ItemCollection> allTasks = findAllTasks();
			for (ItemCollection task : allTasks) {
				if (group.equals(task.getItemValueString("txtworkflowgroup"))) {
					result.add(task);
				}
			}
		}
		return result;
	}

	protected void setDefinition(ItemCollection profile) {
		this.definition = profile;
	}

	/**
	 * Adds a ProcessEntiy into the process list
	 * 
	 * @param entity
	 * @throws ModelException
	 */
	protected void addTask(ItemCollection entity) throws ModelException {
		if (entity == null)
			return;

		if (!"ProcessEntity".equals(entity.getItemValueString("type"))) {
			logger.warning("Invalid Process Entity - wrong type '" + entity.getItemValueString("type") + "'");
			throw new ModelException(ModelException.INVALID_MODEL_ENTRY,
					"Invalid Process Entity - wrong type '" + entity.getItemValueString("type") + "'");
		}

		// add group?
		String group = entity.getItemValueString("txtworkflowgroup");
		if (!workflowGroups.contains(group)) {
			workflowGroups.add(group);
		}
		taskList.put(entity.getItemValueInteger("numprocessid"), entity);
	}

	/**
	 * Adds a ProcessEntiy into the process list
	 * 
	 * @param entity
	 */
	protected void addEvent(ItemCollection aentity) throws ModelException {
		if (aentity == null)
			return;

		// we need to clone the entity because of shared events....
		ItemCollection clonedEntity = new ItemCollection(aentity);

		if (!"ActivityEntity".equals(clonedEntity.getItemValueString("type"))) {
			logger.warning("Invalid Activity Entity - wrong type '" + clonedEntity.getItemValueString("type") + "'");
		}

		int pID = clonedEntity.getItemValueInteger("numprocessid");
		if (pID <= 0) {
			logger.warning("Invalid Activiyt Entity - no numprocessid defined!");
			throw new ModelException(ModelException.INVALID_MODEL_ENTRY,
					"Invalid Activiyt Entity - no numprocessid defined!");
		}

		// test version
		String activitymodelversion = clonedEntity.getItemValueString(WorkflowKernel.MODELVERSION);
		ItemCollection process = this.getTask(pID);
		if (process == null) {
			logger.warning("Invalid Activiyt Entity - no numprocessid defined in model version '" + activitymodelversion
					+ "' ");
			throw new ModelException(ModelException.INVALID_MODEL_ENTRY,
					"Invalid Activiyt Entity - no numprocessid defined!");
		}

		List<ItemCollection> activities = findAllEventsByTask(pID);

		activities.add(clonedEntity);

		// sort event list
		Collections.sort(activities, new ItemCollectionComparator("numactivityid", true));

		eventList.put(pID, activities);
	}

}
