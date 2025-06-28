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
 * An InvalidAccessException is a runtime exception which should be thrown by a
 * Imixs Workflow component if a method call is invalid or the data structure is
 * in an invalid state.
 * 
 * The property errorCode specifies the exception type. Extensions of this
 * Exception may add additional errorCodes.
 * 
 * @author rsoika
 * 
 */
public class InvalidAccessException extends RuntimeException {

    public static final String OPERATION_NOTALLOWED = "OPERATION_NOTALLOWED";
    public static final String INVALID_ID = "INVALID_ID";
    public static final String INVALID_INDEX = "INVALID_INDEX";

    protected String errorCode = "UNDEFINED";
    protected String errorContext = "UNDEFINED";

    private static final long serialVersionUID = 1L;

    public InvalidAccessException(String message) {
        super(message);
    }

    public InvalidAccessException(String message, Exception e) {
        super(message, e);
    }

    public InvalidAccessException(String aErrorCode, String message) {
        super(message);
        errorCode = aErrorCode;
    }

    public InvalidAccessException(String aErrorCode, String message, Exception e) {
        super(message, e);
        errorCode = aErrorCode;
    }

    public InvalidAccessException(String aErrorContext, String aErrorCode, String message) {
        super(message);
        errorContext = aErrorContext;
        errorCode = aErrorCode;

    }

    public InvalidAccessException(String aErrorContext, String aErrorCode, String message, Exception e) {
        super(message, e);
        errorContext = aErrorContext;
        errorCode = aErrorCode;

    }

    public String getErrorContext() {
        return errorContext;
    }

    public void setErrorContext(String errorContext) {
        this.errorContext = errorContext;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
