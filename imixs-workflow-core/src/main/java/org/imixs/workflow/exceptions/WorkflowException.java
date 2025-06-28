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
 * WorkflowException is the abstract super class for all Imixs Workflow
 * Exception classes. A WorkflowException signals an error in the business
 * logic. WorkflowExceptions need to be caught.
 * 
 * @author rsoika
 */
public abstract class WorkflowException extends Exception {

    private static final long serialVersionUID = 1L;

    protected String errorContext = "UNDEFINED";
    protected String errorCode = "UNDEFINED";
    protected java.lang.Object[] params = null;

    public WorkflowException(String aErrorCode, String message) {
        super(message);
        errorCode = aErrorCode;
    }

    public WorkflowException(String aErrorContext, String aErrorCode, String message) {
        super(message);
        errorContext = aErrorContext;
        errorCode = aErrorCode;
    }

    public WorkflowException(String aErrorContext, String aErrorCode, String message, Exception e) {
        super(message, e);
        errorContext = aErrorContext;
        errorCode = aErrorCode;
    }

    public WorkflowException(String aErrorCode, String message, Exception e) {
        super(message, e);
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

    public Object[] getErrorParameters() {
        return params;
    }

    protected void setErrorParameters(java.lang.Object[] aparams) {
        this.params = aparams;
    }

    public String formatErrorMessageWithParameters(String message) {
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                message = message.replace("{" + i + "}", params[i].toString());
            }
        }
        return message;
    }
}
