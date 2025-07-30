/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow.engine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.exceptions.ModelException;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;

/**
 * The ModelService provides methods to load and save BPMNModel data form the
 * Database and methods to find model versions based on meta information.
 * <p>
 * Note: BPMModel instances are not Thread save. For this reason the service
 * the method getBPMNModel returns an exclusive version of a BPMNModel object.
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
    private final ConcurrentHashMap<String, BPMNModelData> modelDataStore = new ConcurrentHashMap<>();

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

    }

    /**
     * Adds a BPMNModel with its metadata into the local model store.
     * <p>
     * The method pre initialize all processes within the BPMNModel to
     * avoid the lazing loading.
     * <p>
     * The Method does not store the data in the database. To store new model data
     * call {@code saveModel}
     * 
     */
    public void addModelData(String version, BPMNModel model, ItemCollection metadata) {
        // pre open all processes
        try {
            Set<BPMNProcess> processes = model.getProcesses();
            for (BPMNProcess process : processes) {
                process.init();
            }
        } catch (BPMNModelException e) {
            logger.warning("Failed to open process: " + e.getMessage());
            e.printStackTrace();
        }

        // if metaData not provided create one on the fly...
        if (metadata == null) {
            metadata = new ItemCollection();
            org.w3c.dom.Document document = model.getDoc();
            byte[] rawData = null;
            try {
                // Convert Document to byte[]
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(document);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                StreamResult result = new StreamResult(outputStream);
                transformer.transform(source, result);
                rawData = outputStream.toByteArray();
            } catch (Exception e) {
                logger.warning("Failed to convert Document to byte[]: " + e.getMessage());
                e.printStackTrace();
            }

            // convert document into a byte array...
            metadata.addFileData(new FileData("model.bpmn", rawData, null, null));

        }

        modelDataStore.put(version, new BPMNModelData(version, model, metadata));

    }

    /**
     * returns a sorted String list of all stored model versions
     * 
     * @return
     */
    public List<String> getVersions() {
        List<String> versions = new ArrayList<>(modelDataStore.keySet());
        Collections.sort(versions);
        return versions;
    }

    /**
     * This method loads a Model metadata form the internal model store.
     * <p>
     * To access a BPMNModel object directly use the method
     * {@code getModel(version)}
     * 
     * @return the ItemCollection with the model meta data
     * @throws ModelException
     */
    public ItemCollection loadModelMetaData(String version) throws ModelException {
        if (version == null || version.isBlank()) {
            throw new ModelException(ModelException.INVALID_ID,
                    "Failed to load model - model version is empty!");
        }
        BPMNModelData modelData = modelDataStore.get(version);
        ItemCollection result = modelData.metadata;
        if (result == null) {
            logger.severe("invalid model version!");
            throw new ModelException(ModelException.INVALID_ID,
                    "Failed to load model - invalid model version: '" + version + "'");
        }
        return result;
    }

    /**
     * This method should return a thread save version of a stored BPMN Model
     * 
     * @param version
     * @return
     * @throws ModelException
     */
    public BPMNModel getBPMNModel(String version) throws ModelException {
        if (version == null || version.isBlank()) {
            throw new ModelException(ModelException.INVALID_ID,
                    "Failed to get model - version is empty!");
        }
        BPMNModelData modelData = modelDataStore.get(version);
        if (modelData == null) {
            throw new ModelException(ModelException.INVALID_ID,
                    "Failed to get model, not found in modelDataStore: '" + version + "'");
        }
        if (modelData.metadata.getFileData().size() == 0) {
            throw new ModelException(ModelException.INVALID_ID,
                    "Failed to get model, BPMN raw data for model version: '" + version + "'' is empty");
        }
        // get raw data from metadata
        FileData fileData = modelData.metadata.getFileData().get(0);
        InputStream inputStream = new ByteArrayInputStream(fileData.getContent());
        BPMNModel modelClone;
        try {
            // create a new instance of BPMNModel
            modelClone = BPMNModelFactory.read(inputStream);
        } catch (BPMNModelException e) {
            throw new ModelException(ModelException.INVALID_ID,
                    "Failed to get model, BPMN raw data invalid for model version: '" + version + "'", e);
        }
        return modelClone;

    }

    /**
     * Removes a BPMNModel form the local model store
     */
    public void removeModelData(String version) {
        modelDataStore.remove(version);
    }

    /**
     * Returns true if the given Model Version exists in the local store
     * <p>
     * 
     * @param group
     * @return
     */
    public boolean hasModelVersion(String version) {
        return modelDataStore.containsKey(version);

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
        for (Map.Entry<String, BPMNModelData> entry : modelDataStore.entrySet()) {
            BPMNModelData modelData = entry.getValue();
            BPMNModel model = modelData.bpmnModel;
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
        for (Map.Entry<String, BPMNModelData> entry : modelDataStore.entrySet()) {
            BPMNModelData modelData = entry.getValue();
            BPMNModel model = modelData.bpmnModel;
            Set<BPMNProcess> processList = model.getProcesses();
            for (BPMNProcess _process : processList) {
                String name = _process.getName();
                if (group.equals(name)) {
                    result.add(entry.getKey());
                }
            }
        }

        if (result.size() > 0) {
            // return first match
            return result.iterator().next();
        }

        // not found
        throw new ModelException(ModelException.INVALID_ID,
                "Failed to find version for group '" + group + "', no matching model available.");

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
            this.removeModelData(version);
            // store model into internal cache

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
            } catch (TransformerException e) {
                throw new ModelException(ModelException.INVALID_MODEL, "Failed to write model: " + e.getMessage());
            }
            // store model locally
            this.addModelData(version, model, modelItemCol);
        }
    }

    /**
     * This method deletes an existing Model from the database and removes the model
     * data form the internal ModelStore.
     * <p>
     * A model entity is identified by the type='model' and its name (model
     * version). After the model entity was deleted form the database, the method
     * will also remove the model from the ModelManager
     * 
     * @param bpmnModel
     * @throws ModelException
     */
    public void deleteModelData(String version) throws ModelException {
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
            this.removeModelData(version);
        } else {
            logger.severe("deleteModel - invalid model version!");
            throw new ModelException(ModelException.INVALID_ID, "deleteModel - invalid model version!");
        }
    }

    /**
     * Internal storage object holing the BPMN Model together with the
     * ItemCollection
     */
    class BPMNModelData {
        ItemCollection metadata;
        BPMNModel bpmnModel;
        String version;

        public BPMNModelData(String version, BPMNModel model, ItemCollection metadata) {
            this.metadata = metadata;
            this.bpmnModel = model;
            this.version = version;
        }

    }
}
