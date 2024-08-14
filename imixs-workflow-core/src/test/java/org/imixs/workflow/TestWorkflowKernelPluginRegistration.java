package org.imixs.workflow;

import java.util.List;

import org.imixs.workflow.exceptions.PluginException;
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
	 * Test register plugins without the Mock Environment
	 *
	 */
	@Test
	public void testKernel() {

		WorkflowKernel kernel = new WorkflowKernel(workflowContext.getWorkflowContext());
		// We expect that no plugin is registered
		List<Plugin> pluginRegistry = kernel.getPluginRegistry();
		Assert.assertNotNull(pluginRegistry);
		Assert.assertEquals(0, pluginRegistry.size());

		// Now we register a Pluign
		MockPluginNull mokPlugin = new MockPluginNull();
		try {
			kernel.registerPlugin(mokPlugin);
			// Now we expect one Plugin
			Assert.assertNotNull(pluginRegistry);
			Assert.assertEquals(1, pluginRegistry.size());
			// Now we unregister a Plugin and expect again an empty registry
			kernel.unregisterPlugin(mokPlugin.getClass().getName());
			pluginRegistry = kernel.getPluginRegistry();
			Assert.assertNotNull(pluginRegistry);
			Assert.assertEquals(0, pluginRegistry.size());
		} catch (PluginException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Test register 2nd plugin with MockEnvironment
	 * 
	 * The Mock Environment has already registered the MockPluign
	 */
	@Test
	public void testInit() {

		// We expect that the mock environment has already one Mock Plugin registered
		List<Plugin> pluginRegistry = workflowContext.getWorkflowKernel().getPluginRegistry();
		Assert.assertNotNull(pluginRegistry);
		Assert.assertEquals(1, pluginRegistry.size());

		// Now we register a 2nd plugin
		MockPluginNull mokPlugin = new MockPluginNull();
		try {
			workflowContext.getWorkflowKernel().registerPlugin(mokPlugin);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
		// We expect 2 Plugins
		pluginRegistry = workflowContext.getWorkflowKernel().getPluginRegistry();
		Assert.assertNotNull(pluginRegistry);
		Assert.assertEquals(2, pluginRegistry.size());
	}

	/**
	 * Test registration of plugin multiple times
	 */
	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testPluginRegistration() {

		MockPlugin mokPlugin = new MockPlugin();
		try {
			// We expect a Plugin Exception
			workflowContext.getWorkflowKernel().registerPlugin(mokPlugin);
			Assert.fail();
		} catch (PluginException e) {
			Assert.assertTrue(e.getMessage().contains("is already registered"));
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
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
		// Now we register the same plugin the 2nd time
		// We expect a Plugin Exception
		try {
			workflowContext.getWorkflowKernel().registerPlugin(mokPlugin);
			Assert.fail();
		} catch (PluginException e) {
			plugins = workflowContext.getWorkflowKernel().getPluginRegistry();
			Assert.assertNotNull(plugins);
			Assert.assertEquals(1, plugins.size());
		}

	}

}
