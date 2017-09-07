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

package org.imixs.workflow.engine;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;

/**
 * The interface <i>ObserverPlugin</i> extends the Imixs Plugin API with an
 * observer pattern. A Plugin can implement the ObserverPlugin interface to
 * listen to the processing lifecycle of the WorkflowService EJB. A
 * ObserverPlugin will automatically be registered by the WorkflowService after
 * the plugin registration phase.
 * 
 * The ObserverPlugin defines the following callback methods:
 * <ul>
 * <li>afterRegistration - called immediately after the plugin registration
 * phase was completed
 * <li>beforeProcess - called before the method workflowkernel.process(workitem)
 * is called and the workitem is completely prepared for processing.
 * <li>afterProcess - called immediately after the method
 * workflowkernel.process(workitem) was called and before the method
 * _documentService.save(workitem) is called.
 * </ul>
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.engine.WorkflowService
 */

public interface ObserverPlugin {

	/**
	 * This method is called immediately after the plugin registration phase was
	 * completed. A plugin can throw a PluginException to cancel the processing
	 * phase.
	 * 
	 * @param workitem
	 */
	public ItemCollection afterRegistration(ItemCollection workitem) throws PluginException;

	/**
	 * called before the method workflowkernel.process(workitem) is called and the
	 * workitem is completely prepared for processing.
	 * 
	 * @param workitem
	 * @return
	 * @throws PluginException
	 */
	public ItemCollection beforeProcess(ItemCollection workitem) throws PluginException;

	/**
	 * called immediately after the method workflowkernel.process(workitem) was
	 * called and before the method _documentService.save(workitem) is called.
	 * 
	 * @param workitem
	 * @return
	 * @throws PluginException
	 */
	public ItemCollection afterProcess(ItemCollection workitem) throws PluginException;

}
