/*  
 *  Imixs-Workflow 
 *  
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
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
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

package org.imixs.workflow.engine;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.bpmn.BPMNParser;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;

import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;

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
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ModelService implements ModelManager {

    private ConcurrentHashMap<String, Model> modelStore = null;
    
    private static Logger logger = Logger.getLogger(ModelService.class.getName());
    @Inject
    private DocumentService documentService;
    @Resource
    private SessionContext ctx;

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
        boolean debug = logger.isLoggable(Level.FINE);
        // load existing models into the ModelManager....
        if (debug) {
            logger.finest("......Initalizing ModelService...");
        }
        // first remove existing model entities
        Collection<ItemCollection> col = documentService.getDocumentsByType("model");
        for (ItemCollection modelEntity : col) {
            List<FileData> files = modelEntity.getFileData();

            for (FileData file : files) {
                if (debug) {
                    logger.finest("......loading file:" + file.getName());
                }
                byte[] rawData = file.getContent();
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
                    logger.warning("Failed to load model '" + file.getName() + "' : " + e.getMessage());
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
        logger.info("⟳ updated model version: '" + model.getVersion() + "'");
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
        boolean debug = logger.isLoggable(Level.FINE);
        getModelStore().remove(modelversion);
        if (debug) {
            logger.finest("......removed BPMNModel '" + modelversion + "'...");
        }
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
        boolean debug = logger.isLoggable(Level.FINE);
        String modelVersion = workitem.getModelVersion();
        String workflowGroup = workitem.getItemValueString(WorkflowKernel.WORKFLOWGROUP);
        // if $workflowgroup is empty try deprecated field txtworkflowgroup
        if (workflowGroup.isEmpty()) {
            workflowGroup = workitem.getItemValueString("txtworkflowgroup");
        }

        Model model = null;
        try {
            model = getModel(modelVersion);
        } catch (ModelException me) {
            model = null;
            List<String> versions = null;
            if (debug) {
                logger.finest(me.getMessage());
            }
            // try to find latest version by regex....
            if (modelVersion != null && !modelVersion.isEmpty()) {
                versions = findVersionsByRegEx(modelVersion);
                if (!versions.isEmpty()) {
                    // we found a match by regex!
                    String newVersion = versions.get(0);
                    logger.info("...... match version '" + newVersion + "' -> by regex '" + modelVersion + "'");
                    workitem.replaceItemValue(WorkflowKernel.MODELVERSION, newVersion);
                    model = getModel(newVersion);
                }
            }

            // try to find model version by group
            if (model == null && !workflowGroup.isEmpty()) {
                versions = findVersionsByGroup(workflowGroup);
                if (!versions.isEmpty()) {
                    String newVersion = versions.get(0);
                    if (!modelVersion.isEmpty()) {
                        logger.warning("Deprecated model version: '" + modelVersion + "' -> migrating to '" + newVersion
                            + "',  $workflowgroup: '" + workflowGroup + "', $uniqueid: " + workitem.getUniqueID());
                    }
                    workitem.replaceItemValue(WorkflowKernel.MODELVERSION, newVersion);
                    model = getModel(newVersion);
                }
            }
        }

        // check if model was found....
        if (model == null) {
            throw new ModelException(ModelException.UNDEFINED_MODEL_VERSION,
                    "No matching $modelversion found for '" + modelVersion + "', $workflowgroup: '" + workflowGroup
                            + "', $uniqueid: " + workitem.getUniqueID());
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
     * Returns a sorted String list of the latest version for each workflowGroup
     * 
     * @return
     */
    public List<String> getLatestVersions() {
        List<String> result = new ArrayList<String>();
        List<String> groups = getGroups();
        for (String group : groups) {
            List<String> versions = findVersionsByGroup(group);
            if (versions != null && versions.size() > 0) {
                // add the latest version
                String version = versions.get(0);
                if (!result.contains(version)) {
                    result.add(version);
                }
            }
        }
        // sort result
        Collections.sort(result);
        return result;
    }

    /**
     * The method returns a sorted list of all available workflow groups
     * 
     * @return
     */
    public List<String> getGroups() {
        List<String> result = new ArrayList<String>();
        Collection<Model> models = getModelStore().values();
        for (Model amodel : models) {
            for (String group : amodel.getGroups()) {
                if (!result.contains(group)) {
                    result.add(group);
                }
            }
        }
        // sort result
        Collections.sort(result);
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
        boolean debug = logger.isLoggable(Level.FINE);
        List<String> result = new ArrayList<String>();
        if (debug) {
            logger.finest("......searching model versions for workflowgroup '" + group + "'...");
        }
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
     * This method returns a sorted list of model versions matching a given regex
     * for a model version. The result is sorted in reverse order, so the highest
     * version number is the first in the result list.
     * 
     * @param group
     * @return
     */
    public List<String> findVersionsByRegEx(String modelRegex) {
        boolean debug = logger.isLoggable(Level.FINE);
        List<String> result = new ArrayList<String>();
        if (debug) {
            logger.finest("......searching model versions for regex '" + modelRegex + "'...");
        }
        // try to find matching model version by regex
        Collection<Model> models = getModelStore().values();
        for (Model amodel : models) {
            if (Pattern.compile(modelRegex).matcher(amodel.getVersion()).find()) {
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
        saveModel(model, null);
    }

    /**
     * This method saves a BPMNModel into the database and adds the model into the
     * internal model store. The model is attached as an embedded file with the
     * given filename.
     * <p>
     * If a model with the same model version exists in the database the old version
     * will be deleted form the database first.
     * <p>
     * The param 'filename' is used to store the bpmn file in the correspondig model
     * document.
     * 
     * @param model
     * @throws ModelException
     */
    public void saveModel(BPMNModel model, String _filename) throws ModelException {
        if (model != null) {
            boolean debug = logger.isLoggable(Level.FINE);
            // first delete existing model entities
            deleteModel(model.getVersion());
            // store model into internal cache
            if (debug) {
                logger.finest("......save BPMNModel '" + model.getVersion() + "'...");
            }
            BPMNModel bpmnModel = (BPMNModel) model;
            addModel(model);
            ItemCollection modelItemCol = new ItemCollection();
            modelItemCol.replaceItemValue("type", "model");
            modelItemCol.replaceItemValue("$snapshot.history", 1);
            modelItemCol.replaceItemValue("$reator", ctx.getCallerPrincipal().getName());
            modelItemCol.replaceItemValue("name", bpmnModel.getVersion());
            
            // deprecated item names
            modelItemCol.replaceItemValue("namcreator", ctx.getCallerPrincipal().getName());
            modelItemCol.replaceItemValue("txtname", bpmnModel.getVersion());

            String filename = _filename;
            if (filename == null || filename.isEmpty()) {
                // default filename
                filename = bpmnModel.getVersion() + ".bpmn";
            }

            FileData fileData = new FileData(filename, bpmnModel.getRawData(), "application/xml", null);
            modelItemCol.addFileData(fileData);
            // store model in database
            modelItemCol.replaceItemValue(DocumentService.NOINDEX, true);
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
            boolean debug = logger.isLoggable(Level.FINE);
            if (debug) {
                logger.finest("......delete BPMNModel '" + version + "'...");
            }
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

        if (version != null && !version.isEmpty()) {
            boolean debug = logger.isLoggable(Level.FINE);
            if (debug) {
                logger.finest("......load BPMNModel Entity '" + version + "'...");
            }

            Collection<ItemCollection> col = documentService.getDocumentsByType("model");
            for (ItemCollection modelEntity : col) {
                // test version...
                String currentVersion = modelEntity.getItemValueString("txtname");
                if (version.equals(currentVersion)) {
                    return modelEntity;
                }
            }
        } else {
            logger.severe("deleteModel - invalid model version!");
            throw new InvalidAccessException(InvalidAccessException.INVALID_ID,
                    "loadModelEntity - invalid model version!");
        }
        return null;

    }

    /**
     * Returns a BPMN DataObject, part of a Task or Event element, by its name
     * <p>
     * DataObjects can be associated in a BPMN Diagram with a Task or an Event
     * element
     * 
     * @param bpmnElement
     * @return
     */
    @SuppressWarnings("unchecked")
    public String getDataObject(ItemCollection bpmnElement, String name) {

        List<List<String>> dataObjects = bpmnElement.getItemValue("dataObjects");

        if (dataObjects != null && dataObjects.size() > 0) {
            for (List<String> dataObject : dataObjects) {
                String key = dataObject.get(0);
                if (name.equals(key)) {
                    return dataObject.get(1);
                }
            }
        }
        // not found!
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
            modelStore = new ConcurrentHashMap<String, Model>();
            init();
        }
        return modelStore;
    }

}
