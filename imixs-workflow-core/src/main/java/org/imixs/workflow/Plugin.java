/*******************************************************************************
 * <pre>
 *  Imixs Workflow 
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
 *      Imixs Software Solutions GmbH - initial API and implementation
 *      Ralph Soika - Software Developer
 * </pre>
 *******************************************************************************/

package org.imixs.workflow;

import org.imixs.workflow.exceptions.PluginException;

/**
 * A Plugin defines the interface between the WorkflowKernel and the WorkflowManager. Each Plugin
 * have to be registered to the WorkflowKernel by the WorkflowManager. The WorkflowKernel executes
 * all registered Plugins when processing a workflow event. A Plugin may throw a PluginException in
 * case the execution failed. This will stop the execution of the process method. A Plugin methods
 * init() and close() can be implemented by Plugin to initialize or tear down external resources or
 * data.
 * 
 * @author Ralph Soika
 * @version 2.0
 * @see org.imixs.workflow.WorkflowKernel
 */

public interface Plugin {

  /**
   * This method is called before the WorklfowKernel starts the execution. A plugin can for example
   * initialize external resources or data.
   * 
   * @param workflowContext defines the context in which the plugin runs. The context can be used to
   *                        get information about the environment
   *
   */
  public void init(WorkflowContext workflowContext) throws PluginException;

  /**
   * @param document the workitem to be processed
   * @param event    the workflow event containing the processing instructions
   * @return updated workitem for further processing
   */
  public ItemCollection run(ItemCollection document, ItemCollection event) throws PluginException;

  /**
   * This method is called after all plugins are executed by the WorkfloKernel. A plugin my tear
   * down external resources.
   * 
   * @param rollbackTransaction indicates if the current transaction will be rolled back.
   */
  public void close(boolean rollbackTransaction) throws PluginException;
}
