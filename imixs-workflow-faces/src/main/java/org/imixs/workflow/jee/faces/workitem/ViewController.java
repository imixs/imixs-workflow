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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ejb.DocumentService;

/**
 * The ViewController can be used in JSF Applications to manage lists of
 * ItemCollections.
 * 
 * The view property defines the view type returned by a method call of
 * getWorkitems. The ViewController implements a lazy loading mechanism to cache
 * the result. The request is delegated to an instance of IViewAdapter to
 * compute the result set. IViewAdapter can be adapted by any custom
 * implementation.
 * 
 * The ViewController bean should be used in ViewScope.
 * 
 * @author rsoika
 * @version 0.0.1
 */
public class ViewController implements Serializable {

	private static final long serialVersionUID = 1L;
	private String type = null;
	private int maxResult = 10;
	private int row = 0;
	private boolean endOfList = false;

	/* views */
	private Map<String, String> views = null;
	private String view = null;

	/* result */
	private List<ItemCollection> workitems = null;
	private IViewAdapter viewAdapter = null;

	@EJB
	DocumentService documentService;
	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	public ViewController() {
		super();
		views = new HashMap<String, String>();
		setType("workitem");
		setView("worklist.created.desc");

	}

	/**
	 * This method is preparing the JPQL statements. views can also be set in
	 * the faces-config.xml
	 */
	@PostConstruct
	public void init() {

		// setup the default view types
		if (views.get("worklist.created.desc") == null)
			views.put("worklist.created.desc", "(type:\"" + getType() + "\")");

		if (views.get("worklist.modified.desc") == null)
			views.put("worklist.created.desc", "(type:\"" + getType() + "\")");

		if (views.get("worklist.name.asc") == null)
			views.put("worklist.created.desc", "(type:\"" + getType() + "\")");
	}

	/**
	 * returns an instance of the DocumentService EJB
	 * 
	 * @return
	 */
	public DocumentService getDocumentService() {
		return documentService;
	}

	/**
	 * set the value for the attribute 'type' of a workitem to be generated or
	 * search by this controller
	 */
	public String getType() {
		return type;
	}

	/**
	 * defines the type attribute of a workitem to be generated or search by
	 * this controller
	 * 
	 * Subclasses may overwrite the type
	 * 
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * A map with containing a list of JPQL definitions. The property 'view'
	 * defines the actual view.
	 * 
	 * @return
	 */
	public Map<String, String> getViews() {
		return views;
	}

	public void setViews(Map<String, String> views) {
		this.views = views;
	}

	/**
	 * Current view
	 * 
	 * @return
	 */
	public String getView() {
		return view;
	}

	public void setView(String view) {
		this.view = view;
		this.doReset();
	}

	/**
	 * returns the maximum size of a search result
	 * 
	 * @return
	 */
	public int getMaxResult() {
		return maxResult;
	}

	/**
	 * set the maximum size of a search result
	 * 
	 * @param searchCount
	 */
	public void setMaxResult(int searchCount) {
		this.maxResult = searchCount;
	}

	/**
	 * resets the current result and set the page pointer to 0.
	 * 
	 * @return
	 */
	public void doReset() {
		workitems = null;
		row = 0;
	}
	public void doReset(ActionEvent event) {
		doReset();
	}
	public void doReset(AjaxBehaviorEvent event) {
		doReset();
	}
	
	/**
	 * refreshes the current workitem list. so the list will be loaded again.
	 * but start pos will not be changed!
	 */
	public void doRefresh() {
		workitems = null;
	}
	public void doRefresh(ActionEvent event) {
		doRefresh();
	}
	public void doRefresh(AjaxBehaviorEvent event) {
		doRefresh();
	}

	public void doLoadNext() {
		row = row + maxResult;
		workitems = null;
	}
	public void doLoadNext(ActionEvent event) {
		doLoadNext();
	}
	public void doLoadNext(AjaxBehaviorEvent event) {
		doLoadNext();
	}

	public void doLoadPrev() {
		row = row - maxResult;
		if (row < 0)
			row = 0;
		workitems = null;
	}
	public void doLoadPrev(ActionEvent event) {
		doLoadPrev();
	}
	
	public void doLoadPrev(AjaxBehaviorEvent event) {
		doLoadPrev();
	}

	/**
	 * Returns the current view result. The request is delegated to an
	 * implementation of IViewAdapter.
	 * 
	 * The method implements a lazy loading mechanism and caches the result
	 * locally.
	 * 
	 * The returned result set is defined by the current view definition. The
	 * view definition can be set by the property view. All view definitions are
	 * stored in the property views.
	 * 
	 * The ViewAdapter implements the behavior to return a collection of
	 * ItemCollections based on the current view type
	 * 
	 * @return view result
	 */
	public List<ItemCollection> getWorkitems() {
		// return a cached result set?
		if (workitems != null)
			return workitems;
	
		long lTime = System.currentTimeMillis();
		// delegate the request to ViewAdapter...
		workitems = getViewAdapter().getViewEntries(this);

		// if no result is defined return an empty list.
		if (workitems == null)
			workitems = new ArrayList<ItemCollection>();

		// compute end of file
		endOfList = (workitems.size() < maxResult);
	
		// logging
		lTime = System.currentTimeMillis() - lTime;
		logger.finest("  getWorkitems (" + lTime + " ms)");
		return workitems;
	}

	public void setWorkitems(List<ItemCollection> workitems) {
		this.workitems = workitems;
	}

	/***************************************************************************
	 * Navigation
	 */

	public int getRow() {
		return row;
	}

	public boolean isEndOfList() {
		return endOfList;
	}

	public void setEndOfList(boolean endOfList) {
		this.endOfList = endOfList;
	}

	public IViewAdapter getViewAdapter() {
		if (viewAdapter == null)
			viewAdapter = new ViewAdapter();

		return viewAdapter;
	}

	public void setViewAdapter(IViewAdapter viewAdapter) {
		this.viewAdapter = viewAdapter;
	}

	protected class ViewAdapter implements IViewAdapter {

		public List<ItemCollection> getViewEntries(
				final ViewController controller) {

			List<ItemCollection> result = controller.getDocumentService()
					.find(controller.views.get(controller.view),
							controller.row, controller.maxResult);

			return result;

		}
	}
}
