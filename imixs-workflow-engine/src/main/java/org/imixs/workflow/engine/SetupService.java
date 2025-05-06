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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.engine.index.SearchService;
import org.imixs.workflow.engine.index.UpdateService;
import org.imixs.workflow.engine.scheduler.Scheduler;
import org.imixs.workflow.engine.scheduler.SchedulerService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.QueryException;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timer;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * The SetupService EJB initializes the Imxis-Workflow engine and returns the
 * current status.
 * <p>
 * During startup, the service loads a default model defined by the optional
 * environment variable 'MODEL_DEFAULT_DATA'. This variable can point to
 * multiple model resources separated by a ';'. A model resource file must have
 * the file extension '.bpmn'.
 * <p>
 * The variable can be defined also in the imixs.properties file. In this case
 * the variable is named: 'model.default.data'.
 * <p>
 * Optional it is also possible to provide setup workflow initial data in a XML
 * file.
 * <p>
 * Finally the service starts optional registered scheduler services.
 * <p>
 * With the method 'getModelCount' the service returns the current status of the
 * workflow engine by returning the count of valid workflow models.
 * <p>
 * The SetupSerivce has a migration method to migrate old Workflow Schedulers
 * into the new Scheduler concept. The method migrateWorkflowScheduler is nust
 * for migration and can be deprecated in future releases.
 * 
 * @author rsoika
 * @version 1.0
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Startup
@Singleton
public class SetupService {
    public static String SETUP_OK = "OK";
    public static String MODEL_INITIALIZED = "MODEL_INITIALIZED";

    private static final Logger logger = Logger.getLogger(SetupService.class.getName());

    @Inject
    @ConfigProperty(name = "model.default.data")
    private Optional<String> modelDefaultData;

    @Inject
    @ConfigProperty(name = "model.default.data.overwrite", defaultValue = "false")
    private boolean modelDefaultDataOverwrite;

    @Inject
    private DocumentService documentService;

    @Inject
    private SearchService indexSearchService;

    @Inject
    private UpdateService indexUpdateService;

    @Inject
    private ModelService modelService;

    @Inject
    private SchedulerService schedulerService;

    @Resource
    private jakarta.ejb.TimerService timerService;

    @Inject
    protected Event<SetupEvent> setupEvents;

    /**
     * This method start the system setup during deployment
     * 
     * @throws AccessDeniedException
     */
    @PostConstruct
    public void startup() {

        // created with linux figlet
        logger.info("   ____      _");
        logger.info("  /  _/_ _  (_)_ __ ___   Workflow");
        logger.info(" _/ //  ' \\/ /\\ \\ /(_-<   Engine");
        logger.info("/___/_/_/_/_//_\\_\\/___/   V6.2");
        logger.info("");

        logger.info("├── initializing models...");

        // Load existing models
        initModels();
        // if no models are loaded scan for default models
        List<String> models = modelService.getVersions();
        if (models.isEmpty() || modelDefaultDataOverwrite == true) {
            scanDefaultModels();
        }

        // Finally fire the SetupEvent. This allows CDI Observers to react on the setup
        if (setupEvents != null) {
            // create Group Event
            SetupEvent setupEvent = new SetupEvent();
            setupEvents.fire(setupEvent);
        } else {
            logger.warning("Missing CDI support for Event<SetupEvent> !");
        }

        // migrate old workflow scheduler
        migrateWorkflowScheduler();

        // Finally start optional schedulers
        logger.info("├── initializing schedulers...");
        schedulerService.startAllSchedulers();

    }

    /**
     * This method loads all existing Model Entities from the database and adds the
     * BPMNModel objects into the ModelManager.
     * <p>
     * The method also checks the stored models for duplicates and removes
     * deprecated duplicated model entities from the database.
     * 
     * @throws AccessDeniedException
     */
    private void initModels() throws AccessDeniedException {
        boolean debug = logger.isLoggable(Level.FINE);

        // first remove existing model entities
        Collection<ItemCollection> col = documentService.getDocumentsByType("model");
        logger.finest("...found " + col.size() + " model entities");
        List<ItemCollection> deprecatedModelEntities = new ArrayList<>();
        for (ItemCollection modelEntity : col) {
            logger.finest(".. " + modelEntity.getItemValueString("name") + " created -> "
                    + modelEntity.getItemValueDate("$created"));
            List<FileData> files = modelEntity.getFileData();
            for (FileData file : files) {
                if (debug) {
                    logger.log(Level.FINEST, "......loading file:{0}", file.getName());
                }
                byte[] rawData = file.getContent();
                InputStream bpmnInputStream = new ByteArrayInputStream(rawData);
                try {
                    BPMNModel model = BPMNModelFactory.read(bpmnInputStream);
                    String version = BPMNUtil.getVersion(model);
                    // test if model is a deprecated duplicate entry!
                    if (modelService.hasModelVersion(version)) {
                        logger.warning("│   ├── duplicated Model Entity found (" + modelEntity.getUniqueID()
                                + ") for model version '" + version
                                + "' - entity will be removed!");
                        deprecatedModelEntities.add(modelEntity);
                    } else {
                        logger.log(Level.INFO, "│   ├── loaded model: {0} ▶ {1}", new Object[] { file.getName(),
                                BPMNUtil.getVersion(model) });
                        // Add the model into the ModelManger and put the model into the ModelService
                        // model store
                        modelService.addModelData(version, model, modelEntity);
                        // modelService.getModelEntityStore().put(version, modelEntity);
                    }
                } catch (BPMNModelException e) {
                    logger.log(Level.WARNING, "Failed to load model ''{0}'' : {1}",
                            new Object[] { file.getName(), e.getMessage() });
                }
            }
        }

        // remove duplicated entries (this should not happen!)
        if (deprecatedModelEntities.size() > 0) {
            for (ItemCollection deprecatedEntry : deprecatedModelEntities) {
                documentService.remove(deprecatedEntry);
            }
        }
    }

    /**
     * Returns the count of available model versions
     * 
     * @return
     */
    public int getModelVersionCount() {
        return modelService.getVersions().size();
    }

    /**
     * Check database access
     * <p>
     * 
     * @return true if database access was successful
     */
    public boolean checkDatabase() {
        try {
            // check database access - with a random uniqueid
            documentService.load(WorkflowKernel.generateUniqueID());
            // we do not care about the result - just if the call was sucessful
        } catch (Exception e) {
            // database/index failed!
            return false;
        }
        return true;
    }

    /**
     * Check database index
     * <p>
     * 
     * @return true if database index access was successful
     */
    public boolean checkIndex() {
        try {
            // check the index
            // write dummy
            ItemCollection dummy = new ItemCollection();
            // ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)
            dummy.setItemValueUnique(WorkflowKernel.UNIQUEID, "00000000-aaaa-0000-0000-luceneindexcheck");
            String checksum = "" + System.currentTimeMillis();
            dummy.setItemValue("$workflowsummary", checksum);
            List<ItemCollection> dummyList = new ArrayList<ItemCollection>();
            dummyList.add(dummy);
            indexUpdateService.updateIndex(dummyList);

            // findStubs with the dummy unqiueid...
            List<ItemCollection> result = indexSearchService
                    .search("$uniqueid:00000000-aaaa-0000-0000-luceneindexcheck", 1, 0, null, null, true);
            // verify checksum
            dummy = result.get(0);
            if (!checksum.equals(dummy.getItemValueString("$workflowsummary"))) {
                logger.warning("SetupService - CheckIndex failed!");
                throw new Exception("lucene index check failed!");
            }
        } catch (Exception e) {
            // database/index failed!
            return false;
        }
        return true;
    }

    /**
     * This method loads the default model if no models exist in the current
     * instance
     * 
     * @return - status
     */
    public void scanDefaultModels() {
        logger.finest("......scan default models...");
        // test if we have an environment variable or a property value...

        if (!modelDefaultData.isPresent() || modelDefaultData.get().isEmpty()) {
            // no model data to scan
            return;
        }

        String modelData = modelDefaultData.get();

        // test if the model data is just a directory
        File modelDirectory = new File(modelData);
        if (modelDirectory.exists() && modelDirectory.isDirectory()) {
            logger.log(Level.INFO, "│   ├── scann default model directory ''{0}''....", modelData);
            // import all files
            File[] files = modelDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        logger.log(Level.FINE, "│   ├── import  file: ''{0}''....", file.getName());
                        FileInputStream inputStream = null;
                        try {
                            inputStream = new FileInputStream(file);
                            if (file.getName().endsWith(".bpmn")) {
                                logger.log(Level.INFO, "│   ├── import model file: ''{0}''....", file.getName());
                                importModelFromStream(inputStream);
                            }
                            if (file.getName().endsWith(".xml")) {
                                logger.log(Level.INFO, "│   ├── import XML file: ''{0}''....", file.getName());
                                importXMLFromStream(inputStream);
                            }
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(
                                    "Failed to load model configuration: " + e.getMessage()
                                            + " check 'model.default.data'",
                                    e);
                        } finally {
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }

        } else {
            // alternatively we try to scann the given files form the modelResources
            String[] modelResources = modelData.split(";");
            for (String modelResource : modelResources) {
                // try to load the resource file....
                if (modelResource.endsWith(".bpmn") || modelResource.endsWith(".xml")) {
                    logger.log(Level.INFO, "│   ├── import default model file: ''{0}''....", modelResource);
                    // if resource starts with '/' then we pickup the file form the filesystem.
                    // otherwise we load it as a resource bundle.
                    InputStream inputStream = null;
                    try {
                        if (modelResource.startsWith("/")) {
                            File initialFile = new File(modelResource);
                            inputStream = new FileInputStream(initialFile);
                        } else {
                            inputStream = SetupService.class.getClassLoader().getResourceAsStream(modelResource);
                            if (inputStream == null) {
                                throw new IOException("the resource '" + modelResource + "' could not be found!");
                            }
                        }

                        // test if it is a bpmn model?
                        if (modelResource.endsWith(".bpmn")) {
                            importModelFromStream(inputStream);
                        } else {
                            importXMLFromStream(inputStream);
                        }

                        // issue #600 return; // MODEL_INITIALIZED;
                    } catch (IOException e) {
                        throw new RuntimeException(
                                "Failed to load model configuration: " + e.getMessage() + " check 'model.default.data'",
                                e);
                    } finally {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    logger.log(Level.SEVERE, "Wrong model format: ''{0}'' - expected *.bpmn or *.xml", modelResource);
                }
            }
        }
        // SETUP_OK;
    }

    /**
     * Imports a single .bpmn file from a inputStream
     * 
     * @param modelResource
     */
    public void importModelFromStream(InputStream inputStream) {
        try {
            BPMNModel model = BPMNModelFactory.read(inputStream);
            modelService.saveModel(model);
        } catch (ModelException | BPMNModelException e) {
            throw new RuntimeException(
                    "Failed to load model configuration: " + e.getMessage() + " check 'model.default.data'",
                    e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Imports a single xml data file from a inputStream
     * 
     * @param inputStream
     */
    public void importXMLFromStream(InputStream inputStream) {
        try {
            // read Imixs XML Data Set
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int next;
            next = inputStream.read();
            while (next > -1) {
                bos.write(next);
                next = inputStream.read();
            }
            bos.flush();
            byte[] result = bos.toByteArray();
            importXmlEntityData(result);

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load model configuration: " + e.getMessage() + " check 'model.default.data'",
                    e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * this method imports an xml entity data stream. This is used to provide model
     * uploads during the system setup. The method can also import general entity
     * data like configuration data.
     * 
     * @param event
     * @throws Exception
     */
    public void importXmlEntityData(byte[] filestream) {
        XMLDocument entity;
        ItemCollection itemCollection;
        String sModelVersion = null;

        if (filestream == null)
            return;
        try {

            XMLDataCollection ecol = null;
            logger.fine("importXmlEntityData - importModel, verifing file content....");

            JAXBContext context;
            Object jaxbObject = null;
            // unmarshall the model file
            ByteArrayInputStream input = new ByteArrayInputStream(filestream);
            try {
                context = JAXBContext.newInstance(XMLDataCollection.class);
                Unmarshaller m = context.createUnmarshaller();
                jaxbObject = m.unmarshal(input);
            } catch (JAXBException e) {
                throw new ModelException(ModelException.INVALID_MODEL,
                        "error - wrong xml file format - unable to import model file: ", e);
            }
            if (jaxbObject == null)
                throw new ModelException(ModelException.INVALID_MODEL,
                        "error - wrong xml file format - unable to import model file!");

            ecol = (XMLDataCollection) jaxbObject;
            // import the model entities....
            if (ecol.getDocument().length > 0) {

                Vector<String> vModelVersions = new Vector<String>();
                // first iterrate over all enttity and find if model entries are
                // included
                for (XMLDocument aentity : ecol.getDocument()) {
                    itemCollection = XMLDocumentAdapter.putDocument(aentity);
                    // test if this is a model entry
                    // (type=WorkflowEnvironmentEntity)
                    if ("WorkflowEnvironmentEntity".equals(itemCollection.getItemValueString("type"))
                            && "environment.profile".equals(itemCollection.getItemValueString("txtName"))) {

                        sModelVersion = itemCollection.getItemValueString("$ModelVersion");
                        if (vModelVersions.indexOf(sModelVersion) == -1)
                            vModelVersions.add(sModelVersion);
                    }
                }
                // now remove old model entries....
                for (String aModelVersion : vModelVersions) {
                    logger.log(Level.FINE,
                            "importXmlEntityData - removing existing configuration for model version ''{0}''",
                            aModelVersion);
                    modelService.removeModelData(aModelVersion);
                }
                // save new entities into database and update modelversion.....
                for (int i = 0; i < ecol.getDocument().length; i++) {
                    entity = ecol.getDocument()[i];
                    itemCollection = XMLDocumentAdapter.putDocument(entity);
                    // save entity
                    documentService.save(itemCollection);
                }

                logger.log(Level.FINE, "importXmlEntityData - {0} entries sucessfull imported",
                        ecol.getDocument().length);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * This method migrates the deprecated WorkflowScheduelr configuration into the
     * new Imixs Scheduler API
     */
    public void migrateWorkflowScheduler() {
        // lets see if we have an old scheduler configuration....

        ItemCollection configItemCollection = null;
        String searchTerm = "(type:\"configuration\" AND txtname:\"org.imixs.workflow.scheduler\")";
        Collection<ItemCollection> col;
        try {
            col = documentService.find(searchTerm, 1, 0);
        } catch (QueryException e) {
            logger.log(Level.SEVERE, "loadConfiguration - invalid param: {0}", e.getMessage());
            throw new InvalidAccessException(InvalidAccessException.INVALID_ID, e.getMessage(), e);
        }

        if (col.size() == 1) {

            configItemCollection = col.iterator().next();
            ItemCollection scheduler = new ItemCollection();

            // create new scheduler??
            if (schedulerService.loadConfiguration(WorkflowScheduler.NAME) == null) {
                logger.info("...migrating deprecated workflow scheduler configuration...");
                scheduler.setItemValue("type", SchedulerService.DOCUMENT_TYPE);
                scheduler.setItemValue(Scheduler.ITEM_SCHEDULER_DEFINITION,
                        configItemCollection.getItemValue("txtConfiguration"));
                scheduler.setItemValue(Scheduler.ITEM_SCHEDULER_CLASS, WorkflowScheduler.class.getName());

                scheduler.setItemValue(Scheduler.ITEM_SCHEDULER_ENABLED,
                        configItemCollection.getItemValueBoolean("_enabled"));

                scheduler.setItemValue("txtname", WorkflowScheduler.NAME);
                schedulerService.saveConfiguration(scheduler);

            }
            Timer oldTimer = this.findTimer(configItemCollection.getUniqueID());
            if (oldTimer != null) {
                logger.info("...stopping deprecated workflow scheduler");
            }
            logger.info("...deleting deprecated workflow scheduler");
            documentService.remove(configItemCollection);

        }

    }

    /**
     * This method returns a timer for a corresponding id if such a timer object
     * exists.
     * 
     * @param id
     * @return Timer
     * @throws Exception
     */
    Timer findTimer(String id) {
        Timer timer = null;
        for (Object obj : timerService.getTimers()) {
            Timer atimer = (Timer) obj;
            String timerID = atimer.getInfo().toString();
            if (id.equals(timerID)) {
                if (timer != null) {
                    logger.log(Level.SEVERE, "more then one timer with id {0} was found!", id);
                }
                timer = atimer;
            }
        }
        return timer;
    }

}
