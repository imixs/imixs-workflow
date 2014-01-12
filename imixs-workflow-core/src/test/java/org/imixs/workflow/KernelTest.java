package org.imixs.workflow;

import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test class for workflow kernel
 * 
 * @author rsoika
 * 
 */
public class KernelTest {

	WorkflowKernel kernel = null;

	@Before
	public void setup() throws PluginException {
		MokWorkflowContext ctx = new MokWorkflowContext();
		kernel = new WorkflowKernel(ctx);

		MokPlugin mokPlugin = new MokPlugin();
		kernel.registerPlugin(mokPlugin);

	}

	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testSave() {
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("txtTitel", "Hello");
		itemCollection.replaceItemValue("$processid", 100);
		itemCollection.replaceItemValue("$activityid", 10);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"),
				"Hello");

		try {
			kernel.process(itemCollection);
		} catch (PluginException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			Assert.fail();
			e.printStackTrace();
		}

		Assert.assertEquals(1, itemCollection.getItemValueInteger("runs"));
		Assert.assertEquals(100,
				itemCollection.getItemValueInteger("$processid"));
	}

	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testForward() {
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("txtTitel", "Hello");
		itemCollection.replaceItemValue("$processid", 100);
		itemCollection.replaceItemValue("$activityid", 20);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"),
				"Hello");

		try {
			kernel.process(itemCollection);
		} catch (PluginException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			Assert.fail();
			e.printStackTrace();
		}

		Assert.assertEquals(1, itemCollection.getItemValueInteger("runs"));
		// test next state
		Assert.assertEquals(200,
				itemCollection.getItemValueInteger("$processid"));
	}

	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testFollowup() {
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("txtTitel", "Hello");
		itemCollection.replaceItemValue("$processid", 100);
		itemCollection.replaceItemValue("$activityid", 11);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"),
				"Hello");

		try {
			kernel.process(itemCollection);
		} catch (PluginException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			Assert.fail();
			e.printStackTrace();
		}

		// runs should be 2
		Assert.assertEquals(2, itemCollection.getItemValueInteger("runs"));
		// test next state
		Assert.assertEquals(200,
				itemCollection.getItemValueInteger("$processid"));
	}

	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testRegisterPlugin() {

		try {
			kernel.unregisterPlugin(MokPlugin.class.getName());
		} catch (PluginException e1) {
			Assert.fail();
			e1.printStackTrace();
		}

		// unregister once again - exception expected
 
		try {
			kernel.unregisterPlugin(MokPlugin.class.getName());
			// exception expected!
			Assert.fail();
		} catch (PluginException e1) {
			Assert.assertEquals(WorkflowKernel.PLUGIN_NOT_REGISTERED,
					e1.getErrorCode());
		}

		try {
			MokPlugin mokPlugin = new MokPlugin();
			kernel.registerPlugin(mokPlugin);
		} catch (PluginException e) {
			Assert.fail();
			e.printStackTrace();
		}

	}

}
