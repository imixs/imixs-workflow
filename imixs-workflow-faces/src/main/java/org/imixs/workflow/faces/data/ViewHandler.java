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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The ViewHandler is a @RequestScoped CDI bean computing the result defined by
 * a ViewController.
 * 
 * @author rsoika
 * @version 0.0.1
 */
@Named
@RequestScoped
public class ViewHandler implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<Integer, List<ItemCollection>> data = null;

    private static Logger logger = Logger.getLogger(ViewHandler.class.getName());

    public ViewHandler() {
        super();
    }

    @PostConstruct
    public void init() {
        logger.finest("......init data map");
        data = new HashMap<Integer, List<ItemCollection>>();
    }

    /**
     * This method can be used in ajax forms to pre-compute the result set for
     * further rendering.
     * 
     * @param viewController
     * @throws QueryException
     */
    public void onLoad(ViewController viewController) throws QueryException {
        getData(viewController);
    }

    public void forward(ViewController viewController) {
        data.remove(getHashKey(viewController));
        viewController.setPageIndex(viewController.getPageIndex() + 1);
    }

    public void back(ViewController viewController) {
        data.remove(getHashKey(viewController));
        int i = viewController.getPageIndex();
        i--;
        if (i < 0) {
            i = 0;
        }
        viewController.setPageIndex(i);
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
    public List<ItemCollection> getData(ViewController viewController) throws QueryException {

        if (viewController == null) {
            return new ArrayList<ItemCollection>();
        }

        String _query = viewController.getQuery();
        if (_query == null || _query.isEmpty()) {
            // no query defined
            logger.finest("......ViewController - no query defined!");
            return new ArrayList<ItemCollection>();
        }

        // Caching mechanism - verify if data is already cached
        List<ItemCollection> result = data.get(getHashKey(viewController));
        if (result != null) {
            // return a cached result set
            return result;
        }

        // load data
        result = viewController.loadData();
        logger.finest("......cache with hash=" + getHashKey(viewController));
        // cache result
        data.put(getHashKey(viewController), result);

        return result;
    }

    private int getHashKey(ViewController viewController) {
        if (viewController == null) {
            return -1;
        }
        String h = viewController.getQuery() + viewController.getPageIndex() + viewController.getPageSize();
        return h.hashCode();
    }

}
