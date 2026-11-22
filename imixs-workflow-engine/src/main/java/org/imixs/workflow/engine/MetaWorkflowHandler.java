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
 * The MetaWorkflowHandler processes the messages sent by subprocesses to their
 * meta instance (Subprocess → Meta direction).
 * <p>
 * Messages are created by the {@link MetaWorkflowPlugin} as EventLog entries
 * with the topic {@code metaworkflow.message}. The EventLog refID is the
 * $uniqueID of the meta instance. The data of the EventLog entry contains:
 * <ul>
 * <li>{@code $metaprocess.subprocess.ref} - $uniqueID of the subprocess
 * <li>{@code $metaprocess.subprocess.group} - workflow group of the subprocess
 * <li>{@code $metaprocess.message} - name of the message
 * </ul>
 * <p>
 * The method {@link #processEventLog()} is called by the
 * {@link AsyncEventScheduler} running as a system user, because the user who
 * processed the subprocess may not have write access to the meta instance. For
 * each message the handler looks for a message event in the current task of the
 * meta instance:
 * <ul>
 * <li>The event defines the Imixs extension item
 * {@code metaworkflow.message.group} with the workflow group of the subprocess.
 * <li>The event refers to a BPMN Message with the name of the message
 * (messageEventDefinition of an Intermediate Message Catch Event).
 * </ul>
 * If a matching message event is found, the meta instance is processed by this
 * event. Otherwise the message is ignored and the meta instance remains
 * unchanged. Message events should be modeled with
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

    public static final String EVENTLOG_TOPIC_MESSAGE = "metaworkflow.message";

    // Event item of a meta model defining the workflow group of a message event
    public static final String EVENT_ITEM_MESSAGE_GROUP = "metaworkflow.message.group";

    // EventLog data items
    public static final String ITEM_SUBPROCESS_REF = "$metaprocess.subprocess.ref";
    public static final String ITEM_SUBPROCESS_GROUP = "$metaprocess.subprocess.group";
    public static final String ITEM_MESSAGE = "$metaprocess.message";

    private static final Logger logger = Logger.getLogger(MetaWorkflowHandler.class.getName());

    @Inject
    EventLogService eventLogService;

    @Inject
    WorkflowService workflowService;

    /**
     * Processes all open EventLog entries with the topic
     * {@code metaworkflow.message} in the order of their creation. This method is
     * expected to be called by the {@link AsyncEventScheduler} running as a system
     * user.
     * <p>
     * Each EventLog entry is removed after processing - also if no matching message
     * event exists or the processing failed.
     */
    public void processEventLog() {
        List<EventLog> eventLogEntries = eventLogService.findEventsByTopic(100, EVENTLOG_TOPIC_MESSAGE);

        for (EventLog eventLogEntry : eventLogEntries) {
            try {
                if (!eventLogService.lock(eventLogEntry)) {
                    continue;
                }
                ItemCollection data = new ItemCollection(eventLogEntry.getData());
                ItemCollection metaWorkitem = workflowService.getWorkItem(eventLogEntry.getRef());
                if (metaWorkitem == null) {
                    logger.log(Level.WARNING, "metaworkflow {0} not found - message of subprocess {1} ignored",
                            new Object[] { eventLogEntry.getRef(), data.getItemValueString(ITEM_SUBPROCESS_REF) });
                } else {
                    processMessage(metaWorkitem, data);
                }
                eventLogService.removeEvent(eventLogEntry.getId());

            } catch (OptimisticLockException e) {
                // lock was not possible - continue....
                logger.log(Level.INFO, "...unable to lock metaworkflow message: {0}", e.getMessage());
            }
        }
    }

    /**
     * Processes the meta instance by the message event matching the given message.
     * If no message event matches, the meta instance remains unchanged.
     *
     * @param metaWorkitem - the meta instance
     * @param data         - EventLog data of the message
     */
    private void processMessage(ItemCollection metaWorkitem, ItemCollection data) {
        String group = data.getItemValueString(ITEM_SUBPROCESS_GROUP);
        String message = data.getItemValueString(ITEM_MESSAGE);
        try {
            ItemCollection messageEvent = findMessageEvent(metaWorkitem, group, message);
            if (messageEvent == null) {
                logger.log(Level.FINE, "...no message event for ''{0}/{1}'' in metaworkflow {2} - message ignored",
                        new Object[] { group, message, metaWorkitem.getUniqueID() });
                return;
            }
            metaWorkitem.event(messageEvent.getItemValueInteger(BPMNUtil.EVENT_ITEM_EVENTID));
            workflowService.processWorkItemByNewTransaction(metaWorkitem);
            logger.log(Level.INFO, "├── metaworkflow {0} triggered by subprocess {1} (''{2}/{3}'')",
                    new Object[] { metaWorkitem.getUniqueID(), data.getItemValueString(ITEM_SUBPROCESS_REF),
                            group, message });

        } catch (WorkflowException | InvalidAccessException | EJBException e) {
            // we also catch EJBExceptions here because we do not want to cancel the timer
            logger.log(Level.SEVERE, "metaworkflow {0} processing failed: {1}",
                    new Object[] { metaWorkitem.getUniqueID(), e.getMessage() });
        }
    }

    /**
     * Returns the first event of the current task of the meta instance matching the
     * given workflow group and message. The message name is resolved from the BPMN
     * Message referred by the event. There is no wildcard - the message name must
     * match exactly.
     *
     * @return matching event or null if no event matches
     */
    private ItemCollection findMessageEvent(ItemCollection metaWorkitem, String group, String message)
            throws ModelException {
        ModelManager modelManager = new ModelManager(workflowService);
        BPMNModel model = modelManager.getModelByWorkitem(metaWorkitem);
        List<ItemCollection> events = modelManager.findEventsByTask(model, metaWorkitem.getTaskID());
        for (ItemCollection event : events) {
            String messageGroup = event.getItemValueString(EVENT_ITEM_MESSAGE_GROUP).trim();
            if (messageGroup.isEmpty() || !messageGroup.equals(group)) {
                continue;
            }
            if (modelManager.findMessagesByEvent(model, event).contains(message)) {
                return event;
            }
        }
        return null;
    }

}