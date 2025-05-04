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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.TransformerException;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.BPMNProcess;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;

/**
 * The ModelService provides methods to load and save BPMNModels form the
 * Database. BPMModel instances are not Thread save. For this reason the service
 * implements a Pool to manage BPMNModel in a thread save way.
 * 
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
public class ModelService {

    private static final Logger logger = Logger.getLogger(ModelService.class.getName());

    // BPMNModel store
    private final ConcurrentHashMap<String, BPMNModel> modelStore = new ConcurrentHashMap<>();
    // Model Entity Store
    private final ConcurrentHashMap<String, ItemCollection> modelEntityStore = new ConcurrentHashMap<>();

    @Inject
    protected DocumentService documentService;

    @Resource
    protected SessionContext ctx;

    public ModelService() {
        super();
    }

    public Map<String, BPMNModel> getModelStore() {
        return modelStore;
    }

    /**
     * Lazy loading of ModelManager
     */
    @PostConstruct
    public void init() {

    }

    /**
     * returns a sorted String list of all stored model versions
     * 
     * @return
     */
    public List<String> getVersions() {
        Set<String> versions = modelStore.keySet();
        // convert to List
        List<String> result = new ArrayList<>();
        result.addAll(versions);
        Collections.sort(result, Collections.reverseOrder());
        return result;
    }

    /**
     * Adds a new model into the local model store
     */
    public void addModel(BPMNModel model) {
        String version = BPMNUtil.getVersion(model);
        modelStore.put(version, model);
    }

    /**
     * This method should return a thread save verison of a stored BPMN Model
     * 
     * @TODO implement a deep copy mechanism
     * @param version
     * @return
     */
    public BPMNModel getModel(String version) {
        logger.warning("Not thread save! - missing implementation!");
        return modelStore.get(version);
    }

    /**
     * Removes a BPMNModel form the local model store
     */
    public void removeModel(String version) {
        // Test if version exists
        if (modelStore.containsKey(version)) {
            modelStore.remove(version);
        }
    }

    /**
     * This method returns a sorted list of all model versions over all models.
     * <p>
     * 
     * @param group
     * @return
     */
    public List<String> getAllModelVersions() {
        List<String> result = new ArrayList<String>();
        for (Map.Entry<String, BPMNModel> entry : modelStore.entrySet()) {
            String version = entry.getKey();
            if (!result.contains(version)) {
                result.add(version);
            }
        }
        // sort result
        Collections.sort(result);
        return result;
    }

    /**
     * This method returns a sorted list of all model groups over all models.
     * <p>
     * Note: A workflow group may exist in different models by the same name!
     * 
     * @param group
     * @return
     */
    public List<String> findAllWorkflowGroups() {
        List<String> result = new ArrayList<String>();
        for (Map.Entry<String, BPMNModel> entry : modelStore.entrySet()) {
            BPMNModel model = entry.getValue();
            Set<BPMNProcess> processList = model.getProcesses();
            for (BPMNProcess _process : processList) {
                String name = _process.getName();
                if (!result.contains(name)) {
                    result.add(name);
                }
            }
        }
        // sort result
        Collections.sort(result);
        return result;
    }

    /**
     * Returns a version by Group.
     * The method computes a sorted list of all model versions containing the
     * requested workflow group. The result is sorted in reverse order, so the
     * highest version number is the first in the result list.
     * 
     * @param group - name of the workflow group
     * @return list of matching model versions
     * @throws ModelException
     */
    // @Override
    public String findVersionByGroup(String group) throws ModelException {
        boolean debug = logger.isLoggable(Level.FINE);
        // Sorted in reverse order
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER.reversed());
        if (debug) {
            logger.log(Level.FINEST, "......searching model versions for workflowgroup ''{0}''...", group);
        }
        // try to find matching model version by group
        for (Map.Entry<String, BPMNModel> entry : modelStore.entrySet()) {
            BPMNModel model = entry.getValue();
            Set<BPMNProcess> processList = model.getProcesses();
            for (BPMNProcess _process : processList) {
                String name = _process.getName();
                if (group.equals(name)) {
                    result.add(entry.getKey());
                }
            }
        }

        if (result.size() > 0) {
            return result.iterator().next();
        }
        return null;
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
     * This method saves a BPMNModel with its meta data into the database and adds
     * the model into the internal model store. In the database entry the BPMNmodel
     * is attached as an embedded XML file with the given filename.
     * <p>
     * If a model with the same model version exists in the database the old version
     * will be deleted form the database first.
     * <p>
     * The param 'filename' is used to store the bpmn file in the corresponding
     * model
     * document.
     * 
     * @param model
     * @throws ModelException
     */
    public void saveModel(BPMNModel model, String _filename) throws ModelException {
        if (model != null) {
            String version = BPMNUtil.getVersion(model);
            // first delete existing model entities
            this.removeModel(version);
            // store model into internal cache

            this.addModel(model);
            ItemCollection modelItemCol = new ItemCollection();
            modelItemCol.replaceItemValue("type", "model");
            modelItemCol.replaceItemValue("$snapshot.history", 1);
            modelItemCol.replaceItemValue(WorkflowKernel.CREATOR, ctx.getCallerPrincipal().getName());
            modelItemCol.replaceItemValue("name", version);

            // deprecated item names
            modelItemCol.replaceItemValue("namcreator", ctx.getCallerPrincipal().getName());
            modelItemCol.replaceItemValue("txtname", version);

            String filename = _filename;
            if (filename == null || filename.isEmpty()) {
                // default filename
                filename = version + ".bpmn";
            }
            logger.log(Level.INFO, "Import bpmn-model: {0} ▶ {1}", new Object[] { filename,
                    BPMNUtil.getVersion(model) });
            // Write the model XML object into a byte array and store it into the
            // modelItemCol as a FileData object
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                model.writeToOutputStream(model.getDoc(), baos);
                FileData fileData = new FileData(filename, baos.toByteArray(), "application/xml", null);
                modelItemCol.addFileData(fileData);
                // store model in database
                modelItemCol.replaceItemValue(DocumentService.NOINDEX, true);
                documentService.save(modelItemCol);
                modelEntityStore.put(version, modelItemCol);
            } catch (TransformerException e) {
                throw new ModelException(ModelException.INVALID_MODEL, "Failed to write model: " + e.getMessage());
            }
        }
    }

    /**
     * This method puts a model entity into the internal store.
     * 
     * @param modelEntity
     */
    public Map<String, ItemCollection> getModelEntityStore() {
        return modelEntityStore;
    }

    /**
     * This method loads a Model entity with its meta data form the internal model
     * store.
     * <p>
     * To access a BPMNModel object directly use the method
     * {@code getModel(version)}
     * 
     * @return the ItemCollection with the model meta data
     */
    public ItemCollection loadModelEntity(String version) {
        ItemCollection result = modelEntityStore.get(version);
        if (result == null) {
            logger.severe("invalid model version!");
            throw new InvalidAccessException(InvalidAccessException.INVALID_ID,
                    "findModelEntity - invalid model version: " + version);
        }
        return result;
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
                logger.log(Level.FINEST, "......delete BPMNModel ''{0}''...", version);
            }
            Collection<ItemCollection> col = documentService.getDocumentsByType("model");
            for (ItemCollection modelEntity : col) {
                // test version...
                String oldVersion = modelEntity.getItemValueString("name");
                if (version.equals(oldVersion)) {
                    documentService.remove(modelEntity);
                }
            }
            this.removeModel(version);
            modelEntityStore.remove(version);
        } else {
            logger.severe("deleteModel - invalid model version!");
            throw new InvalidAccessException(InvalidAccessException.INVALID_ID, "deleteModel - invalid model version!");
        }
    }

    /**
     * This method fetches an existing Model Entity from the model store
     * 
     * @param model
     */
    public ItemCollection findModelEntity(String version) {
        ItemCollection result = modelEntityStore.get(version);
        if (result == null) {
            logger.severe("invalid model version!");
            throw new InvalidAccessException(InvalidAccessException.INVALID_ID,
                    "findModelEntity - invalid model version: " + version);
        }
        return result;

    }

}
