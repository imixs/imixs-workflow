package org.imixs.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class to test Loop Situations.
 * 
 * The WorklfowKernel should detect infinite event loops
 * 
 * 
 * @see issue #299
 * @author rsoika
 */
public class TestWorkflowKernelLoop {

	private MockWorkflowEnvironment workflowEnvironment;

	@Before
	public void setup() throws PluginException {
		workflowEnvironment = new MockWorkflowEnvironment();
		// load default model
		workflowEnvironment.loadBPMNModel("/bpmn/loop-event.bpmn");
	}

	/**
	 * Here we test _capacity <= 1.00
	 * No Loop expected
	 * 
	 */
	@Test
	public void testLoopCase1() {

		ItemCollection workItem = new ItemCollection();
		workItem.setItemValue("budget", 1.0);
		workItem.model("1.0.0")
				.task(1000)
				.event(10);

		try {
			workflowEnvironment.getWorkflowKernel().process(workItem);
			// We expect 1200 because budget is <= 1.00
			assertEquals(1200, workItem.getTaskID());
			assertEquals(2, workItem.getItemValueInteger("runs"));
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

	}

	/**
	 * Here we test _capacity <= 1.00 with the eval method
	 * No Loop expected
	 * 
	 */
	@Test
	public void testLoopCase1Eval() {

		ItemCollection workItem = new ItemCollection();
		workItem.setItemValue("budget", 1.0);
		workItem.model("1.0.0")
				.task(1000)
				.event(10);

		try {
			ItemCollection task = workflowEnvironment.getWorkflowKernel().eval(workItem);
			// We expect 1200 because budget is <= 1.00
			assertEquals(1200, task.getItemValueInteger("taskID"));
			assertEquals(0, workItem.getItemValueInteger("runs"));
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

	}

	/**
	 * Here we test _capacity > 1.00
	 * Loop expected
	 * 
	 */
	@Test
	public void testLoopCase2() {
		ItemCollection workItem = new ItemCollection();
		workItem.setItemValue("budget", 100.50);
		workItem.model("1.0.0")
				.task(1000)
				.event(10);
		try {
			workflowEnvironment.getWorkflowKernel().process(workItem);
			// We expect a ProcessingErrorException! budget is <= 1.00
			Assert.fail();
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			Assert.fail();

		} catch (ProcessingErrorException e) {
			// Expected situation
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().contains("loop detected"));
		}

	}

	/**
	 * Here we test _capacity > 1.00 with the eval() method
	 * Loop expected
	 * 
	 */
	@Test
	public void testLoopCase2Eval() {
		ItemCollection workItem = new ItemCollection();
		workItem.setItemValue("budget", 100.50);
		workItem.model("1.0.0")
				.task(1000)
				.event(10);
		try {
			workflowEnvironment.getWorkflowKernel().eval(workItem);
			// We expect a ProcessingErrorException! budget is <= 1.00
			Assert.fail();
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			Assert.fail();

		} catch (ProcessingErrorException e) {
			// Expected situation
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().contains("loop detected"));
		}

	}

}