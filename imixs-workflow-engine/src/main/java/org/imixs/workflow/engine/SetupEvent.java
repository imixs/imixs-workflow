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

package org.imixs.workflow.engine;

/**
 * The SetupEvent provides a CDI observer pattern. The SetupEvent is fired by
 * the SetupService EJB. An event Observer can react on this event to extend the
 * setup routine.
 * 
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.engine.SetupService
 */
public class SetupEvent {

    public SetupEvent() {
        super();

    }

}
