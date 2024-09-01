package org.imixs.workflow;

import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
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
public class MockWorkflowEngine {
    private final WorkflowContext workflowContext;
    private final WorkflowKernel workflowKernel;

    /**
     * Constructor creates a MockWorkflowContext and a WorkflowKernel and registers
     * a MockPlugin
     * 
     * @throws PluginException
     */
    public MockWorkflowEngine() throws PluginException {
        workflowContext = new MockWorkflowContext();
        workflowKernel = new WorkflowKernel(workflowContext);

        // Register a Test Plugin
        MockPlugin mokPlugin = new MockPlugin();
        workflowKernel.registerPlugin(mokPlugin);
    }

    public ModelManager getModelManager() {
        return workflowContext.getModelManager();
    }

    /**
     * Loads a new model
     * 
     * @param modelPath
     */
    public void loadBPMNModel(String modelPath) {
        try {
            BPMNModel model = BPMNModelFactory.read(modelPath);
            getModelManager().addModel(model);
        } catch (BPMNModelException | ModelException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    /**
     * Returns the instance of the WorkflowKernel
     * 
     * @return
     */
    public WorkflowKernel getWorkflowKernel() {
        return workflowKernel;
    }

    public ModelManager getOpenBPMNModelManager() {
        return (ModelManager) workflowContext.getModelManager();
    }
}
