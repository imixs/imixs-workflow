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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.Adapter;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.exceptions.QueryException;
import org.openbpmn.bpmn.BPMNModel;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.LocalBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * The WorkflowService is the Java EE Implementation for the Imixs Workflow Core
 * API. This interface acts as a service facade and supports basic methods to
 * create, process and access workitems. The Interface extends the core api
 * interface org.imixs.workflow.WorkflowManager with getter methods to fetch
 * collections of workitems.
 * 
 * @author rsoika
 * 
 */

@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
public class WorkflowService {

    // workitem properties
    public static final String UNIQUEIDREF = "$uniqueidref";
    public static final String READACCESS = "$readaccess";
    public static final String WRITEACCESS = "$writeaccess";
    public static final String PARTICIPANTS = "$participants";
    public static final String DEFAULT_TYPE = "workitem";

    // view properties
    public static final int SORT_ORDER_CREATED_DESC = 0;
    public static final int SORT_ORDER_CREATED_ASC = 1;
    public static final int SORT_ORDER_MODIFIED_DESC = 2;
    public static final int SORT_ORDER_MODIFIED_ASC = 3;

    public static final String INVALID_ITEMVALUE_FORMAT = "INVALID_ITEMVALUE_FORMAT";
    public static final String INVALID_TAG_FORMAT = "INVALID_TAG_FORMAT";

    @Inject
    @Any
    private Instance<Plugin> plugins;

    @Inject
    @Any
    protected Instance<Adapter> adapters;

    @Inject
    DocumentService documentService;

    @Inject
    ModelService modelService;

    @Inject
    ReportService reportService;

    @Inject
    WorkflowContextService workflowContextService;

    @Resource
    SessionContext ctx;

    @Inject
    protected Event<ProcessingEvent> processingEvents;

    @Inject
    protected Event<TextEvent> textEvents;

    private static final Logger logger = Logger.getLogger(WorkflowService.class.getName());

    public WorkflowService() {
        super();
    }

    /**
     * This method loads a Workitem with the corresponding uniqueid.
     * 
     */
    public ItemCollection getWorkItem(String uniqueid) {
        return documentService.load(uniqueid);
    }

    /**
     * Returns a collection of workitems containing a '$owner' item belonging to a
     * specified username. The '$owner' item can be controlled by the plug-in
     * {@code org.imixs.workflow.plugins.OwnerPlugin}
     * 
     * @param name        = username for itme '$owner' - if null current username
     *                    will be used
     * @param pageSize    = optional page count (default 20)
     * @param pageIndex   = optional start position
     * @param type        = defines the type property of the workitems to be
     *                    returnd. can be null
     * @param sortBy      -optional field to sort the result
     * @param sortReverse - optional sort direction
     * 
     * @return List of workitems
     * 
     */
    public List<ItemCollection> getWorkListByOwner(String name, String type, int pageSize, int pageIndex, String sortBy,
            boolean sortReverse) {

        if (name == null || "".equals(name))
            name = ctx.getCallerPrincipal().getName();

        String searchTerm = "(";
        if (type != null && !"".equals(type)) {
            searchTerm += " type:\"" + type + "\" AND ";
        }

        // support deprecated namowner field
        searchTerm += " (namowner:\"" + name + "\" OR $owner:\"" + name + "\") )";
        try {
            return documentService.find(searchTerm, pageSize, pageIndex, sortBy, sortReverse);
        } catch (QueryException e) {
            logger.log(Level.SEVERE, "getWorkListByOwner - invalid param: {0}", e.getMessage());
            return null;
        }
    }

    /**
     * Returns a collection of workitems for which the specified user has explicit
     * write permission.
     * The name is a username or role contained in the $WriteAccess attribute of the
     * workItem.
     * 
     * The method returns only workitems the call has sufficient read access for.
     * 
     * @param name        = username or role contained in $writeAccess - if null
     *                    current username will be used
     * @param pageSize    = optional page count (default 20)
     * @param pageIndex   = optional start position
     * @param type        = defines the type property of the workitems to be
     *                    returned. can be null
     * @param sortBy      -optional field to sort the result
     * @param sortReverse - optional sort direction
     * 
     * @return List of workitems
     * 
     */
    public List<ItemCollection> getWorkListByAuthor(String name, String type, int pageSize, int pageIndex,
            String sortBy, boolean sortReverse) {

        if (name == null || "".equals(name))
            name = ctx.getCallerPrincipal().getName();

        String searchTerm = "(";
        if (type != null && !"".equals(type)) {
            searchTerm += " type:\"" + type + "\" AND ";
        }
        searchTerm += " $writeaccess:\"" + name + "\" )";

        try {
            return documentService.find(searchTerm, pageSize, pageIndex, sortBy, sortReverse);
        } catch (QueryException e) {
            logger.log(Level.SEVERE, "getWorkListByAuthor - invalid param: {0}", e.getMessage());
            return null;
        }
    }

    /**
     * Returns a collection of workitems created by a specified user ($Creator). The
     * behaivor is simmilar to the method getWorkList.
     * 
     * 
     * @param name        = username for property $Creator - if null current
     *                    username will be used
     * @param pageSize    = optional page count (default 20)
     * @param pageIndex   = optional start position
     * @param type        = defines the type property of the workitems to be
     *                    returnd. can be null
     * @param sortBy      -optional field to sort the result
     * @param sortReverse - optional sort direction
     * 
     * @return List of workitems
     * 
     */
    public List<ItemCollection> getWorkListByCreator(String name, String type, int pageSize, int pageIndex,
            String sortBy, boolean sortReverse) {

        if (name == null || "".equals(name))
            name = ctx.getCallerPrincipal().getName();

        String searchTerm = "(";
        if (type != null && !"".equals(type)) {
            searchTerm += " type:\"" + type + "\" AND ";
        }
        searchTerm += " $creator:\"" + name + "\" )";
        try {
            return documentService.find(searchTerm, pageSize, pageIndex, sortBy, sortReverse);
        } catch (QueryException e) {
            logger.log(Level.SEVERE, "getWorkListByCreator - invalid param: {0}", e.getMessage());
            return null;
        }
    }

    /**
     * Returns a collection of workitems where the current user has a writeAccess.
     * This means that at least one of the userNames is contained in
     * the $writeaccess property.
     * 
     * 
     * @param pageSize    = optional page count (default 20)
     * @param pageIndex   = optional start position
     * @param type        = defines the type property of the workitems to be
     *                    returnd. can be null
     * @param sortorder   = defines sortorder (SORT_ORDER_CREATED_DESC = 0
     *                    SORT_ORDER_CREATED_ASC = 1 SORT_ORDER_MODIFIED_DESC = 2
     *                    SORT_ORDER_MODIFIED_ASC = 3)
     * @param sortBy      -optional field to sort the result
     * @param sortReverse - optional sort direction
     * 
     * @return List of workitems
     * 
     */
    public List<ItemCollection> getWorkListByWriteAccess(String type, int pageSize, int pageIndex, String sortBy,
            boolean sortReverse) {
        StringBuffer nameListBuffer = new StringBuffer();
        nameListBuffer.append("(");
        // construct a name list query for $writeaccess
        List<String> userNames = documentService.getUserNameList();
        for (int i = 0; i < userNames.size(); i++) {
            String userName = userNames.get(i);
            if (i > 0) {
                nameListBuffer.append(" OR ");
            }
            nameListBuffer.append(" $writeaccess:\"" + userName + "\" ");
        }
        nameListBuffer.append(")");

        String searchTerm = nameListBuffer.toString();
        if (type != null && !"".equals(type)) {
            searchTerm += " AND type:\"" + type + "\"";
        }
        try {
            return documentService.find(searchTerm, pageSize, pageIndex, sortBy, sortReverse);
        } catch (QueryException e) {
            logger.log(Level.SEVERE, "getWorkListByWriteAccess - invalid param: {0}", e.getMessage());
            return null;
        }
    }

    /**
     * Returns a list of workitems filtered by the field $workflowgroup
     * 
     * the method supports still also the deprecated field "txtworkflowgroup"
     * 
     * @param name
     * @param type
     * @param pageSize    = optional page count (default 20)
     * @param pageIndex   = optional start position
     * @param sortBy      -optional field to sort the result
     * @param sortReverse - optional sort direction
     * 
     * @return
     */

    public List<ItemCollection> getWorkListByGroup(String name, String type, int pageSize, int pageIndex, String sortBy,
            boolean sortReverse) {

        String searchTerm = "(";
        if (type != null && !"".equals(type)) {
            searchTerm += " type:\"" + type + "\" AND ";
        }
        // we support still the deprecated txtworkflowgroup
        searchTerm += " ($workflowgroup:\"" + name + "\" OR txtworkflowgroup:\"" + name + "\") )";
        try {
            return documentService.find(searchTerm, pageSize, pageIndex, sortBy, sortReverse);
        } catch (QueryException e) {
            logger.log(Level.SEVERE, "getWorkListByGroup - invalid param: {0}", e.getMessage());
            return null;
        }
    }

    /**
     * Returns a collection of workitems belonging to a specified $taskID defined by
     * the workflow model. The behaivor is simmilar to the method getWorkList.
     * 
     * @param aID         = $taskID for the workitems to be returned.
     * @param pageSize    = optional page count (default 20)
     * @param pageIndex   = optional start position
     * @param type        = defines the type property of the workitems to be
     *                    returnd. can be null
     * @param sortBy      -optional field to sort the result
     * @param sortReverse - optional sort direction
     * 
     * @return List of workitems
     * 
     */
    public List<ItemCollection> getWorkListByProcessID(int aid, String type, int pageSize, int pageIndex, String sortBy,
            boolean sortReverse) {

        String searchTerm = "(";
        if (type != null && !"".equals(type)) {
            searchTerm += " type:\"" + type + "\" AND ";
        }
        // need to be fixed during slow migration issue #384
        searchTerm += " $processid:\"" + aid + "\" )";
        try {
            return documentService.find(searchTerm, pageSize, pageIndex, sortBy, sortReverse);
        } catch (QueryException e) {
            logger.log(Level.SEVERE, "getWorkListByProcessID - invalid param: {0}", e.getMessage());
            return null;
        }
    }

    /**
     * Returns a collection of workitems belonging to a specified workitem
     * identified by the attribute $UniqueIDRef.
     * 
     * The behaivor of this Mehtod is simmilar to the method getWorkList.
     * 
     * @param aref        A unique reference to another workitem inside a database *
     * @param pageSize    = optional page count (default 20)
     * @param pageIndex   = optional start position
     * @param type        = defines the type property of the workitems to be
     *                    returnd. can be null
     * @param sortBy      -optional field to sort the result
     * @param sortReverse - optional sort direction
     * 
     * @return List of workitems
     */
    public List<ItemCollection> getWorkListByRef(String aref, String type, int pageSize, int pageIndex, String sortBy,
            boolean sortReverse) {

        String searchTerm = "(";
        if (type != null && !"".equals(type)) {
            searchTerm += " type:\"" + type + "\" AND ";
        }
        searchTerm += " $uniqueidref:\"" + aref + "\" )";
        try {
            return documentService.find(searchTerm, pageSize, pageIndex, sortBy, sortReverse);
        } catch (QueryException e) {
            logger.log(Level.SEVERE, "getWorkListByRef - invalid param: {0}", e.getMessage());
            return null;
        }
    }

    /**
     * Returns a collection of all workitems belonging to a specified workitem
     * identified by the attribute $UniqueIDRef.
     * 
     * @return List of workitems
     */
    public List<ItemCollection> getWorkListByRef(String aref) {
        return getWorkListByRef(aref, null, 0, 0, null, false);
    }

    /**
     * This method processes a workItem by the WorkflowKernel and saves the workitem
     * after the processing was finished successful. The workitem have to provide at
     * least the properties '$modelversion', '$taskid' and '$eventid'
     * <p>
     * Before the method starts processing the workitem, the method load the current
     * instance of the given workitem and compares the property $taskID. If it is
     * not equal the method throws an ProcessingErrorException.
     * <p>
     * After the workitem was processed successful, the method verifies the property
     * $workitemList. If this property holds a list of entities these entities will
     * be saved and the property will be removed automatically.
     * <p>
     * The method provides a observer pattern for plugins to get called during the
     * processing phase.
     * 
     * @param workitem - the workItem to be processed
     * @return updated version of the processed workItem
     * @throws AccessDeniedException    - thrown if the user has insufficient access
     *                                  to update the workItem
     * @throws ProcessingErrorException - thrown if the workitem could not be
     *                                  processed by the workflowKernel
     * @throws PluginException          - thrown if processing by a plugin fails
     * @throws ModelException
     */
    public ItemCollection processWorkItem(ItemCollection workitem)
            throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {
        boolean debug = logger.isLoggable(Level.FINE);
        long lStartTime = System.currentTimeMillis();

        if (workitem == null)
            throw new ProcessingErrorException(WorkflowService.class.getSimpleName(),
                    ProcessingErrorException.INVALID_WORKITEM, "workitem Is Null!");

        // fire event
        if (processingEvents != null) {
            processingEvents.fire(new ProcessingEvent(workitem, ProcessingEvent.BEFORE_PROCESS));
        } else {
            logger.warning("CDI Support is missing - ProcessingEvents Not Supported!");
        }
        // load current instance of this workitem if a unqiueID is provided
        if (!workitem.getUniqueID().isEmpty()) {
            // try to load the instance
            ItemCollection currentInstance = this.getWorkItem(workitem.getUniqueID());
            // Instance successful loaded ?
            if (currentInstance != null) {
                // test for author access
                if (!currentInstance.getItemValueBoolean(DocumentService.ISAUTHOR)) {
                    throw new AccessDeniedException(AccessDeniedException.OPERATION_NOTALLOWED, "$uniqueid: "
                            + workitem.getItemValueInteger(WorkflowKernel.UNIQUEID) + " - No Author Access!");
                }
                // test if $taskID matches current instance
                if (workitem.getTaskID() > 0 && currentInstance.getTaskID() != workitem.getTaskID()) {
                    throw new ProcessingErrorException(WorkflowService.class.getSimpleName(),
                            ProcessingErrorException.INVALID_PROCESSID,
                            "$uniqueid: " + workitem.getItemValueInteger(WorkflowKernel.UNIQUEID) + " - $taskid="
                                    + workitem.getTaskID() + " Did Not Match Expected $taskid="
                                    + currentInstance.getTaskID());
                }
                // merge workitem into current instance (issue #86, issue #507)
                // an instance of this WorkItem still exists! so we update the new
                // values....
                workitem.mergeItems(currentInstance.getAllItems());

            } else {
                // In case we have a $UniqueId but did not found an matching workitem
                // and the workitem miss a valid model assignment than
                // processing is not possible - OPERATION_NOTALLOWED

                if ((workitem.getTaskID() <= 0) || (workitem.getEventID() <= 0)
                        || (workitem.getModelVersion().isEmpty() && workitem.getWorkflowGroup().isEmpty())) {
                    // user has no read access -> throw AccessDeniedException
                    throw new InvalidAccessException(InvalidAccessException.OPERATION_NOTALLOWED,
                            "$uniqueid: " + workitem.getItemValueInteger(WorkflowKernel.UNIQUEID)
                                    + " - Insufficient Data or Lack Of Permission!");
                }

            }
        }

        // verify type attribute
        if ("".equals(workitem.getType())) {
            workitem.replaceItemValue("type", DEFAULT_TYPE);
        }

        // Lookup current model. If not found update model by regex
        String version = workflowContextService.findModelVersionByWorkitem(workitem);
        BPMNModel model = workflowContextService.fetchModel(version);
        WorkflowKernel workflowkernel = new WorkflowKernel(workflowContextService);
        ItemCollection profile = workflowkernel.getModelManager().loadDefinition(model);
        // register plugins...
        registerPlugins(workflowkernel, profile);
        // register adapters.....
        registerAdapters(workflowkernel);
        // udpate workitem metadata...
        updateMetadata(workitem);

        // now process the workitem
        try {
            long lKernelTime = System.currentTimeMillis();
            workitem = workflowkernel.process(workitem);
            if (debug) {
                logger.log(Level.FINE, "...WorkflowKernel processing time={0}ms",
                        System.currentTimeMillis() - lKernelTime);
            }
        } catch (PluginException pe) {
            // if a plugin exception occurs we roll back the transaction.
            logger.log(Level.SEVERE, "processing workitem ''{0} failed, rollback transaction...",
                    workitem.getItemValueString(WorkflowKernel.UNIQUEID));
            ctx.setRollbackOnly();
            throw pe;
        }

        // fire event
        if (processingEvents != null) {
            processingEvents.fire(new ProcessingEvent(workitem, ProcessingEvent.AFTER_PROCESS));
        }
        // Now fire also events for all split versions.....
        List<ItemCollection> splitWorkitems = workflowkernel.getSplitWorkitems();
        for (ItemCollection splitWorkitemm : splitWorkitems) {
            // fire event
            if (processingEvents != null) {
                processingEvents.fire(new ProcessingEvent(splitWorkitemm, ProcessingEvent.AFTER_PROCESS));
            }
            documentService.save(splitWorkitemm);
        }

        workitem = documentService.save(workitem);
        if (debug) {
            logger.log(Level.FINE, "...total processing time={0}ms", System.currentTimeMillis() - lStartTime);
        }
        return workitem;
    }

    /**
     * This method processes a workItem based on a given event.
     * 
     * @see method ItemCollection processWorkItem(ItemCollection workitem)
     * 
     * @param workitem - the workItem to be processed
     * @param event    - event object
     * @return updated version of the processed workItem
     * @throws AccessDeniedException    - thrown if the user has insufficient access
     *                                  to update the workItem
     * @throws ProcessingErrorException - thrown if the workitem could not be
     *                                  processed by the workflowKernel
     * @throws PluginException          - thrown if processing by a plugin fails
     * @throws ModelException
     **/
    public ItemCollection processWorkItem(ItemCollection workitem, ItemCollection event)
            throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

        return processWorkItem(workitem, event.getItemValueInteger("numactivityid"));
    }

    /**
     * This method processes a workItem based on a given event.
     * 
     * @see method ItemCollection processWorkItem(ItemCollection workitem)
     * 
     * @param workitem - the workItem to be processed
     * @param event    - event object
     * @return updated version of the processed workItem
     * @throws AccessDeniedException    - thrown if the user has insufficient access
     *                                  to update the workItem
     * @throws ProcessingErrorException - thrown if the workitem could not be
     *                                  processed by the workflowKernel
     * @throws PluginException          - thrown if processing by a plugin fails
     * @throws ModelException
     **/
    public ItemCollection processWorkItem(ItemCollection workitem, int eventID)
            throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

        workitem.setEventID(eventID);
        return processWorkItem(workitem);
    }

    /**
     * This method processes a workitem in a new transaction.
     * 
     * @throws ModelException
     * @throws PluginException
     * @throws ProcessingErrorException
     * @throws AccessDeniedException
     * 
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public ItemCollection processWorkItemByNewTransaction(ItemCollection workitem)
            throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {
        boolean debug = logger.isLoggable(Level.FINE);
        if (debug) {
            logger.finest(" ....processing workitem by by new transaction...");
        }
        return processWorkItem(workitem);
    }

    public void removeWorkItem(ItemCollection aworkitem) throws AccessDeniedException {
        documentService.remove(aworkitem);
    }

    // /**
    // * This Method returns the modelManager Instance. The current ModelVersion is
    // * automatically updated during the Method updateProfileEntity which is called
    // * from the processWorktiem method.
    // *
    // */
    // public ModelManager getModelManager() {
    // return modelService.getModelManager();
    // }

    /**
     * Returns an instance of the EJB session context.
     * 
     * @return
     */
    public SessionContext getSessionContext() {
        return ctx;
    }

    /**
     * Returns an instance of the DocumentService EJB.
     * 
     * @return
     */
    public DocumentService getDocumentService() {
        return documentService;
    }

    /**
     * Returns an instance of the ReportService EJB.
     * 
     * @return
     */
    public ReportService getReportService() {
        return reportService;
    }

    /**
     * The method evaluates the next task for a process instance (workitem) based on
     * the current model definition. A Workitem must at least provide the properties
     * $TASKID and $EVENTID.
     * <p>
     * During the evaluation life-cycle more than one events can be evaluated. This
     * depends on the model definition which can define follow-up-events,
     * split-events and conditional events.
     * <p>
     * The method did not persist the process instance or execute any plugin or
     * adapter classes.
     * 
     * @return Task entity
     * @throws PluginException
     * @throws ModelException
     */
    public ItemCollection evalNextTask(ItemCollection workitem) throws PluginException, ModelException {
        WorkflowKernel workflowkernel = new WorkflowKernel(workflowContextService);
        ItemCollection task = workflowkernel.eval(workitem);
        return task;
    }

    /**
     * This method register all plugin classes listed in the model profile
     * 
     * @throws PluginException
     * @throws ModelException
     */
    @SuppressWarnings("unchecked")
    protected void registerPlugins(WorkflowKernel workflowkernel, ItemCollection profile)
            throws PluginException, ModelException {
        boolean debug = logger.isLoggable(Level.FINE);

        // register plugins defined in the environment.profile ....
        List<String> vPlugins = (List<String>) profile.getItemValue("txtPlugins");
        for (int i = 0; i < vPlugins.size(); i++) {
            String aPluginClassName = vPlugins.get(i);

            Plugin aPlugin = findPluginByName(aPluginClassName);
            // aPlugin=null;
            if (aPlugin != null) {
                // register injected CDI Plugin
                if (debug) {
                    logger.log(Level.FINEST, "......register CDI plugin class: {0}...", aPluginClassName);
                }
                workflowkernel.registerPlugin(aPlugin);
            } else {
                // register plugin by class name
                workflowkernel.registerPlugin(aPluginClassName);
            }
        }
    }

    protected void registerAdapters(WorkflowKernel workflowkernel) {
        boolean debug = logger.isLoggable(Level.FINE);
        if (debug && (adapters == null || !adapters.iterator().hasNext())) {
            logger.finest("......no CDI Adapters injected");
        } else if (this.adapters != null) {
            // iterate over all injected adapters....
            for (Adapter adapter : this.adapters) {
                if (debug) {
                    logger.log(Level.FINEST, "......register CDI Adapter class ''{0}''", adapter.getClass().getName());
                }
                workflowkernel.registerAdapter(adapter);
            }
        }
    }

    /**
     * This method updates the workitem metadata. The following items will be
     * updated:
     * 
     * <ul>
     * <li>$creator</li>
     * <li>$editor</li>
     * <li>$lasteditor</li>
     * <li>$participants</li>
     * </ul>
     * <p>
     * The method also migrates deprected items.
     * 
     * @param workitem
     */
    protected void updateMetadata(ItemCollection workitem) {

        // identify Caller and update CurrentEditor
        String nameEditor;
        nameEditor = ctx.getCallerPrincipal().getName();

        // add namCreator if empty
        // migrate $creator (Backward compatibility)
        if (workitem.getItemValueString("$creator").isEmpty() && !workitem.getItemValueString("namCreator").isEmpty()) {
            workitem.replaceItemValue("$creator", workitem.getItemValue("namCreator"));
        }

        if (workitem.getItemValueString("$creator").isEmpty()) {
            workitem.replaceItemValue("$creator", nameEditor);
            // support deprecated fieldname
            workitem.replaceItemValue("namCreator", nameEditor);
        }

        // update namLastEditor only if current editor has changed
        if (!nameEditor.equals(workitem.getItemValueString("$editor"))
                && !workitem.getItemValueString("$editor").isEmpty()) {
            workitem.replaceItemValue("$lasteditor", workitem.getItemValueString("$editor"));
            // deprecated
            workitem.replaceItemValue("namlasteditor", workitem.getItemValueString("$editor"));
        }

        // update $editor
        workitem.replaceItemValue("$editor", nameEditor);
        // deprecated
        workitem.replaceItemValue("namcurrenteditor", nameEditor);
    }

    /**
     * This method returns an injected Plugin by name or null if no plugin with the
     * requested class name is injected.
     * 
     * @param pluginClassName
     * @return plugin class or null if not found
     */
    private Plugin findPluginByName(String pluginClassName) {
        if (pluginClassName == null || pluginClassName.isEmpty())
            return null;
        boolean debug = logger.isLoggable(Level.FINE);

        if (plugins == null || !plugins.iterator().hasNext()) {
            if (debug) {
                logger.finest("......no CDI plugins injected");
            }
            return null;
        }
        // iterate over all injected plugins....
        for (Plugin plugin : this.plugins) {
            if (plugin.getClass().getName().equals(pluginClassName)) {
                if (debug) {
                    logger.log(Level.FINEST, "......CDI plugin ''{0}'' successful injected", pluginClassName);
                }
                return plugin;
            }
        }

        return null;
    }

}
