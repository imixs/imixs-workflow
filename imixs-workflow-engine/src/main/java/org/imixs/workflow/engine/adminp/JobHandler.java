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
