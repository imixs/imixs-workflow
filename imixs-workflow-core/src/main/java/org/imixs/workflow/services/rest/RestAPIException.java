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

package org.imixs.workflow.services.rest;

/**
 * RestAPIException signals an error in the communication with the Imixs Rest
 * API using the Imixs RestClient. The error code is the HTTP responce code.
 * 
 * @see RestClient
 * @author rsoika
 * @version 2.0
 */
public class RestAPIException extends Exception {

    private static final long serialVersionUID = 1L;

    protected String errorContext = "UNDEFINED";
    protected int errorCode = 0;

    public RestAPIException(int aErrorCode, String message) {
        super(message);
        errorCode = aErrorCode;

    }

    public RestAPIException(String aErrorContext, int aErrorCode, String message) {
        super(message);
        errorContext = aErrorContext;
        errorCode = aErrorCode;

    }

    public RestAPIException(String aErrorContext, int aErrorCode, String message, Exception e) {
        super(message, e);
        errorContext = aErrorContext;
        errorCode = aErrorCode;

    }

    public RestAPIException(int aErrorCode, String message, Exception e) {
        super(message, e);

        errorCode = aErrorCode;

    }

    public String getErrorContext() {
        return errorContext;
    }

    public void setErrorContext(String errorContext) {
        this.errorContext = errorContext;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

}
