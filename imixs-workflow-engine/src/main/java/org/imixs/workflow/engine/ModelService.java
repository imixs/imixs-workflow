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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.TransformerException;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.bpmn.OpenBPMNModelManager;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.QueryException;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.LocalBean;
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
@LocalBean
@ConcurrencyManagement
// (ConcurrencyManagementType.BEAN)
public class ModelService implements ModelManager {

    private static final Logger logger = Logger.getLogger(ModelService.class.getName());

    private OpenBPMNModelManager openBPMNModelManager = null;

    @Inject
    private DocumentService documentService;

    @Resource
    private SessionContext ctx;

    public ModelService() {
        super();
        openBPMNModelManager = new OpenBPMNModelManager();
    }

    /**
     * Returns an instance of a OpenBPMNModelManager
     * 
     * @return
     */
    public OpenBPMNModelManager getOpenBPMNModelManager() {
        return openBPMNModelManager;
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
            logger.finest("......Initializing ModelService...");
        }
        // first remove existing model entities
        Collection<ItemCollection> col = documentService.getDocumentsByType("model");
        for (ItemCollection modelEntity : col) {
            List<FileData> files = modelEntity.getFileData();
            for (FileData file : files) {
                if (debug) {
                    logger.log(Level.FINEST, "......loading file:{0}", file.getName());
                }
                byte[] rawData = file.getContent();
                InputStream bpmnInputStream = new ByteArrayInputStream(rawData);
                try {
                    BPMNModel model = BPMNModelFactory.read(bpmnInputStream);
                    addModel(model);
                } catch (BPMNModelException | ModelException e) {
                    logger.log(Level.WARNING, "Failed to load model ''{0}'' : {1}",
                            new Object[] { file.getName(), e.getMessage() });
                }
            }
        }
    }

    /**
     * This Method adds a model into the internal model store. The model will not be
     * saved in the database! Use saveModel to store the model permanently.
     */
    @Override
    public void addModel(BPMNModel model) throws ModelException {
        openBPMNModelManager.addModel(model);

    }

    /**
     * Returns the BPMN Process entity associated with a given workitem, based on
     * its attributes "$modelVersion", "$taskID". The process holds the name
     * for the attribute $worklfowGroup
     * <p>
     * The taskID has to be unique in a process. The method throws a
     * {@link ModelException} if no Process can be resolved based on the given model
     * information.
     * <p>
     * The method is called by the {@link WorkflowKernel} during the processing
     * life cycle to update the process group information.
     * 
     * @param workitem
     * @return BPMN Event entity - {@link ItemCollection}
     * @throws ModelException if no event was found
     */
    @Override
    public ItemCollection loadProcess(ItemCollection workitem) throws ModelException {
        return openBPMNModelManager.loadProcess(workitem);
    }

    @Override
    public ItemCollection loadDefinition(BPMNModel model) throws ModelException {
        return openBPMNModelManager.loadDefinition(model);
    }

    @Override
    public ItemCollection loadEvent(ItemCollection workitem) throws ModelException {
        return openBPMNModelManager.loadEvent(workitem);
    }

    @Override
    public ItemCollection loadTask(ItemCollection workitem) throws ModelException {
        return openBPMNModelManager.loadTask(workitem);
    }

    @Override
    public ItemCollection nextModelElement(ItemCollection event, ItemCollection workitem) throws ModelException {
        return openBPMNModelManager.nextModelElement(event, workitem);
    }

    /**
     * This method removes a specific ModelVersion form the internal model store.
     * The model will not be
     * removed from the database. Use deleteModel to delete the model from the
     * database.
     * 
     * @throws AccessDeniedException
     */
    public void removeModel(String modelversion) {
        openBPMNModelManager.removeModel(modelversion);
    }

    /**
     * Returns a Model by version. In case no matching model version exits, the
     * method throws a ModelException.
     **/
    @Override
    public BPMNModel getModel(String version) throws ModelException {
        return openBPMNModelManager.getModel(version);
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
    @Override
    public BPMNModel getModelByWorkitem(ItemCollection workitem) throws ModelException {
        return openBPMNModelManager.getModelByWorkitem(workitem);
    }

    /**
     * returns a sorted String list of all stored model versions
     * 
     * @return
     */
    @Override
    public List<String> getVersions() {
        return openBPMNModelManager.getVersions();
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
            boolean debug = logger.isLoggable(Level.FINE);
            String version = BPMNUtil.getVersion(model);
            // first delete existing model entities
            removeModel(version);
            // store model into internal cache
            if (debug) {
                logger.log(Level.FINEST, "......save BPMNModel ''{0}''...", version);
            }

            addModel(model);
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
            } catch (TransformerException e) {
                throw new ModelException(ModelException.INVALID_MODEL, "Failed to write model: " + e.getMessage());
            }
        }
    }

    /**
     * This method loads a Model with its meta data form the database and retuns the
     * data in ItemCollection.
     * <p>
     * To access a BPMNModel object directly use the method
     * {@code getModel(version)}
     * 
     * @return the ItemCollection with the model meta data
     */
    public ItemCollection loadModel(String version) {
        try {
            String sQuery = "(type:\"model\" AND name:\"" + version + "\")";
            Collection<ItemCollection> col;
            col = documentService.find(sQuery, 0, 1);
            if (col.size() > 0) {
                return col.iterator().next();
            }
        } catch (QueryException e) {
            logger.severe("Failed to load model: " + version);
        }
        // not found
        return null;
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
            removeModel(version);
        } else {
            logger.severe("deleteModel - invalid model version!");
            throw new InvalidAccessException(InvalidAccessException.INVALID_ID, "deleteModel - invalid model version!");
        }
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

}
