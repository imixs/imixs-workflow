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

package org.imixs.workflow.engine.index;

import org.imixs.workflow.ItemCollection;

/**
 * The IndexEvent provides a CDI event fired immediately before a document is
 * indexed by the search service implementation.
 * <p>
 * An observer CDI bean can change or extend the text content to be indexed. The
 * IndexEvent defines only one event type:
 * <ul>
 * <li>ON_INDEX_UPDATE - is send immediately before a document will be indexed
 * </ul>
 * The property 'textContent' can be updated or extended by a client.
 * 
 * @author Ralph Soika
 * @version 1.0
 */
public class IndexEvent {

    public static final int ON_INDEX_UPDATE = 1;

    private int eventType;
    private String textContent;
    private ItemCollection document;

    public IndexEvent(int eventType, ItemCollection document) {
        this.eventType = eventType;
        this.document = document;
    }

    /**
     * Returns the textContent for the given document to be indexed.
     * 
     * @return
     */
    public String getTextContent() {
        return textContent;
    }

    /**
     * Update the textContent for the given document to be indexed.
     * 
     * @return
     */
    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public int getEventType() {
        return eventType;
    }

    /**
     * Returns the document to be indexed
     * 
     * @return
     */
    public ItemCollection getDocument() {
        return document;
    }

}
