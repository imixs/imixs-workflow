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

package org.imixs.workflow.faces.util;

import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerFactory;

/**
 * This class is used to handle expired sessions. In case a session was expired
 * the handler caught the ViewExpiredException and redirects into a new page.
 *
 * This class expects a jsf page called 'sessionexpired.xhtml' in the web root
 * context!
 * 
 * 
 * @see ed burns ' dealing_gracefully_with_viewexpiredexception'
 * 
 *      https://www.nofluffjuststuff.com/blog/ed_burns/2009/09/
 *      dealing_gracefully_with_viewexpiredexception_in_jsf2
 * 
 * @author rsoika
 * 
 */
public class ViewExpiredExceptionHandlerFactory extends ExceptionHandlerFactory {
    private ExceptionHandlerFactory parent;

    public ViewExpiredExceptionHandlerFactory(ExceptionHandlerFactory parent) {
        this.parent = parent;
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        ExceptionHandler result = parent.getExceptionHandler();
        result = new ViewExpiredExceptionHandler(result);

        return result;
    }
}
