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

package org.imixs.workflow.faces.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.QueryException;

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
@Named
@ViewScoped
public class ViewController implements Serializable {

	private static final long serialVersionUID = 1L;
	private String type = null;
	private String query = null;
	private String sortBy = null;
	private boolean sortReverse = false;
	private int pageSize = 10;
	private int pageIndex = 0;
	private boolean endOfList = false;

	/* result */
	private List<ItemCollection> workitems = null;
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(ViewController.class.getName());

	@EJB
	DocumentService documentService;

	public ViewController() {
		super();

	}

	/**
	 * This method is preparing the query and sort order. Can also be set in the
	 * faces-config.xml
	 */
	@PostConstruct
	public void init() {

		if (type == null || type.isEmpty()) {
			setType("workitem");
		}
		if (query == null || query.isEmpty()) {
			setQuery("(type:\"" + getType() + "\")");
		}

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
	 * defines the type attribute of a workitem to be generated or search by this
	 * controller
	 * 
	 * Subclasses may overwrite the type
	 * 
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Returns the search Query
	 * 
	 * @return
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * set the search query
	 * 
	 * @param query
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	public String getSortBy() {
		return sortBy;
	}

	public void setSortBy(String sortBy) {
		this.sortBy = sortBy;
	}

	public boolean isSortReverse() {
		return sortReverse;
	}

	public void setSortReverse(boolean sortReverse) {
		this.sortReverse = sortReverse;
	}

	/**
	 * returns the maximum size of a search result
	 * 
	 * @return
	 */
	public int getPageSize() {
		return pageSize;
	}

	/**
	 * set the maximum size of a search result
	 * 
	 * @param searchCount
	 */
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * resets the current result and set the page pointer to 0.
	 * 
	 * @return
	 */
	public void doReset() {
		workitems = null;
		pageIndex = 0;
	}

	public void doReset(ActionEvent event) {
		doReset();
	}

	public void doReset(AjaxBehaviorEvent event) {
		doReset();
	}

	/**
	 * refreshes the current workitem list. so the list will be loaded again. but
	 * start pos will not be changed!
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
		pageIndex++;
		workitems = null;
	}

	public void doLoadNext(ActionEvent event) {
		doLoadNext();
	}

	public void doLoadNext(AjaxBehaviorEvent event) {
		doLoadNext();
	}

	public void doLoadPrev() {
		pageIndex--;
		if (pageIndex < 0) {
			pageIndex = 0;
		}
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
	 * The method implements a lazy loading mechanism and caches the result locally.
	 * 
	 * The returned result set is defined by the current view definition. The view
	 * definition can be set by the property view. All view definitions are stored
	 * in the property views.
	 * 
	 * The ViewAdapter implements the behavior to return a collection of
	 * ItemCollections based on the current view type
	 * 
	 * @return view result
	 * @throws QueryException
	 */
	public List<ItemCollection> getWorkitems() throws QueryException {
		// return a cached result set?
		if (workitems != null)
			return workitems;

		workitems = new ArrayList<ItemCollection>();

		String _query = getQuery();
		if (_query == null || _query.isEmpty()) {
			// no query defined
			return workitems;
		}

		// load data
		workitems = getDocumentService().find(_query, getPageSize(), getPageIndex(), getSortBy(), isSortReverse());

		// if no result is defined return an empty list.
		if (workitems == null) {
			workitems = new ArrayList<ItemCollection>();
		}

		// The end of a list is reached when the size is below or equal the
		// pageSize. See issue #287
		if (workitems.size() < pageSize) {
			endOfList = true;
		} else {
			// look ahead if we have more entries...
			int iAhead = (getPageSize() * (getPageIndex() + 1)) + 1;
			if (getDocumentService().count(_query, iAhead) < iAhead) {
				// there is no more data
				endOfList = true;
			} else {
				endOfList = false;
			}
		}

		return workitems;
	}

	public void setWorkitems(List<ItemCollection> workitems) {
		this.workitems = workitems;
	}

	/***************************************************************************
	 * Navigation
	 */

	public int getPageIndex() {
		return pageIndex;
	}

	public void setPageIndex(int pageIndex) {
		this.pageIndex = pageIndex;
	}

	public boolean isEndOfList() {
		return endOfList;
	}

	public void setEndOfList(boolean endOfList) {
		this.endOfList = endOfList;
	}

}
