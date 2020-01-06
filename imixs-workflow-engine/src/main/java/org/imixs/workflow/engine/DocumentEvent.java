/*******************************************************************************
 * <pre>
 *  Imixs Workflow 
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
 *      Imixs Software Solutions GmbH - initial API and implementation
 *      Ralph Soika - Software Developer
 * </pre>
 *******************************************************************************/

package org.imixs.workflow.engine;

import org.imixs.workflow.ItemCollection;

/**
 * The DocumentEvent provides a CDI observer pattern. The DocumentEvent is fired by the
 * DocumentService EJB. An event Observer can react on a save or load event.
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
