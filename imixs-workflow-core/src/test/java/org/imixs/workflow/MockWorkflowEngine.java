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
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

/**
 * The MockWorkflowEngine is a mock to simulate the behavior of a Workflow
 * Engine. It can be used for junit tests.
 * 
 * The Mock also registers a Test Plugin that simply counts the runs in a
 * processing life cycle.
 */
public class MockWorkflowEngine implements WorkflowContext {

    private Logger logger = Logger.getLogger(MockWorkflowEngine.class.getName());

    private final WorkflowKernel workflowKernel;

    private final Map<String, BPMNModel> modelStore = new ConcurrentHashMap<>();

    /**
     * Constructor creates a WorkflowKernel and registers
     * a MockPlugin
     * 
     * @throws PluginException
     */
    public MockWorkflowEngine() throws PluginException {

        workflowKernel = new WorkflowKernel(this);

        // Register a Test Plugin
        MockPlugin mokPlugin = new MockPlugin();
        workflowKernel.registerPlugin(mokPlugin);
    }

    @Override
    public BPMNModel loadModel(String version) throws ModelException {

        return modelStore.get(version);

    }

    public WorkflowKernel getWorkflowKernel() {
        return workflowKernel;
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

    // public ModelManager getModelManager() {
    // return modelManager;
    // }

    public ItemCollection process(ItemCollection workitem) throws PluginException, ModelException {

        // BPMNModel model = modelManager.getModel(workitem.getModelVersion());
        return workflowKernel.process(workitem);

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
    public BPMNModel findModelByWorkitem(ItemCollection workitem) throws ModelException {
        BPMNModel result = null;
        String version = workitem.getModelVersion();
        // first try a direct fetch....
        if (version != null && !version.isEmpty()) {
            result = modelStore.get(version);
        }

        if (result != null) {
            return result;
        } else {
            // try to find model by regex if version is not empty...
            if (version != null && !version.isEmpty()) {
                String matchingVersion = findVersionByRegEx(version);
                if (matchingVersion != null && !matchingVersion.isEmpty()) {
                    result = modelStore.get(matchingVersion);
                    if (result != null) {
                        // match
                        // update $modelVersion
                        logger.fine("Update $modelversion by regex " + version + " ▷ " + matchingVersion);
                        workitem.model(matchingVersion);
                        return result;
                    }
                }
            }

            // Still no match, try to find model version by group
            if (!workitem.getWorkflowGroup().isEmpty()) {
                String matchingVersion = findVersionByGroup(workitem.getWorkflowGroup());
                if (matchingVersion != null && !matchingVersion.isEmpty()) {

                    // loggin...
                    if (version.isEmpty()) {
                        logger.log(Level.INFO, "Set model version ''{1}'',"
                                + "  $workflowgroup: ''{2}'', $uniqueid: {3}",
                                new Object[] { version, matchingVersion, workitem.getWorkflowGroup(),
                                        workitem.getUniqueID() });
                    } else {
                        logger.log(Level.INFO, "Update model version: ''{0}'' ▶ ''{1}'',"
                                + "  $workflowgroup: ''{2}'', $uniqueid: {3}",
                                new Object[] { version, matchingVersion, workitem.getWorkflowGroup(),
                                        workitem.getUniqueID() });
                    }

                    // update $modelVersion
                    workitem.model(matchingVersion);
                    result = modelStore.get(matchingVersion);
                    return result;
                }
            }

            // no match!
            throw new ModelException(ModelException.UNDEFINED_MODEL_VERSION,
                    "$modelversion '" + version + "' not found");
        }
    }

    /**
     * This method returns a sorted list of model versions matching a given regex
     * for a model version. The result is sorted in reverse order, so the highest
     * version number is the first in the result list.
     * 
     * @param group
     * @return
     */
    @Override
    public String findVersionByRegEx(String modelRegex) {
        boolean debug = logger.isLoggable(Level.FINE);
        // List<String> result = new ArrayList<String>();
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
        return null;
    }

    /**
     * Returns a version by Group.
     * The method computes a sorted list of all model versions containing the
     * requested
     * workflow group. The result is sorted in reverse order, so the highest version
     * number is the first in the result list.
     * 
     * @param group - name of the workflow group
     * @return list of matching model versions
     * @throws ModelException
     */
    @Override
    public String findVersionByGroup(String group) throws ModelException {
        boolean debug = logger.isLoggable(Level.FINE);
        // Sorted in reverse order
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER.reversed());
        if (debug) {
            logger.log(Level.FINEST, "......searching model versions for workflowgroup ''{0}''...", group);
        }
        // try to find matching model version by group
        Collection<BPMNModel> models = modelStore.values();
        for (BPMNModel _model : models) {
            Set<String> allGroups = workflowKernel.getModelManager().findAllGroupsByModel(_model);
            if (allGroups.contains(group)) {
                result.add(BPMNUtil.getVersion(_model));
            }
        }

        if (result.size() > 0) {
            return result.iterator().next();
        }
        return null;

    }
}
