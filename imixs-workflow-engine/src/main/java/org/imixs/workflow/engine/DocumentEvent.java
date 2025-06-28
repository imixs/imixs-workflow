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

import org.imixs.workflow.ItemCollection;

/**
 * The DocumentEvent provides a CDI observer pattern. The DocumentEvent is fired
 * by the DocumentService EJB. An event Observer can react on a save or load
 * event.
 * 
 * 
 * The DocumentEvent defines the following event types:
 * <ul>
 * <li>ON_DOCUMENT_SAVE - send immediately before a document will be saved
 * <li>ON_DOCUMENT_LOAD - send immediately after a document was loaded
 * <li>ON_DOCUMENT_DELETE - send immediately before a document will be deleted
 * </ul>
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.engine.WorkflowService
 */
public class DocumentEvent {

    public static final int ON_DOCUMENT_SAVE = 1;
    public static final int ON_DOCUMENT_LOAD = 2;
    public static final int ON_DOCUMENT_DELETE = 3;

    private int eventType;
    private ItemCollection document;

    public DocumentEvent(ItemCollection document, int eventType) {
        this.eventType = eventType;
        this.document = document;
    }

    public int getEventType() {
        return eventType;
    }

    public ItemCollection getDocument() {
        return document;
    }

}
