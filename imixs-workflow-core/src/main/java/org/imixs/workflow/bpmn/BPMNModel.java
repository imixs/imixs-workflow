package org.imixs.workflow.bpmn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.ModelException;

/**
 * The BPMNModel implements the Imixs Model Interface used by the Imixs
 * BPMNDefaultHandler.
 * 
 * 
 * @author rsoika
 * 
 */
public class BPMNModel implements Model {

	Map<Integer, ItemCollection> processList = null;
	Map<Integer, List<ItemCollection>> activityList = null;
	List<String> workflowGroups = null;
	ItemCollection profile = null;
	private static Logger logger = Logger.getLogger(BPMNModel.class.getName());

	public BPMNModel() {
		processList = new HashMap<Integer, ItemCollection>();
		activityList = new HashMap<Integer, List<ItemCollection>>();
		workflowGroups = new ArrayList<String>();
	}

	/**
	 * Returns the model profile entity
	 * 
	 * @return
	 */
	public ItemCollection getProfile() {
		return profile;
	}

	public void setProfile(ItemCollection profile) {
		this.profile = profile;
	}

	public List<String> getWorkflowGroups() {
		return workflowGroups;
	}

	/**
	 * Adds a ProcessEntiy into the process list
	 * 
	 * @param entity
	 * @throws ModelException
	 */
	public void addProcessEntity(ItemCollection entity) throws ModelException {
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
		processList.put(entity.getItemValueInteger("numprocessid"), entity);
	}

	/**
	 * Adds a ProcessEntiy into the process list
	 * 
	 * @param entity
	 */
	public void addActivityEntity(ItemCollection aentity) throws ModelException {
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
		ItemCollection process = this.getProcessEntity(pID, activitymodelversion);
		if (process == null) {
			logger.warning("Invalid Activiyt Entity - no numprocessid defined in model version '" + activitymodelversion
					+ "' ");
			throw new ModelException(ModelException.INVALID_MODEL_ENTRY,
					"Invalid Activiyt Entity - no numprocessid defined!");
		}

		List<ItemCollection> activities = getActivityEntityList(pID, activitymodelversion);

		activities.add(clonedEntity);
		activityList.put(pID, activities);
	}

	@Override
	public ItemCollection getProcessEntity(int processid, String modelVersion) {
		ItemCollection process = processList.get(processid);
		if (process != null && modelVersion.equals(process.getItemValueString(WorkflowKernel.MODELVERSION)))
			return process;
		else
			return null;
	}

	@Override
	public ItemCollection getActivityEntity(int processid, int activityid, String modelVersion) {
		List<ItemCollection> activities = getActivityEntityList(processid, modelVersion);
		for (ItemCollection aactivity : activities) {
			if (activityid == aactivity.getItemValueInteger("numactivityid")
					&& modelVersion.equals(aactivity.getItemValueString(WorkflowKernel.MODELVERSION))) {
				return aactivity;
			}
		}
		// not found!
		return null;
	}

	@Override
	public List<ItemCollection> getProcessEntityList(String modelVersion) {
		return new ArrayList<ItemCollection>(processList.values());
	}

	@Override
	public List<ItemCollection> getActivityEntityList(int processid, String modelVersion) {
		List<ItemCollection> result = activityList.get(processid);
		if (result == null)
			result = new ArrayList<ItemCollection>();
		return result;
	}

}
