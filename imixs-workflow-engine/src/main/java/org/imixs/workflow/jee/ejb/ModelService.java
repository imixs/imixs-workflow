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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;

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
@LocalBean
public class ModelService implements ModelManager {

	private Map<String, Model> modelStore = null;
	private static Logger logger = Logger.getLogger(ModelService.class.getName());
	@EJB
	EntityService entityService;

	public ModelService() {
		super();
		// create store
		modelStore = new HashMap<String, Model>();
	}

	@PostConstruct
	void initIndex() throws AccessDeniedException {
		// load models....
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
		Model model = null;
		try {
			model = getModel(modelVersion);
		} catch (ModelException me) {
			logger.fine(me.getMessage());
			if (!workflowGroup.isEmpty()) {
				logger.fine("searching latest model version for workflowgroup '" + workflowGroup + "'...");
				// try to find matching model version by group
				for (Model amodel : modelStore.values()) {
					if (amodel.getGroups().contains(workflowGroup)) {
						// higher version?
						if (amodel.getVersion().compareTo(bestVersionMatch) > 0) {
							bestVersionMatch = amodel.getVersion();
						}
					}
				}
				if (!bestVersionMatch.isEmpty()) {
					logger.warning("Deprecated model version: '" + modelVersion + "' -> migrating to version '"
							+ bestVersionMatch + "'");
					model = getModel(bestVersionMatch);
				}
			} else {
				// model not found and no txtworkflowgroup defined!
				throw new ModelException(ModelException.UNDEFINED_MODEL_VERSION, "Modelversion '" + modelVersion
						+ "' not found! No WorkflowGroup defind for workitem '" + workitem.getUniqueID() + "' ");
			}
		}
		if (model == null) {
			throw new ModelException(ModelException.UNDEFINED_MODEL_VERSION,
					"Modelversion '" + modelVersion + "' not found!");
		}

		return model;
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
