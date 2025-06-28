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
 * An IndexException is a runtime exception which is thrown by a Imixs Workflow
 * component if a index is not read or writable. .
 * 
 * @see org.imixs.workflow.engine.lucene.LuceneUpdateService
 * @author rsoika
 * 
 */
public class IndexException extends RuntimeException {

    public static final String INVALID_INDEX = "INVALID_INDEX";

    protected String errorCode = "UNDEFINED";
    protected String errorContext = "UNDEFINED";

    private static final long serialVersionUID = 1L;

    public IndexException(String message) {
        super(message);
    }

    public IndexException(String message, Exception e) {
        super(message, e);
    }

    public IndexException(String aErrorCode, String message) {
        super(message);
        errorCode = aErrorCode;
    }

    public IndexException(String aErrorCode, String message, Exception e) {
        super(message, e);
        errorCode = aErrorCode;
    }

    public IndexException(String aErrorContext, String aErrorCode, String message) {
        super(message);
        errorContext = aErrorContext;
        errorCode = aErrorCode;

    }

    public IndexException(String aErrorContext, String aErrorCode, String message, Exception e) {
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
