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
import org.imixs.workflow.bpmn.BPMNEntityBuilder;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.SequenceFlow;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;

import jakarta.inject.Inject;

/**
 * The MetaWorkflowPlugin controls the communication between a meta workflow and
 * its subprocesses. The plugin is registered in the meta model and in the
 * technical models.
 * <p>
 * <strong>Meta → Subprocess</strong>
 * <p>
 * An event of a meta model can start a technical workflow by the Imixs
 * extension item {@code metaworkflow.subprocess.group}. The value is the name
 * of the workflow group of the technical model (e.g. 'Inquiry'). The plugin
 * resolves the start task and the init event from the latest model version of
 * the workflow group, creates the subprocess and links it with the meta
 * instance:
 * <ul>
 * <li>The subprocess gets the item {@code $metaprocess.ref} pointing to the
 * meta instance.
 * <li>The meta instance appends the new subprocess to the list
 * {@code $metaprocess.subprocesses}.
 * </ul>
 * An event can be triggered several times, so a meta instance can hold any
 * number of subprocesses of the same workflow group.
 * <p>
 * <strong>Subprocess → Meta</strong>
 * <p>
 * A subprocess sends signals to its meta instance in two cases:
 * <ul>
 * <li>The event defines a signal in its workflow result:
 *
 * <pre>
 * {@code
 * <metaworkflow name="SIGNAL">
 *   <name>Initial sampling requested</name>
 * </metaworkflow>
 * }
 * </pre>
 *
 * <li>The event leads into an end task (a task followed by a BPMN end event).
 * The name of the end task is sent as a signal. A completed subprocess is
 * marked with the items {@code $metaprocess.completed} and
 * {@code $metaprocess.result} and sends its completion signal only once.
 * </ul>
 * Each signal is stored as an EventLog entry with the topic
 * {@code metaworkflow.signal} and processed asynchronously by the
 * {@link MetaWorkflowHandler}.
 *
 * @author rsoika
 * @version 1.0
 * @see MetaWorkflowHandler
 */
public class MetaWorkflowPlugin extends AbstractPlugin {

    // Event item defining the workflow group of the subprocess to be started
    public static final String EVENT_ITEM_SUBPROCESS_GROUP = "metaworkflow.subprocess.group";

    // Workflow result definition of a signal
    public static final String RESULT_TAG = "metaworkflow";
    public static final String RESULT_NAME_SIGNAL = "SIGNAL";

    // Item stored in a subprocess pointing to its meta instance
    public static final String ITEM_REF = "$metaprocess.ref";
    // Item stored in the meta instance holding all subprocesses created so far
    public static final String ITEM_SUBPROCESSES = "$metaprocess.subprocesses";
    // Items stored in a subprocess after its completion signal was sent
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
            // Meta → Subprocess
            String workflowGroup = event.getItemValueString(EVENT_ITEM_SUBPROCESS_GROUP).trim();
            if (!workflowGroup.isEmpty()) {
                ItemCollection subprocess = createSubprocess(workflowGroup, workitem, modelManager);
                workitem.appendItemValueUnique(ITEM_SUBPROCESSES, subprocess.getUniqueID());
                logger.log(Level.INFO, "├── metaworkflow {0} created subprocess {1} ({2})",
                        new Object[] { workitem.getUniqueID(), subprocess.getUniqueID(), workflowGroup });
            }

            // Subprocess → Meta
            if (!workitem.getItemValueString(ITEM_REF).isEmpty()) {
                sendPublishedSignals(workitem, event, modelManager);
                sendCompletionSignal(workitem, event, modelManager);
            }

        } catch (ModelException e) {
            throw new PluginException(MetaWorkflowPlugin.class.getSimpleName(), INVALID_MODEL,
                    "Failed to resolve meta workflow model: " + e.getMessage(), e);
        }
        return workitem;
    }

    /**
     * Sends all signals defined in the workflow result of the current event.
     */
    private void sendPublishedSignals(ItemCollection subprocess, ItemCollection event, ModelManager modelManager)
            throws PluginException, ModelException {
        List<ItemCollection> signalDefinitions = getWorkflowService().evalWorkflowResultXML(event, RESULT_TAG,
                RESULT_NAME_SIGNAL, subprocess, true);
        for (ItemCollection signalDefinition : signalDefinitions) {
            sendSignal(subprocess, signalDefinition.getItemValueString("name"), modelManager);
        }
    }

    /**
     * Sends the completion signal if the current event leads into an end task. As
     * plugins run before the kernel updates $taskID, the target task is resolved
     * from the model.
     */
    private void sendCompletionSignal(ItemCollection subprocess, ItemCollection event, ModelManager modelManager)
            throws ModelException {
        if (subprocess.getItemValueBoolean(ITEM_COMPLETED)) {
            // completion signal was already sent
            return;
        }
        BPMNModel model = modelManager.getModelByWorkitem(subprocess);
        ItemCollection nextElement = modelManager.nextModelElement(model, event, subprocess);
        if (nextElement == null || !BPMNTypes.BPMN_TASKS.contains(nextElement.getType())) {
            // a follow-up event - the end task is verified by the next event
            return;
        }
        int nextTaskID = nextElement.getItemValueInteger(BPMNUtil.TASK_ITEM_TASKID);
        String group = resolveWorkflowGroup(subprocess, modelManager);
        for (ItemCollection endTask : modelManager.findEndTasks(model, group)) {
            if (endTask.getItemValueInteger(BPMNUtil.TASK_ITEM_TASKID) == nextTaskID) {
                String result = endTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME);
                subprocess.replaceItemValue(ITEM_COMPLETED, true);
                subprocess.replaceItemValue(ITEM_RESULT, result);
                sendSignal(subprocess, result, modelManager);
                return;
            }
        }
    }

    /**
     * Sends a signal from a subprocess to its meta instance by creating a new
     * EventLog entry with the topic {@code metaworkflow.signal}.
     *
     * @param subprocess   - the subprocess sending the signal
     * @param signal       - name of the signal
     * @param modelManager - model manager instance
     */
    protected void sendSignal(ItemCollection subprocess, String signal, ModelManager modelManager)
            throws ModelException {
        if (signal == null || signal.isBlank()) {
            return;
        }
        String metaRef = subprocess.getItemValueString(ITEM_REF);
        ItemCollection data = new ItemCollection();
        data.replaceItemValue(MetaWorkflowHandler.ITEM_SUBPROCESS_REF, subprocess.getUniqueID());
        data.replaceItemValue(MetaWorkflowHandler.ITEM_SUBPROCESS_GROUP,
                resolveWorkflowGroup(subprocess, modelManager));
        data.replaceItemValue(MetaWorkflowHandler.ITEM_SIGNAL, signal.trim());
        eventLogService.createEvent(MetaWorkflowHandler.EVENTLOG_TOPIC_SIGNAL, metaRef, data);

        logger.log(Level.INFO, "├── subprocess {0} sent signal ''{1}'' to metaworkflow {2}",
                new Object[] { subprocess.getUniqueID(), signal.trim(), metaRef });
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

    /**
     * Creates and processes a new subprocess for the given workflow group. The
     * start task and the init event are resolved from the latest model version of
     * the workflow group.
     *
     * @param workflowGroup - name of the technical workflow group
     * @param metaWorkitem  - the meta instance
     * @param modelManager  - model manager instance
     * @return the processed subprocess
     */
    protected ItemCollection createSubprocess(String workflowGroup, ItemCollection metaWorkitem,
            ModelManager modelManager) throws PluginException, ModelException {

        String version = getWorkflowService().findModelVersionByGroup(workflowGroup);
        BPMNModel subModel = modelManager.getModel(version);

        // resolve the start task - we expect exactly one
        List<ItemCollection> startTasks = modelManager.findStartTasks(subModel, workflowGroup);
        if (startTasks.size() != 1) {
            throw new PluginException(MetaWorkflowPlugin.class.getSimpleName(), INVALID_MODEL,
                    "Workflow group '" + workflowGroup + "' (" + version
                            + ") must define exactly one start task, found "
                            + startTasks.size());
        }
        ItemCollection startTask = startTasks.get(0);

        // resolve the init event leading into the start task
        BPMNElementNode taskNode = subModel.findElementNodeById(startTask.getItemValueString("id"));
        ItemCollection initEvent = findInitEvent(taskNode);
        if (initEvent == null) {
            throw new PluginException(MetaWorkflowPlugin.class.getSimpleName(), INVALID_MODEL,
                    "Workflow group '" + workflowGroup + "' (" + version
                            + ") defines no init event for start task "
                            + startTask.getItemValueInteger(BPMNUtil.TASK_ITEM_TASKID));
        }

        ItemCollection subprocess = new ItemCollection()
                .model(version)
                .task(startTask.getItemValueInteger(BPMNUtil.TASK_ITEM_TASKID))
                .event(initEvent.getItemValueInteger(BPMNUtil.EVENT_ITEM_EVENTID));
        subprocess.replaceItemValue(ITEM_REF, metaWorkitem.getUniqueID());

        // hook for future data mapping between meta instance and subprocess
        prepareSubprocess(metaWorkitem, subprocess);

        try {
            return getWorkflowService().processWorkItem(subprocess);
        } catch (InvalidAccessException e) {
            // AccessDeniedException and ProcessingErrorException
            throw new PluginException(MetaWorkflowPlugin.class.getSimpleName(), SUBPROCESS_FAILED,
                    "Failed to create subprocess for workflow group '" + workflowGroup + "': " + e.getMessage(), e);
        }
    }

    /**
     * Hook method called before a new subprocess is processed the first time. Can
     * be overwritten to copy data from the meta instance into the subprocess. The
     * default implementation does nothing.
     *
     * @param metaWorkitem - the meta instance
     * @param subprocess   - the new subprocess (not yet processed)
     */
    protected void prepareSubprocess(ItemCollection metaWorkitem, ItemCollection subprocess) {
        // no op
    }

    /**
     * Returns the init event of a given task node. An init event is an Imixs event
     * with no incoming flow or with one incoming flow coming from a BPMN start
     * event. Chained events are resolved recursively.
     *
     * @param node - the BPMN task or event node
     * @return the init event or null if no init event exists
     */
    private ItemCollection findInitEvent(BPMNElementNode node) {
        if (node == null) {
            return null;
        }
        for (SequenceFlow flow : node.getIngoingSequenceFlows()) {
            BPMNElementNode source = flow.getSourceElement();
            if (BPMNUtil.isInitEventNode(source)) {
                return BPMNEntityBuilder.build(source);
            }
            if (source != null && BPMNUtil.isImixsEventElement(source)) {
                ItemCollection result = findInitEvent(source);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}