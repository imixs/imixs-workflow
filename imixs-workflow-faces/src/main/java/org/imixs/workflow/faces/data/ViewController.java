/*  
 *  Imixs-Workflow 
 *  
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
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

package org.imixs.workflow.faces.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The ViewController can be used in JSF Applications to manage lists of
 * ItemCollections.
 * <p>
 * The view property defines the view type returned by a method call of
 * loadData. The ViewController implements a lazy loading mechanism to cache the
 * result.
 * <p>
 * The property 'loadStubs' can be used to define if only the Document Stubs
 * (default) or the full Document should be loaded.
 * <p>
 * The ViewController bean should be used in ViewScope.
 * 
 * @author rsoika
 * @version 0.0.1
 */
@Named
@ViewScoped
public class ViewController implements Serializable {

    private static final long serialVersionUID = 1L;

    private String query = null;
    private String sortBy = null;
    private boolean sortReverse = false;
    private int pageSize = 10;
    private int pageIndex = 0;
    private boolean endOfList = false;
    private boolean loadStubs = true;

    private static Logger logger = Logger.getLogger(ViewController.class.getName());

    @Inject
    private DocumentService documentService;

    public ViewController() {
        super();
        logger.finest("...construct...");
    }

    @PostConstruct
    public void init() {
        logger.finest("init...");
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

    public boolean isLoadStubs() {
        return loadStubs;
    }

    public void setLoadStubs(boolean loadStubs) {
        this.loadStubs = loadStubs;
    }

    /**
     * resets the current result and set the page pointer to 0.
     * 
     * @return
     */
    public void reset() {
        pageIndex = 0;
    }

    @Deprecated
    public List<ItemCollection> getWorkitems() throws QueryException {
        logger.warning("getWorkitems is deprected - replace with viewHandler#getData(viewController)");
        return null;
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

    /**
     * Returns the current view result. The returned result set is defined by the
     * current query definition.
     * <p>
     * The method implements a lazy loading mechanism and caches the result locally.
     * 
     * @return view result
     * @throws QueryException
     */
    public List<ItemCollection> loadData() throws QueryException {

        String _query = getQuery();

        if (_query == null || _query.isEmpty()) {
            // no query defined
            logger.warning("no query defined!");
            return new ArrayList<ItemCollection>();
        }

        // load data
        logger.finest("...... load data - query=" + _query + " pageIndex=" + getPageIndex());

        List<ItemCollection> result = null;
        if (this.isLoadStubs()) {
            result = documentService.findStubs(_query, getPageSize(), getPageIndex(), getSortBy(), isSortReverse());

        } else {
            result = documentService.find(_query, getPageSize(), getPageIndex(), getSortBy(), isSortReverse());

        }

        // The end of a list is reached when the size is below or equal the
        // pageSize. See issue #287
        if (result.size() < getPageSize()) {
            setEndOfList(true);
        } else {
            // look ahead if we have more entries...
            int iAhead = (getPageSize() * (getPageIndex() + 1)) + 1;
            if (documentService.count(_query, iAhead) < iAhead) {
                // there is no more data
                setEndOfList(true);
            } else {
                setEndOfList(false);
            }
        }

        return result;

    }

}
