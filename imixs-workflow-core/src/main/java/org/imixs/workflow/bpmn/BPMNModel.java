package org.imixs.workflow.bpmn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
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
	Map<Integer, Collection<ItemCollection>> activityList = null;
	ItemCollection profile=null;
	private static Logger logger = Logger.getLogger(BPMNModel.class.getName());

	public BPMNModel() {
		processList = new HashMap<Integer, ItemCollection>();
		activityList = new HashMap<Integer, Collection<ItemCollection>>();
	}
	

	/**
	 * Returns the model profile entity
	 * @return
	 */
	public ItemCollection getProfile() {
		return profile;
	}

	public void setProfile(ItemCollection profile) {
		this.profile = profile;
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

		if (!"processentity".equals(entity.getItemValueString("type"))) {
			logger.warning("Invalid Process Entity - wrong type '"
					+ entity.getItemValueString("type") + "'");
			throw new ModelException(ModelException.INVALID_MODEL_ENTRY,
					"Invalid Process Entity - wrong type '"
							+ entity.getItemValueString("type") + "'");
		}
		processList.put(entity.getItemValueInteger("numprocessid"), entity);
	}

	/**
	 * Adds a ProcessEntiy into the process list
	 * 
	 * @param entity
	 */
	public void addActivityEntity(ItemCollection entity) throws ModelException {
		if (entity == null)
			return;

		if (!"activityentity".equals(entity.getItemValueString("type"))) {
			logger.warning("Invalid Activity Entity - wrong type '"
					+ entity.getItemValueString("type") + "'");
		}

		int pID = entity.getItemValueInteger("numprocessid");
		if (pID <= 0) {
			logger.warning("Invalid Activiyt Entity - no numprocessid defined!");
			throw new ModelException(ModelException.INVALID_MODEL_ENTRY,
					"Invalid Activiyt Entity - no numprocessid defined!");
		}
		Collection<ItemCollection> activities = getActivityEntityList(pID);

		activities.add(entity);
		activityList.put(pID, activities);
	}

	@Override
	public ItemCollection getProcessEntity(int processid) {
		return processList.get(processid);
	}

	@Override
	public ItemCollection getActivityEntity(int processid, int activityid) {
		Collection<ItemCollection> activities = getActivityEntityList(processid);
		for (ItemCollection aactivity : activities) {
			if (activityid == aactivity.getItemValueInteger("numactivityid")) {
				return aactivity;
			}
		}
		// not found!
		return null;
	}

	@Override
	public Collection<ItemCollection> getProcessEntityList() {
		return processList.values();
	}

	@Override
	public Collection<ItemCollection> getActivityEntityList(int processid) {
		Collection<ItemCollection> result = activityList.get(processid);
		if (result == null)
			result = new ArrayList<ItemCollection>();
		return result;
	}

}
