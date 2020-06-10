/*  
 *  Imixs-Workflow 
 *  
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

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
