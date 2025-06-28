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
 * The QueryException is thrown in case a search query can not be pased.
 * 
 * @see org.imixs.workflow.engine.lucene.LuceneSearchService
 * @author rsoika
 * 
 */
public class QueryException extends WorkflowException {

    public static final String QUERY_NOT_UNDERSTANDABLE = "QUERY_NOT_UNDERSTANDABLE";

    private static final long serialVersionUID = 1L;

    public QueryException(String aErrorCode, String message) {
        super(aErrorCode, message);
    }

    public QueryException(String aErrorCode, String message, Exception e) {
        super(aErrorCode, message, e);
    }

}
