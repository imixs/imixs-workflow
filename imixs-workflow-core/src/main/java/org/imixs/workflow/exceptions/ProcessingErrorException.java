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

package org.imixs.workflow.exceptions;

/**
 * An ProcessingErrorException is a RuntimeExcption thrown by the
 * workflowManager if an error occurs during the process method
 * 
 * @author rsoika
 * 
 */
public class ProcessingErrorException extends InvalidAccessException {

    private static final long serialVersionUID = 1L;

    public static final String INVALID_MODELVERSION = "INVALID_MODELVERSION";
    public static final String INVALID_WORKITEM = "INVALID_WORKITEM";
    public static final String INVALID_PROCESSID = "INVALID_PROCESSID";

    public ProcessingErrorException(String aErrorCode, String message) {
        super(aErrorCode, message);
    }

    public ProcessingErrorException(String aErrorContext, String aErrorCode, String message) {
        super(message);
        errorContext = aErrorContext;
        errorCode = aErrorCode;

    }

    public ProcessingErrorException(String aErrorContext, String aErrorCode, String message, Exception e) {
        super(message, e);
        errorContext = aErrorContext;
        errorCode = aErrorCode;

    }

}
