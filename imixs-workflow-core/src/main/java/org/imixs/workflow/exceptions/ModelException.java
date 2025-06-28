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
 * An ModelException should be thrown by a service component if a model entity
 * is invalid or does not exist
 * 
 * @author rsoika
 * 
 */
public class ModelException extends WorkflowException {

    public static final String INVALID_MODEL = "INVALID_MODEL";
    public static final String INVALID_MODEL_ENTRY = "INVALID_MODEL_ENTRY";
    public static final String UNDEFINED_MODEL_ENTRY = "UNDEFINED_MODEL_ENTRY";
    public static final String UNDEFINED_MODEL_VERSION = "UNDEFINED_MODEL_VERSION";
    public static final String AMBIGUOUS_MODEL_ENTRY = "AMBIGUOUS_MODEL_ENTRY";

    private static final long serialVersionUID = 1L;

    public ModelException(String aErrorCode, String message) {
        super(aErrorCode, message);
    }

    public ModelException(String aErrorCode, String message, Exception e) {
        super(aErrorCode, message, e);
    }

}
