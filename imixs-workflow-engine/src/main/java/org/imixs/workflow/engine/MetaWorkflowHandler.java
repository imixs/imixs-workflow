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
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.SequenceFlow;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;

/**
 * The MetaWorkflowHandler handles the Subprocess → Meta direction of a meta
 * workflow in two steps:
 * <p>
 * <strong>1. Signal (synchronous)</strong>
 * <p>
 * The handler observes the processing life cycle of all workitems. If a
 * subprocess of a meta instance reaches an end task of its workflow group (a
 * task followed by a BPMN end event), the handler creates an EventLog entry
 * with the topic {@code metaworkflow.subprocess.completed}. The EventLog refID
 * is the $uniqueID of the meta instance. The data of the EventLog entry
 * contains:
 * <ul>
 * <li>{@code $metaprocess.subprocess.ref} - $uniqueID of the subprocess
 * <li>{@code $metaprocess.subprocess.group} - workflow group of the subprocess
 * <li>{@code $metaprocess.result} - name of the reached end event (or the end
 * task name as a fallback)
 * </ul>
 * A completed subprocess is marked with the item {@code $metaprocess.completed}
 * and will not signal its meta instance again.
 * <p>
 * <strong>2. Reaction (asynchronous)</strong>
 * <p>
 * The method {@link #processEventLog()} is called by the
 * {@link AsyncEventScheduler} running as a system user. For each EventLog entry
 * the handler looks for an event in the current task of the meta instance
 * defining the following Imixs extension items:
 * <ul>
 * <li>{@code metaworkflow.trigger.group} - workflow group of the subprocess
 * (required)
 * <li>{@code metaworkflow.trigger.result} - result of the subprocess (optional,
 * an empty value matches any result)
 * </ul>
 * If a matching event is found, the meta instance is processed by this event.
 * Otherwise the signal is ignored and the meta instance remains unchanged. So
 * the meta model alone decides if the completion of a subprocess changes the
 * meta instance. Trigger events should be modeled with
 * {@code workflow.public=false}, so they are not shown to the user.
 * <p>
 * Technical models do not need any configuration for this mechanism.
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

    public static final String EVENTLOG_TOPIC_COMPLETED = "metaworkflow.subprocess.completed";

    // Event items of a meta model defining a trigger event
    public static final String EVENT_ITEM_TRIGGER_GROUP = "metaworkflow.trigger.group";
    public static final String EVENT_ITEM_TRIGGER_RESULT = "metaworkflow.trigger.result";

    // Item stored in a subprocess marking that its completion was signaled
    public static final String ITEM_COMPLETED = "$metaprocess.completed";
    // Item holding the result (name of the end event) of a subprocess
    public static final String ITEM_RESULT = "$metaprocess.result";
    // EventLog data items
    public static final String ITEM_SUBPROCESS_REF = "$metaprocess.subprocess.ref";
    public static final String ITEM_SUBPROCESS_GROUP = "$metaprocess.subprocess.group";

    private static final Logger logger = Logger.getLogger(MetaWorkflowHandler.class.getName());

    @Inject
    EventLogService eventLogService;

    @Inject
    WorkflowService workflowService;

    /**
     * Verifies after each processing step if a subprocess of a meta instance has
     * reached an end task. If so, a new EventLog entry is created.
     * <p>
     * The AFTER_PROCESS event is fired before the workitem is saved, so the items
     * set here are persisted together with the subprocess in the same transaction.
     *
     * @param processingEvent - the CDI processing event
     * @throws ModelException if the model of the subprocess can not be resolved
     */
    public void onProcess(@Observes ProcessingEvent processingEvent) throws ModelException {
        if (ProcessingEvent.AFTER_PROCESS != processingEvent.getEventType()) {
            return;
        }

        ItemCollection subprocess = processingEvent.getDocument();
        String metaRef = subprocess.getItemValueString(MetaWorkflowPlugin.ITEM_REF);
        if (metaRef.isEmpty()) {
            // not part of a meta workflow
            return;
        }
        if (subprocess.getItemValueBoolean(ITEM_COMPLETED)) {
            // completion was already signaled
            return;
        }

        ModelManager modelManager = new ModelManager(workflowService);
        BPMNModel model = modelManager.getModelByWorkitem(subprocess);
        ItemCollection endTask = findEndTask(modelManager, model, subprocess);
        if (endTask == null) {
            // subprocess is still running
            return;
        }

        String result = findEndEventName(model, endTask);

        // mark the subprocess as completed (idempotency)
        subprocess.replaceItemValue(ITEM_COMPLETED, true);
        subprocess.replaceItemValue(ITEM_RESULT, result);

        // signal the meta instance
        ItemCollection data = new ItemCollection();
        data.replaceItemValue(ITEM_SUBPROCESS_REF, subprocess.getUniqueID());
        data.replaceItemValue(ITEM_SUBPROCESS_GROUP, subprocess.getWorkflowGroup());
        data.replaceItemValue(ITEM_RESULT, result);
        eventLogService.createEvent(EVENTLOG_TOPIC_COMPLETED, metaRef, data);

        logger.log(Level.INFO, "├── subprocess {0} completed with result ''{1}'' - signal metaworkflow {2}",
                new Object[] { subprocess.getUniqueID(), result, metaRef });
    }

    /**
     * Processes all open EventLog entries with the topic
     * {@code metaworkflow.subprocess.completed}. This method is expected to be
     * called by the {@link AsyncEventScheduler} running as a system user.
     * <p>
     * Each EventLog entry is removed after processing - also if no matching trigger
     * event exists or the processing failed.
     */
    public void processEventLog() {
        List<EventLog> eventLogEntries = eventLogService.findEventsByTopic(100, EVENTLOG_TOPIC_COMPLETED);

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
                logger.log(Level.INFO, "...unable to lock metaworkflow event: {0}", e.getMessage());
            }
        }
    }

    /**
     * Processes the meta instance by the trigger event matching the given signal
     * data. If no trigger event matches, the meta instance remains unchanged.
     *
     * @param metaWorkitem - the meta instance
     * @param data         - EventLog data of the completed subprocess
     */
    private void processSignal(ItemCollection metaWorkitem, ItemCollection data) {
        String group = data.getItemValueString(ITEM_SUBPROCESS_GROUP);
        String result = data.getItemValueString(ITEM_RESULT);
        try {
            ItemCollection triggerEvent = findTriggerEvent(metaWorkitem, group, result);
            if (triggerEvent == null) {
                logger.log(Level.FINE, "...no trigger event for ''{0}/{1}'' in metaworkflow {2} - signal ignored",
                        new Object[] { group, result, metaWorkitem.getUniqueID() });
                return;
            }
            metaWorkitem.event(triggerEvent.getItemValueInteger(BPMNUtil.EVENT_ITEM_EVENTID));
            workflowService.processWorkItemByNewTransaction(metaWorkitem);
            logger.log(Level.INFO, "├── metaworkflow {0} triggered by subprocess {1} (''{2}/{3}'')",
                    new Object[] { metaWorkitem.getUniqueID(), data.getItemValueString(ITEM_SUBPROCESS_REF),
                            group, result });

        } catch (WorkflowException | InvalidAccessException | EJBException e) {
            // we also catch EJBExceptions here because we do not want to cancel the timer
            logger.log(Level.SEVERE, "metaworkflow {0} processing failed: {1}",
                    new Object[] { metaWorkitem.getUniqueID(), e.getMessage() });
        }
    }

    /**
     * Returns the first event of the current task of the meta instance matching the
     * given workflow group and result. An empty trigger result matches any result.
     *
     * @return matching event or null if no event matches
     */
    private ItemCollection findTriggerEvent(ItemCollection metaWorkitem, String group, String result)
            throws ModelException {
        ModelManager modelManager = new ModelManager(workflowService);
        BPMNModel model = modelManager.getModelByWorkitem(metaWorkitem);
        List<ItemCollection> events = modelManager.findEventsByTask(model, metaWorkitem.getTaskID());
        for (ItemCollection event : events) {
            String triggerGroup = event.getItemValueString(EVENT_ITEM_TRIGGER_GROUP).trim();
            if (triggerGroup.isEmpty() || !triggerGroup.equals(group)) {
                continue;
            }
            String triggerResult = event.getItemValueString(EVENT_ITEM_TRIGGER_RESULT).trim();
            if (triggerResult.isEmpty() || triggerResult.equals(result)) {
                return event;
            }
        }
        return null;
    }

    /**
     * Returns the end task of the subprocess workflow group matching the current
     * $taskID, or null if the current task is not an end task.
     */
    private ItemCollection findEndTask(ModelManager modelManager, BPMNModel model, ItemCollection subprocess)
            throws ModelException {
        List<ItemCollection> endTasks = modelManager.findEndTasks(model, subprocess.getWorkflowGroup());
        for (ItemCollection endTask : endTasks) {
            if (endTask.getItemValueInteger(BPMNUtil.TASK_ITEM_TASKID) == subprocess.getTaskID()) {
                return endTask;
            }
        }
        return null;
    }

    /**
     * Returns the name of the BPMN end event following the given end task. The
     * named end events are the public interface of a technical model. If no named
     * end event exists, the name of the end task is returned.
     */
    private String findEndEventName(BPMNModel model, ItemCollection endTask) {
        BPMNElementNode taskNode = model.findElementNodeById(endTask.getItemValueString("id"));
        if (taskNode != null) {
            for (SequenceFlow flow : taskNode.getOutgoingSequenceFlows()) {
                BPMNElementNode target = flow.getTargetElement();
                if (target != null && BPMNTypes.END_EVENT.equals(target.getType())) {
                    String name = target.getAttribute("name");
                    if (name != null && !name.isBlank()) {
                        return name.trim();
                    }
                }
            }
        }
        // fallback
        return endTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME);
    }
}