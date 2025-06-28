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

package org.imixs.workflow;

import java.util.List;

import org.imixs.workflow.exceptions.QueryException;

/**
 * The Interface QuerySelector can be implemented as a CDI Bean to provide a
 * custom selection of workitems. One example usage for the QuerySelector
 * interface is the WorkflowScheduler which supports this interface to quey a
 * custom selection of scheduled workitems.
 * <p>
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.engine.WorkflowScheduler
 */
public interface QuerySelector {

    /**
     * Returns a selection of workitems. The method may throw a QueryExeption.
     * 
     * @param pageSize  - total docs per page
     * @param pageIndex - number of page to start (default = 0)
     * @return workitem selection, can be null
     * @throws QueryException
     */
    public List<ItemCollection> find(int pageSize, int pageIndex) throws QueryException;

}
