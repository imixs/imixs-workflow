package org.imixs.workflow;

import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;

import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import jakarta.ejb.SessionContext;

/**
 * Test class for Imixs WorkflowKernel. The test verifies the plugin
 * registration core functionality.
 * 
 * @author rsoika
 */
public class TestWorkflowKernelPluginRegistration {

	protected WorkflowKernel kernel = null;
	protected SessionContext ctx;
	protected WorkflowContext workflowContext;

	@Before
	public void setup() throws PluginException {

		ctx = Mockito.mock(SessionContext.class);
		// simulate SessionContext ctx.getCallerPrincipal().getName()
		Principal principal = Mockito.mock(Principal.class);
		when(principal.getName()).thenReturn("manfred");
		when(ctx.getCallerPrincipal()).thenReturn(principal);

		workflowContext = Mockito.mock(WorkflowContext.class);

		// provide a mock modelManger class
		when(workflowContext.getModelManager()).thenReturn(new MokModelManager());

	}

	/**
	 * Test without context
	 */
	@Test(expected = ProcessingErrorException.class)
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testInit() {
		// ProcessingErrorException is expected here
		kernel = new WorkflowKernel(null);
	}

	/**
	 * Test registration of plugin without context
	 */
	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testPluginRegistration() {

		kernel = new WorkflowKernel(workflowContext);

		MokPlugin mokPlugin = new MokPlugin();
		try {
			kernel.registerPlugin(mokPlugin);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
		
		// try plugin registration without conext..
		kernel.unregisterAllPlugins();
		try {
			workflowContext=null;
			kernel.registerPlugin(mokPlugin);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
		
		List<Plugin> plugins = kernel.getPluginRegistry();
		Assert.assertNotNull(plugins);
		Assert.assertEquals(1, plugins.size());
		
		// unregister plugin
		try {
			kernel.unregisterPlugin(MokPlugin.class.getName());
			plugins = kernel.getPluginRegistry();
			Assert.assertNotNull(plugins);
			Assert.assertEquals(0, plugins.size());
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
		
		// register 2 plugins
		try {
			kernel.registerPlugin(mokPlugin);
			kernel.registerPlugin(mokPlugin);
			plugins = kernel.getPluginRegistry();
			Assert.assertNotNull(plugins);
			Assert.assertEquals(2, plugins.size());
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

}
