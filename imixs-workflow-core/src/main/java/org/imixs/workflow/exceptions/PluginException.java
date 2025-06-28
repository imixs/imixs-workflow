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
 * A PluginException is thrown by an Imixs-Workflow plugin implementation.
 * 
 * @author rsoika
 * 
 */
public class PluginException extends WorkflowException {

    private static final long serialVersionUID = 1L;

    public PluginException(String aErrorContext, String aErrorCode, String message) {
        super(aErrorContext, aErrorCode, message);
    }

    public PluginException(String aErrorContext, String aErrorCode, String message, Exception e) {
        super(aErrorContext, aErrorCode, message, e);
    }

    public PluginException(AdapterException e) {
        super(e.getErrorContext(), e.getErrorCode(), e.getMessage(), e);
    }

    public PluginException(String aErrorContext, String aErrorCode, String message, java.lang.Object[] params) {
        super(aErrorContext, aErrorCode, message);
        this.params = params;
    }

}
