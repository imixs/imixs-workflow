package org.imixs.workflow.engine.cluster.events;

import org.imixs.workflow.ItemCollection;

/**
 * The ArchiveEvent is fired by the DataService EJB each time a snapshot is
 * stored in the cassandra cluster. An event Observer can react on a save or
 * load event.
 * 
 * 
 * The ArchiveEvent defines the following event types:
 * <ul>
 * <li>ON_ARCHIVE - send immediately before a document will be saved
 * <li>ON_RESTORE - send immediately after a document was restored
 * <li>ON_DELETE - send immediately before a document will be deleted
 * </ul>
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.engine.WorkflowService
 */
public class ArchiveEvent {

	public static final int ON_ARCHIVE = 1;
	public static final int ON_RESTORE = 2;
	public static final int ON_DELETE = 3;

	private int eventType;
	private ItemCollection document;

	public ArchiveEvent(ItemCollection document, int eventType) {
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
