package org.imixs.workflow;

import static org.mockito.Mockito.when;

import java.security.Principal;

import org.imixs.workflow.bpmn.OpenBPMNModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.mockito.Mockito;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

import jakarta.ejb.SessionContext;

/**
 * The MockWorkflowContext encapsulates the initialization of a WorkflowContext
 * used in a jUnit test class.
 * 
 */
public class MockWorkflowContext {
    private final SessionContext ctx;
    private final WorkflowContext workflowContext;
    private final WorkflowKernel kernel;

    public MockWorkflowContext() throws PluginException {
        ctx = Mockito.mock(SessionContext.class);
        workflowContext = Mockito.mock(WorkflowContext.class);

        setupSessionContext();
        setupWorkflowContext();
        kernel = new WorkflowKernel(workflowContext);
        loadBPMNModel("/bpmn/simple.bpmn");

        MockPlugin mokPlugin = new MockPlugin();
        kernel.registerPlugin(mokPlugin);
    }

    private void setupSessionContext() {
        Principal principal = Mockito.mock(Principal.class);
        when(principal.getName()).thenReturn("manfred");
        when(ctx.getCallerPrincipal()).thenReturn(principal);
    }

    private void setupWorkflowContext() {
        when(workflowContext.getModelManager()).thenReturn(new OpenBPMNModelManager());
    }

    /**
     * Loads a new model
     * 
     * @param modelPath
     */
    public void loadBPMNModel(String modelPath) {
        try {
            BPMNModel model = BPMNModelFactory.read(modelPath);
            workflowContext.getModelManager().addModel(model);
        } catch (BPMNModelException | ModelException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    public SessionContext getSessionContext() {
        return ctx;
    }

    public WorkflowContext getWorkflowContext() {
        return workflowContext;
    }

    public WorkflowKernel getWorkflowKernel() {
        return kernel;
    }
}
