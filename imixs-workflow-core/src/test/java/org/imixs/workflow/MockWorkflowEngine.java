package org.imixs.workflow;

import java.util.logging.Logger;

import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * The MockWorkflowEngine is a mock to simulate the behavior of a Workflow
 * Engine. It can be used for junit tests.
 * 
 * The Mock also registers a Test Plugin that simply counts the runs in a
 * processing life cycle.
 */
public class MockWorkflowEngine {

    private Logger logger = Logger.getLogger(MockWorkflowEngine.class.getName());

    private WorkflowKernel workflowKernel;

    /**
     * Constructor creates a WorkflowKernel and registers
     * a MockPlugin
     * 
     * @throws PluginException
     */
    public MockWorkflowEngine(WorkflowContext workflowContext) throws PluginException {
        workflowKernel = new WorkflowKernel(workflowContext);

        // Register a Test Plugin
        MockPlugin mokPlugin = new MockPlugin();
        workflowKernel.registerPlugin(mokPlugin);
    }

    public WorkflowKernel getWorkflowKernel() {
        return workflowKernel;
    }

    public ItemCollection process(ItemCollection workitem) throws PluginException, ModelException {
        return workflowKernel.process(workitem);
    }

}
