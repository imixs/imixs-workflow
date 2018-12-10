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
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.imixs.workflow.engine.DocumentService;

/**
 * The ViewController can be used in JSF Applications to select a data result to
 * be displayed by a view.
 * 
 * <p>
 * The query can be defined by the jsf tag, <f:viewAction>. The viewAction
 * component must be declared as a child of the metadata facet (<f:metadata>).
 * 
 * <pre>
 * {@code
 *    <f:metadata> 
 *      <f:viewAction action=" viewController.setQuery('...." />
 *    </f:metadata>
 * }
 * </pre>
 *
 *
 *
 * <p>
 * The view property defines the view type returned by a method call of getData.
 * The ViewController is ConversationScoped to support a paging navigation.
 * <p>
 * The query can be defined by the jsf tag, <f:viewAction>. The viewAction
 * component must be declared as a child of the metadata facet (<f:metadata>).
 * 
 * <pre>
 * {@code
 *    <f:metadata> 
 *      <f:viewAction action=" viewController.setQuery('...." />
 *    </f:metadata>
 * }
 * </pre>
 *
 *
 * @author rsoika
 * @version 0.0.1
 */
@Named
@ConversationScoped
public class ViewController implements Serializable {

	private static Logger logger = Logger.getLogger(ViewController.class.getName());

	private static final long serialVersionUID = 1L;
	private String query = null;
	private String sortBy = null;
	private boolean sortReverse = false;
	private int pageSize = 10;
	private int pageIndex = 0;
	private boolean endOfList = false;

	@Inject
	Conversation conversation;

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
		if (conversation.isTransient()) {
			conversation.setTimeout(((HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext()
					.getRequest()).getSession().getMaxInactiveInterval()*1000);
			conversation.begin();
			logger.finest("......start new conversation, id=" + conversation.getId());
		}
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
	 * resets the current page index to 0.
	 * 
	 * @return
	 */
	public void doReset() {
		pageIndex = 0;
	}

	public void doLoadNext() {
		pageIndex++;

	}

	public void doLoadPrev() {
		pageIndex--;
		if (pageIndex < 0) {
			pageIndex = 0;
		}
	}

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

	@Deprecated
	public void setType(String a) {
		logger.warning("attribute type is deprecated");
	}

	/**
	 * Just for backward compatibility.
	 * 
	 * @return
	 */
	@Deprecated
	public String getType() {
		logger.warning("attribute type is deprecated");
		return null;
	}

}
