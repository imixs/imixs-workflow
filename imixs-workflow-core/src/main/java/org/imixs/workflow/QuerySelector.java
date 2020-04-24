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
