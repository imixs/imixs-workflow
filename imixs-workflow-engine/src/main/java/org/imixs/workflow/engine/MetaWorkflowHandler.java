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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.engine.jpa.EventLog;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.WorkflowException;
import org.openbpmn.bpmn.BPMNModel;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;

/**
 * The MetaWorkflowHandler processes the signals sent by subprocesses to their
 * meta instance (Subprocess → Meta direction).
 * <p>
 * Signals are created by the {@link MetaWorkflowPlugin} as EventLog entries
 * with the topic {@code metaworkflow.signal}. The EventLog refID is the
 * $uniqueID of the meta instance. The data of the EventLog entry contains:
 * <ul>
 * <li>{@code $metaprocess.subprocess.ref} - $uniqueID of the subprocess
 * <li>{@code $metaprocess.subprocess.group} - workflow group of the subprocess
 * <li>{@code $metaprocess.signal} - name of the signal
 * </ul>
 * <p>
 * The method {@link #processEventLog()} is called by the
 * {@link AsyncEventScheduler} running as a system user, because the user who
 * processed the subprocess may not have write access to the meta instance. For
 * each signal the handler looks for an event in the current task of the meta
 * instance defining the following Imixs extension items:
 * <ul>
 * <li>{@code metaworkflow.signal.group} - workflow group of the subprocess
 * (required)
 * <li>{@code metaworkflow.signal.name} - name of the signal (optional, an empty
 * value matches any signal)
 * </ul>
 * If a matching signal event is found, the meta instance is processed by this
 * event. Otherwise the signal is ignored and the meta instance remains
 * unchanged. Signal events should be modeled with
 * {@code workflow.public=false}, so they are not shown to the user.
 *
 * @author rsoika
 * @version 1.0
 * @see MetaWorkflowPlugin
 * @see AsyncEventScheduler
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
public class MetaWorkflowHandler {

    public static final String EVENTLOG_TOPIC_SIGNAL = "metaworkflow.signal";

    // Event items of a meta model defining a signal event
    public static final String EVENT_ITEM_SIGNAL_GROUP = "metaworkflow.signal.group";
    public static final String EVENT_ITEM_SIGNAL_NAME = "metaworkflow.signal.name";

    // EventLog data items
    public static final String ITEM_SUBPROCESS_REF = "$metaprocess.subprocess.ref";
    public static final String ITEM_SUBPROCESS_GROUP = "$metaprocess.subprocess.group";
    public static final String ITEM_SIGNAL = "$metaprocess.signal";

    private static final Logger logger = Logger.getLogger(MetaWorkflowHandler.class.getName());

    @Inject
    EventLogService eventLogService;

    @Inject
    WorkflowService workflowService;

    /**
     * Processes all open EventLog entries with the topic
     * {@code metaworkflow.signal} in the order of their creation. This method is
     * expected to be called by the {@link AsyncEventScheduler} running as a system
     * user.
     * <p>
     * Each EventLog entry is removed after processing - also if no matching signal
     * event exists or the processing failed.
     */
    public void processEventLog() {
        List<EventLog> eventLogEntries = eventLogService.findEventsByTopic(100, EVENTLOG_TOPIC_SIGNAL);

        for (EventLog eventLogEntry : eventLogEntries) {
            try {
                if (!eventLogService.lock(eventLogEntry)) {
                    continue;
                }
                ItemCollection data = new ItemCollection(eventLogEntry.getData());
                ItemCollection metaWorkitem = workflowService.getWorkItem(eventLogEntry.getRef());
                if (metaWorkitem == null) {
                    logger.log(Level.WARNING, "metaworkflow {0} not found - signal of subprocess {1} ignored",
                            new Object[] { eventLogEntry.getRef(), data.getItemValueString(ITEM_SUBPROCESS_REF) });
                } else {
                    processSignal(metaWorkitem, data);
                }
                eventLogService.removeEvent(eventLogEntry.getId());

            } catch (OptimisticLockException e) {
                // lock was not possible - continue....
                logger.log(Level.INFO, "...unable to lock metaworkflow signal: {0}", e.getMessage());
            }
        }
    }

    /**
     * Processes the meta instance by the signal event matching the given signal. If
     * no signal event matches, the meta instance remains unchanged.
     *
     * @param metaWorkitem - the meta instance
     * @param data         - EventLog data of the signal
     */
    private void processSignal(ItemCollection metaWorkitem, ItemCollection data) {
        String group = data.getItemValueString(ITEM_SUBPROCESS_GROUP);
        String signal = data.getItemValueString(ITEM_SIGNAL);
        try {
            ItemCollection signalEvent = findSignalEvent(metaWorkitem, group, signal);
            if (signalEvent == null) {
                logger.log(Level.FINE, "...no signal event for ''{0}/{1}'' in metaworkflow {2} - signal ignored",
                        new Object[] { group, signal, metaWorkitem.getUniqueID() });
                return;
            }
            metaWorkitem.event(signalEvent.getItemValueInteger(BPMNUtil.EVENT_ITEM_EVENTID));
            workflowService.processWorkItemByNewTransaction(metaWorkitem);
            logger.log(Level.INFO, "├── metaworkflow {0} triggered by subprocess {1} (''{2}/{3}'')",
                    new Object[] { metaWorkitem.getUniqueID(), data.getItemValueString(ITEM_SUBPROCESS_REF),
                            group, signal });

        } catch (WorkflowException | InvalidAccessException | EJBException e) {
            // we also catch EJBExceptions here because we do not want to cancel the timer
            logger.log(Level.SEVERE, "metaworkflow {0} processing failed: {1}",
                    new Object[] { metaWorkitem.getUniqueID(), e.getMessage() });
        }
    }

    /**
     * Returns the first event of the current task of the meta instance matching the
     * given workflow group and signal. An empty signal name matches any signal.
     *
     * @return matching event or null if no event matches
     */
    private ItemCollection findSignalEvent(ItemCollection metaWorkitem, String group, String signal)
            throws ModelException {
        ModelManager modelManager = new ModelManager(workflowService);
        BPMNModel model = modelManager.getModelByWorkitem(metaWorkitem);
        List<ItemCollection> events = modelManager.findEventsByTask(model, metaWorkitem.getTaskID());
        for (ItemCollection event : events) {
            String signalGroup = event.getItemValueString(EVENT_ITEM_SIGNAL_GROUP).trim();
            if (signalGroup.isEmpty() || !signalGroup.equals(group)) {
                continue;
            }
            String signalName = event.getItemValueString(EVENT_ITEM_SIGNAL_NAME).trim();
            if (signalName.isEmpty() || signalName.equals(signal)) {
                return event;
            }
        }
        return null;
    }

}