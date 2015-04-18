/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.workflow.jee.ejb;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.imixs.workflow.ExtendedModel;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.jee.jpa.EntityIndex;
import org.imixs.workflow.xml.EntityCollection;
import org.imixs.workflow.xml.XMLItemCollection;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;

/**
 * The ModelManager is independend form the IX JEE Entity EJBs and uses the
 * standard IntemCollection Object as a data transfer object to comunitcate with
 * clients.
 * 
 * 
 * Since Version 1.7.0
 * 
 * The Implementation handles multiple model versions. Different Versions of an
 * Model Entity can be saved and updated. The Getter methods can be furthermore
 * Controlled by providing a valid Model Version. If no model version is set
 * this Implementation automatically defaults to the highest available
 * ModelVersion
 * 
 * @see org.imixs.workflow.ModelManager
 * @see org.imixs.workflow.jee.ejb.ModelManager
 * @author rsoika
 * 
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
public class ModelService implements ExtendedModel, ModelServiceRemote {

	@EJB
	EntityService entityService;

	@Resource
	SessionContext ctx;

	private static Logger logger = Logger.getLogger(ModelService.class
			.getName());

	@PostConstruct
	void initIndex() throws AccessDeniedException {
		// create necessary index entities
		entityService.addIndex("numProcessID", EntityIndex.TYP_INT);
		entityService.addIndex("numActivityID", EntityIndex.TYP_INT);
		entityService.addIndex("$modelversion", EntityIndex.TYP_TEXT);
		entityService.addIndex("Type", EntityIndex.TYP_TEXT);
		entityService.addIndex("txtname", EntityIndex.TYP_TEXT);
		entityService.addIndex("txtworkflowgroup", EntityIndex.TYP_TEXT);
	}

	/**
	 * This method returns the ActivityEntity with the highest Version number
	 * ($modelversion)
	 */
	public ItemCollection getActivityEntity(int processid, int activityid) {

		try {
			return findActivityEntity(processid, activityid,
					this.getLatestVersion());
		} catch (ModelException e) {
			logger.warning("[ModelService] activity " + processid + "."
					+ activityid + " not found");
			return null;
		}

	}

	public ItemCollection getActivityEntityByVersion(int processid,
			int activityid, String modelVersion) {
		return findActivityEntity(processid, activityid, modelVersion);
	}

	public ItemCollection getProcessEntity(int processid) {
		try {
			return findProcessEntity(processid, this.getLatestVersion());
		} catch (ModelException e) {
			logger.warning("[ModelService] process " + processid + " not found");
			return null;
		}
	}

	/**
	 * returns a process entity by version
	 */
	public ItemCollection getProcessEntityByVersion(int processid,
			String modelversion) {

		return findProcessEntity(processid, modelversion);
	}

	/**
	 * returns a collection of ItemCollections representing the model activity
	 * Entities for the coresponding processid
	 * 
	 * @throws ModelException
	 * 
	 */
	public List<ItemCollection> getActivityEntityList(int processid) {

		String sQuery = null;
		sQuery = "SELECT ";
		try {
			sQuery += " wi FROM Entity AS wi" + " JOIN wi.integerItems as i1 "
					+ " JOIN wi.integerItems as i2 "
					+ " JOIN wi.textItems AS v"
					+ " WHERE wi.type= 'ActivityEntity' "
					+ " AND i1.itemName = 'numprocessid' AND i1.itemValue = '"
					+ processid + "' " + " AND i2.itemName = 'numactivityid' "
					+ " AND v.itemName = '$modelversion' AND v.itemValue = '"
					+ this.getLatestVersion() + "'"
					+ " ORDER BY i2.itemValue ASC";
			return entityService.findAllEntities(sQuery, 0, -1);
		} catch (ModelException e) {
			logger.warning("[ModelService] activitylist for process "
					+ processid + " not found");
			return null;
		}

	}

	public List<ItemCollection> getActivityEntityListByVersion(int processid,
			String aModelVersion) {

		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " wi FROM Entity AS wi" + " JOIN wi.integerItems as i1 "
				+ " JOIN wi.integerItems as i2 " + " JOIN wi.textItems AS v"
				+ " WHERE wi.type= 'ActivityEntity' "
				+ " AND i1.itemName = 'numprocessid' AND i1.itemValue = '"
				+ processid + "' " + " AND i2.itemName = 'numactivityid' "
				+ " AND v.itemName = '$modelversion' AND v.itemValue = '"
				+ aModelVersion + "'" + " ORDER BY i2.itemValue ASC";
		return entityService.findAllEntities(sQuery, 0, -1);

	}

	/**
	 * returns a collection of ItemCollections representing the model process
	 * Entities
	 * 
	 */
	public List<ItemCollection> getProcessEntityList() {
		String sQuery = null;
		sQuery = "SELECT";
		try {
			sQuery += " wi FROM Entity AS wi " + " JOIN wi.integerItems as i  "
					+ " JOIN wi.textItems AS v"
					+ " WHERE wi.type= 'ProcessEntity' "
					+ " AND i.itemName = 'numprocessid' "
					+ " AND v.itemName = '$modelversion' AND v.itemValue = '"
					+ getLatestVersion() + "'" + " ORDER BY i.itemValue ASC";
			return entityService.findAllEntities(sQuery, 0, -1);
		} catch (ModelException e) {
			logger.warning("[ModelService] processlist not found");
			return null;
		}

	}

	/**
	 * returns a collection of ItemCollections representing the model process
	 * Entities
	 * 
	 */
	public List<ItemCollection> getProcessEntityListByVersion(
			String aModelVersion) {

		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " wi FROM Entity AS wi " + " JOIN wi.integerItems as i  "
				+ " JOIN wi.textItems AS v"
				+ " WHERE wi.type= 'ProcessEntity' "
				+ " AND i.itemName = 'numprocessid' "
				+ " AND v.itemName = '$modelversion' AND v.itemValue = '"
				+ aModelVersion + "'" + " ORDER BY i.itemValue ASC";

		return entityService.findAllEntities(sQuery, 0, -1);

	}

	public List<ItemCollection> getEnvironmentEntityList() {

		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " wi FROM Entity as wi "
				+ " WHERE wi.type= 'WorkflowEnvironmentEntity'";

		return entityService.findAllEntities(sQuery, 0, -1);

	}

	public List<ItemCollection> getEnvironmentEntityListByVersion(
			String modelversion) {

		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " environment FROM Entity AS environment "
				+ " JOIN environment.textItems as v"
				+ " WHERE environment.type = 'WorkflowEnvironmentEntity'"
				+ " AND v.itemName = '$modelversion' AND v.itemValue = '"
				+ modelversion + "'";
		return entityService.findAllEntities(sQuery, 0, -1);

	}

	/**
	 * Saves or updates an ActivityEntity represented by an ItemCollection. The
	 * Entity is unique identified of its Attributes 'numProcessID',
	 * 'numActivityID' and '$modelversion' The Method verifies that an existing
	 * instance will be updated.
	 * 
	 * @param ic
	 * 
	 * @throws AccessDeniedException
	 * @throws Exception
	 */
	public void saveActivityEntity(ItemCollection ic) throws ModelException,
			AccessDeniedException {
		int processid = ic.getItemValueInteger("numProcessID");
		if (processid <= 0)
			throw new ModelException(ModelException.INVALID_MODEL_ENTRY,
					"invalid ProcessEntity");

		int activityid = ic.getItemValueInteger("numActivityID");
		if (activityid <= 0)
			throw new ModelException(ModelException.INVALID_MODEL_ENTRY,
					"invalid ActivityEntity id: " + activityid);

		ic.replaceItemValue("Type", "ActivityEntity");

		entityService.save(ic);

	}

	/**
	 * Saves or updates a ProcessEntitiy represented by an ItemCollection. The
	 * Entity is unique identified of its Attributes 'numProcessID' and
	 * '$modelversion' The Method verifies that an existing instance will be
	 * updated.
	 * 
	 * @param ic
	 * 
	 * @throws AccessDeniedException
	 * @throws ModelException
	 */
	public void saveProcessEntity(ItemCollection ic) throws ModelException,
			AccessDeniedException {
		// Verify existing Instance
		int processid = ic.getItemValueInteger("numProcessID");
		if (processid <= 0)
			throw new ModelException(ModelException.INVALID_MODEL_ENTRY,
					"invalid ProcessEntity: " + processid);

		ic.replaceItemValue("Type", "ProcessEntity");
		entityService.save(ic);

	}

	/**
	 * Saves or updates an EnvironmentEntity represented by an ItemCollection.
	 * The Entity is unique identified of its Attributes 'txtName' and
	 * '$modelversion' The Method verifies that an existing instance will be
	 * updated.
	 * 
	 * @param ic
	 * @throws AccessDeniedException
	 * @throws Exception
	 */
	public void saveEnvironmentEntity(ItemCollection ic) throws ModelException,
			AccessDeniedException {
		ic.replaceItemValue("Type", "WorkflowEnvironmentEntity");

		entityService.save(ic);

	}

	/**
	 * This method removes a spcific ModelVersion defined by param
	 * 'modelversion'. If modelVersion is null the method will remove all models
	 * 
	 * @throws AccessDeniedException
	 */
	public void removeModelVersion(String modelversion) throws ModelException,
			AccessDeniedException {
		// remove all existing entities
		logger.fine("remove modelVersion: " + modelversion);

		String sQuery = null;
		sQuery = "SELECT";
		if (modelversion != null) {
			// select model entities for this specific version
			sQuery += " entity FROM Entity AS entity "
					+ " JOIN entity.textItems as v"
					+ " WHERE entity.type IN ('ProcessEntity', 'ActivityEntity', 'WorkflowEnvironmentEntity')"
					+ " AND v.itemName = '$modelversion' AND v.itemValue = '"
					+ modelversion + "'";
		} else {
			// select all model entities
			sQuery += " entity FROM Entity AS entity "
					+ " WHERE entity.type IN ('ProcessEntity', 'ActivityEntity', 'WorkflowEnvironmentEntity')";
		}
		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, -1);

		logger.fine(col.size() + " model entities will be removed...");
		Iterator<ItemCollection> it = col.iterator();
		while (it.hasNext()) {
			entityService.remove(it.next());
		}

	}

	/**
	 * This mehtode finds a ProcessEntity identified by its processid
	 * (numProcessid) and model version ($modelVersion)
	 * 
	 * @param processid
	 *            , modelversion
	 * @return returns null if no entity was found
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private ItemCollection findEnvironmentEntity(String name,
			String modelversion) {
		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " environment FROM Entity AS environment "
				+ " JOIN environment.textItems AS n "
				+ " JOIN environment.textItems as v"
				+ " WHERE environment.type = 'WorkflowEnvironmentEntity'"
				+ " AND n.itemName = 'txtName' and n.itemValue = '" + name
				+ "'" + " AND v.itemName = '$modelversion' AND v.itemValue = '"
				+ modelversion + "'";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);
		Iterator<ItemCollection> it = col.iterator();
		if (!it.hasNext())
			return null;

		return col.iterator().next();
	}

	/**
	 * This method finds a ProcessEntity identified by its processid
	 * (numProcessid) and model version ($modelVersion)
	 * 
	 * @param processid
	 *            , modelversion
	 * @return returns null if no entity was found
	 * @throws Exception
	 */
	private ItemCollection findProcessEntity(int processid, String modelversion) {
		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " process FROM Entity AS process "
				+ " JOIN process.integerItems AS i "
				+ " JOIN process.textItems as v"
				+ " WHERE process.type = 'ProcessEntity'"
				+ " AND i.itemName = 'numprocessid' and i.itemValue = '"
				+ processid + "'"
				+ " AND v.itemName = '$modelversion' AND v.itemValue = '"
				+ modelversion + "'";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);
		Iterator<ItemCollection> it = col.iterator();
		if (!it.hasNext())
			return null;

		return col.iterator().next();
	}

	/**
	 * This method finds a ActivityEntity identified by its processid,
	 * activityID (numProcessid, numActivityID) and model version
	 * ($modelVersion)
	 * 
	 * @param processid
	 *            , modelversion
	 * @return returns null if no entity was found
	 * @throws Exception
	 */
	private ItemCollection findActivityEntity(int processid, int activityid,
			String modelversion) {
		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " activity FROM Entity as activity "
				+ " JOIN activity.integerItems as i "
				+ " JOIN activity.integerItems as i2 "
				+ " JOIN activity.textItems as v"
				+ " WHERE activity.type = 'ActivityEntity'"
				+ " AND i.itemName = 'numprocessid' " + " AND i.itemValue = '"
				+ processid + "'"
				+ " AND i2.itemName = 'numactivityid' and i2.itemValue = '"
				+ activityid + "' "
				+ " AND v.itemName = '$modelversion' AND v.itemValue = '"
				+ modelversion + "'";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);
		Iterator<ItemCollection> it = col.iterator();
		if (!it.hasNext())
			return null;

		return col.iterator().next();
	}

	/**
	 * This helper method finds the highest Model Version available in the
	 * system. Returns an empty String if no version was found!
	 * 
	 * @return String with the latest model version
	 */
	public String getLatestVersion() throws ModelException {
		String sQuery = "SELECT process FROM Entity AS process"
				+ " JOIN process.textItems as v"
				+ " JOIN process.textItems as n"
				+ " WHERE process.type = 'WorkflowEnvironmentEntity'"
				+ " AND n.itemName = 'txtname' AND n.itemValue = 'environment.profile'"
				+ " AND v.itemName='$modelversion' "
				+ " ORDER BY v.itemValue DESC";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);

		if (col.size() > 0) {
			Iterator<ItemCollection> iter = col.iterator();
			String sModelVersion = iter.next().getItemValueString(
					"$modelversion");
			return sModelVersion;
		} else
			throw new ModelException(ModelException.UNDEFINED_MODEL_ENTRY,
					"[ModelService] no model definition found!");
	}

	/**
	 * returns a String list of all accessible Modelversions
	 * 
	 * @return
	 */
	public List<String> getAllModelVersions() {
		ArrayList<String> colVersions = new ArrayList<String>();

		try {
			Collection<ItemCollection> colEntities = getEnvironmentEntityList();
			for (ItemCollection aworkitem : colEntities) {
				String sName = aworkitem.getItemValueString("txtName");
				String sVersion = aworkitem.getItemValueString("$modelversion");
				if ("environment.profile".equals(sName)) {
					if (colVersions.indexOf(sVersion) == -1)
						colVersions.add(sVersion);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return colVersions;
	}

	/**
	 * returns all the activities in a list for a corresponding process entity
	 * The method returns only Activities where keypublicresult != "0"
	 * 
	 * @return List<ItemCollection> of activity Entities
	 */
	public List<ItemCollection> getPublicActivities(int aprocessid) {

		return getPublicActivitiesByVersion(aprocessid, null);
	}

	/**
	 * returns all the activities in a list for a corresponding process entity
	 * The method returns only Activities where keypublicresult != "0"
	 * 
	 * @return List<ItemCollection> of activity Entities
	 */
	public List<ItemCollection> getPublicActivitiesByVersion(int aprocessid,
			String version) {
		ArrayList<ItemCollection> colActivities = null;
		try {
			// read activites from workflowmanager
			Collection<ItemCollection> colEntities;

			if (version != null)
				colEntities = getActivityEntityListByVersion(aprocessid,
						version);
			else
				colEntities = getActivityEntityList(aprocessid);

			colActivities = new ArrayList<ItemCollection>();
			for (ItemCollection aworkitem : colEntities) {
				// ad only activities with usercontrolled != No
				if (!"0".equals(aworkitem.getItemValueString("keypublicresult")))
					colActivities.add(aworkitem);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return colActivities;
	}

	/**
	 * returns a String list of all existing ProcessGroup Names
	 * 
	 * @return
	 */
	public List<String> getAllWorkflowGroups() {
		ArrayList<String> colGroups = new ArrayList<String>();

		try {
			Collection<ItemCollection> colEntities = getProcessEntityList();

			for (ItemCollection aworkitem : colEntities) {
				String sGroup = aworkitem
						.getItemValueString("txtworkflowgroup");
				if (colGroups.indexOf(sGroup) == -1)
					colGroups.add(sGroup);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return colGroups;
	}

	public List<String> getAllWorkflowGroupsByVersion(String version) {
		ArrayList<String> colGroups = new ArrayList<String>();

		try {
			Collection<ItemCollection> colEntities = getProcessEntityListByVersion(version);

			for (ItemCollection aworkitem : colEntities) {
				String sGroup = aworkitem
						.getItemValueString("txtworkflowgroup");
				if (colGroups.indexOf(sGroup) == -1)
					colGroups.add(sGroup);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return colGroups;
	}

	/**
	 * returns a list of all ProcessEntities which are the first one in each
	 * ProcessGroup. So for each ProcessGroup the ProcessEntity with the lowest
	 * processID will be returned. The method builds a cash with the best
	 * (lowest) ProcessID for each process group.
	 * 
	 * @return
	 */
	public List<ItemCollection> getAllStartProcessEntities() {
		return getAllStartProcessEntitiesByVersion(null);
	}

	/**
	 * returns a list of all ProcessEntities which are the first one in each
	 * ProcessGroup. The ModelVersion specifies the Model to be analiezed.
	 * 
	 * So for each ProcessGroup the ProcessEntity with the lowest processID will
	 * be returned. The method builds a cash with the best (lowest) ProcessID
	 * for each process group.
	 * 
	 * The collection returned will be sorted by the numProcessID
	 * 
	 * @return
	 */
	public List<ItemCollection> getAllStartProcessEntitiesByVersion(
			String version) {
		HashMap<String, ItemCollection> cashBestProcessID = new HashMap<String, ItemCollection>();
		ArrayList<ItemCollection> colStartProcessEntities = new ArrayList<ItemCollection>();

		try {
			// As the process Entity List can be unordered each processEntity
			// will be checked for the lowest ProcessID
			Collection<ItemCollection> colEntities;
			if (version == null)
				colEntities = getProcessEntityList();
			else
				colEntities = getProcessEntityListByVersion(version);

			for (ItemCollection processEntity : colEntities) {
				String sGroup = processEntity
						.getItemValueString("txtworkflowgroup");
				Integer iProcessID = processEntity
						.getItemValueInteger("numProcessID");

				// check if processid is lower as the current best id
				Integer iBestProcessID = null;
				ItemCollection itemColBestProcess = cashBestProcessID
						.get(sGroup);
				if (itemColBestProcess != null)
					iBestProcessID = itemColBestProcess
							.getItemValueInteger("numProcessID");
				if (iBestProcessID == null
						|| iProcessID.intValue() < iBestProcessID.intValue()) {
					cashBestProcessID.put(sGroup, processEntity);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// now return the arrayList of all collected best processEntities
		for (ItemCollection bestProcessEntity : cashBestProcessID.values()) {
			colStartProcessEntities.add(bestProcessEntity);
		}

		// sort processEntites by numProcessID
		Collections
				.sort(colStartProcessEntities, new ProcessEntityComparator());

		return colStartProcessEntities;
	}

	/**
	 * returns a list of all ProcessEntities for a specific ProcessGroup
	 * independent from the modelversion. The same workflowGroup contained in to
	 * different model versions will be returned twice.
	 * 
	 * @return
	 */
	public List<ItemCollection> getAllProcessEntitiesByGroup(String aGroup) {
		return getAllProcessEntitiesByGroupByVersion(aGroup, null);
	}

	/**
	 * returns a list of all ProcessEntities for a specific ProcessGroup and
	 * modelversion.
	 * 
	 * @param aGroup
	 * @param aversion
	 * @return
	 */
	public List<ItemCollection> getAllProcessEntitiesByGroupByVersion(
			String aGroup, String aversion) {
		ArrayList<ItemCollection> processList = new ArrayList<ItemCollection>();

		String sQuery = "SELECT DISTINCT process FROM Entity AS process "
				+ " JOIN process.textItems AS t2"
				+ " JOIN process.integerItems AS t3"
				+ " WHERE process.type= 'ProcessEntity'"
				+ " AND t2.itemName = 'txtworkflowgroup' "
				+ " AND t2.itemValue = '" + aGroup + "' "
				+ " AND t3.itemName = 'numprocessid'"
				+ " ORDER BY t3.itemValue asc";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, -1);

		for (ItemCollection aworkitem : col) {
			// test if version matches
			String sVersion = aworkitem.getItemValueString("$ModelVersion");
			if (aversion != null && !aversion.equals(sVersion))
				continue;

			processList.add(aworkitem);
		}
		return processList;
	}

	/**
	 * This method imports a workflow model file from an imputStream object. The
	 * method can be used to initialize a workflow system with a workflow model
	 * or provide an update service to import new model files without the need
	 * to use the Imixs RESTservice api.
	 * 
	 * The expected file format of the model file is based on the Imixs JAX-B
	 * XMLItemCollection.
	 * 
	 * The file may only contain one modelVersion! The ModelVersion is
	 * Identified by the entity type 'WorkflowEnvironmentEntity'. If more than
	 * one WorkflowEnvironmentEntity is found the method throws an
	 * ModelException.
	 * 
	 * The method automatically removes an old existing model version.
	 * 
	 * @throws AccessDeniedException
	 *             - if user is not allowed to remove old model
	 * @throws ModelException
	 *             - if fileformat is invalid
	 */
	public void importModel(InputStream input) throws ModelException,
			AccessDeniedException {
		XMLItemCollection entity;
		ItemCollection itemCollection;
		String sModelVersion = null;

		if (input == null)
			return;

		EntityCollection ecol = null;
		logger.info("[ModelService] importModel, verifing file content....");

		JAXBContext context;
		Object jaxbObject = null;
		// unmarshall the model file
		try {
			context = JAXBContext.newInstance(EntityCollection.class);
			Unmarshaller m = context.createUnmarshaller();
			jaxbObject = m.unmarshal(input);
		} catch (JAXBException e) {
			throw new ModelException(
					ModelException.INVALID_MODEL,
					"[ModelService] error - wrong xml file format - unable to import model file: ",
					e);
		}
		if (jaxbObject == null)
			throw new ModelException(ModelException.INVALID_MODEL,
					"[ModelService] error - wrong xml file format - unable to import model file!");

		ecol = (EntityCollection) jaxbObject;
		// import the model entities....
		if (ecol.getEntity().length > 0) {

			/*
			 * first iterate over all entities and find the
			 * WorkflowEnvironmentEntity. The method expects a model file with
			 * exactly one instance of WorkflowEnvironmentEntity. Otherwise an
			 * exception is thrown!
			 */
			for (XMLItemCollection aentity : ecol.getEntity()) {
				itemCollection = XMLItemCollectionAdapter
						.getItemCollection(aentity);
				// test for WorkflowEnvironmentEntity
				if ("WorkflowEnvironmentEntity".equals(itemCollection
						.getItemValueString("type"))
						&& "environment.profile".equals(itemCollection
								.getItemValueString("txtName"))) {

					if (sModelVersion != null)
						throw new ModelException(ModelException.INVALID_MODEL,
								"[ModelService] error importModel - file contains more than one modelversion!");

					sModelVersion = itemCollection
							.getItemValueString("$ModelVersion");

				}
			}

			if (sModelVersion == null)
				throw new ModelException(
						ModelException.INVALID_MODEL,
						"[ModelService] error importModel - file did "
								+ "not contain a environment.profile entity with a valid modelversion!");

			// now remove old model entries....
			logger.info("[ModelService] removing existing configuration for model version '"
					+ sModelVersion + "'");
			removeModelVersion(sModelVersion);

			// save new entities into database and update modelversion for all
			// entities.....
			for (int i = 0; i < ecol.getEntity().length; i++) {
				entity = ecol.getEntity()[i];
				itemCollection = XMLItemCollectionAdapter
						.getItemCollection(entity);

				// verify type and update the model version
				String sType = itemCollection.getItemValueString("Type");
				if ("ProcessEntity".equals(sType)
						|| "ActivityEntity".equals(sType)
						|| "WorkflowEnvironmentEntity".equals(sType)) {
					itemCollection.replaceItemValue("$modelVersion",
							sModelVersion);
					// save entity
					entityService.save(itemCollection);
				} else
					logger.warning("[ModelService] importModel: unsported entity type="
							+ sType + "!");
			}

			logger.info("[ModelService] " + ecol.getEntity().length
					+ " model entries sucessfull imported");
		}
	}

	/**
	 * compares two processEntities by its numProcessID
	 * 
	 * @author rsoika
	 * 
	 */
	static class ProcessEntityComparator implements Comparator<ItemCollection>,
			Serializable {
		private static final long serialVersionUID = 1L;

		public int compare(ItemCollection o1, ItemCollection o2) {
			return (o1.getItemValueInteger("numProcessID") - o2
					.getItemValueInteger("numProcessID"));

		}

	}

}
