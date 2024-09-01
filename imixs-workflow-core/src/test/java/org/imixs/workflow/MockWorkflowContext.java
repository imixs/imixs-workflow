package org.imixs.workflow;

import static org.mockito.Mockito.when;

import java.security.Principal;

import org.imixs.workflow.exceptions.PluginException;
import org.mockito.Mockito;

import jakarta.ejb.SessionContext;

/**
 * The MockWorkflowContext is a mock for a Workflow Environment used for junit
 * tests.
 * It encapsulates the initialization of a ModelManager and a WorkflowKernel.
 * 
 * A junit test can access the WorkflowKernel.
 */
public class MockWorkflowContext implements WorkflowContext {
    private final SessionContext ctx;
    private ModelManager openBPMNModelManager;

    /**
     * Constructor creates a Mock Environment
     * 
     * @throws PluginException
     */
    public MockWorkflowContext() {
        openBPMNModelManager = new ModelManager();
        ctx = Mockito.mock(SessionContext.class);
        setupSessionContext();
    }

    @Override
    public ModelManager getModelManager() {
        return openBPMNModelManager;
    }

    @Override
    public SessionContext getSessionContext() {
        return ctx;
    }

    /**
     * Creates a Mock for a Session Context with a test principal 'manfred'
     */
    private void setupSessionContext() {
        Principal principal = Mockito.mock(Principal.class);
        when(principal.getName()).thenReturn("manfred");
        when(ctx.getCallerPrincipal()).thenReturn(principal);
    }

}
