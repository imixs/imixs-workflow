package org.imixs.workflow.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.imixs.workflow.MockPlugin;
import org.imixs.workflow.MockPluginNull;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for Imixs WorkflowKernel. The test verifies the plugin
 * registration core functionality.
 * 
 * @author rsoika
 */
public class TestWorkflowKernelPluginRegistration {

	private MockWorkflowContext workflowContext;

	@BeforeEach
	public void setup() throws PluginException {
		workflowContext = new MockWorkflowContext();
	}

	/**
	 * Test register plugins without the Mock Environment
	 *
	 */
	@Test
	public void testKernel() {

		WorkflowKernel kernel = new WorkflowKernel(workflowContext);
		// We expect that no plugin is registered
		List<Plugin> pluginRegistry = kernel.getPluginRegistry();
		assertNotNull(pluginRegistry);
		assertEquals(0, pluginRegistry.size());

		// Now we register a Pluign
		MockPluginNull mokPlugin = new MockPluginNull();
		try {
			kernel.registerPlugin(mokPlugin);
			// Now we expect one Plugin
			assertNotNull(pluginRegistry);
			assertEquals(1, pluginRegistry.size());
			// Now we unregister a Plugin and expect again an empty registry
			kernel.unregisterPlugin(mokPlugin.getClass().getName());
			pluginRegistry = kernel.getPluginRegistry();
			assertNotNull(pluginRegistry);
			assertEquals(0, pluginRegistry.size());
		} catch (PluginException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test register 2nd plugin with MockEnvironment
	 * 
	 * The Mock Environment has already registered the MockPluign
	 */
	@Test
	public void testInit() {
		WorkflowKernel kernel = new WorkflowKernel(workflowContext);
		// Now we register a Pluign
		MockPlugin mockPlugin = new MockPlugin();
		try {
			kernel.registerPlugin(mockPlugin);
		} catch (PluginException e) {
			fail(e.getMessage());
		}
		// We expect that the mock environment has already one Mock Plugin registered
		List<Plugin> pluginRegistry = kernel.getPluginRegistry();
		assertNotNull(pluginRegistry);
		assertEquals(1, pluginRegistry.size());

		// Now we register a 2nd plugin
		MockPluginNull mockPluginNull = new MockPluginNull();
		try {
			kernel.registerPlugin(mockPluginNull);
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}
		// We expect 2 Plugins
		pluginRegistry = kernel.getPluginRegistry();
		assertNotNull(pluginRegistry);
		assertEquals(2, pluginRegistry.size());
	}

	/**
	 * Test registration of plugin multiple times
	 */
	@Test
	public void testPluginRegistration() {
		WorkflowKernel kernel = new WorkflowKernel(workflowContext);
		// Now we register a Pluign
		MockPlugin mockPlugin = new MockPlugin();
		try {
			kernel.registerPlugin(mockPlugin);
		} catch (PluginException e) {
			fail(e.getMessage());
		}

		// try to register the same plugin twice
		MockPlugin mokPlugin2 = new MockPlugin();
		try {
			// We expect a Plugin Exception
			kernel.registerPlugin(mokPlugin2);
			fail();
		} catch (PluginException e) {
			assertTrue(e.getMessage().contains("is already registered"));
		}

		List<Plugin> plugins = kernel.getPluginRegistry();
		assertNotNull(plugins);
		assertEquals(1, plugins.size());

		// unregister plugin
		try {
			kernel.unregisterPlugin(MockPlugin.class.getName());
			plugins = kernel.getPluginRegistry();
			assertNotNull(plugins);
			assertEquals(0, plugins.size());
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}

		// register the MockPlugin again
		try {
			kernel.registerPlugin(mockPlugin);
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}
		// Now we register the same plugin the 2nd time
		// We expect a Plugin Exception
		try {
			kernel.registerPlugin(mockPlugin);
			fail();
		} catch (PluginException e) {
			plugins = kernel.getPluginRegistry();
			assertNotNull(plugins);
			assertEquals(1, plugins.size());
		}

	}

}
