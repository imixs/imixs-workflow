/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
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

package org.imixs.workflow.engine;

import static org.imixs.workflow.engine.AsyncEventSchedulerConfig.ASYNCEVENT_PROCESSOR_ENABLED;
import static org.imixs.workflow.engine.AsyncEventSchedulerConfig.EVENTLOG_TOPIC_ASYNC_EVENT;

import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.jpa.EventLog;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.WorkflowException;
import org.openbpmn.bpmn.BPMNModel;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;

/**
 * The AsyncEventService can be used to process workflow events in an
 * asynchronous batch process. The AsyncEventService lookup eventLog entries of
 * the topic "async.event".
 * <p>
 * The processor look up the workItem and starts a processing life cycle.
 * <p>
 * The AsyncEventService is called only by the AsyncEventScheduler which is
 * implementing a ManagedScheduledExecutorService.
 * <p>
 * To prevent concurrent processes to handle the same workitems the batch
 * process uses a Optimistic lock strategy. After fetching new event log entries
 * the processor updates the eventLog entry in a new transaction and set the
 * topic to 'batch.process.lock'. After that update we can be sure that no other
 * process is dealing with these entries. After completing the processing step
 * the eventlog entry will be removed.
 * <p>
 * To avoid ad deadlock the processor set an expiration time on the lock, so the
 * lock will be auto-released after 1 minute (batch.processor.deadlock).
 * 
 * @see AsyncEventScheduler
 * @version 1.0
 * @author rsoika
 *
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
public class AsyncEventService {

    // enabled
    @Inject
    @ConfigProperty(name = ASYNCEVENT_PROCESSOR_ENABLED, defaultValue = "false")
    boolean enabled;

    private static final Logger logger = Logger.getLogger(AsyncEventService.class.getName());

    @Inject
    EventLogService eventLogService;

    @Inject
    private WorkflowService workflowService;

    /**
     * The observer method verifies if the current task contains a AsyncEvent
     * definition.
     * 
     * @throws ModelException
     * 
     */
    public void onProcess(@Observes ProcessingEvent processingEvent) throws PluginException, ModelException {

        if (!enabled) {
            // no op
            return;
        }
        boolean debug = logger.isLoggable(Level.FINE);
        if (ProcessingEvent.AFTER_PROCESS == processingEvent.getEventType()) {
            // load target task
            ModelManager modelManager = new ModelManager(workflowService);
            BPMNModel model = modelManager.getModelByWorkitem(processingEvent.getDocument());
            ItemCollection task = modelManager.loadTask(processingEvent.getDocument(), model);
            if (task != null) {
                int boundaryTarget = task.getItemValueInteger("boundaryEvent.targetEvent");
                int boundaryDuration = task.getItemValueInteger("boundaryEvent.timerEventDefinition.timeDuration");
                // create new eventLog ?
                if (boundaryTarget > 0) {
                    if (debug) {
                        logger.log(Level.FINEST, "......create new async event - eventId={0}", boundaryTarget);
                    }
                    // compute timeout
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.MILLISECOND, boundaryDuration);

                    // create EventLogEntry....
                    ItemCollection asyncEventData = new ItemCollection().event(boundaryTarget);
                    asyncEventData.setItemValue("timeDuration", boundaryDuration);
                    asyncEventData.setItemValue(WorkflowKernel.TRANSACTIONID,
                            processingEvent.getDocument().getItemValueString(WorkflowKernel.TRANSACTIONID));
                    eventLogService.createEvent(EVENTLOG_TOPIC_ASYNC_EVENT,
                            processingEvent.getDocument().getUniqueID(), asyncEventData, cal);

                }
            }
        }

    }

    /**
     * The method lookups for batch event log entries and processed workitems in a
     * batch process.
     * <p>
     * Each eventLogEntry is cached in the eventCache. The cache is cleared from all
     * eventLogEntries not part of the current collection. We can assume that the
     * event was succefully processed by the ArchiveHandler
     * 
     * @throws ArchiveException
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void processEventLog() {
        long l = System.currentTimeMillis();
        boolean debug = logger.isLoggable(Level.FINE);

        // test for new event log entries by timeout...
        List<EventLog> events = eventLogService.findEventsByTimeout(100,
                EVENTLOG_TOPIC_ASYNC_EVENT);

        if (debug) {
            logger.log(Level.FINEST, "......found {0} eventLog entries", events.size());
        }
        for (EventLog eventLogEntry : events) {
            try {
                // first try to lock the eventLog entry....
                if (eventLogService.lock(eventLogEntry)) {
                    // now load the workitem
                    ItemCollection workitem = workflowService.getWorkItem(eventLogEntry.getRef());
                    if (workitem != null) {
                        // process workitem....
                        try {
                            // get the data object
                            ItemCollection syncEventData = new ItemCollection(eventLogEntry.getData());
                            // verify the $transactionID
                            // we only process the workitem if the last transactionID matches the
                            // transactionID form the eventLog entry

                            if (workitem.getItemValueString(WorkflowKernel.TRANSACTIONID)
                                    .equals(syncEventData.getItemValueString(WorkflowKernel.TRANSACTIONID))) {
                                // set the event id....
                                workitem.setEventID(syncEventData.getEventID());
                                workitem = workflowService.processWorkItemByNewTransaction(workitem);
                            } else {
                                // just a normal log message
                                logger.log(Level.INFO,
                                        "...AsyncEvent {0} for {1} is deprecated and will be removed. ({2} \u2260 {3}",
                                        new Object[] { syncEventData.getEventID(), workitem.getUniqueID(),
                                                workitem.getItemValueString(WorkflowKernel.TRANSACTIONID),
                                                syncEventData.getItemValueString(WorkflowKernel.TRANSACTIONID) });
                            }
                            // finally remove the event log entry...
                            eventLogService.removeEvent(eventLogEntry.getId());
                        } catch (WorkflowException | InvalidAccessException | EJBException e) {
                            // we also catch EJBExceptions here because we do not want to cancel the
                            // ManagedScheduledExecutorService
                            logger.log(Level.SEVERE, "AsyncEvent {0} processing failed: {1}",
                                    new Object[] { workitem.getUniqueID(), e.getMessage() });
                            // now we need to remove the batch event
                            logger.log(Level.WARNING, "AsyncEvent {0} will be removed!", workitem.getUniqueID());
                            eventLogService.removeEvent(eventLogEntry.getId());
                        }
                    }
                }

            } catch (OptimisticLockException e) {
                // lock was not possible - continue....
                logger.log(Level.INFO, "...unable to lock AsyncEvent: {0}", e.getMessage());
            }

        }

        if (debug) {
            logger.log(Level.FINE, "...{0} AsyncEvents processed in {1}ms",
                    new Object[] { events.size(), System.currentTimeMillis() - l });
        }
    }

}
