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
 * The AsyncEventService executes BPMN boundary timer events asynchronously
 * (Async Events). The service has two responsibilities:
 * <p>
 * <strong>1. Scheduling an async event</strong>
 * <p>
 * The method {@link #onProcess(ProcessingEvent)} observes the processing life
 * cycle of all workitems. After a workitem was processed, the service verifies
 * if the new task of the workitem defines a boundary timer event. If so, a new
 * EventLog entry with the topic {@code async.event} is created. The entry
 * contains:
 * <ul>
 * <li>the target event of the boundary event,
 * <li>the timer duration of the boundary event,
 * <li>the current {@code $transactionID} of the workitem.
 * </ul>
 * The timeout of the EventLog entry is set to the current time plus the timer
 * duration.
 * <p>
 * <strong>2. Executing an async event</strong>
 * <p>
 * The method {@link #processEventLog()} is called periodically by the
 * {@link AsyncEventScheduler}, running as a system user. It looks up all
 * EventLog entries with the topic {@code async.event} that have reached their
 * timeout and processes the referred workitem with the target event in a new
 * transaction.
 * <p>
 * An async event is only executed if the {@code $transactionID} of the workitem
 * still matches the {@code $transactionID} stored in the EventLog entry. If the
 * workitem was processed in the meantime, the async event is outdated and the
 * EventLog entry is discarded. This ensures that an async event is never
 * executed twice or outside of the task it was defined for.
 * <p>
 * To prevent concurrent processes from handling the same EventLog entry, each
 * entry is locked before processing (optimistic locking). A lock expires after
 * a deadlock timeout (default 1 minute, see
 * {@code ASYNCEVENT_PROCESSOR_DEADLOCK}), so a lock of a crashed process is
 * released automatically by the {@link AsyncEventScheduler}.
 * <p>
 * The service is only active if {@code ASYNCEVENT_PROCESSOR_ENABLED} is set to
 * true (default = false).
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
     * Schedules an async event. After a workitem was processed, the method verifies
     * if the new task of the workitem defines a boundary timer event. If so, a new
     * EventLog entry with the topic {@code async.event} is created. The timeout of
     * the entry is set to the current time plus the timer duration of the boundary
     * event.
     * <p>
     * The AFTER_PROCESS event is fired before the workitem is saved, so the
     * EventLog entry is created in the same transaction as the processing of the
     * workitem.
     *
     * @param processingEvent - the CDI processing event
     * @throws ModelException if the model of the workitem can not be resolved
     */
    public void onProcess(@Observes ProcessingEvent processingEvent) throws ModelException {
        if (!enabled) {
            // no op
            return;
        }
        boolean debug = logger.isLoggable(Level.FINE);
        if (ProcessingEvent.AFTER_PROCESS == processingEvent.getEventType()) {
            // load the new task of the workitem
            ModelManager modelManager = new ModelManager(workflowService);
            BPMNModel model = modelManager.getModelByWorkitem(processingEvent.getDocument());
            ItemCollection task = modelManager.loadTask(processingEvent.getDocument(), model);
            if (task != null) {
                int boundaryTarget = task.getItemValueInteger("boundaryEvent.targetEvent");
                int boundaryDuration = task.getItemValueInteger("boundaryEvent.timerEventDefinition.timeDuration");
                // does the task define a boundary timer event?
                if (boundaryTarget > 0) {
                    if (debug) {
                        logger.log(Level.FINE, "├── create new async event - eventId={0}", boundaryTarget);
                    }
                    // the event is due after the timer duration
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.MILLISECOND, boundaryDuration);

                    // store target event, duration and the current $transactionID
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
     * Executes all due async events. The method looks up the EventLog entries with
     * the topic {@code async.event} that have reached their timeout (max 100
     * entries per call). For each entry:
     * <ol>
     * <li>The entry is locked. If the lock fails, another process is handling the
     * entry and it is skipped.
     * <li>The referred workitem is loaded.
     * <li>If the {@code $transactionID} of the workitem matches the
     * {@code $transactionID} of the entry, the workitem is processed with the
     * target event in a new transaction. Otherwise the async event is outdated and
     * only a log message is written.
     * <li>The entry is removed - also if the processing failed.
     * </ol>
     * This method is called periodically by the {@link AsyncEventScheduler}.
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void processEventLog() {
        long l = System.currentTimeMillis();
        boolean debug = logger.isLoggable(Level.FINE);

        // find all due async events...
        List<EventLog> events = eventLogService.findEventsByTimeout(100,
                EVENTLOG_TOPIC_ASYNC_EVENT);

        if (debug) {
            logger.log(Level.FINE, "│   ├── found {0} eventLog entries", events.size());
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
                            // We only process the workitem if its current $transactionID matches the
                            // $transactionID of the eventLog entry. Otherwise the workitem was processed
                            // in the meantime and the async event is outdated.
                            if (workitem.getItemValueString(WorkflowKernel.TRANSACTIONID)
                                    .equals(syncEventData.getItemValueString(WorkflowKernel.TRANSACTIONID))) {
                                // set the event id....
                                workitem.setEventID(syncEventData.getEventID());
                                workitem = workflowService.processWorkItemByNewTransaction(workitem);
                            } else {
                                // just a normal log message
                                logger.log(Level.INFO,
                                        "├── AsyncEvent {0} for {1} is deprecated and will be removed. ({2} \u2260 {3}",
                                        new Object[] { syncEventData.getEventID(), workitem.getUniqueID(),
                                                workitem.getItemValueString(WorkflowKernel.TRANSACTIONID),
                                                syncEventData.getItemValueString(WorkflowKernel.TRANSACTIONID) });
                            }
                            // finally remove the event log entry...
                            eventLogService.removeEvent(eventLogEntry.getId());
                        } catch (WorkflowException | InvalidAccessException | EJBException e) {
                            // we also catch EJBExceptions here because we do not want to cancel the
                            // timer of the AsyncEventScheduler
                            logger.log(Level.SEVERE, "├── AsyncEvent {0} processing failed: {1}",
                                    new Object[] { workitem.getUniqueID(), e.getMessage() });
                            // the eventLog entry is removed, so the async event will not be retried
                            logger.log(Level.WARNING, "AsyncEvent {0} will be removed!", workitem.getUniqueID());
                            eventLogService.removeEvent(eventLogEntry.getId());
                        }
                    } else {
                        // the workitem no longer exists - the async event is obsolete
                        logger.log(Level.WARNING,
                                "├── AsyncEvent for {0} - workitem not found, eventLog entry will be removed",
                                eventLogEntry.getRef());
                        eventLogService.removeEvent(eventLogEntry.getId());
                    }
                }

            } catch (OptimisticLockException e) {
                // lock was not possible - continue....
                logger.log(Level.INFO, "├── unable to lock AsyncEvent: {0}", e.getMessage());
            }

        }

        if (debug) {
            logger.log(Level.FINE, "└── {0} AsyncEvents processed in {1}ms",
                    new Object[] { events.size(), System.currentTimeMillis() - l });
        }
    }

}