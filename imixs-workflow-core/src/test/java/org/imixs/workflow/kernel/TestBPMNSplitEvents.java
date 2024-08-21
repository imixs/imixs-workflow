package org.imixs.workflow.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowEngine;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.exceptions.WorkflowException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbpmn.bpmn.BPMNModel;

/**
 * Test class test the Imixs BPMNParser.
 * 
 * Special case: Conditional-Events
 * 
 * @see issue #299
 * @author rsoika
 */
public class TestBPMNSplitEvents {

	private final static Logger logger = Logger.getLogger(TestBPMNSplitEvents.class.getName());

	private BPMNModel model;

	private MockWorkflowEngine workflowEngine;

	@Before
	public void setup() throws PluginException {
		workflowEngine = new MockWorkflowEngine();
		workflowEngine.loadBPMNModel("/bpmn/split_event_with_condition.bpmn");
	}

	/**
	 * Test case Issue #590
	 * <p>
	 * The initial split event should not contain the exclusiveContiditions of task
	 * 1100!
	 * 
	 * 
	 */

	@Test
	public void testSplitWithConditions() {

		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0")
				.task(1000)
				.event(10);

		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
		} catch (WorkflowException e) {
			Assert.fail(e.getMessage());
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			Assert.fail();
			e.printStackTrace();
		}
		Assert.assertEquals(1, workItem.getItemValueInteger("runs"));
		Assert.assertEquals(1100, workItem.getTaskID());

		// Validate split Workitem....
		List<ItemCollection> splitWorkitems = workflowEngine.getWorkflowKernel().getSplitWorkitems();
		assertEquals(1, splitWorkitems.size());
		ItemCollection splitWorkitem = splitWorkitems.get(0);
		assertNotNull(splitWorkitem);
		Assert.assertEquals(2, splitWorkitem.getItemValueInteger("runs"));
		Assert.assertEquals(1200, splitWorkitem.getTaskID());

		// continue processing of main workitem
		try {
			workItem.event(80);
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			Assert.assertEquals(3, workItem.getItemValueInteger("runs"));
			Assert.assertEquals(1300, workItem.getTaskID());

		} catch (WorkflowException e) {
			Assert.fail(e.getMessage());
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			Assert.fail();
			e.printStackTrace();
		}

		// calling a processing cycle one again should fail as we have not more events
		// defined!
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			Assert.fail();
		} catch (WorkflowException e) {
			Assert.fail(e.getMessage());
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			logger.info(e.getMessage());
		}
	}

}