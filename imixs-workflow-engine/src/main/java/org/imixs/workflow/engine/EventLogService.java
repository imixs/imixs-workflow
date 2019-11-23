/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
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
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.workflow.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.jpa.EventLog;

/**
 * The EventLogService is a service to create and access log events.
 * <p>
 * An event that occurs during an update or a processing function within a
 * transaction becomes a fact when the transaction completes successfully. The
 * EventLogService can be used to create this kind of "Change Data Capture"
 * events. An example is the LuceneUpdateService, which should update the index
 * of a document only if the document was successfully written to the database.
 * <p>
 * The service is bound to the current PersistenceContext and stores a defined
 * type of document entity directly in the database to represent an event. These
 * types of events can be queried by clients through the service.
 * 
 * @see org.imixs.workflow.engine.index.UpdateService
 * @author rsoika
 * @version 1.0
 * 
 */

@Stateless
public class EventLogService {

	@PersistenceContext(unitName = "org.imixs.workflow.jpa")
	private EntityManager manager;

	private static Logger logger = Logger.getLogger(EventLogService.class.getName());

	/**
	 * Creates/updates a new event log entry.
	 * 
	 * @param refID
	 *            - uniqueid of the document to be assigned to the event
	 * @param topic
	 *            - the topic of the event.
	 * @return - generated event log entry
	 */
	public EventLog createEvent(String topic, String refID) {
		return createEvent(topic, refID, (Map<String, List<Object>>) null);
	}

	/**
	 * Creates/updates a new event log entry.
	 *
	 * @param refID
	 *            - uniqueId of the document to be assigned to the event
	 * @param topic
	 *            - the topic of the event.
	 * @param document
	 *            - optional document providing a data map
	 * @return - generated event log entry
	 */
	public EventLog createEvent(String topic, String refID, ItemCollection document) {
		return this.createEvent(topic, refID, document.getAllItems());
	}

	/**
	 * Creates/updates a new event log entry.
	 *
	 * @param refID
	 *            - uniqueId of the document to be assigned to the event
	 * @param topic
	 *            - the topic of the event.
	 * @param data
	 *            - optional data map
	 * @return - generated event log entry
	 */
	public EventLog createEvent(String topic, String refID, Map<String, List<Object>> data) {
		boolean debug = logger.isLoggable(Level.FINE);
		if (refID == null || refID.isEmpty()) {
			logger.warning("create EventLog failed - given ref-id is empty!");
			return null;
		}
		// Now set flush Mode to COMMIT
		manager.setFlushMode(FlushModeType.COMMIT);
		// now create a new event log entry
		EventLog eventLog = new EventLog(topic, refID, data);
		manager.persist(eventLog);
		if (debug) {
			logger.finest("......created new eventLog '" + refID + "' => " + topic);
		}
		return eventLog;
	}

	/**
	 * Finds events for one or many given topics
	 * 
	 * @param maxCount
	 *            - maximum count of events to be returned
	 * @param topic
	 *            - list of topics
	 * @return - list of eventLogEntries
	 */
	@SuppressWarnings("unchecked")
	public List<EventLog> findEventsByTopic(int maxCount, String... topic) {
		boolean debug = logger.isLoggable(Level.FINE);
		List<EventLog> result = new ArrayList<>();
		String query = "SELECT eventlog FROM EventLog AS eventlog ";
		query += "WHERE (";
		for (String _topic : topic) {
			if (_topic != null && !_topic.isEmpty()) {
				query += "eventlog.topic = '" + _topic + "' OR ";
			}
		}
		// cut last OR
		query = query.substring(0, query.length() - 3);
		query += ") ORDER BY eventlog.created ASC";

		// find all eventLogEntries....
		Query q = manager.createQuery(query);
		q.setMaxResults(maxCount);
		result = q.getResultList();
		if (debug) {
			logger.fine("found " + result.size() + " event for topic " + topic);
		}
		return result;

	}

	/**
	 * Finds events for one or many given topics assigned to a given document
	 * reference ($uniqueId). The method returns an empty list if no event log
	 * entries exist of the given refId,
	 * 
	 * @param maxCount
	 *            - maximum count of events to be returned
	 * @param ref
	 *            - a reference ID for an assigned Document or Workitem instance
	 * @param topic
	 *            - list of topics
	 * 
	 * @return - list of eventLogEntries
	 */
	@SuppressWarnings("unchecked")
	public List<EventLog> findEventsByRef(int maxCount, String ref, String... topic) {
		boolean debug = logger.isLoggable(Level.FINE);
		List<EventLog> result = null;
		String query = "SELECT eventlog FROM EventLog AS eventlog ";
		query += "WHERE (eventlog.ref = '" + ref + "' AND (";
		for (String _topic : topic) {
			query += "eventlog.topic = '" + _topic + "' OR ";
		}
		// cut last OR
		query = query.substring(0, query.length() - 3);
		query += ") ORDER BY eventlog.created ASC";

		// find all eventLogEntries....
		Query q = manager.createQuery(query);
		q.setMaxResults(maxCount);
		result = q.getResultList();
		if (debug) {
			logger.fine("found " + result.size() + " event for topic " + topic);
		}
		return result;

	}

	/**
	 * Deletes an existing eventLog. The method catches
	 * javax.persistence.OptimisticLockException as this may occur during parallel
	 * requests.
	 * 
	 * @param eventLog
	 */
	public void removeEvent(final EventLog _eventLog) {
		boolean debug = logger.isLoggable(Level.FINE);
		EventLog eventLog = _eventLog;
		if (eventLog != null && !manager.contains(eventLog)) {
			// entity is not atached - so lookup the entity....
			eventLog = manager.find(EventLog.class, eventLog.getId());
		}
		if (eventLog != null) {
			try {
				manager.remove(eventLog);
			} catch (javax.persistence.OptimisticLockException e) {
				// no todo - can occure during parallel requests
				if (debug) {
					logger.finest(e.getMessage());
				}
			}
		}
	}

	/**
	 * Deletes an existing eventLog by its id. The method catches
	 * javax.persistence.OptimisticLockException as this may occur during parallel
	 * requests.
	 * 
	 * @param eventLog
	 */
	public void removeEvent(final String id) {
		EventLog eventLog = null;
		boolean debug = logger.isLoggable(Level.FINE);
		// lookup the entity....
		eventLog = manager.find(EventLog.class, id);

		if (eventLog != null) {
			try {
				manager.remove(eventLog);
			} catch (javax.persistence.OptimisticLockException e) {
				// no todo - can occure during parallel requests
				if (debug) {
					logger.finest(e.getMessage());
				}
			}
		}
	}

	/**
	 * Returns an detached event log entry by its ID.
	 * 
	 * @param id
	 *            - id of the eventLog Entry
	 * @return detached eventLog entry or null if not found
	 */
	public EventLog getEvent(String id) {
		EventLog eventLog = manager.find(EventLog.class, id);
		manager.detach(eventLog);
		return eventLog;
	}
}
