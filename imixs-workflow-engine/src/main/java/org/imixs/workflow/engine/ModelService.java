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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.TransformerException;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
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
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;

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
@ConcurrencyManagement
public class ModelService {

    private static final Logger logger = Logger.getLogger(ModelService.class.getName());

    protected ModelManager modelManager = null;
    // private final Map<String, ItemCollection> modelEntityStore = new
    // ConcurrentHashMap<>();
    private final SortedMap<String, ItemCollection> modelEntityStore = new TreeMap<>();

    @Inject
    protected DocumentService documentService;

    @Resource
    protected SessionContext ctx;

    public ModelService() {
        super();
    }

    /**
     * Lazy loading of ModelManager
     */
    @PostConstruct
    public void init() {
        modelManager = new ModelManager();
    }

    /**
     * Returns an instance of a modelManager
     * 
     * @return
     */
    public ModelManager getModelManager() {
        return modelManager;
    }

    /**
     * Deprecated method, use instead getModelManager()..
     **/
    @Deprecated
    public void addModel(BPMNModel model) throws ModelException {
        modelManager.addModel(model);
    }

    /**
     * Deprecated method, use instead getModelManager()..
     **/
    @Deprecated
    public ItemCollection loadProcess(ItemCollection workitem) throws ModelException {
        return modelManager.loadProcess(workitem);
    }

    /**
     * Deprecated method, use instead getModelManager()..
     **/
    @Deprecated
    public ItemCollection loadDefinition(BPMNModel model) throws ModelException {
        return modelManager.loadDefinition(model);
    }

    /**
     * Deprecated method, use instead getModelManager()..
     **/
    @Deprecated
    public ItemCollection loadEvent(ItemCollection workitem) throws ModelException {
        return modelManager.loadEvent(workitem);
    }

    /**
     * Deprecated method, use instead getModelManager()..
     **/
    @Deprecated
    public ItemCollection loadTask(ItemCollection workitem) throws ModelException {
        return modelManager.loadTask(workitem);
    }

    /**
     * Deprecated method, use instead getModelManager()..
     **/
    @Deprecated
    public ItemCollection nextModelElement(ItemCollection event, ItemCollection workitem) throws ModelException {
        return modelManager.nextModelElement(event, workitem);
    }

    /**
     * Deprecated method, use instead getModelManager()..
     **/
    @Deprecated
    public void removeModel(String modelversion) {
        modelManager.removeModel(modelversion);
    }

    /**
     * Deprecated method, use instead getModelManager()..
     **/
    @Deprecated
    public BPMNModel getModel(String version) throws ModelException {
        return modelManager.getModel(version);
    }

    /**
     * Returns a BPMNModel by a workItem. The workitem must at least provide the
     * item '$modelversion' or '$workflowgroup' to resolve the model.
     * The $modelversion can be a regular expression.
     * <p>
     * The BPMNModel instance can be used to access all BPMN model elements.
     * 
     * @param version - $modelVersion
     * @return a BPMN model instance or null if not found by $modelVersion
     * 
     * @see https://github.com/imixs/open-bpmn/tree/master/open-bpmn.metamodel
     */
    @Deprecated
    public BPMNModel getModelByWorkitem(ItemCollection workitem) throws ModelException {
        return modelManager.getModelByWorkitem(workitem);
    }

    /**
     * Deprecated method, use instead getModelManager()..
     **/
    @Deprecated
    public List<String> getVersions() {
        return modelManager.getVersions();
    }

    /**
     * This method returns a sorted list of all model groups stored in a given
     * BPMNModel.
     * <p>
     * Note: A workflow group may exist in different models by the same name!
     * 
     * @param group
     * @return
     */
    @Deprecated
    public List<String> getWorkflowGroups(BPMNModel model) {
        List<String> result = new ArrayList<String>();
        Set<BPMNProcess> processList = model.getProcesses();
        for (BPMNProcess _process : processList) {
            result.add(_process.getName());
        }
        // sort result
        Collections.sort(result);
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
            modelManager.removeModel(version);
            // store model into internal cache

            modelManager.addModel(model);
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
            logger.log(Level.INFO, "Import bpmn-model: {0} ▶ {1}", new Object[] { _filename,
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
    public ItemCollection loadModel(String version) {
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
            modelManager.removeModel(version);
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
