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

package org.imixs.workflow.faces.data;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.ModelService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.plugins.OwnerPlugin;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.faces.util.ErrorHandler;
import org.imixs.workflow.faces.util.LoginController;
import org.imixs.workflow.faces.util.ValidationException;

import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObserverException;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * 
 * The WorkflowController is a @ConversationScoped CDI bean to control the
 * processing life cycle of a workitem in JSF an application. The bean can be
 * used in single page applications, as well for complex page flows. The
 * controller supports bookmarkable URLs.
 * <p>
 * The WorkflowController fires CDI events from the type WorkflowEvent. A CDI
 * bean can observe these events to participate in the processing life cycle.
 * <p>
 * To load a workitem the methods load(id) and onLoad() can be used. The method
 * load expects a valid uniqueId of a workItem to be loaded. The onLoad() method
 * extracts the uniqueid from the query parameter 'id'. This is the recommended
 * way to support bookmarkable URLs when opening a JSF page with the data of a
 * workitem. The onLoad method can be triggered by an jsf viewAction placed in
 * the header of a JSF page:
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
 * automatically redirects the user to the new form outcome. This guarantees
 * bookmarkable URLs.
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
    private static final Logger logger = Logger.getLogger(WorkflowController.class.getName());

    @Inject
    ModelService modelService;

    @Inject
    WorkflowService workflowService;

    @Inject
    Event<WorkflowEvent> events;

    @Inject
    LoginController loginController;

    List<ItemCollection> activities = null;

    public static final String DEFAULT_TYPE = "workitem";

    public WorkflowController() {
        super();
        setDefaultType("workitem");
    }

    /**
     * Returns the current workItem. If no workitem is defined the method
     * Instantiates a empty ItemCollection.
     * 
     * @return - current workItem or an emtpy workitem if not set
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
        activities = null;
    }

    /**
     * This action method is used to initialize a new workitem with the initial
     * values of the assigned workflow task. The method updates the Workflow
     * attributes '$WriteAccess','$workflowgroup', '$workflowStatus',
     * 'txtWorkflowImageURL' and 'txtWorkflowEditorid'.
     * 
     * @param action - the action returned by this method
     * @return - action
     * @throws ModelException is thrown in case not valid workflow task if defined
     *                        by the current model.
     */
    public void create() throws ModelException {

        if (data == null) {
            return;
        }

        ItemCollection startProcessEntity = null;
        // if no process id was set fetch the first start workitem
        if (data.getTaskID() <= 0) {
            throw new InvalidAccessException(ModelException.INVALID_MODEL_ENTRY,
                    "missing $taskID ");
        }

        // find the ProcessEntity
        startProcessEntity = modelService.getModelManager().loadTask(data);

        // ProcessEntity found?
        if (startProcessEntity == null) {
            throw new InvalidAccessException(ModelException.INVALID_MODEL_ENTRY,
                    "unable to find ProcessEntity in model version " + data.getModelVersion() + " for ID="
                            + data.getTaskID());
        }

        // get type...
        String type = startProcessEntity.getItemValueString("txttype");
        if (type.isEmpty()) {
            type = DEFAULT_TYPE;
        }
        data.replaceItemValue("type", type);

        if (loginController != null) {
            String user = loginController.getUserPrincipal();
            // set creator
            data.replaceItemValue(WorkflowKernel.CREATOR, user);
            // support deprecated field
            data.replaceItemValue("namcreator", user);

            // set default owner
            data.replaceItemValue(OwnerPlugin.OWNER, user);
            // support deprecated field
            data.replaceItemValue("namowner", user);
        }

        // update $WriteAccess
        data.replaceItemValue("$writeaccess", data.getItemValue(WorkflowKernel.CREATOR));

        // assign WorkflowGroup and editor
        data.replaceItemValue("$workflowgroup", startProcessEntity.getItemValueString("txtworkflowgroup"));
        data.replaceItemValue("$workflowStatus", startProcessEntity.getItemValueString("txtname"));
        data.replaceItemValue("txtWorkflowImageURL", startProcessEntity.getItemValueString("txtimageurl"));
        data.replaceItemValue("txtWorkflowEditorid", startProcessEntity.getItemValueString("txteditorid"));

        // deprecated field
        data.replaceItemValue("txtworkflowgroup", startProcessEntity.getItemValueString("txtworkflowgroup"));
        data.replaceItemValue("txtworkflowStatus", startProcessEntity.getItemValueString("txtname"));

        activities = null;
        startConversation();
        // fire event
        events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_CREATED));
    }

    /**
     * This method creates a new empty workitem. An existing workitem and optional
     * conversation context will be reset.
     * 
     * The method assigns the initial values '$ModelVersion', '$ProcessID' and
     * '$UniqueIDRef' to the new workitem. The method creates the empty field
     * '$workitemID' and the item '$owner' which is assigned to the current user.
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
        data = new ItemCollection();
        data.model(modelVersion).task(taskID);

        // set optional uniqueidRef
        if (uniqueIdRef != null && !uniqueIdRef.isEmpty()) {
            data.replaceItemValue(WorkflowService.UNIQUEIDREF, uniqueIdRef);
        }

        // set empty $workitemid
        data.replaceItemValue("$workitemid", "");

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
            logger.log(Level.FINEST, "......fire WORKITEM_BEFORE_PROCESS event: '' in {0}ms",
                    System.currentTimeMillis() - l1);

            // process workItem now...
            data = workflowService.processWorkItem(data);

            // test if the property 'action' is provided
            actionResult = data.getItemValueString("action");

            // If no action was defined computed it from the current viewID...
            if (actionResult == null || actionResult.isEmpty()) {
                FacesContext facesContext = FacesContext.getCurrentInstance();
                actionResult = facesContext.getViewRoot().getViewId();
                actionResult = actionResult + "?id=" + getWorkitem().getUniqueID() + "&faces-redirect=true";
            }

            // test if 'faces-redirect' is included in actionResult
            if (!actionResult.startsWith("http") && actionResult.contains("/")
                    && !actionResult.contains("faces-redirect=")) {
                // append faces-redirect=true
                if (!actionResult.contains("?")) {
                    actionResult = actionResult + "?faces-redirect=true";
                } else {
                    actionResult = actionResult + "&faces-redirect=true";
                }
            }
            logger.log(Level.FINE, "... new actionResult={0}", actionResult);

            // fire event
            long l2 = System.currentTimeMillis();
            events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_AFTER_PROCESS));
            logger.log(Level.FINEST, "......[process] fire WORKITEM_AFTER_PROCESS event: '' in {0}ms",
                    System.currentTimeMillis() - l2);

            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "......[process] ''{0}'' completed in {1}ms",
                        new Object[] { getWorkitem().getItemValueString(WorkflowKernel.UNIQUEID),
                                System.currentTimeMillis() - lTotal });
            }
            // Finally we close the conversation. In case of an exception, the conversation
            // will stay open
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

        // test if the action result is an external URL. In this case initiate a
        // redirect
        if (actionResult != null && actionResult.startsWith("http")) {
            // clear action result and start redirect
            String externalURL = actionResult;
            actionResult = "";
            try {
                ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
                externalContext.redirect(externalURL);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to redirect action result: {0} - Error: {1}",
                        new Object[] { externalURL, e.getMessage() });
                e.printStackTrace();
            }
        }

        activities = null;
        // return the action result (Post-Redirect-Get).
        // can be null in case of an exception
        return actionResult;
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
            logger.finest("......process workitem is null");
        } else {
            logger.log(Level.FINEST, "......process workitem id: {0}", data.getUniqueID());
        }
        this.getWorkitem().setEventID(id);
        return process();
    }

    /**
     * This method returns a List of workflow events assigned to the corresponding
     * '$taskId' and '$modelVersion' of the current WorkItem.
     * <p>
     * The method uses a caching mechanism to load the event list only once
     * 
     * @return
     * @throws ModelException
     */
    public List<ItemCollection> getEvents() throws ModelException {

        // use cache?
        if (activities == null) {
            // no - lookup activities....
            activities = new ArrayList<ItemCollection>();
            if (getWorkitem() == null || (getWorkitem().getModelVersion().isEmpty()
                    && getWorkitem().getItemValueString(WorkflowKernel.WORKFLOWGROUP).isEmpty())) {
                return activities;
            }
            // get Events form workflowService
            try {
                activities = workflowService.getEvents(getWorkitem());
            } catch (ModelException e) {
                logger.log(Level.WARNING, "Unable to resolve workflow event list: {0}",
                        e.getMessage());
                throw new InvalidAccessException(ModelException.INVALID_MODEL,
                        e.getMessage());
            }
        }
        return activities;
    }

    /**
     * Loads a workitem by a given $uniqueID and starts a new conversation. The
     * conversation will be ended after the workitem was processed or after the
     * MaxInactiveInterval from the session.
     * 
     * @param uniqueID
     */
    public void load(String uniqueid) {
        super.load(uniqueid);
        activities = null;
        if (data != null) {
            // fire event
            events.fire(new WorkflowEvent(data, WorkflowEvent.WORKITEM_CHANGED));
        }

    }

}
