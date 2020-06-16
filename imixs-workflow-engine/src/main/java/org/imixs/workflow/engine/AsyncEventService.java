/*  
 *  Imixs-Workflow 
 *  
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
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
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

package org.imixs.workflow.engine;

import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.OptimisticLockException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.jpa.EventLog;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.WorkflowException;

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
@LocalBean
public class AsyncEventService {

    // enabled
    @Inject
    @ConfigProperty(name = AsyncEventScheduler.ASYNCEVENT_PROCESSOR_ENABLED, defaultValue = "false")
    boolean enabled;

    private static Logger logger = Logger.getLogger(AsyncEventService.class.getName());

    @Inject
    EventLogService eventLogService;

    @Inject
    private WorkflowService workflowService;

    @Inject
    private ModelService modelService;

    /**
     * The observer method verifies if the current task contains a AsyncEvent
     * definition.
     * 
     * @throws ModelException
     * 
     */
    public void onProcess(@Observes ProcessingEvent processingEvent) throws ModelException {

        if (!enabled) {
            // no op
            return;
        }
        boolean debug = logger.isLoggable(Level.FINE);
        if (ProcessingEvent.AFTER_PROCESS == processingEvent.getEventType()) {

            // load target task
            int taskID = processingEvent.getDocument().getTaskID();
            Model model = modelService.getModelByWorkitem(processingEvent.getDocument());
            ItemCollection task = model.getTask(taskID);
            if (task != null) {
                int boundaryTarget = task.getItemValueInteger("boundaryEvent.targetEvent");
                int boundaryDuration = task.getItemValueInteger("boundaryEvent.timerEventDefinition.timeDuration");
                // create new eventLog ?
                if (boundaryTarget > 0) {
                    if (debug) {
                        logger.finest("......create new async event - eventId=" + boundaryTarget);
                    }
                    // compute timeout
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.MILLISECOND, boundaryDuration);

                    // create EventLogEntry....
                    ItemCollection asyncEventData = new ItemCollection().event(boundaryTarget);
                    asyncEventData.setItemValue("timeDuration", boundaryDuration);
                    asyncEventData.setItemValue(WorkflowKernel.TRANSACTIONID,
                            processingEvent.getDocument().getItemValueString(WorkflowKernel.TRANSACTIONID));
                    eventLogService.createEvent(AsyncEventScheduler.EVENTLOG_TOPIC_ASYNC_EVENT,
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
                AsyncEventScheduler.EVENTLOG_TOPIC_ASYNC_EVENT);

        if (debug) {
            logger.finest("......found " + events.size() + " eventLog entries");
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
                                logger.info("...AsyncEvent " + syncEventData.getEventID() + " for "
                                        + workitem.getUniqueID() + " is deprecated and will be removed. ("
                                        + workitem.getItemValueString(WorkflowKernel.TRANSACTIONID) + " ≠ "
                                        + syncEventData.getItemValueString(WorkflowKernel.TRANSACTIONID));
                            }
                            // finally remove the event log entry...
                            eventLogService.removeEvent(eventLogEntry.getId());
                        } catch (WorkflowException | InvalidAccessException | EJBException e) {
                            // we also catch EJBExceptions here because we do not want to cancel the
                            // ManagedScheduledExecutorService
                            logger.severe(
                                    "AsyncEvent " + workitem.getUniqueID() + " processing failed: " + e.getMessage());
                            // now we need to remove the batch event
                            logger.warning("AsyncEvent " + workitem.getUniqueID() + " will be removed!");
                            eventLogService.removeEvent(eventLogEntry.getId());
                        }
                    }
                }

            } catch (OptimisticLockException e) {
                // lock was not possible - continue....
                logger.info("...unable to lock AsyncEvent: " + e.getMessage());
            }

        }

        if (debug) {
            logger.fine("..." + events.size() + " AsyncEvents processed in " + (System.currentTimeMillis() - l) + "ms");
        }
    }

}
