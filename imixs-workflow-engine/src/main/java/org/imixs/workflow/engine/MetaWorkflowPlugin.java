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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNTypes;

import jakarta.inject.Inject;

/**
 * The MetaWorkflowPlugin controls the communication between a meta workflow and
 * its core processes. The communication is based on BPMN Messages only - no
 * Imixs extension items are needed. The plugin is registered in the meta model
 * and in the core models.
 * <p>
 * <strong>Meta → Core: starting a core process</strong>
 * <p>
 * An event of a meta model can refer to BPMN Messages in the form
 * {@code <workflow group>:<message>} (e.g. 'Offer:create'). If the core model
 * of the workflow group defines a Message Start Event with this message, the
 * plugin starts a new core process:
 * <ul>
 * <li>The init event following the Message Start Event and its start task are
 * resolved from the latest model version of the workflow group.
 * <li>The core process gets the item {@code $metaprocess.ref} pointing to the
 * meta instance.
 * <li>The meta instance appends the new core process to the list
 * {@code $metaprocess.subprocesses}.
 * </ul>
 * If the core model defines no matching Message Start Event, the message is a
 * message to be received from a running core process and is ignored by the
 * plugin (see {@link MetaWorkflowHandler}).
 * <p>
 * <strong>Core → Meta: sending messages</strong>
 * <p>
 * A core process sends messages to its meta instance in two cases:
 * <ul>
 * <li>The event leads into a task followed by a BPMN Message Throw Event. The
 * name of the referred BPMN Message is sent - each time the task is reached,
 * also if the event leads back into the same task.
 * <li>The event leads into an end task (a task followed by a BPMN End Event).
 * The name of the end task is sent as a message. A completed core process is
 * marked with the items {@code $metaprocess.completed} and
 * {@code $metaprocess.result} and sends its completion message only once.
 * </ul>
 * Each message is stored as an EventLog entry with the topic
 * {@code metaworkflow.message} and processed asynchronously by the
 * {@link MetaWorkflowHandler}.
 *
 * @author rsoika
 * @version 1.0
 * @see MetaWorkflowHandler
 */
public class MetaWorkflowPlugin extends AbstractPlugin {

    // Separator between workflow group and message name in a meta model
    public static final String MESSAGE_SEPARATOR = ":";

    // Item stored in a core process pointing to its meta instance
    public static final String ITEM_REF = "$metaprocess.ref";
    // Item stored in the meta instance holding all core processes created so far
    public static final String ITEM_SUBPROCESSES = "$metaprocess.subprocesses";
    // Items stored in a core process after its completion message was sent
    public static final String ITEM_COMPLETED = "$metaprocess.completed";
    public static final String ITEM_RESULT = "$metaprocess.result";

    public static final String INVALID_MODEL = "INVALID_MODEL";
    public static final String SUBPROCESS_FAILED = "SUBPROCESS_FAILED";

    private static final Logger logger = Logger.getLogger(MetaWorkflowPlugin.class.getName());

    @Inject
    EventLogService eventLogService;

    @Override
    public ItemCollection run(ItemCollection workitem, ItemCollection event) throws PluginException {
        ModelManager modelManager = new ModelManager(getWorkflowService());
        try {
            BPMNModel model = modelManager.getModelByWorkitem(workitem);

            // Meta → Core
            for (String message : modelManager.findMessagesByEvent(model, event)) {
                startCoreProcess(workitem, message, modelManager);
            }

            // Core → Meta
            if (!workitem.getItemValueString(ITEM_REF).isEmpty()) {
                sendMessages(workitem, event, model, modelManager);
            }

        } catch (ModelException e) {
            throw new PluginException(MetaWorkflowPlugin.class.getSimpleName(), INVALID_MODEL,
                    "Failed to resolve meta workflow model: " + e.getMessage(), e);
        }
        return workitem;
    }

    /**
     * Starts a new core process if the given message refers to a Message Start
     * Event of a core model. Messages not matching a Message Start Event are
     * messages to be received from a core process and are ignored here.
     *
     * @param metaWorkitem - the meta instance
     * @param message      - message in the form {@code <workflow group>:<message>}
     * @param modelManager - model manager instance
     */
    private void startCoreProcess(ItemCollection metaWorkitem, String message, ModelManager modelManager)
            throws PluginException, ModelException {
        int pos = message.indexOf(MESSAGE_SEPARATOR);
        if (pos <= 0 || pos == message.length() - 1) {
            // not a meta workflow message
            return;
        }
        String workflowGroup = message.substring(0, pos).trim();
        String startMessage = message.substring(pos + 1).trim();

        String version = getWorkflowService().findModelVersionByGroup(workflowGroup);
        BPMNModel coreModel = modelManager.getModel(version);
        ItemCollection initEvent = modelManager.findInitEventByStartMessage(coreModel, workflowGroup, startMessage);
        if (initEvent == null) {
            // no Message Start Event - the message is received from a core process
            return;
        }

        ItemCollection coreProcess = createCoreProcess(metaWorkitem, initEvent, coreModel, version, modelManager);
        metaWorkitem.appendItemValueUnique(ITEM_SUBPROCESSES, coreProcess.getUniqueID());
        logger.log(Level.INFO, "├── metaworkflow {0} started core process {1} ({2})",
                new Object[] { metaWorkitem.getUniqueID(), coreProcess.getUniqueID(), message });
    }

    /**
     * Creates and processes a new core process starting with the given init event.
     *
     * @param metaWorkitem - the meta instance
     * @param initEvent    - the init event following the Message Start Event
     * @param coreModel    - the core model
     * @param version      - the model version of the core model
     * @param modelManager - model manager instance
     * @return the processed core process
     */
    protected ItemCollection createCoreProcess(ItemCollection metaWorkitem, ItemCollection initEvent,
            BPMNModel coreModel, String version, ModelManager modelManager)
            throws PluginException, ModelException {

        // resolve the start task following the init event
        ItemCollection startTask = modelManager.nextModelElement(coreModel, initEvent, new ItemCollection());
        if (startTask == null || !BPMNTypes.BPMN_TASKS.contains(startTask.getType())) {
            throw new PluginException(MetaWorkflowPlugin.class.getSimpleName(), INVALID_MODEL,
                    "Init event '" + initEvent.getItemValueString(BPMNUtil.EVENT_ITEM_NAME) + "' (" + version
                            + ") must be followed by a start task");
        }

        ItemCollection coreProcess = new ItemCollection()
                .model(version)
                .task(startTask.getItemValueInteger(BPMNUtil.TASK_ITEM_TASKID))
                .event(initEvent.getItemValueInteger(BPMNUtil.EVENT_ITEM_EVENTID));
        coreProcess.replaceItemValue(ITEM_REF, metaWorkitem.getUniqueID());

        // hook for future data mapping between meta instance and core process
        prepareCoreProcess(metaWorkitem, coreProcess);

        try {
            return getWorkflowService().processWorkItem(coreProcess);
        } catch (InvalidAccessException e) {
            // AccessDeniedException and ProcessingErrorException
            throw new PluginException(MetaWorkflowPlugin.class.getSimpleName(), SUBPROCESS_FAILED,
                    "Failed to create core process (" + version + "): " + e.getMessage(), e);
        }
    }

    /**
     * Hook method called before a new core process is processed the first time. Can
     * be overwritten to copy data from the meta instance into the core process. The
     * default implementation does nothing.
     *
     * @param metaWorkitem - the meta instance
     * @param coreProcess  - the new core process (not yet processed)
     */
    protected void prepareCoreProcess(ItemCollection metaWorkitem, ItemCollection coreProcess) {
        // no op
    }

    /**
     * Sends the messages of the target task of the current event. As plugins run
     * before the kernel updates $taskID, the target task is resolved from the
     * model.
     * <ul>
     * <li>For each BPMN Message Throw Event following the target task, the name of
     * the referred BPMN Message is sent.
     * <li>If the target task is an end task, its name is sent as the completion
     * message (only once).
     * </ul>
     */
    private void sendMessages(ItemCollection coreProcess, ItemCollection event, BPMNModel model,
            ModelManager modelManager) throws ModelException {
        ItemCollection nextElement = modelManager.nextModelElement(model, event, coreProcess);
        if (nextElement == null || !BPMNTypes.BPMN_TASKS.contains(nextElement.getType())) {
            // a follow-up event - the target task is verified by the next event
            return;
        }
        int nextTaskID = nextElement.getItemValueInteger(BPMNUtil.TASK_ITEM_TASKID);

        // messages thrown by the target task
        for (String message : modelManager.findMessagesByTask(model, nextTaskID)) {
            sendMessage(coreProcess, message, modelManager);
        }

        // completion message
        if (coreProcess.getItemValueBoolean(ITEM_COMPLETED)) {
            return;
        }
        String group = resolveWorkflowGroup(coreProcess, modelManager);
        for (ItemCollection endTask : modelManager.findEndTasks(model, group)) {
            if (endTask.getItemValueInteger(BPMNUtil.TASK_ITEM_TASKID) == nextTaskID) {
                String result = endTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME);
                coreProcess.replaceItemValue(ITEM_COMPLETED, true);
                coreProcess.replaceItemValue(ITEM_RESULT, result);
                sendMessage(coreProcess, result, modelManager);
                return;
            }
        }
    }

    /**
     * Sends a message from a core process to its meta instance by creating a new
     * EventLog entry with the topic {@code metaworkflow.message}.
     *
     * @param coreProcess  - the core process sending the message
     * @param message      - name of the message
     * @param modelManager - model manager instance
     */
    protected void sendMessage(ItemCollection coreProcess, String message, ModelManager modelManager)
            throws ModelException {
        if (message == null || message.isBlank()) {
            return;
        }
        String metaRef = coreProcess.getItemValueString(ITEM_REF);
        ItemCollection data = new ItemCollection();
        data.replaceItemValue(MetaWorkflowHandler.ITEM_SUBPROCESS_REF, coreProcess.getUniqueID());
        data.replaceItemValue(MetaWorkflowHandler.ITEM_SUBPROCESS_GROUP,
                resolveWorkflowGroup(coreProcess, modelManager));
        data.replaceItemValue(MetaWorkflowHandler.ITEM_MESSAGE, message.trim());
        eventLogService.createEvent(MetaWorkflowHandler.EVENTLOG_TOPIC_MESSAGE, metaRef, data);

        logger.log(Level.INFO, "├── core process {0} sent message ''{1}'' to metaworkflow {2}",
                new Object[] { coreProcess.getUniqueID(), message.trim(), metaRef });
    }

    /**
     * Returns the workflow group of a workitem. A new workitem gets its
     * $workflowgroup only at the end of the first processing step, so in this case
     * the group is resolved from the model.
     */
    private String resolveWorkflowGroup(ItemCollection workitem, ModelManager modelManager)
            throws ModelException {
        String group = workitem.getWorkflowGroup();
        if (group.isEmpty()) {
            BPMNModel model = modelManager.getModelByWorkitem(workitem);
            group = modelManager.loadProcess(workitem, model).getItemValueString("name");
        }
        return group;
    }
}