package org.imixs.workflow.engine;

import org.imixs.workflow.ItemCollection;

/**
 * The ProcessingEvent provides a CDI observer pattern. The ProcessingEvent is fired
 * by the WorkflowService EJB. An event observer can react on a phased of a processing life cycle.
 * 
 * 
 * The ProcessingEvent defines the following event types:
 * <ul>
 * <li>BEFORE_PROCESS - is send immediately before a workitem will be processed 
 * <li>AFTER_PROCESS - is send immediately after a workitem was processed
 * </ul>
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
