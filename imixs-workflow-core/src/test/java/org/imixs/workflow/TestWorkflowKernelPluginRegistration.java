package org.imixs.workflow;

import java.util.List;

import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test class for Imixs WorkflowKernel. The test verifies the plugin
 * registration core functionality.
 * 
 * @author rsoika
 */
public class TestWorkflowKernelPluginRegistration {

	private MockWorkflowEnvironment workflowContext;

	@Before
	public void setup() throws PluginException {
		workflowContext = new MockWorkflowEnvironment();
	}

	/**
	 * Test without context
	 */
	@Test(expected = ProcessingErrorException.class)
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testInit() {
		// ProcessingErrorException is expected here
		WorkflowKernel kernel = new WorkflowKernel(null);
	}

	/**
	 * Test registration of plugin without context
	 */
	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testPluginRegistration() {

		MockPlugin mokPlugin = new MockPlugin();
		try {
			workflowContext.getWorkflowKernel().registerPlugin(mokPlugin);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		// try plugin registration without conext..
		workflowContext.getWorkflowKernel().unregisterAllPlugins();
		try {
			workflowContext = null;
			workflowContext.getWorkflowKernel().registerPlugin(mokPlugin);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List<Plugin> plugins = workflowContext.getWorkflowKernel().getPluginRegistry();
		Assert.assertNotNull(plugins);
		Assert.assertEquals(1, plugins.size());

		// unregister plugin
		try {
			workflowContext.getWorkflowKernel().unregisterPlugin(MockPlugin.class.getName());
			plugins = workflowContext.getWorkflowKernel().getPluginRegistry();
			Assert.assertNotNull(plugins);
			Assert.assertEquals(0, plugins.size());
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		// register 2 plugins
		try {
			workflowContext.getWorkflowKernel().registerPlugin(mokPlugin);
			workflowContext.getWorkflowKernel().registerPlugin(mokPlugin);
			plugins = workflowContext.getWorkflowKernel().getPluginRegistry();
			Assert.assertNotNull(plugins);
			Assert.assertEquals(2, plugins.size());
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

}
