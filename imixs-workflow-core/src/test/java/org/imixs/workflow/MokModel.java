package org.imixs.workflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.imixs.workflow.exceptions.InvalidAccessException;

/**
 * Static mokup model
 * 
 * 100.10 - save -> 100
 * <p>
 * 100.11 -foollow up -> 100
 * <p>
 * 100.20 - forward -> 200
 * <p>
 * 
 * 
 * entities is a mapobject storing the process and activity entities. the key is
 * a string (e.g. '100', '100.10)
 * 
 * @author rsoika
 * 
 */
public class MokModel implements Model {

	public final static String DEFAULT_MODEL_VERSION="1.0.0";

	Map<String, ItemCollection> entities = null;
	ItemCollection definition=null;

	/** 
	 * 
	 * prepare a mok model
	 **/
	public MokModel() {
		ItemCollection entity;

		// build list
		entities = new HashMap<String, ItemCollection>();

		// ProcessEntity 'OPEN'
		entity = new ItemCollection();
		entity.replaceItemValue("numprocessid", 100);
		entity.replaceItemValue("txtName", "OPEN");
		entities.put("100", entity);

		// ProcessEntity 'COMPLETED'
		entity = new ItemCollection();
		entity.replaceItemValue("numprocessid", 200);
		entity.replaceItemValue("txtName", "COMPLETED");
		entities.put("200", entity);

		// Activity 'SAVE' 100.10
		entity = new ItemCollection();
		entity.replaceItemValue("numprocessid", 100);
		entity.replaceItemValue("numnextprocessid", 100);
		entity.replaceItemValue("numactivityid", 10);
		entity.replaceItemValue("txtName", "SAVE");
		entities.put("100.10", entity);

		// Activity 'FOLLOWUP' 100.11
		entity = new ItemCollection();
		entity.replaceItemValue("numprocessid", 100);
		entity.replaceItemValue("numnextprocessid", 100);
		entity.replaceItemValue("numactivityid", 11);
		entity.replaceItemValue("txtName", "FOLLOWUP");
		entity.replaceItemValue("numNextActivityID", 20);
		entity.replaceItemValue("keyFollowUp", "1");
		entities.put("100.11", entity);

		// Activity 'FORWARD' 100.20
		entity = new ItemCollection();
		entity.replaceItemValue("numprocessid", 100);
		entity.replaceItemValue("numnextprocessid", 200);
		entity.replaceItemValue("numactivityid", 20);
		entity.replaceItemValue("txtName", "FORWARD");
		entities.put("100.20", entity);

		// Activity 'SAVE' 200.10
		entity = new ItemCollection();
		entity.replaceItemValue("numprocessid", 200);
		entity.replaceItemValue("numnextprocessid", 200);
		entity.replaceItemValue("numactivityid", 10);
		entity.replaceItemValue("txtName", "SAVE");
		entities.put("200.10", entity);

		
		definition=new ItemCollection();
		definition.replaceItemValue(WorkflowKernel.MODELVERSION,DEFAULT_MODEL_VERSION);
	}

	@Override
	public String getVersion() {		
		return DEFAULT_MODEL_VERSION;
	}

	@Override
	public ItemCollection getTask(int processid) throws InvalidAccessException {
		return entities.get(processid + "");
	}

	@Override
	public ItemCollection getEvent(int processid, int activityid)
			throws InvalidAccessException {

		return entities.get( processid + "." + activityid);
	}

	@Override
	public List<ItemCollection> findAllTasks() {
		Vector<ItemCollection> list = new Vector<ItemCollection>();
		list.add(getTask(100));
		list.add(getTask(200));
		return list;
	}

	@Override
	public List<ItemCollection> findAllEventsByTask(int processid)
		 {
		// not implemented
		return null;
	}

	@Override
	public ItemCollection getDefinition() {
		return definition;
	}

	

	@Override
	public List<String> getGroups() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ItemCollection> findTasksByGroup(String group) {
		// TODO Auto-generated method stub
		return null;
	}

	

}
