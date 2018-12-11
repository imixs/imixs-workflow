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

package org.imixs.workflow.faces.workitem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.event.Event;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.ModelService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * The WorkflowController is a conversation scoped CDI bean and can be used in
 * JSF Applications to manage workflow processing on any workitem.
 * <p>
 * The behavior of the bean can be controlled by reacting on the CDI event
 * WorkflowEvent.
 * <p>
 * To load a workitem the query param 'id' with the $uniqueid of an existing
 * workitem can be used.
 * <p>
 * {@code
 *   /...?id=[UNIQUEID]
 * }
 * 
 * @author rsoika
 * @version 2.0.0
 */
@Named
@ConversationScoped
public class WorkflowController implements Serializable {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(WorkflowController.class.getName());


	@Inject
	protected Event<WorkflowEvent> events;

	
	ItemCollection workitem = null;

	@Inject
	Conversation conversation;

	@EJB
	ModelService modelService;

	@EJB
	WorkflowService workflowService;

	public static final String DEFAULT_TYPE = "workitem";

	public WorkflowController() {
		super();
	}

	public void reset() {
		workitem = new ItemCollection();
		workitem.replaceItemValue("type", DEFAULT_TYPE);
	}

	/**
	 * Returns the current workItem. If no workitem is defined the method
	 * Instantiates a empty ItemCollection.
	 * 
	 * @return - current workItem or null if not set
	 */
	public ItemCollection getWorkitem() {
		// do initialize an empty workItem here if null
		if (workitem == null) {
			workitem = new ItemCollection();
			workitem.replaceItemValue("type", DEFAULT_TYPE);
		}
		return workitem;
	}

	/**
	 * Set the current worktItem
	 * 
	 * @param workitem - new reference or null to clear the current workItem.
	 */
	public void setWorkitem(ItemCollection workitem) {
		this.workitem = workitem;
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
	public String init(String action) throws ModelException {

		if (workitem == null) {
			return action;
		}
		ItemCollection startProcessEntity = null;
		// if not process id was set fetch the first start workitem
		if (workitem.getTaskID() <= 0) {
			// get ProcessEntities by version
			List<ItemCollection> col;
			col = modelService.getModelByWorkitem(getWorkitem()).findAllTasks();
			if (!col.isEmpty()) {
				startProcessEntity = col.iterator().next();
				getWorkitem().setTaskID(startProcessEntity.getItemValueInteger("numProcessID"));
			}
		}

		// find the ProcessEntity
		startProcessEntity = modelService.getModelByWorkitem(workitem).getTask(workitem.getTaskID());

		// ProcessEntity found?
		if (startProcessEntity == null)
			throw new InvalidAccessException(ModelException.INVALID_MODEL_ENTRY,
					"unable to find ProcessEntity in model version " + workitem.getModelVersion() + " for ID="
							+ workitem.getTaskID());

		// update $WriteAccess
		workitem.replaceItemValue("$writeaccess", workitem.getItemValue("$creator"));

		// assign WorkflowGroup and editor
		workitem.replaceItemValue("$workflowgroup", startProcessEntity.getItemValueString("txtworkflowgroup"));
		workitem.replaceItemValue("$workflowStatus", startProcessEntity.getItemValueString("txtname"));
		workitem.replaceItemValue("txtWorkflowImageURL", startProcessEntity.getItemValueString("txtimageurl"));
		workitem.replaceItemValue("txtWorkflowEditorid", startProcessEntity.getItemValueString("txteditorid"));

		// deprecated field
		workitem.replaceItemValue("txtworkflowgroup", startProcessEntity.getItemValueString("txtworkflowgroup"));
		workitem.replaceItemValue("txtworkflowStatus", startProcessEntity.getItemValueString("txtname"));
		return action;
	}

	/**
	 * This method processes the current workItem and returns an action result. The
	 * method expects that the current workItem provides a valid $ActiviytID.
	 * 
	 * The method returns the value of the property 'action' if provided by the
	 * workflow model or a plug-in. The 'action' property is typically evaluated
	 * from the ResultPlugin. Alternatively the property can be provided by an
	 * application. If no 'action' property is provided the method returns null.
	 * 
	 * 
	 * @return the action result provided in the 'action' property or evaluated from
	 *         the default property 'txtworkflowResultmessage' from the
	 *         ActivityEntity
	 * @throws AccessDeniedException
	 * @throws PluginException
	 * @throws ModelException
	 */
	public String process() throws PluginException, ModelException {
		if (workitem == null) {
			logger.warning("Unable to process workitem == null!");
			return null;
		}

		// clear last action
		workitem.replaceItemValue("action", "");

		long l1 = System.currentTimeMillis();
		events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_BEFORE_PROCESS));
		logger.finest(
				"......fire WORKITEM_BEFORE_PROCESS event: ' in " + (System.currentTimeMillis() - l1) + "ms");

		// process workItem now...
		workitem = workflowService.processWorkItem(workitem);
		
		// fire event
					long l2 = System.currentTimeMillis();
					events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_AFTER_PROCESS));
					logger.finest(
							"[process] fire WORKITEM_AFTER_PROCESS event: ' in " + (System.currentTimeMillis() - l2) + "ms");


		// test if the property 'action' is provided
		String action = workitem.getItemValueString("action");
		
		// close conversation
		if (!conversation.isTransient()) {
			logger.fine("close conversation, id=" + conversation.getId());
			conversation.end();
		}

		return ("".equals(action) ? null : action);
	}


	/**
	 * This method processes the current workItem with the provided eventID. The
	 * method can be used as an action or actionListener.
	 * 
	 * @param id - activityID to be processed
	 * @throws PluginException
	 * 
	 * @see process()
	 * @see process(id,resetWorkitem)
	 */
	public String process(int id) throws ModelException, PluginException {
		// update the eventID
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
	 * Starts a new conversation
	 */
	@PostConstruct
	protected void init() {
		loadWorkitem();
		if (conversation.isTransient()) {
			conversation.setTimeout(((HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext()
					.getRequest()).getSession().getMaxInactiveInterval()*1000);
			conversation.begin();
			logger.finest("......start new conversation, id=" + conversation.getId());
		}
	}

	/**
	 * Loads a workitem based on the query params 'id' or 'workitem'
	 */
	protected void loadWorkitem() {
		FacesContext fc = FacesContext.getCurrentInstance();
		Map<String, String> paramMap = fc.getExternalContext().getRequestParameterMap();
		// try to extract tjhe uniqueid form the query string...
	
		String uniqueid = paramMap.get("id");
		if (uniqueid == null || uniqueid.isEmpty()) {
			// alternative 'workitem=...'
			uniqueid = paramMap.get("workitem");
		}
	
		workitem = workflowService.getWorkItem(uniqueid);
		if (workitem == null) {
			reset();
		} 
		
		events.fire(new WorkflowEvent(workitem, WorkflowEvent.WORKITEM_CHANGED));
	
	}

}
