/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow.engine.cluster;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.EventLogService;
import org.imixs.workflow.engine.cluster.exceptions.ClusterException;
import org.imixs.workflow.engine.cluster.exceptions.DataException;
import org.imixs.workflow.engine.jpa.EventLog;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;

/**
 * The ClusterOperator is responsible to process Imixs-Cluster events
 * cluster.persist and cluster.persist.
 * 
 * The ClusterOperator is called only by the
 * clusterService.
 * <p>
 * The IndexOperator reacts on events from the following types:
 * <ul>
 * <li>cluster.persist - index a new workitem</li>
 * <li>cluster.remove - delete an entry</li>
 * </ul>
 * <p>
 * To prevent concurrent processes to handle the same workitems the batch
 * process uses a Optimistic lock strategy. After fetching new event log entries
 * the processor updates the eventLog entry in a new transaction and set the
 * topic to 'batch.process.lock'. After that update we can be sure that no other
 * process is dealing with these entries. After completing the processing step
 * the eventlog entry will be removed.
 * <p>
 * To avoid a deadlock the processor set an expiration time on the lock, so the
 * lock will be auto-released after 1 minute (batch.processor.deadlock).
 * 
 * 
 * 
 * @see ClusterService
 * @version 1.0
 * @author rsoika
 *
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Stateless
@LocalBean
public class ClusterOperator {

    private static final Logger logger = Logger.getLogger(ClusterOperator.class.getName());

    @Inject
    EventLogService eventLogService;

    @Inject
    private DataService dataService;

    /**
     * The method lookups for cluster event log entries and updates the cluster
     * index. The method reacts on the following event type:
     * <ul>
     * <li>'cluster.persist'</li>
     * <li>'cluster.remove'</li>
     * </ul>
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void processEventLog() {
        long l = System.currentTimeMillis();

        // test for new event log entries by timeout...
        List<EventLog> events = eventLogService.findEventsByTimeout(100,
                ClusterService.EVENTLOG_TOPIC_PERSIST,
                ClusterService.EVENTLOG_TOPIC_REMOVE);

        if (events.size() > 0) {
            logger.log(Level.INFO, "├── 🔃 processing {0} cluster events....", events.size());

            for (EventLog eventLogEntry : events) {
                try {

                    logger.log(Level.INFO,
                            "│   ├── Event: " + eventLogEntry.getTopic() + " - " + eventLogEntry.getRef());
                    // first try to lock the eventLog entry....
                    if (eventLogService.lock(eventLogEntry)) {

                        // Delete Event?
                        if (eventLogEntry.getTopic().equals(ClusterService.EVENTLOG_TOPIC_REMOVE + ".lock")) {
                            logger.info(" do remove not yet implemented");
                            // remove the event log entry...
                            eventLogService.removeEvent(eventLogEntry.getId());
                            continue;
                        }

                        if (eventLogEntry.getTopic().equals(ClusterService.EVENTLOG_TOPIC_PERSIST + ".lock")) {
                            // Index / Update event?
                            Map<String, List<Object>> workitemData = eventLogEntry.getData();
                            if (workitemData == null) {
                                logger.log(Level.WARNING,
                                        "│   ├── ⚠️ unable to persist workitem: " + eventLogEntry.getRef());
                            } else {
                                try {
                                    dataService.saveSnapshot(new ItemCollection(workitemData));

                                } catch (DataException | ClusterException e) {
                                    logger.log(Level.WARNING,
                                            "│   ├── ⚠️ failed to save snapshot : " + eventLogEntry.getRef()
                                                    + " Error: " + e.getMessage());
                                }
                            }
                            eventLogService.removeEvent(eventLogEntry.getId());
                        }
                    }

                } catch (OptimisticLockException e) {
                    // lock was not possible - continue....
                    logger.log(Level.INFO, "│   ├── ⚠️ unable to lock ClusterEvent: {0}", e.getMessage());
                }
            }

            logger.log(Level.INFO, "├── ✅ {0} ClusterEvents processed in {1}ms",
                    new Object[] { events.size(), System.currentTimeMillis() - l });
        }
    }

}
