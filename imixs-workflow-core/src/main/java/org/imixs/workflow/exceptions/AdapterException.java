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
 * An AdapterException is thrown by an Imixs-Workflow Adapter implementation
 * 
 * @author rsoika
 * 
 */
public class AdapterException extends WorkflowException {

    private static final long serialVersionUID = 1L;
    private Object[] params = null;

    public AdapterException(String aErrorContext, String aErrorCode, String message) {
        super(aErrorContext, aErrorCode, message);
    }

    public AdapterException(String aErrorContext, String aErrorCode, String message, Exception e) {
        super(aErrorContext, aErrorCode, message, e);
    }

    public AdapterException(String aErrorContext, String aErrorCode, String message, Object[] params) {
        super(aErrorContext, aErrorCode, message);
        this.params = params;
    }
    
    public AdapterException(PluginException e) {
        super(e.getErrorContext(), e.getErrorCode(), e.getMessage(), e);
    }

    public Object[] getErrorParameters() {
        return params;
    }

    protected void setErrorParameters(Object[] aparams) {
        this.params = aparams;
    }

}
