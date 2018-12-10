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
