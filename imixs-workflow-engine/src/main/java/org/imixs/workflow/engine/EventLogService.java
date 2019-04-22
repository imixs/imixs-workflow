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

	public static final String EVENTLOG_TYPE = "event";

	@PersistenceContext(unitName = "org.imixs.workflow.jpa")
	private EntityManager manager;

	private static Logger logger = Logger.getLogger(EventLogService.class.getName());

	/**
	 * This method creates/updates a new event log entry. The identifier of an
	 * eventLogEnty is sufixed with the event topic. The type for all event entities
	 * is 'event'.
	 * <p>
	 * If an event for the same uniqueId and the same topic already exists, than the
	 * existing event will be updated.
	 * 
	 * @param uniqueID
	 *            - uniqueid of the document to be assigned to the event
	 * @param topic
	 *            - the topic of the event.
	 */
	public void createEvent(String uniqueID, String topic) {
		org.imixs.workflow.engine.jpa.Document eventLogEntry = null;
		if (uniqueID == null || uniqueID.isEmpty()) {
			logger.warning("WriteEventLog failed - given id is empty!");
			return;
		}

		// Now set flush Mode to COMMIT
		manager.setFlushMode(FlushModeType.COMMIT);
		// now create a new event log entry
		uniqueID = topic + "_" + uniqueID;

		// test if the event alredy exists
		eventLogEntry = manager.find(Document.class, uniqueID);

		if (eventLogEntry == null) {
			eventLogEntry = new org.imixs.workflow.engine.jpa.Document(uniqueID);
			eventLogEntry.setType(EVENTLOG_TYPE);
			manager.persist(eventLogEntry);
		} else {
			// update modified
			Calendar cal = Calendar.getInstance();
			eventLogEntry.setModified(cal);
			// there is no need to merge the persistedDocument because it is
			// already managed by JPA!
		}
	
		logger.finest("......create new eventLogEntry '" + uniqueID + "' => " + topic);

	}

	/**
	 * Finds events for one or many given topics
	 * 
	 * @param maxCount
	 *            - maximum count of events to be returned
	 * @param topic
	 *            - list of topics
	 * @return - list of events
	 */
	public List<Document> findEvents(int maxCount, String... topic) {
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
		List<org.imixs.workflow.engine.jpa.Document> documentList = q.getResultList();
		return documentList;
	}
}
