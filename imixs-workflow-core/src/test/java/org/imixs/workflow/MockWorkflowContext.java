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
 * This Mock implements the interface {@link WorkflowContext} and can be used
 * for junit tests.
 * 
 */
public class MockWorkflowContext implements WorkflowContext {

    private Logger logger = Logger.getLogger(MockWorkflowContext.class.getName());

    private final Map<String, BPMNModel> modelStore = new ConcurrentHashMap<>();

    /**
     * Constructor creates a WorkflowKernel and registers
     * a MockPlugin
     * 
     * @throws PluginException
     */
    public MockWorkflowContext() {
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
     * from the list of known versions.
     * If multiple versions match the given regex the method returns the highest
     * version number which is the first entry in the result list.
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
}
