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

package org.imixs.workflow;

import org.imixs.workflow.exceptions.PluginException;

/**
 * A Plugin defines the interface between the WorkflowKernel and the
 * WorkflowManager. Each Plugin have to be registered to the WorkflowKernel by
 * the WorkflowManager. The WorkflowKernel executes all registered Plugins when
 * processing a workflow event. A Plugin may throw a PluginException in case the
 * execution failed. This will stop the execution of the process method. A
 * Plugin methods init() and close() can be implemented by Plugin to initialize
 * or tear down external resources or data.
 * 
 * @author Ralph Soika
 * @version 2.0
 * @see org.imixs.workflow.WorkflowKernel
 */

public interface Plugin {

    /**
     * This method is called before the WorkflowKernel starts the execution. A
     * plugin can for example initialize external resources or data.
     * 
     * @param model provides an instance to the current bpmn model (Note: this
     *              instance is not thread save!)
     *
     */
    public void init(WorkflowContext ctx) throws PluginException;

    /**
     * @param document the workitem to be processed
     * @param event    the workflow event containing the processing instructions
     * @return updated workitem for further processing
     */
    public ItemCollection run(ItemCollection document, ItemCollection event) throws PluginException;

    /**
     * This method is called after all plugins are executed by the WorkfloKernel. A
     * plugin my tear down external resources.
     * 
     * @param rollbackTransaction indicates if the current transaction will be
     *                            rolled back.
     */
    public void close(boolean rollbackTransaction) throws PluginException;
}
