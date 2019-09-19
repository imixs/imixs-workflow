/*******************************************************************************
 *  Imixs IX Workflow Technology
 *  Copyright (C) 2001, 2008 Imixs Software Solutions GmbH,  
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
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika
 *******************************************************************************/
package org.imixs.workflow.engine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.bpmn.BPMNParser;
import org.imixs.workflow.engine.scheduler.Scheduler;
import org.imixs.workflow.engine.scheduler.SchedulerService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.QueryException;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;
import org.xml.sax.SAXException;

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

	private static Logger logger = Logger.getLogger(SetupService.class.getName());

	@Inject
	@ConfigProperty(name = "model.default.data", defaultValue = "")
	private String modelDefaultData;

	@Inject
	@ConfigProperty(name = "model.default.data.overwrite", defaultValue = "false")
	private boolean modelDefaultDataOverwrite;

	@Inject
	private DocumentService documentService;

	@Inject
	private ModelService modelService;

	@Inject
	private SchedulerService schedulerService;

	@Resource
	private javax.ejb.TimerService timerService;

	@Inject
	protected Event<SetupEvent> setupEvents;

	public int getModelCount() {
		return modelService.getVersions().size();
	}

	/**
	 * This method start the system setup during deployment
	 * 
	 * @throws AccessDeniedException
	 */
	@PostConstruct
	public void startup() {
		
		logger.info("   ____      _             _      __         __    _____ ");
		logger.info("  /  _/_ _  (_)_ __ ______| | /| / /__  ____/ /__ / _/ /__ _    __");
		logger.info(" _/ //  ' \\/ /\\ \\ /(_-<___/ |/ |/ / _ \\/ __/  '_// _/ / _ \\ |/|/ /");
		logger.info("/___/_/_/_/_//_\\_\\/___/   |__/|__/\\___/_/ /_/\\_\\/_//_/\\___/__,__/ 5.1");
		logger.info("");	
		
		logger.info("...initalizing models...");

		// first we scan for default models
		List<String> models = modelService.getVersions();
		if (models.isEmpty() || modelDefaultDataOverwrite==true) {
			scanDefaultModels();
		} else {
			for (String model: models) {
				logger.info("...model: " + model + " ...OK");
			}
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
		logger.info("...initalizing schedulers...");
		schedulerService.startAllSchedulers();
	
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
		String modelData =  modelDefaultData;

		if ("".equals(modelData)) {
			// no model data to scan
			return;
		}
		
		
		String[] modelResources = modelData.split(";");
		for (String modelResource : modelResources) {

			// try to load this model

			// test if bpmn model?
			if (modelResource.endsWith(".bpmn") || modelResource.endsWith(".xml")) {
				logger.info("...uploading default model file: '" + modelResource + "'....");
				// if resource starts with '/' then we pickp the file form the filesystem.
				// otherwise we load it as a resource bundle.
				InputStream inputStream = null;
				try {
					if (modelResource.startsWith("/")) {
						File initialFile = new File(modelResource);
						inputStream = new FileInputStream(initialFile);
					} else {
						inputStream = SetupService.class.getClassLoader().getResourceAsStream(modelResource);
					}
					// parse model file....

					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					int next;

					next = inputStream.read();
					while (next > -1) {
						bos.write(next);
						next = inputStream.read();
					}
					bos.flush();
					byte[] result = bos.toByteArray();

					// is BPMN?
					if (modelResource.endsWith(".bpmn")) {
						BPMNModel model = BPMNParser.parseModel(result, "UTF-8");
						modelService.saveModel(model);
					} else {
						// XML
						importXmlEntityData(result);
					}

					return; // MODEL_INITIALIZED;
				} catch (IOException | ModelException | ParseException | ParserConfigurationException
						| SAXException e) {
					throw new RuntimeException("Failed to load model configuration: " + e.getMessage() + " check 'model.default.data'",e);
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
				logger.severe("Wrong model format: '" + modelResource + "' - expected *.bpmn or *.xml");
			}

		}
		// SETUP_OK;

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
					logger.fine("importXmlEntityData - removing existing configuration for model version '"
							+ aModelVersion + "'");
					modelService.removeModel(aModelVersion);
				}
				// save new entities into database and update modelversion.....
				for (int i = 0; i < ecol.getDocument().length; i++) {
					entity = ecol.getDocument()[i];
					itemCollection = XMLDocumentAdapter.putDocument(entity);
					// save entity
					documentService.save(itemCollection);
				}

				logger.fine("importXmlEntityData - " + ecol.getDocument().length + " entries sucessfull imported");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * This method migrates the deprecated WorkflowScheduelr configuraiton into the
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
			logger.severe("loadConfiguration - invalid param: " + e.getMessage());
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
					logger.severe("more then one timer with id " + id + " was found!");
				}
				timer = atimer;
			}
		}
		return timer;
	}

}