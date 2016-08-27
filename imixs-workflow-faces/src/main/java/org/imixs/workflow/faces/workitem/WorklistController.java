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

import javax.ejb.EJB;

import org.imixs.workflow.engine.WorkflowService;

/**
 * The WorkflowController extends the ViewControler and provides a set of
 * workflow specific sortable view types. The sortorder can be defined by the
 * property 'sortOrder'.
 * 
 * The default view type is 'worklist.all'. The default search order is
 * 'SORT_ORDER_CREATED_DESC'
 * 
 * @author rsoika
 * @version 0.0.2
 */
public class WorklistController extends ViewController {

	private static final long serialVersionUID = 1L;


	@EJB
	private WorkflowService workflowService;

	/**
	 * Set default values for view type and sort order.
	 */
	public WorklistController() {
		super();
	
	}

	public WorkflowService getWorkflowService() {
		return workflowService;
	}

	

	
}
