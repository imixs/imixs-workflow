package org.imixs.workflow.engine;

import java.util.Calendar;

/**
 * This class represents an EventLogEntry consisting of a documentID and a
 * Topic.
 * <p>
 * Note: for the same documentID ($uniqueid) there can exist different eventlog
 * entries. Eventlog entries are unique over ther documentID and topic only! To
 * get the unique identifire of an eventLogEntry use the method getID()
 * 
 * @author rsoika
 * @version 1.0
 */
public class EventLogEntry implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	private String uniqueID;
	private String topic;
	private Calendar modified;

	public EventLogEntry(String uniqueID, String topic) {
		super();
		this.setUniqueID(uniqueID);
		this.topic = topic;

	}

	public String getUniqueID() {
		return uniqueID;
	}

	public void setUniqueID(String uniqueID) {
		this.uniqueID = uniqueID;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public Calendar getModified() {
		return modified;
	}

	public void setModified(Calendar modified) {
		this.modified = modified;
	}

	public String getID() {
		return topic + "_" + uniqueID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((topic == null) ? 0 : topic.hashCode());
		result = prime * result + ((uniqueID == null) ? 0 : uniqueID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EventLogEntry other = (EventLogEntry) obj;
		
		if (topic == null) {
			if (other.topic != null)
				return false;
		} else if (!topic.equals(other.topic))
			return false;
		if (uniqueID == null) {
			if (other.uniqueID != null)
				return false;
		} else if (!uniqueID.equals(other.uniqueID))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return this.getID();
	}

}
