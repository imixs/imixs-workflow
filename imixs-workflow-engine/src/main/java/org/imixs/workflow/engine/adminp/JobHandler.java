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

package org.imixs.workflow.engine.adminp;

import org.imixs.workflow.ItemCollection;

public interface JobHandler {

    public final static String ISCOMPLETED = "iscompleted";

    /**
     * Called by the AdminPService. The JobHandler returns the job description with
     * pre defined fields to signal the status.
     * 
     * The AdminPService will terminate the job in cases the job is complete.
     * Otherwise the AdminPServcie will wait for the next timeout.
     * <p>
     * Fields:
     * <ul>
     * <li>type - fixed to value 'adminp'</li>
     * <li>job - the job type/name, defined by handler</li>
     * <li>$WorkflowStatus - status controlled by AdminP Service</li>
     * <li>$WorkflowSummary - summary of job description</li>
     * <li>isCompleted - boolean indicates if job is completed - controlled by job
     * handler</li>
     * </ul>
     * 
     * The AdminPService will not call the JobHandler if the job description field
     * 'isCompleted==true'
     * 
     * A JobHandler may throw a AdminPException if something went wrong.
     * 
     * @param job description
     * @return updated job description
     */
    public ItemCollection run(ItemCollection job) throws AdminPException;
}
