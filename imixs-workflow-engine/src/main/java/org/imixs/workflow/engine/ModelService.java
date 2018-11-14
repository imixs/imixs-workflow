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

package org.imixs.workflow.engine;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.bpmn.BPMNParser;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The ModelManager is independent form the IX JEE Entity EJBs and uses the
 * standard IntemCollection Object as a data transfer object to communicate with
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
	DocumentService documentService;
	@Resource
	SessionContext ctx;

	public ModelService() {
		super();
	}

	/**
	 * This method initializes the modelManager and loads existing Models from the
	 * database. The method can not be annotated with @PostConstruct because in case
	 * a servlet with @RunAs annotation will not propagate the principal in a
	 * PostConstruct. For that reason the method is called indirectly.
	 * 
	 * @throws AccessDeniedException
	 */
	void init() throws AccessDeniedException {

		// load existing models into the ModelManager....
		logger.info("Initalizing ModelService...");
		// first remove existing model entities
		Collection<ItemCollection> col = documentService.getDocumentsByType("model");
		for (ItemCollection modelEntity : col) {
			Map<String, List<Object>> files = modelEntity.getFiles();
			if (files != null) {
				Iterator<Map.Entry<String, List<Object>>> entries = files.entrySet().iterator();
				while (entries.hasNext()) {
					Map.Entry<String, List<Object>> entry = entries.next();
					String fileName = entry.getKey();
					logger.finest("......loading file:" + fileName);
					List<Object> fileData = entry.getValue();
					byte[] rawData = (byte[]) fileData.get(1);
					InputStream bpmnInputStream = new ByteArrayInputStream(rawData);
					try {
						Model model = BPMNParser.parseModel(bpmnInputStream, "UTF-8");

						ItemCollection definition = model.getDefinition();
						if (definition != null) {
							String modelVersion = definition.getModelVersion();

							try {
								if (getModel(modelVersion) != null) {
									// no op 
									logger.warning("Model '" + modelVersion
											+ "' is dupplicated! Please update the model version!");
								}
							} catch (ModelException e) {
								// exception is expected
								addModel(model);
							}
						}

					} catch (Exception e) {
						logger.warning("Failed to load model '" + fileName + "' : " + e.getMessage());
					}
				}
			}
		}
	}

	/**
	 * This Method adds a model into the internal model store. The model will not be
	 * saved in the database! Use saveModel to store the model permanently.
	 */
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

		logger.finest("......add BPMNModel '" + modelVersion + "'...");
		getModelStore().put(modelVersion, model);
	}

	/**
	 * This method removes a specific ModelVersion form the internal model store. If
	 * modelVersion is null the method will remove all models. The model will not be
	 * removed from the database. Use deleteModel to delete the model from the
	 * database.
	 * 
	 * @throws AccessDeniedException
	 */
	public void removeModel(String modelversion) {
		getModelStore().remove(modelversion);
		logger.finest("......removed BPMNModel '" + modelversion + "'...");
	}

	/**
	 * Returns a Model by version. In case no matching model version exits, the
	 * method throws a ModelException.
	 **/
	@Override
	public Model getModel(String version) throws ModelException {
		Model model = getModelStore().get(version);
		if (model == null) {
			throw new ModelException(ModelException.UNDEFINED_MODEL_VERSION,
					"Modelversion '" + version + "' not found!");
		}
		return model;
	}

	/**
	 * Returns a Model matching a given workitem. In case not matching model version
	 * exits, the method returns the highest Model Version matching the
	 * corresponding workflow group.
	 * 
	 * The method throws a ModelException in case the model version did not exits.
	 **/
	@Override
	public Model getModelByWorkitem(ItemCollection workitem) throws ModelException {
		String modelVersion = workitem.getModelVersion();
		String workflowGroup = workitem.getItemValueString("$workflowgroup");
		// if $workflowgroup is empty try deprecated field txtworkflowgroup
		if (workflowGroup.isEmpty()) {
			workflowGroup = workitem.getItemValueString("txtworkflowgroup");
		}

		Model model = null;
		try {
			model = getModel(modelVersion);
		} catch (ModelException me) {
			logger.finest(me.getMessage());
			if (!workflowGroup.isEmpty()) {
				// find latest version
				List<String> versions = findVersionsByGroup(workflowGroup);
				if (!versions.isEmpty()) {
					String newVersion = versions.get(0);
					logger.warning("Deprecated model version: '" + modelVersion + "' -> migrating $uniqueID="
							+ workitem.getUniqueID() + ", workflowgroup='" + workflowGroup + "' to model version '"
							+ newVersion + "' ");
					workitem.replaceItemValue(WorkflowKernel.MODELVERSION, newVersion);
					model = getModel(newVersion);
				}
			} else {
				// model not found and no match for current txtworkflowgroup
				// defined!
				model = null;
			}
		}

		// check if model was found....
		if (model == null) {
			throw new ModelException(ModelException.UNDEFINED_MODEL_VERSION,
					"Modelversion '" + modelVersion + "' not found! No matching version found for WorkflowGroup '"
							+ workflowGroup + "' ($uniqueid=" + workitem.getUniqueID() + ")");
		}

		return model;
	}

	/**
	 * returns a sorted String list of all stored model versions
	 * 
	 * @return
	 */
	public List<String> getVersions() {
		// convert Set to List
		Set<String> set = getModelStore().keySet();
		List<String> result = new ArrayList<String>(set);
		return result;
	}

	/**
	 * This method returns a sorted list of model versions containing the requested
	 * workflow group. The result is sorted in reverse order, so the highest version
	 * number is the first in the result list.
	 * 
	 * @param group
	 * @return
	 */
	public List<String> findVersionsByGroup(String group) {
		List<String> result = new ArrayList<String>();
		logger.finest("......searching model versions for workflowgroup '" + group + "'...");
		// try to find matching model version by group
		Collection<Model> models = getModelStore().values();
		for (Model amodel : models) {
			if (amodel.getGroups().contains(group)) {
				result.add(amodel.getVersion());
			}
		}
		// sort result
		Collections.sort(result, Collections.reverseOrder());
		return result;
	}

	/**
	 * This method saves a BPMNModel into the database and adds the model into the
	 * internal model store.
	 * <p>
	 * If a model with the same model version exists in the database the old version
	 * will be deleted form the database first.
	 * 
	 * @param model
	 * @throws ModelException
	 */
	public void saveModel(BPMNModel model) throws ModelException {
		if (model != null) {
			// first delete existing model entities
			deleteModel(model.getVersion());
			// store model into internal cache
			logger.finest("......save BPMNModel '" + model.getVersion() + "'...");
			BPMNModel bpmnModel = (BPMNModel) model;
			addModel(model);
			ItemCollection modelItemCol = new ItemCollection();
			modelItemCol.replaceItemValue("type", "model");
			modelItemCol.replaceItemValue("namcreator", ctx.getCallerPrincipal().getName());
			modelItemCol.replaceItemValue("txtname", bpmnModel.getVersion());
			modelItemCol.addFile(bpmnModel.getRawData(), bpmnModel.getVersion() + ".bpmn", "application/xml");
			// store model in database
			documentService.save(modelItemCol);
		}
	}

	/**
	 * This method deletes an existing Model from the database and removes the model
	 * form the internal ModelStore.
	 * <p>
	 * A model entity is identified by the type='model' and its name (model
	 * version). After the model entity was deleted form the database, the method
	 * will also remove the model from the ModelManager
	 * 
	 * @param model
	 */
	public void deleteModel(String version) {
		if (version != null && !version.isEmpty()) {
			logger.finest("......delete BPMNModel '" + version + "'...");

			Collection<ItemCollection> col = documentService.getDocumentsByType("model");
			for (ItemCollection modelEntity : col) {
				// test version...
				String oldVersion = modelEntity.getItemValueString("txtname");
				if (version.equals(oldVersion)) {
					documentService.remove(modelEntity);
				}
			}

			removeModel(version);
		} else {
			logger.severe("deleteModel - invalid model version!");
			throw new InvalidAccessException(InvalidAccessException.INVALID_ID, "deleteModel - invalid model version!");
		}
	}

	/**
	 * This method loads an existing Model Entities from the database. A model
	 * entity is identified by its name (model version).
	 * 
	 * @param model
	 */
	public ItemCollection loadModelEntity(String version) {
		if (version != null) {
			long loadTime = System.currentTimeMillis();
			// find model by version
			String searchTerm = "(type:\"model\" AND txtname:\"" + version + "\")";
			Collection<ItemCollection> col;
			try {
				col = documentService.find(searchTerm, 1, 0);
			} catch (QueryException e) {
				logger.severe("loadModelEntity - invalid version: " + e.getMessage());
				return null;
			}
			if (col != null && col.size() > 0) {
				ItemCollection model = col.iterator().next();
				logger.fine("...load BPMNModel '" + version + "' in " + (System.currentTimeMillis() - loadTime) + "ms");
				return model;
			}
			logger.finest("......BPMNModel Entity '" + version + "' not found!");
		}
		return null;
	}

	/**
	 * This method returns the modelStore or initialize it if not yet created.
	 * 
	 * @return
	 */
	private Map<String, Model> getModelStore() {
		if (modelStore == null) {
			// create store (sorted map)
			modelStore = new TreeMap<String, Model>();
			init();
		}
		return modelStore;
	}

}
