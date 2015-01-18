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

package org.imixs.workflow.jee.faces.workitem;

import java.util.List;

import javax.ejb.EJB;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.WorkflowService;

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
	private int sortOrder;

	/* Worklist and Search */
	public static final String QUERY_WORKLIST_BY_OWNER = "worklist.owner";
	public static final String QUERY_WORKLIST_BY_CREATOR = "worklist.creator";
	public static final String QUERY_WORKLIST_BY_AUTHOR = "worklist.author";
	public static final String QUERY_WORKLIST_BY_WRITEACCESS = "worklist.writeaccess";
	public static final String QUERY_WORKLIST_ALL = "worklist.all";

	@EJB
	private org.imixs.workflow.jee.ejb.WorkflowService workflowService;

	/**
	 * Set default values for view type and sort order.
	 */
	public WorklistController() {
		super();
		setType("workitem");
		setView(QUERY_WORKLIST_ALL);
		setSortOrder(WorkflowService.SORT_ORDER_CREATED_DESC);
		setViewAdapter(new WorkflowViewAdapter());
	}

	public org.imixs.workflow.jee.ejb.WorkflowService getWorkflowService() {
		return workflowService;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	/**
	 * Custom implementation of a ViewAdapter to return workflow specific result
	 * lists.
	 * 
	 * @author rsoika
	 *
	 */
	class WorkflowViewAdapter extends ViewAdapter {

		public List<ItemCollection> getViewEntries(
				final ViewController controller) {
			if (QUERY_WORKLIST_BY_AUTHOR.equals(getView()))
				return workflowService.getWorkListByAuthor(null,
						controller.getRow(), controller.getMaxResult(),
						controller.getType(), getSortOrder());

			if (QUERY_WORKLIST_BY_CREATOR.equals(getView()))
				return workflowService.getWorkListByCreator(null,
						controller.getRow(), controller.getMaxResult(),
						controller.getType(), getSortOrder());
			if (QUERY_WORKLIST_BY_OWNER.equals(getView()))
				return workflowService.getWorkListByOwner(null,
						controller.getRow(), controller.getMaxResult(),
						controller.getType(), getSortOrder());
			if (QUERY_WORKLIST_BY_WRITEACCESS.equals(getView()))
				return workflowService.getWorkListByWriteAccess(
						controller.getRow(), controller.getMaxResult(),
						controller.getType(), getSortOrder());
			
			if (QUERY_WORKLIST_ALL.equals(getView()))
				return workflowService.getWorkList(
						controller.getRow(), controller.getMaxResult(),
						controller.getType(), getSortOrder());
			

			// default behaivor - QUERY_WORKLIST_ALL
			return super.getViewEntries(controller);

		}
	}
}
