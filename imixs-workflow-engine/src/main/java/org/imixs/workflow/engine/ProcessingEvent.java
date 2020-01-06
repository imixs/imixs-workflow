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

package org.imixs.workflow.engine;

import org.imixs.workflow.ItemCollection;

/**
 * The ProcessingEvent provides a CDI event fired by the WorkflowService EJB.
 * This even can be used in a observer pattern of a service EJB to react on the
 * life-cycle of a process instance.
 * <p>
 * The ProcessingEvent defines the following event types:
 * <ul>
 * <li>BEFORE_PROCESS - is send immediately before a workitem will be processed
 * <li>AFTER_PROCESS - is send immediately after a workitem was processed
 * </ul>
 * <p>
 * To react on changes on a workitem in the front-end see the CDI event
 * 'org.imixs.workflow.faces.workitem.WorkflowEvent'
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.engine.WorkflowService
 */
public class ProcessingEvent {

    public static final int BEFORE_PROCESS = 1;
    public static final int AFTER_PROCESS = 2;

    private int eventType;
    private ItemCollection document;

    public ProcessingEvent(ItemCollection document, int eventType) {
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
