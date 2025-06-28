/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow.faces.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import java.util.logging.Level;
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

    private static final Logger logger = Logger.getLogger(ViewHandler.class.getName());

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
        logger.log(Level.FINEST, "......cache with hash={0}", getHashKey(viewController));
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
