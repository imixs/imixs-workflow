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

package org.imixs.workflow.faces.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.ObserverException;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.ModelService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.faces.util.ErrorHandler;
import org.imixs.workflow.faces.util.LoginController;
import org.imixs.workflow.faces.util.ValidationException;

/**
 * 
 * The WorkflowController is a @ConversationScoped CDI bean to control the
 * processing life cycle of a workitem in JSF an application. The bean can be
 * used in single page applications, as well for complex page flows. The
 * controller is easy to use and supports bookmarkable URLs.
 * <p>
 * The WorkflowController fires CDI events from the type WorkflowEvent. A CDI
 * bean can observe these events to participate in the processing life cycle.
 * <p>
 * To load a workitem the methods load(id) and onLoad() can be used. The method
 * load expects the uniqueId of a workItem to be loaded. The onLoad() method
 * extracts the uniqueid from the query parameter 'id'. This is the recommended
 * way to support bookmarkable URLs. To load a workitem the onLoad method can be
 * triggered by an jsf viewAction placed in the header of a JSF page:
 * 
 * <pre>
 * {@code
    <f:metadata>
      <f:viewAction action="... workflowController.onLoad()" />
    </f:metadata> }
 * </pre>
 * <p>
 * A bookmarkable URL looks like this:
 * <p>
 * {@code /myForm.xthml?id=[UNIQUEID] }
 * <p>
 * In combination with the viewAction the WorkflowController is automatically
 * initialized.
 * <p>
 * After a workitem is loaded, a new conversation is started and the CDI event
 * WorkflowEvent.WORKITEM_CHANGED is fired.
 * <p>
 * After a workitem was processed, the conversation is automatically closed.
 * Stale conversations will automatically timeout with the default session
 * timeout.
 * <p>
 * After each call of the method process the Post-Redirect-Get is initialized
 * with the default URL from the start of the conversation. If an alternative
 * action result is provided by the workflow engine, the WorkflowController
 * automaticall redirects the user to the new form outcome. This guarantees
 * bookmakrable URLs.
 * <p>
 * Call the close() method when the workitem data is no longer needed.
 * <p>
 * Within a JSF form, the items of a workitem can be accessed by the getter
 * method getWorkitem().
 * 
 * <pre>
 *   #{workflowController.workitem.item['$workflowstatus']}
 * </pre>
 * 
 * @author rsoika
 * @version 2.0.0
 */
@Named
@ConversationScoped
public class WorkflowController extends AbstractDataController implements Serializable {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(WorkflowController.class.getName());

	@EJB
	ModelService modelService;

	@EJB
	WorkflowService workflowService;

	@Inject
	Event<WorkflowEvent> events;

	@Inject
	LoginController loginController;

	public static final String DEFAULT_TYPE = "workitem";

	public WorkflowController() {
		super();
		logger.info("...constructor..");
		setDefaultType("workitem");
	}

	/**
	 * Returns the current workItem. If no workitem is defined the method
	 * Instantiates a empty ItemCollection.
	 * 
	 * @return - current workItem or null if not set
	 */
	public ItemCollection getWorkitem() {
		// do initialize an empty workItem here if null
		if (data == null) {
			reset();
		}
		return data;
	}

	/**
	 * Set the current worktItem
	 * 
	 * @param workitem - new reference or null to clear the current workItem.
	 */
	public void setWorkitem(ItemCollection workitem) {
		this.data = workitem;
	}

	/**
	 * This action method is used to initialize a new workitem with the initial
	 * values of the assigned workflow task. The method updates the Workflow
	 * attributes '$WriteAccess','$workflowgroup', '$workflowStatus',
	 * 'txtWorkflowImageURL' and 'txtWorkflowEditorid'.
	 * 
	 * @param action - the action returned by this method
	 * @return - action
	 * @throws ModelException is thrown in case not valid worklfow task if defined
	 *                        by the current model.
	 */
	public void create() throws ModelException {

		if (data == null) {
			return;
		}
		ItemCollection startProcessEntity = null;
		// if no process id was set fetch the first start workitem
		if (data.getTaskID() <= 0) {
			// get ProcessEntities by version
			List<ItemCollection> col;
			col = modelService.getModelByWorkitem(getWorkitem()).findAllTasks();
			if (!col.isEmpty()) {
				startProcessEntity = col.iterator().next();
				getWorkitem().setTaskID(startProcessEntity.getItemValueInteger("numProcessID"));
			}
		}

		// find the ProcessEntity
		startProcessEntity = modelService.getModelByWorkitem(data).getTask(data.getTaskID());

		// ProcessEntity found?
		if (startProcessEntity == null)
			throw new InvalidAccessException(ModelException.INVALID_MODEL_ENTRY,
					"unable to find ProcessEntity in model version " + data.getModelVersion() + " for ID="
							+ data.getTaskID());

		// update $WriteAccess
		data.replaceItemValue("$writeaccess", data.getItemValue("$creator"));

		// assign WorkflowGroup and editor
		data.replaceItemValue("$workflowgroup", startProcessEntity.getItemValueString("txtworkflowgroup"));
		data.replaceItemValue("$workflowStatus", startProcessEntity.getItemValueString("txtname"));
		data.replaceItemValue("txtWorkflowImageURL", startProcessEntity.getItemValueString("txtimageurl"));
		data.replaceItemValue("txtWorkflowEditorid", startProcessEntity.getItemValueString("txteditorid"));

		// deprecated field
		data.replaceItemValue("txtworkflowgroup", startProcessEntity.getItemValueString("txtworkflowgroup"));
		data.replaceItemValue("txtworkflowStatus", startProcessEntity.getItemValueString("txtname"));

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_CREATED));
	}

	/**
	 * This method creates a new empty workitem. An existing workitem and optional
	 * conversation context will be reset.
	 * 
	 * The method assigns the initial values '$ModelVersion', '$ProcessID' and
	 * '$UniqueIDRef' to the new workitem. The method creates the empty field
	 * '$workitemID' and the field 'namowner' which is assigned to the current user.
	 * This data can be used in case that a workitem is not processed but saved
	 * (e.g. by the dmsController).
	 * 
	 * The method starts a new conversation context. Finally the method fires the
	 * WorkfowEvent WORKITEM_CREATED.
	 * 
	 * @param modelVersion - model version
	 * @param processID    - processID
	 * @param processRef   - uniqueid ref
	 */

	public void create(String modelVersion, int taskID, String uniqueIdRef) throws ModelException {
		// set model information..
		getWorkitem().model(modelVersion).task(taskID);

		// set default owner
		getWorkitem().replaceItemValue("namowner", loginController.getUserPrincipal());

		// set empty $workitemid
		getWorkitem().replaceItemValue("$workitemid", "");

		this.create();
	}

	/**
	 * This method processes the current workItem and returns a new action result.
	 * The action result redirects the user to the default action result or to a new
	 * result provided by the workflow model. The 'action' property is typically
	 * evaluated from the ResultPlugin. Alternatively the property can be provided
	 * by an application. If no 'action' property is provided the method returns
	 * null.
	 * <p>
	 * The method fires the WorkflowEvents WORKITEM_BEFORE_PROCESS and
	 * WORKITEM_AFTER_PROCESS.
	 * <p>
	 * The Method also catches PluginExceptions and adds the corresponding Faces
	 * Error Message into the FacesContext. In case of an exception the
	 * WorkflowEvent WORKITEM_AFTER_PROCESS will not be fired.
	 * <p>
	 * In case the processing was successful, the current conversation will be
	 * closed. In Case of an Exception (e.g PluginException) the conversation will
	 * not be closed, so that the current workitem data is still available.
	 * 
	 * @return the action result provided in the 'action' property or evaluated from
	 *         the default property 'txtworkflowResultmessage' from the
	 *         ActivityEntity
	 * @throws PluginException
	 * @throws ModelException
	 */
	public String process() throws PluginException, ModelException {
		String actionResult = null;
		long lTotal = System.currentTimeMillis();

		if (data == null) {
			logger.warning("Unable to process workitem == null!");
			return actionResult;
		}

		// clear last action
		data.replaceItemValue("action", "");

		try {
			long l1 = System.currentTimeMillis();
			events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_BEFORE_PROCESS));
			logger.finest("......fire WORKITEM_BEFORE_PROCESS event: ' in " + (System.currentTimeMillis() - l1) + "ms");

			// process workItem now...
			data = workflowService.processWorkItem(data);

			// fire event
			long l2 = System.currentTimeMillis();
			events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_AFTER_PROCESS));
			logger.finest(
					"[process] fire WORKITEM_AFTER_PROCESS event: ' in " + (System.currentTimeMillis() - l2) + "ms");

			// test if the property 'action' is provided
			actionResult = data.getItemValueString("action");

			// compute the Action result...
			if ((actionResult == null || actionResult.isEmpty()) && !getDefaultActionResult().isEmpty()) {
				// construct default action result if no actionResult was
				// defined
				actionResult = getDefaultActionResult() + "?id=" + getWorkitem().getUniqueID() + "&faces-redirect=true";
			}

			// test if 'faces-redirect' is included in actionResult
			if (actionResult.contains("/") && !actionResult.contains("faces-redirect=")) {
				// append faces-redirect=true
				if (!actionResult.contains("?")) {
					actionResult = actionResult + "?faces-redirect=true";
				} else {
					actionResult = actionResult + "&faces-redirect=true";
				}
			}

			logger.fine("... new actionResult=" + actionResult);

			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("[process] '" + getWorkitem().getItemValueString(WorkflowKernel.UNIQUEID)
						+ "' completed in " + (System.currentTimeMillis() - lTotal) + "ms");
			}

			// stop current conversation - in case of an exception, the conversation will
			// not be closed.
			close();

		} catch (ObserverException oe) {
			actionResult = null;
			// test if we can handle the exception...
			if (oe.getCause() instanceof PluginException) {
				// add error message into current form
				ErrorHandler.addErrorMessage((PluginException) oe.getCause());
			} else {
				if (oe.getCause() instanceof ValidationException) {
					// add error message into current form
					ErrorHandler.addErrorMessage((ValidationException) oe.getCause());
				} else {
					// throw unknown exception
					throw oe;
				}
			}
		} catch (PluginException pe) {
			actionResult = null;
			// add a new FacesMessage into the FacesContext
			ErrorHandler.handlePluginException(pe);
		} catch (ModelException me) {
			actionResult = null;
			// add a new FacesMessage into the FacesContext
			ErrorHandler.handleModelException(me);
		}

		return ("".equals(actionResult) ? null : actionResult);
	}

	/**
	 * This method processes the current workItem with the provided eventID. The
	 * method can be used as an action or actionListener.
	 * 
	 * @param id - activityID to be processed
	 * @throws PluginException
	 * 
	 * @see WorkflowController#process()
	 */
	public String process(int id) throws ModelException, PluginException {
		// update the eventID
		if (data == null) {
			logger.info("...process workitem is null");
		} else {
			logger.info("...process workitem id: " + data.getUniqueID());
		}
		this.getWorkitem().setEventID(id);
		return process();
	}

	/**
	 * This method returns a List of workflow events assigned to the corresponding
	 * '$taskid' and '$modelversion' of the current WorkItem.
	 * 
	 * @return
	 */
	public List<ItemCollection> getEvents() {
		List<ItemCollection> activityList = new ArrayList<ItemCollection>();

		if (getWorkitem() == null || (getWorkitem().getModelVersion().isEmpty()
				&& getWorkitem().getItemValueString(WorkflowKernel.WORKFLOWGROUP).isEmpty())) {
			return activityList;
		}

		// get Events form workflowService
		try {
			activityList = workflowService.getEvents(getWorkitem());
		} catch (ModelException e) {
			logger.warning("Unable to get workflow event list: " + e.getMessage());
		}

		return activityList;
	}

	/**
	 * Loads a workitem by a given $uniqueid and starts a new conversaton. The
	 * conversaion will be ended after the workitem was processed or after the
	 * MaxInactiveInterval from the session.
	 * 
	 * @param uniqueid
	 */
	public void load(String uniqueid) {
		super.load(uniqueid);
		if (data != null) {
			// fire event
			events.fire(new WorkflowEvent(data, WorkflowEvent.WORKITEM_CHANGED));
		}

	}

}
