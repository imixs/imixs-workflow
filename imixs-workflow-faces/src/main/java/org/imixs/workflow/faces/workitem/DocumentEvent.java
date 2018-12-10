package org.imixs.workflow.faces.workitem;

import org.imixs.workflow.ItemCollection;

/**
 * The DocumentEvent provides a CDI event fired by the DocumentController. This
 * even can be used in a observer pattern of a CDI bean to react on UI events in
 * a jsf page.
 * <p>
 * The DocumentEvent defines the following event types:
 * <ul>
 * <li>DOCUMENT_BEFORE_SAVE - is send immediately before the document will be
 * processed
 * <li>DOCUMENT_AFTER_SAVE - is send immediately after the document was
 * processed
 * <li>DOCUMENT_CHANGED - is send immediately after the document was loaded
 * <li>DOCUMENT_BEFORE_DELETE - is send immediately before the document will be
 * deleted
 * <li>DOCUMENT_AFTER_DELETE - is send immediately after the document was
 * deleted
 * </ul>
 * 
 * 
 * THe WorkflowEvent is used for CDI events fired by teh WorkflowController.
 */
public class DocumentEvent {

	public static final int DOCUMENT_CREATED = 1;
	public static final int DOCUMENT_INITIALIZED = 2;
	public static final int DOCUMENT_CHANGED = 3;
	public static final int DOCUMENT_BEFORE_SAVE = 14;
	public static final int DOCUMENT_AFTER_SAVE = 15;
	
	public static final int DOCUMENT_BEFORE_DELETE = 16;
	public static final int DOCUMENT_AFTER_DELETE = 17;

	private int eventType;
	private ItemCollection workitem;

	public DocumentEvent(ItemCollection workitem, int eventType) {
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
