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

import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;

/**
 * The WorkflowManager is the general interface for a concrete implementation of a workflow
 * management system. The Interface defines the basic methods for processing and encountering a
 * workItem. The WorkflowManger instantiate a WorkflowKernel, an supports the platform dependent
 * environment for concrete Workitems and Workflow models.
 * 
 * @author Ralph Soika
 * @version 1.1
 * @see org.imixs.workflow.WorkflowKernel
 */

public interface WorkflowManager {

  /**
   * This method processes a workItem. The workItem needs at least provide the valid attributes
   * $taskID and $EventID (integer values) to identify the current processEntity the workItem
   * belongs to and the concrete activtyEntity which should be processed by the wokflowManager
   * implementation. If the workItem is new the method creates a new instance for the corresponding
   * process.
   * <p>
   * The method is responsible to persist the workItem after successfully processing. The method
   * returns the workItem with additional workflow informations defined by the workfowManager
   * Implementation.
   * <p>
   * The Method throws an InvalidWorkitemException if the provided workItem is invalid or the
   * provided attributes $taskID and $EventID (integer) did not match an valid modelEntity the
   * workItem can be processed to.
   * 
   * @param workitem a workItem instance which should be processed
   * @return the workItem instance after successful processing
   * 
   * @throws AccessDeniedException            - thrown if the user has insufficient access to update
   *                                          the workItem
   * @throws ProcessingErrorException         - thrown if the workitem could not be processed by the
   *                                          workflowKernel
   * @throws AdapterExceptionAdapterException - thrown if processing by an adapter fails
   * @throws PluginException                  - thrown if processing by a plugin fails
   */
  public ItemCollection processWorkItem(ItemCollection workitem)
      throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException;

  /**
   * returns a workItem by its uniuqeID ($uniqueID)
   * 
   * @param uniqueid
   * @return WorkItem
   * 
   */
  public ItemCollection getWorkItem(String uniqueid);

  /**
   * The method removes the provide Workitem form the persistence unit managed by the
   * WorkflowManager implementation.
   * 
   * The Method throws an InvalidWorkitemException if the provided Workitem is invalid.
   * 
   * @param uniqueid of the WorkItem to be removed
   * @throws AccessDeniedException
   */
  public void removeWorkItem(ItemCollection workitem) throws AccessDeniedException;

}
