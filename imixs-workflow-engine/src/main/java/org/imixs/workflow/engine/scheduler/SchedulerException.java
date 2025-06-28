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

package org.imixs.workflow.engine.scheduler;

import org.imixs.workflow.exceptions.WorkflowException;

/**
 * The SchedulerException is thrown from the generic scheduler service
 * 
 * @see org.imixs.workflow.engine.scheduler.GenericScheduelrService
 * @author rsoika
 * 
 */
public class SchedulerException extends WorkflowException {

    private static final long serialVersionUID = 1L;

    public static final String INVALID_MODELVERSION = "INVALID_MODELVERSION";
    public static final String INVALID_WORKITEM = "INVALID_WORKITEM";
    public static final String INVALID_PROCESSID = "INVALID_PROCESSID";

    protected String errorContext = "UNDEFINED";
    protected String errorCode = "UNDEFINED";

    public SchedulerException(String aErrorCode, String message) {
        super(aErrorCode, message);
    }

    public SchedulerException(String aErrorContext, String aErrorCode, String message) {
        super(aErrorContext, aErrorCode, message);
    }

    public SchedulerException(String aErrorContext, String aErrorCode, String message, Exception e) {
        super(aErrorContext, aErrorCode, message, e);
    }

    public SchedulerException(String aErrorCode, String message, Exception e) {
        super(aErrorCode, message, e);
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

}
