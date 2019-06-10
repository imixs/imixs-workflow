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
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.imixs.workflow.engine.jpa.Document;

/**
 * The EventLogService is a service to log events into the database to be
 * processed in an asynchronous way.
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
 * @see org.imixs.workflow.engine.lucene.LuceneUpdateService
 * @author rsoika
 * @version 1.0
 * 
 */

@Stateless
public class EventLogService {

	public static final String EVENTLOG_TYPE = "eventlog";

	@PersistenceContext(unitName = "org.imixs.workflow.jpa")
	private EntityManager manager;

	private static Logger logger = Logger.getLogger(EventLogService.class.getName());

	/**
	 * Creates/updates a new event log entry. The identifier of an eventLogEnty is
	 * sufixed with the event topic. The type for all event entities is 'event'.
	 * <p>
	 * If an event for the same uniqueId and the same topic already exists, than the
	 * existing event will be updated.
	 * 
	 * @param uniqueID
	 *            - uniqueid of the document to be assigned to the event
	 * @param topic
	 *            - the topic of the event.
	 * @return - generated event log entry
	 */
	public EventLogEntry createEvent(String uniqueID, String topic) {
		org.imixs.workflow.engine.jpa.Document eventLogDocument = null;
		if (uniqueID == null || uniqueID.isEmpty()) {
			logger.warning("WriteEventLog failed - given id is empty!");
			return null;
		}

		// Now set flush Mode to COMMIT
		manager.setFlushMode(FlushModeType.COMMIT);
		// now create a new event log entry
		EventLogEntry eventLogEntry = new EventLogEntry(uniqueID, topic);

		// test if the event alredy exists
		eventLogDocument = manager.find(Document.class, eventLogEntry.getID());

		if (eventLogDocument == null) {
			eventLogDocument = new org.imixs.workflow.engine.jpa.Document(eventLogEntry.getID());
			eventLogDocument.setType(EVENTLOG_TYPE);
			manager.persist(eventLogDocument);
		} else {
			// update modified
			Calendar cal = Calendar.getInstance();
			eventLogDocument.setModified(cal);
			// there is no need to merge the persistedDocument because it is
			// already managed by JPA!
		}

		logger.finest("......create new eventLogEntry '" + uniqueID + "' => " + topic);

		// update the modified date
		eventLogEntry.setModified(eventLogDocument.getModified());

		return eventLogEntry;
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
	public List<EventLogEntry> findEvents(int maxCount, String... topic) {
		List<EventLogEntry> result = new ArrayList<>();
		String query = "SELECT document FROM Document AS document ";
		query += "WHERE document.type = '" + EventLogService.EVENTLOG_TYPE + "' ";
		query += "AND (";
		for (String _topic : topic) {
			query += "document.id LIKE '" + _topic + "%' OR ";
		}
		// cut last OR
		query = query.substring(0, query.length() - 3);
		query += ") ORDER BY document.modified ASC";

		// find all eventLogEntries....
		Query q = manager.createQuery(query);
		// we try to search one more log entry as requested to see if the cache is
		// empty...
		q.setMaxResults(maxCount);

		@SuppressWarnings("unchecked")
		List<Document> documentList = q.getResultList();

		// now create a list of EventLog entries....
		if (documentList != null && documentList.size() > 0) {
			for (Document doc : documentList) {
				String id = doc.getId();
				// extract topic and id
				String _uniqueID = id.substring(id.lastIndexOf('_') + 1);
				String _topic = id.substring(0, id.lastIndexOf('_'));
				EventLogEntry eventLogEntry = new EventLogEntry(_uniqueID, _topic);
				eventLogEntry.setModified(doc.getModified());
				result.add(eventLogEntry);
			}
		}

		return result;
	}

	/**
	 * Deletes an existing eventLogEntry. The method cathces
	 * javax.persistence.OptimisticLockException as this may occure during parallel
	 * requests.
	 * 
	 * @param eventLogID
	 */
	public void removeEvent(EventLogEntry eventLogEntry) {
		Document eventLogEntryDocument = manager.find(Document.class, eventLogEntry.getID());
		if (eventLogEntryDocument != null) {
			try {
				manager.remove(eventLogEntryDocument);
			} catch (javax.persistence.OptimisticLockException e) {
				// no todo - can occure during parallel requests
				logger.finest(e.getMessage());
			}
		}
	}
}
