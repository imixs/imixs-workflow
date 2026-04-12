/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
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

package org.imixs.workflow.engine.cluster.exceptions;

/**
 * The ClusterException is the general exception class used by the
 * ClusterService to indicate infrastructure exceptions.
 * 
 * @author rsoika
 * @version 1.0
 */
public class ClusterException extends Exception {

    private static final long serialVersionUID = 1L;

    protected String errorContext = "UNDEFINED";
    protected String errorCode = "UNDEFINED";

    public static final String INVALID_DOCUMENT_OBJECT = "INVALID_DOCUMENT_OBJECT";
    public static final String INVALID_KEYSPACE = "INVALID_KEYSPACE";
    public static final String CLUSTER_ERROR = "CLUSTER_ERROR";
    public static final String INVALID_WORKITEM = "INVALID_WORKITEM";
    public static final String MD5_ERROR = "MD5_ERROR";

    public static final String MISSING_CONTACTPOINT = "MISSING_CONTACTPOINT";
    public static final String SYNC_ERROR = "SYNC_ERROR";

    public ClusterException(String aErrorCode, String message) {
        super(message);
        errorCode = aErrorCode;

    }

    public ClusterException(String aErrorContext, String aErrorCode, String message) {
        super(message);
        errorContext = aErrorContext;
        errorCode = aErrorCode;

    }

    public ClusterException(String aErrorContext, String aErrorCode, String message, Exception e) {
        super(message, e);
        errorContext = aErrorContext;
        errorCode = aErrorCode;

    }

    public ClusterException(String aErrorCode, String message, Exception e) {
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

}
