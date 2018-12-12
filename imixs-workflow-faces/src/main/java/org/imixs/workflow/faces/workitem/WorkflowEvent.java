package org.imixs.workflow.faces.workitem;

import org.imixs.workflow.ItemCollection;

/**
 * The WorkflowEvent provides a CDI event fired by the DocumentController and
 * WorkflowController. This event can be used in a observer pattern of a CDI
 * bean to react on UI events in a jsf page.
 * <p>
 * The WorkflowEvent defines the following event types fired by the
 * WorkflowController:
 * <ul>
 * <li>WORKITEM_BEFORE_PROCESS - is send immediately before a workitem will be
 * processed
 * <li>WORKITEM_AFTER_PROCESS - is send immediately after a workitem was
 * processed
 * <li>WORKITEM_CHANGED - is send immediately after a workitem was loaded
 * </ul>
 * <p>
 * The following event types are fired by the DocumentController:
 * <ul>
 * <li>DOCUMENT_BEFORE_SAVE - is send immediately before the document will be
 * saved
 * <li>DOCUMENT_AFTER_SAVE - is send immediately after the document was saved
 * <li>DOCUMENT_CHANGED - is send immediately after the document was loaded
 * <li>DOCUMENT_BEFORE_DELETE - is send immediately before the document will be
 * deleted
 * <li>DOCUMENT_AFTER_DELETE - is send immediately after the document was
 * deleted
 * </ul>
 * 
 */
public class WorkflowEvent {

	public static final int DOCUMENT_CREATED = 1;
	public static final int DOCUMENT_INITIALIZED = 2;
	public static final int DOCUMENT_CHANGED = 3;
	public static final int DOCUMENT_BEFORE_SAVE = 4;
	public static final int DOCUMENT_AFTER_SAVE = 5;
	public static final int DOCUMENT_BEFORE_DELETE = 6;
	public static final int DOCUMENT_AFTER_DELETE = 7;

	public static final int WORKITEM_CREATED = 20;
	public static final int WORKITEM_CHANGED = 21;
	public static final int WORKITEM_BEFORE_PROCESS = 22;
	public static final int WORKITEM_AFTER_PROCESS = 23;

	private int eventType;
	private ItemCollection workitem;

	public WorkflowEvent(ItemCollection workitem, int eventType) {
		this.eventType = eventType;
		this.workitem = workitem;
	}

	public int getEventType() {
		return eventType;
	}

	public ItemCollection getWorkitem() {
		return workitem;
	}

}
