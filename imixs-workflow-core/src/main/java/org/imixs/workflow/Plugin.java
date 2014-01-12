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

package org.imixs.workflow;

import org.imixs.workflow.exceptions.PluginException;


/** 
 * A Plugin defines the interface between the WorkflowKernel and the underlying softwaresystem in which the plugin runns.
 * Every Plugin have to be registerd to the workflowkernel by the workflowmanager. In this way the WorkflowManager can control
 * the functionallity of a single workflowactivity by defining the used plugin-moduls.
 * 
 * @author Ralph Soika
 * @version 1.0 
 * @see    org.imixs.workflow.WorkflowKernel 
 */ 


public interface Plugin {
	
	public final int PLUGIN_ERROR = 2;
	public final int PLUGIN_WARNING = 1;
	public final int PLUGIN_OK = 0;

	
	/**
	 * The init Methode is usesd to initialize the plugin.
	 * 
	 * @param actx defines the context in which the plugin runs
	 * a Plugin can use this context to get information about the enviroment
	 *
	 */
	public void init(WorkflowContext actx) throws PluginException;

	/**
	 * @param documentContext defines the document to be processed
	 * @param documentActivity defines the activity document which contains the workflowprocessing instructions
	 * @return the current status for this plugin
	 */
	public int run(ItemCollection documentContext, ItemCollection documentActivity) throws PluginException;


	/**
	 * This CallBack method is used to give the plugin the chance to close plugin specific data
	 * @param status gives the plugin information about the current status. this parameter is delivered by the workflowKernel.  
	 */
	public void close(int status) throws PluginException;
}
