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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The ViewHandler is a RequestScoped CDI bean to display a data result within a
 * JSF page. The result is controlled by the ViewController which defines the
 * query and the paging mechanism to navigate through a view of data result.
 * <p>
 * To load the data the public method loadData() can be called which expects a
 * ViewController. This can be placed in the f:metadata tag of a JSF page:
 * 
 * <pre>{@code
  	<f:metadata>
       <f:viewAction action="#{viewController.setQuery(..." />
       <f:viewAction action="#{viewHandler.loadData(viewController)... />
 </f:metadata>
  }</pre>

 * 
 * @author rsoika
 * 
 * @version 0.0.1
 */
@Named
@RequestScoped
public class ViewHandler implements Serializable {

	private static Logger logger = Logger.getLogger(ViewHandler.class.getName());

	private static final long serialVersionUID = 1L;

	private List<ItemCollection> data = null;

	@EJB
	DocumentService documentService;

	public ViewHandler() {
		super();
	}

	/**
	 * Returns the view result which is computed after the view was initialized.
	 * 
	 * @return view result
	 * @throws QueryException
	 */
	/*
	 * public List<ItemCollection> getResult(ViewController viewController) { if
	 * (data == null) { loadData(viewController); } return data; }
	 */

	/**
	 * This method computes the view result
	 */
	public void loadData(ViewController viewController) {
		String _query = viewController.getQuery();
		logger.info("..... loading view data: " + _query);
		if (_query == null || _query.isEmpty()) {
			// no query defined - return empty list
			data = new ArrayList<ItemCollection>();
		}

		// load data
		try {
			data = documentService.find(_query, viewController.getPageSize(), viewController.getPageIndex(),
					viewController.getSortBy(), viewController.isSortReverse());
		} catch (QueryException e) {
			logger.warning("Unable to load view data: " + e.getMessage());
			data = new ArrayList<ItemCollection>();
		}

		// The end of a list is reached when the size is below or equal the
		// pageSize. See issue #287
		if (data.size() < viewController.getPageSize()) {
			viewController.setEndOfList(true);
		} else {
			// look ahead if we have more entries...
			int iAhead = (viewController.getPageSize() * (viewController.getPageIndex() + 1)) + 1;
			try {
				if (documentService.count(viewController.getQuery(), iAhead) < iAhead) {
					// there is no more data
					viewController.setEndOfList(true);
				} else {
					viewController.setEndOfList(false);
				}
			} catch (QueryException e) {
				logger.warning("Unable to compute count of data: " + e.getMessage());
			}
		}

	}

	public List<ItemCollection> getData() {
		if (data == null) {
			data = new ArrayList<ItemCollection>();
		}
		return data;
	}
}
