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
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;

import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

/**
 * The SimulationService can be used to simulate a process life cycle without
 * storing any data into the database.
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
public class SimulationService implements WorkflowContext {

    private static Logger logger = Logger.getLogger(SimulationService.class.getName());

    @Resource
    private SessionContext ctx;

    @Inject
    protected Event<ProcessingEvent> events;

    @Inject
    @Any
    private Instance<Plugin> plugins;

    @Inject
    private ModelService modelService;

    public ModelService getModelService() {
        return modelService;
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }

    public SessionContext getCtx() {
        return ctx;
    }

    public void setCtx(SessionContext ctx) {
        this.ctx = ctx;
    }

    /**
     * This method simulates a processing life cycle of a process instance without
     * storing any data into the database.
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
    public ItemCollection processWorkItem(final ItemCollection _workitem, final List<String> vPlugins)
            throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

        ItemCollection workitem = _workitem;
        long l = System.currentTimeMillis();

        if (workitem == null)
            throw new ProcessingErrorException(SimulationService.class.getSimpleName(),
                    ProcessingErrorException.INVALID_WORKITEM, "WorkflowService: error - workitem is null");

        // fire event
        if (events != null) {
            events.fire(new ProcessingEvent(workitem, ProcessingEvent.BEFORE_PROCESS));
        } else {
            logger.warning("CDI Support is missing - ProcessingEvent will not be fired");
        }
        // Fetch the current Profile Entity for this version.
        WorkflowKernel workflowkernel = new WorkflowKernel(this);
        // register plugins defined in the environment.profile ....
        if (vPlugins != null && vPlugins.size() > 0) {
            for (int i = 0; i < vPlugins.size(); i++) {
                String aPluginClassName = vPlugins.get(i);

                Plugin aPlugin = findPluginByName(aPluginClassName);
                // aPlugin=null;
                if (aPlugin != null) {
                    // register injected CDI Plugin
                    logger.fine("register CDI plugin class: " + aPluginClassName + "...");
                    workflowkernel.registerPlugin(aPlugin);
                } else {
                    // register plugin by class name
                    workflowkernel.registerPlugin(aPluginClassName);
                }

            }
        }

        // now process the workitem
        try {
            workitem = workflowkernel.process(workitem);
        } catch (PluginException pe) {
            // if a plugin exception occurs we roll back the transaction.
            logger.severe("processing workitem '" + workitem.getItemValueString(WorkflowKernel.UNIQUEID)
                    + " failed, rollback transaction...");
            throw pe;
        }
        logger.fine("workitem '" + workitem.getItemValueString(WorkflowKernel.UNIQUEID) + "' simulated in "
                + (System.currentTimeMillis() - l) + "ms");

        // fire event
        if (events != null) {
            events.fire(new ProcessingEvent(workitem, ProcessingEvent.AFTER_PROCESS));
        }
        // Now fire also events for all split versions.....
        List<ItemCollection> splitWorkitems = workflowkernel.getSplitWorkitems();
        for (ItemCollection splitWorkitemm : splitWorkitems) {
            // fire event
            if (events != null) {
                events.fire(new ProcessingEvent(splitWorkitemm, ProcessingEvent.AFTER_PROCESS));
            }
        }

        return workitem;

    }

    /**
     * This Method returns the modelManager Instance. The current ModelVersion is
     * automatically updated during the Method updateProfileEntity which is called
     * from the processWorktiem method.
     * 
     */
    public ModelManager getModelManager() {
        return modelService;
    }

    /**
     * Returns an instance of the EJB session context.
     * 
     * @return
     */
    public SessionContext getSessionContext() {
        return ctx;
    }

    /**
     * This method returns a n injected Plugin by name or null if not plugin with
     * the requested class name is injected.
     * 
     * @param pluginClassName
     * @return plugin class or null if not found
     */
    private Plugin findPluginByName(String pluginClassName) {
        if (pluginClassName == null || pluginClassName.isEmpty())
            return null;

        if (plugins == null || !plugins.iterator().hasNext()) {
            logger.fine("[WorkflowService] no CDI plugins injected");
            return null;
        }
        // iterate over all injected plugins....
        for (Plugin plugin : this.plugins) {
            if (plugin.getClass().getName().equals(pluginClassName)) {
                logger.fine("[WorkflowService] CDI plugin '" + pluginClassName + "' successful injected");
                return plugin;
            }
        }

        return null;
    }

}
