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
import org.openbpmn.bpmn.elements.SequenceFlow;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;

/**
 * The MetaWorkflowPlugin controls the relation between a meta workflow and its
 * subprocesses (Meta → Subprocess direction).
 * <p>
 * A meta workflow is a business-level workflow defined by the process owner. An
 * event of a meta model can start a technical workflow by the Imixs extension
 * item {@code metaworkflow.subprocess.group}. The value is the name of the
 * workflow group of the technical model (e.g. 'Inquiry'). The item is written
 * by the meta modeler like any other Imixs event item.
 * <p>
 * When a meta instance is processed by such an event, the plugin creates a new
 * subprocess:
 * <ul>
 * <li>The latest model version of the referenced workflow group is resolved.
 * <li>The start task and its init event are resolved from the BPMN model. No
 * task or event IDs need to be configured.
 * <li>The subprocess gets the item {@code $metaprocess.ref} pointing to the
 * meta instance.
 * <li>The meta instance appends the new subprocess to the list
 * {@code $metaprocess.subprocesses}.
 * </ul>
 * <p>
 * An event can be triggered several times, so a meta instance can hold any
 * number of subprocesses of the same workflow group (e.g. a second offer or a
 * list of requirements).
 * <p>
 * Events without the item {@code metaworkflow.subprocess.group} (e.g. a 'save'
 * event) are ignored by the plugin.
 * <p>
 * The opposite direction (Subprocess → Meta) is handled by the
 * {@link MetaWorkflowHandler}. Technical models do not need to know anything
 * about meta workflows.
 *
 * @author rsoika
 * @version 1.0
 * @see MetaWorkflowHandler
 */
public class MetaWorkflowPlugin extends AbstractPlugin {

    // Event item defining the workflow group of the subprocess to be started
    public static final String EVENT_ITEM_SUBPROCESS_GROUP = "metaworkflow.subprocess.group";

    // Item stored in a subprocess pointing to its meta instance
    public static final String ITEM_REF = "$metaprocess.ref";
    // Item stored in the meta instance holding all subprocesses created so far
    public static final String ITEM_SUBPROCESSES = "$metaprocess.subprocesses";

    public static final String INVALID_MODEL = "INVALID_MODEL";
    public static final String SUBPROCESS_FAILED = "SUBPROCESS_FAILED";

    private static final Logger logger = Logger.getLogger(MetaWorkflowPlugin.class.getName());

    /**
     * Creates a new subprocess if the current event defines the item
     * {@code metaworkflow.subprocess.group}.
     */
    @Override
    public ItemCollection run(ItemCollection metaWorkitem, ItemCollection event) throws PluginException {

        String workflowGroup = event.getItemValueString(EVENT_ITEM_SUBPROCESS_GROUP).trim();
        if (workflowGroup.isEmpty()) {
            // this event does not start a subprocess
            return metaWorkitem;
        }

        ModelManager modelManager = new ModelManager(getWorkflowService());
        try {
            ItemCollection subprocess = createSubprocess(workflowGroup, metaWorkitem, modelManager);

            metaWorkitem.appendItemValueUnique(ITEM_SUBPROCESSES, subprocess.getUniqueID());
            logger.log(Level.INFO, "├── metaworkflow {0} created subprocess {1} ({2})",
                    new Object[] { metaWorkitem.getUniqueID(), subprocess.getUniqueID(), workflowGroup });

        } catch (ModelException e) {
            throw new PluginException(MetaWorkflowPlugin.class.getSimpleName(), INVALID_MODEL,
                    "Failed to resolve model for workflow group '" + workflowGroup + "': " + e.getMessage(), e);
        }
        return metaWorkitem;
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