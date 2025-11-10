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

package org.imixs.workflow;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

/**
 * The MockWorkflowContext is a mock to simulate the behavior of a Workflow
 * Engine. It can be used for junit tests.
 * 
 * The Mock also registers a Test Plugin that simply counts the runs in a
 * processing life cycle.
 */
public class MockWorkflowContext implements WorkflowContext {

    private Logger logger = Logger.getLogger(MockWorkflowContext.class.getName());

    private final Map<String, BPMNModel> modelStore = new ConcurrentHashMap<>();

    private WorkflowKernel workflowKernel;

    /**
     * Constructor creates a WorkflowKernel and registers a MockPlugin
     * 
     * @throws PluginException
     */
    public MockWorkflowContext() throws PluginException {
        workflowKernel = new WorkflowKernel(this);

        // Register a Test Plugin
        MockPlugin mokPlugin = new MockPlugin();
        workflowKernel.registerPlugin(mokPlugin);
    }

    public WorkflowKernel getWorkflowKernel() {
        return workflowKernel;
    }

    /**
     * Helper method to load a model from internal cache (not thread save)
     * 
     * @param version
     * @return
     * @throws ModelException
     */
    public BPMNModel fetchModel(String version) throws ModelException {
        return modelStore.get(version);
    }

    /**
     * Loads a new model
     * 
     * @param modelPath
     */
    public void loadBPMNModelFromFile(String modelPath) {
        try {
            BPMNModel model = BPMNModelFactory.read(modelPath);
            modelStore.put(BPMNUtil.getVersion(model), model);

        } catch (BPMNModelException e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Returns a Model matching the $modelversion of a given workitem. The
     * $modelversion can optional be provided as a regular expression.
     * <p>
     * In case no matching model version exits, the method tries to find the highest
     * Model Version matching the corresponding workflow group.
     * <p>
     * The method throws a ModelException in case the model version did not exits.
     **/
    @Override
    public String findModelVersionByWorkitem(ItemCollection workitem) throws ModelException {
        String version = workitem.getModelVersion();
        // first try a direct fetch....
        if (version != null && !version.isEmpty()
                && modelStore.containsKey(version)) {
            // current version matches a valid model
            return version;
        }

        // ...try to find model version by regex...
        if (version != null && !version.isEmpty()) {
            String matchingVersion = findModelVersionByRegEx(version);
            if (matchingVersion != null && !matchingVersion.isEmpty()
                    && modelStore.containsKey(matchingVersion)) {
                // match - update $modelVersion
                logger.fine("Update $modelversion by regex " + version + " ▷ " + matchingVersion);
                workitem.model(matchingVersion);
                return matchingVersion;

            }
        }
        // ...try to find model version by group
        if (!workitem.getWorkflowGroup().isEmpty()) {
            String matchingVersion = findModelVersionByGroup(workitem.getWorkflowGroup());
            if (matchingVersion != null && !matchingVersion.isEmpty()
                    && modelStore.containsKey(matchingVersion)) {
                // match - update $modelVersion
                logger.fine("Update $modelversion by regex " + version + " ▷ " + matchingVersion);
                workitem.model(matchingVersion);
                return matchingVersion;

            }
        }
        // no match!
        throw new ModelException(ModelException.UNDEFINED_MODEL_VERSION, "$modelversion '" + version + "' not found");

    }

    /**
     * This method returns a best match of a model version matching a given regex
     * from the list of known versions. If multiple versions match the given regex
     * the method returns the highest version number which is the first entry in the
     * result list.
     * 
     * @param group
     * @return
     */
    @Override
    public String findModelVersionByRegEx(String modelRegex) throws ModelException {
        boolean debug = logger.isLoggable(Level.FINE);

        // Sorted in reverse order
        Set<String> result = new TreeSet<>(Collections.reverseOrder());
        if (debug) {
            logger.log(Level.FINEST, "......searching model versions for regex ''{0}''...", modelRegex);
        }
        // try to find matching model version by regex
        Collection<BPMNModel> models = modelStore.values();
        for (BPMNModel amodel : models) {
            String _version = BPMNUtil.getVersion(amodel);
            if (Pattern.compile(modelRegex).matcher(_version).find()) {
                result.add(_version);
            }
        }
        if (result.size() > 0) {
            return result.iterator().next();
        }
        // no match!
        throw new ModelException(ModelException.UNDEFINED_MODEL_VERSION,
                "no matching model version found for regex: '" + modelRegex + "'");

    }

    /**
     * Returns a version by Group. The method computes a sorted list of all model
     * versions containing the requested workflow group. The result is sorted in
     * reverse order, so the highest version number is the first in the result list.
     * 
     * @param group - name of the workflow group
     * @return list of matching model versions
     * @throws ModelException
     */
    @Override
    public String findModelVersionByGroup(String group) throws ModelException {
        boolean debug = logger.isLoggable(Level.FINE);
        // Sorted in reverse order
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER.reversed());
        if (debug) {
            logger.log(Level.FINEST, "......searching model versions for workflowgroup ''{0}''...", group);
        }
        // try to find matching model version by group
        Collection<BPMNModel> models = modelStore.values();
        ModelManager _modelManager = new ModelManager(this);
        for (BPMNModel _model : models) {
            Set<String> allGroups = _modelManager.findAllGroupsByModel(_model);
            if (allGroups.contains(group)) {
                result.add(BPMNUtil.getVersion(_model));
            }
        }

        if (result.size() > 0) {
            return result.iterator().next();
        }
        // no match!
        throw new ModelException(ModelException.UNDEFINED_MODEL_VERSION,
                "no matching model version found for workflow group: '" + group + "'");

    }

    @Override
    public ItemCollection getWorkItem(String uniqueid) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getWorkItem'");
    }

    @Override
    public ItemCollection processWorkItem(ItemCollection workitem)
            throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {
        return workflowKernel.process(workitem);
    }

    @Override
    public ItemCollection evalNextTask(ItemCollection workitem) throws PluginException, ModelException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'evalNextTask'");
    }

    @Override
    public ItemCollection evalWorkflowResult(ItemCollection event, String xmlTag, ItemCollection documentContext,
            boolean resolveItemValues) throws PluginException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'evalWorkflowResult'");
    }

    @Override
    public ItemCollection evalWorkflowResult(ItemCollection event, String tag, ItemCollection documentContext)
            throws PluginException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'evalWorkflowResult'");
    }

    @Override
    public String evalConditionalExpression(String expression, ItemCollection workitem) {
        return expression;
    }

}
