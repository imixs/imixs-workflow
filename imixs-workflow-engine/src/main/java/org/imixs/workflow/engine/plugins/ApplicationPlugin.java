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

package org.imixs.workflow.engine.plugins;

import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This Plugin updates application specific settings.
 * 
 * <ul>
 * <li>txtWorkflowEditorID - optional EditorID to be used by an application
 * <li>txtWorkflowImageURL - visual image can be displayed by an application
 * <li>$WorkflowAbstract - Abstract text
 * <li>$WorkflowSummary - Summary
 * 
 * These settings can be configured by the imixs modeler on the Application
 * Property Tab on a ProcessEntity.
 * 
 * The Plugin determines the new settings by fetching the next Task Entity.
 * 
 * Version 1.1
 * 
 * The Plugin will test if the provided Model supports ExtendedModels. If so the
 * Plugin will fetch the next ProcessEntity by the current used modelVersion of
 * the workitem.
 * 
 * Version 1.2 The plugin submits the new settings directly in the run() method,
 * so other plugins can access the new properties for further operations
 * http://java.net/jira/browse/IMIXS_WORKFLOW-81
 * 
 * Version 1.3: type, workflowgroup and workfowstatus are handled by the
 * WorkflowKernel
 * 
 * @author Ralph Soika
 * @version 1.3
 * @see org.imixs.workflow.WorkflowManager
 * 
 */
public class ApplicationPlugin extends AbstractPlugin {

    public final static String WORKFLOWABSTRACT = "$workflowabstract";
    public final static String WORKFLOWSUMMARY = "$workflowsummary";

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ApplicationPlugin.class.getName());

    public ItemCollection run(ItemCollection workitem, ItemCollection event)
            throws PluginException {

        String sEditorID = null;
        String sImageURL = null;
        String sAbstract = null;
        String sSummary = null;
        ItemCollection nextTask = null;

        // get next process entity
        try {
            nextTask = this.getWorkflowContext().evalNextTask(workitem);
        } catch (ModelException e) {
            throw new PluginException(ApplicationPlugin.class.getSimpleName(), e.getErrorCode(), e.getMessage());
        }

        // fetch task properties
        sEditorID = getWorkflowService().adaptText(nextTask.getItemValueString("txtEditorID"), workitem);
        sImageURL = getWorkflowService().adaptText(nextTask.getItemValueString("txtImageURL"), workitem);
        sAbstract = getWorkflowService().adaptText(nextTask.getItemValueString("txtworkflowabstract"), workitem);
        sSummary = getWorkflowService().adaptText(nextTask.getItemValueString("txtworkflowsummary"), workitem);

        // set Editor if defined
        if (sEditorID != null && !sEditorID.isBlank()) {
            workitem.setItemValue("txtWorkflowEditorID", sEditorID);
        }

        // set ImageURL if defined
        if (sImageURL != null && !sImageURL.isBlank()) {
            workitem.setItemValue("txtWorkflowImageURL", sImageURL);
        }

        // set Abstract if defined
        if (sAbstract != null && !sAbstract.isBlank()) {
            workitem.setItemValue(WORKFLOWABSTRACT, sAbstract);
        }

        // set Summary if defined
        if (sSummary != null && !sSummary.isBlank()) {
            workitem.setItemValue(WORKFLOWSUMMARY, sSummary);
        }
        return workitem;
    }

}
