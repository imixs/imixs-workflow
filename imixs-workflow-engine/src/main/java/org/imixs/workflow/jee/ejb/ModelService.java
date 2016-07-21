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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.Model;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.jee.jpa.EntityIndex;

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
@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Singleton
public class ModelService implements ModelManager {

	@EJB
	EntityService entityService;

	@Resource
	SessionContext ctx;

	private Map<String, Model> modelStore = null;

	private static Logger logger = Logger.getLogger(ModelService.class.getName());

	@PostConstruct
	void initIndex() throws AccessDeniedException {
		// create necessary index entities
		entityService.addIndex("numProcessID", EntityIndex.TYP_INT);
		entityService.addIndex("numActivityID", EntityIndex.TYP_INT);
		entityService.addIndex("$modelversion", EntityIndex.TYP_TEXT);
		entityService.addIndex("Type", EntityIndex.TYP_TEXT);
		entityService.addIndex("txtname", EntityIndex.TYP_TEXT);
		entityService.addIndex("txtworkflowgroup", EntityIndex.TYP_TEXT);

		// create store

		modelStore = new HashMap<String, Model>();
	}

	/**
	 * Returns a Model by version. In case no matching model version exits, the
	 * method throws a ModelException.
	 **/
	@Override
	public Model getModel(String version) throws ModelException {
		Model model = modelStore.get(version);
		if (model == null) {
			throw new ModelException(ModelException.UNDEFINED_MODEL_VERSION,
					"Modelversion '" + version + "' not found!");
		}

		return model;
	}

	/**
	 * Returns a Model matching a given workitem. In case not matching model
	 * version exits, the method returns the highest Model Version matching the
	 * corresponding workflow group.
	 * 
	 * The method throws a ModelException in case the model version did not
	 * exits.
	 **/
	@Override
	public Model getModelByWorkitem(ItemCollection workitem) throws ModelException {
		String modelVersion = workitem.getModelVersion();
		String workflowGroup = workitem.getItemValueString("txtWorkflowGroup");
		String bestVersionMatch = "";
		Model model = getModel(modelVersion);
		if (model == null && !workflowGroup.isEmpty()) {
			// try to find matching model version by group
			for (Model amodel : modelStore.values()) {
				ItemCollection definition = amodel.getDefinition();
				if (definition != null) {
					String group = definition.getItemValueString("txtworkflowgroup");
					String version = definition.getModelVersion();

					if (workflowGroup.equals(group)) {
						// higher version?
						if (version.compareTo(bestVersionMatch) > 0) {
							bestVersionMatch = version;
						}
					}

				}
			}

			if (!bestVersionMatch.isEmpty()) {
				logger.warning("Deprecated model version: '" + modelVersion + "' -> migrating to version '"
						+ bestVersionMatch + "'");
				model = getModel(bestVersionMatch);

			}
		}

		if (model == null) {
			throw new ModelException(ModelException.UNDEFINED_MODEL_VERSION,
					"Modelversion '" + modelVersion + "' not found!");
		}

		return model;
	}

	/**
	 * This helper method finds the highest Model Version available in the
	 * system corresponding a given workitem. The method compares the
	 * txtWorkflowGroup and $ProcessID. The method returns an empty String if no
	 * matching version was found!
	 * 
	 * @return String with the latest model version for the given workitem
	 */
	public String xgetLatestVersionByWorkitem(ItemCollection workitem) throws ModelException {

		// fist select all versions for matching processid and workflowgroup
		String workflowGroup = workitem.getItemValueString("txtWorkflowGroup");
		int processId = workitem.getItemValueInteger(WorkflowService.PROCESSID);

		// find all process entities
		String sQuery = "SELECT process FROM Entity AS process" + " JOIN process.textItems as g"
				+ " JOIN process.integerItems as n" + " WHERE process.type = 'ProcessEntity'"
				+ " AND n.itemName = 'numprocessid' AND n.itemValue = " + processId
				+ " AND g.itemName='txtworkflowgroup' AND g.itemValue= '" + workflowGroup + "'";

		List<ItemCollection> col = entityService.findAllEntities(sQuery, 0, -1);

		// now sort the result by $modelversion
		Collections.sort(col, new ItemCollectionComparator(WorkflowService.MODELVERSION));

		if (col.size() > 0) {
			Iterator<ItemCollection> iter = col.iterator();
			String sModelVersion = iter.next().getItemValueString("$modelversion");
			return sModelVersion;
		} else
			throw new ModelException(ModelException.UNDEFINED_MODEL_ENTRY,
					"[ModelService] no matching model definition found for $processid=" + processId + " workflowgroup='"
							+ workflowGroup + "'!");
	}

	@Override
	public void addModel(Model model) throws ModelException {

		ItemCollection definition = model.getDefinition();
		if (definition == null) {
			throw new ModelException(ModelException.INVALID_MODEL, "Invalid Model: Model Definition not provided! ");
		}
		String modelVersion = definition.getModelVersion();
		if (modelVersion.isEmpty()) {
			throw new ModelException(ModelException.INVALID_MODEL, "Invalid Model: Model Version not provided! ");
		}

		modelStore.put(modelVersion, model);

	}

	/**
	 * This method removes a specific ModelVersion. If modelVersion is null the
	 * method will remove all models
	 * 
	 * @throws AccessDeniedException
	 */
	public void removeModel(String modelversion) {
		modelStore.remove(modelversion);
		logger.info("removed modelversion: " + modelversion);
	}

	/**
	 * returns a String list of all accessible Modelversions
	 * 
	 * @return
	 */
	public List<String> getAllModelVersions() {
		// convert Set to List
		Set<String> set = modelStore.keySet();
		return new ArrayList<String>(set);
	}

	/**
	 * returns a String list of all existing ProcessGroup Names
	 * 
	 * @return
	 */
	public List<String> getAllWorkflowGroups(String modelVersion) {
		ArrayList<String> colGroups = new ArrayList<String>();
		// iterating over values only
		for (Model model : modelStore.values()) {
			ItemCollection definition = model.getDefinition();
			if (definition != null) {
				String group = definition.getItemValueString("txtworkflowgroup");
				if (!group.isEmpty()) {
					colGroups.add(group);
				}
			}
		}
		return colGroups;
	}

	

	/**
	 * Imports a BPMN model. Existing model with same model version will be
	 * replaced by this method.
	 * 
	 * @param bpmnmodel
	 * @throws ModelException
	 */
	public void importBPMNModel(BPMNModel bpmnmodel) throws ModelException {

		if (bpmnmodel == null || bpmnmodel.getDefinition() == null) {
			throw new ModelException(ModelException.INVALID_MODEL,
					"Invalid Model file: No Imixs Definitions Extension found! ");

		}

		// verify $modelversion
		String modelVersion = bpmnmodel.getDefinition().getItemValueString("$ModelVersion");

		logger.fine("import BPMN model $modelversion=" + modelVersion + "....");
		if (modelVersion.isEmpty()) {
			throw new ModelException(ModelException.INVALID_MODEL, "Invalid Model: Model Version not provided! ");
		}

		addModel(bpmnmodel);

		logger.info("imported BPMN model $modelversion=" + modelVersion);

	}

}
