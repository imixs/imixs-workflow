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

package org.imixs.workflow.jee.faces.workitem;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.jee.ejb.ModelService;

/**
 * The WorkflowController can be used in JSF Applications to manage workflow
 * processing on any workitem.
 * 
 * The WorklfowController can be extended by a web module implementing a project
 * specific behavior.
 * 
 * The properties modelVersion and processID are used to control and create new
 * workitem instances based on a model definition
 * 
 * The workflowController bean is typically used in session scope.
 * 
 * @author rsoika
 * @version 0.0.2
 */
public class WorkflowController extends DataController {

	private static final long serialVersionUID = 1L;

	private List<ItemCollection> activityList;

	@EJB
	private org.imixs.workflow.jee.ejb.ModelService modelService;

	@EJB
	private org.imixs.workflow.jee.ejb.WorkflowService workflowService;

	private static Logger logger = Logger.getLogger(WorkflowController.class
			.getName());

	public WorkflowController() {
		super();
		// set default type
		setType("workitem");
	}

	/**
	 * returns an instance of the ModelService EJB
	 * 
	 * @return
	 */
	public ModelService getModelService() {
		return modelService;
	}

	public org.imixs.workflow.jee.ejb.WorkflowService getWorkflowService() {
		return workflowService;
	}

	/**
	 * Updates the current workItem and reset the activityList.
	 */
	@Override
	public void setWorkitem(ItemCollection aworkitem) {
		super.setWorkitem(aworkitem);
		activityList = null;
	}

	/**
	 * This action method is used to initialize a new workitem. There for the
	 * method loads the initial ProcessEntity and updates the Workflow
	 * attributes WorfklowGroup and EditorId.
	 * 
	 * if the new workItem has no $modelversion the method set a default model
	 * version (computed by the latest version and the first processEntiy).
	 * 
	 * 
	 * @param action
	 *            - the action returned by this method
	 * @return - action
	 * @throws ModelException
	 */
	public String init(String action) {

		ItemCollection startProcessEntity = null;

		activityList = null;

		if (!getWorkitem().hasItem("$ModelVersion")) {
			String modelVersion;
			try {
				modelVersion = modelService.getLatestVersion();
			} catch (ModelException e) {
				throw new InvalidAccessException(e.getErrorContext(),
						e.getErrorCode(), e.getMessage(), e);
			}
			getWorkitem().replaceItemValue("$ModelVersion", modelVersion);
		}

		// if not process id was set fetch the first start workitem
		if (getWorkitem().getItemValueInteger("$ProcessID") <= 0) {
			// get ProcessEntities by version
			List<ItemCollection> col;
			col = modelService.getAllStartProcessEntities(getWorkitem()
					.getItemValueString(WorkflowKernel.MODELVERSION));
			if (!col.isEmpty()) {
				startProcessEntity = col.iterator().next();
				getWorkitem().replaceItemValue("$ProcessID",
						startProcessEntity.getItemValueInteger("numProcessID"));
			}
		}

		// find the ProcessEntity
		startProcessEntity = modelService.getProcessEntity(getWorkitem()
				.getItemValueInteger(WorkflowKernel.PROCESSID), getWorkitem()
				.getItemValueString(WorkflowKernel.MODELVERSION));

		// ProcessEntity found?
		if (startProcessEntity == null)
			throw new InvalidAccessException(
					ModelException.INVALID_MODEL_ENTRY,
					"unable to find ProcessEntity in model version "
							+ getWorkitem().getItemValueString(
									WorkflowKernel.MODELVERSION)
							+ " for ID="
							+ getWorkitem().getItemValueInteger(
									WorkflowKernel.PROCESSID));

		// update processId and WriteAccess
		getWorkitem().replaceItemValue("$WriteAccess",
				getWorkitem().getItemValue("namCreator"));

		// assign WorkflowGroup and editor
		getWorkitem().replaceItemValue("txtworkflowgroup",
				startProcessEntity.getItemValueString("txtworkflowgroup"));
		getWorkitem().replaceItemValue("txtworkflowStatus",
				startProcessEntity.getItemValueString("txtname"));
		getWorkitem().replaceItemValue("txtWorkflowImageURL",
				startProcessEntity.getItemValueString("txtimageurl"));

		getWorkitem().replaceItemValue("txtWorkflowEditorid",
				startProcessEntity.getItemValueString("txteditorid"));

		return action;
	}

	/**
	 * This method processes the current workItem and returns an action result.
	 * The method expects that the current workItem provides a valid
	 * $ActiviytID.
	 * 
	 * The method returns the value of the property 'action' if provided by the
	 * workflow model or a plug-in. The 'action' property is typically evaluated
	 * from the ResultPlugin. Alternatively the property can be provided by an
	 * application. If no 'action' property is provided the method evaluates the
	 * default property 'txtworkflowResultmessage' from the model as an action
	 * result.
	 * 
	 * The method resets the current ActivityList after the workitem was
	 * processed.
	 * 
	 * @return the action result provided in the 'action' property or evaluated
	 *         from the default property 'txtworkflowResultmessage' from the
	 *         ActivityEntity
	 * @throws AccessDeniedException
	 * @throws PluginException
	 */
	public String process() throws AccessDeniedException,
			ProcessingErrorException, PluginException {
		// clear last action
		workitem.removeItem("action");

		// process workItem now...
		workitem = this.getWorkflowService().processWorkItem(workitem);

		// reset the activity list!
		// Never call setWorkitem because this affects behavior of subclasses!
		activityList = null;

		// test if the property 'action' is provided
		String action = workitem.getItemValueString("action");
		if ("".equals(action))
			// get default workflowResult message
			action = workitem.getItemValueString("txtworkflowresultmessage");
		return ("".equals(action) ? null : action);
	}

	/**
	 * This method processes the current workItem with the provided activityID.
	 * The meethod can be used as an actionListener.
	 * 
	 * @param id
	 *            - activityID to be processed
	 * @throws PluginException
	 * 
	 * @see process()
	 * @see process(id,resetWorkitem)
	 */
	public String process(int id) throws AccessDeniedException,
			ProcessingErrorException, PluginException {
		return process(id, false);
	}

	/**
	 * This method processes the current workItem with the provided activityID.
	 * The method can be used as an actionListener. If the Boolean 'reset' is
	 * true then the controller will be reset after process.
	 * 
	 * @param id
	 *            - activityID to be processed
	 * @param resetWorkitem
	 *            - boolean indicates if the workitem should be reset
	 * @throws PluginException
	 * @see process()
	 */
	public String process(int id, boolean resetWorkitem)
			throws AccessDeniedException, ProcessingErrorException,
			PluginException {
		// update the property $ActivityID
		this.getWorkitem().replaceItemValue("$ActivityID", id);
		String result = process();

		if (resetWorkitem)
			reset(null);

		return result;
	}

	/**
	 * This method overwrite the default behavior and processes the current
	 * workItem.
	 */
	@Override
	public String save(String action) throws AccessDeniedException {
		// process workItem.
		try {
			return process();
		} catch (ProcessingErrorException e) {
			throw new InvalidAccessException(e.getErrorContext(),
					e.getErrorCode(), e.getMessage(), e);
		} catch (PluginException e) {
			throw new InvalidAccessException(e.getErrorContext(),
					e.getErrorCode(), e.getMessage(), e);
		}
	}

	/**
	 * returns the last computed workflow resultmessage. The property
	 * 'txtworkflowresultmessage' is computed by the ResultPlugin.
	 * 
	 * If no txtworkflowresultmessage is found an empty string will be returned
	 * 
	 * @return - the action result
	 */
	public String getAction() {
		String action = getWorkitem().getItemValueString(
				"txtworkflowresultmessage");
		return ("".equals(action) ? null : action);
	}

	/**
	 * returns a arrayList of Activities to the corresponding processiD of the
	 * current WorkItem. The Method returns the activities corresponding to the
	 * workItems modelVersionID. The list of computed activities is cashed until
	 * a new workItem was set or the current workItem was processed.
	 * 
	 * @return
	 */
	public List<ItemCollection> getActivities() {
		if (activityList != null)
			return activityList;

		activityList = new ArrayList<ItemCollection>();

		if (getWorkitem() == null)
			return activityList;

		int processId = getWorkitem().getItemValueInteger("$processid");

		if (processId == 0)
			return activityList;

		// verify if modelversion is defined by workitem
		String sversion = getWorkitem().getItemValueString("$modelversion");
		if (sversion == null || "".equals(sversion)) {
			try {
				// try to get latest version...
				sversion = this.getModelService().getLatestVersionByWorkitem(
						getWorkitem());
			} catch (ModelException e) {
				logger.warning("[WorkflwoControler] unable to getactivitylist: "
						+ e.getMessage());
			}
		}
		// get Workflow-Activities by version 
		List<ItemCollection> col = null;
		col = this.getModelService().getPublicActivities(processId, sversion);
		if (col == null || col.size() == 0) {
			// try to upgrade model version
			try {
				sversion = this.getModelService().getLatestVersionByWorkitem(
						getWorkitem());
				col = this.getModelService().getPublicActivities(processId,
						sversion);
			} catch (ModelException e) {
				logger.warning("[WorkflwoControler] unable to getactivitylist: "
						+ e.getMessage());
			}
		}

		for (ItemCollection aworkitem : col) {
			activityList.add(aworkitem);
		}
		return activityList;
	}

	/**
	 * Returns true if the current workItem was processed before by the
	 * workflowService. This is identified by the property 'numLastActivityID'
	 * which is computed by the WorkflowKernel.
	 * 
	 * @return true if the current workItem was already processed by the
	 *         WorkflowKernel
	 */
	public boolean isNewWorkitem() {
		try {
			return (!getWorkitem().hasItem("numlastactivityid"));
		} catch (Exception e) {
			return true;
		}
	}

}
