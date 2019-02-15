package org.imixs.workflow.engine;

import java.util.List;

/**
 * The UserGroupEvent provides a CDI observer pattern. The UserGroupEvent is
 * fired by the DocumentService EJB. An event Observer can react on this event
 * to extend the current user group list.
 * 
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.engine.DocumentService
 */
public class UserGroupEvent {

	private String userId;
	private List<String> groups;

	public UserGroupEvent(String userId) {
		super();
		this.userId = userId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public void setGroups(List<String> groups) {
		this.groups = groups;
	}

	public List<String> getGroups() {
		return groups;
	}

}
