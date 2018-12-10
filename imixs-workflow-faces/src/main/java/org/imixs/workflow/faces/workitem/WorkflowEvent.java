package org.imixs.workflow.faces.workitem;

import org.imixs.workflow.ItemCollection;

/**
 * The WorkflowEvent provides a CDI event fired by the WorkflowController.
 * This even can be used in a observer pattern of a CDI bean to react on UI events in a jsf page.
 * <p>
 * The WorkflowEvent defines the following event types:
 * <ul>
 * <li>WORKITEM_BEFORE_PROCESS - is send immediately before a workitem will be processed
 * <li>WORKITEM_AFTER_PROCESS - is send immediately after a workitem was processed
 * <li>WORKITEM_CHANGED - is send immediately after a workitem was loaded
 * </ul>
 * 
 * 
 * THe WorkflowEvent is used for CDI events fired by teh WorkflowController. 
 */
public class WorkflowEvent {

	public static final int WORKITEM_CHANGED = 3;
	public static final int WORKITEM_BEFORE_PROCESS = 4;
	public static final int WORKITEM_AFTER_PROCESS = 5;

 
	
	
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
