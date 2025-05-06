package org.imixs.workflow.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.exceptions.WorkflowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

	MockWorkflowContext workflowEngine;

	@BeforeEach
	public void setup() {
		try {
			workflowEngine = new MockWorkflowContext();
			workflowEngine.loadBPMNModelFromFile("/bpmn/split_event_with_condition.bpmn");
			BPMNModel model = workflowEngine.fetchModel("1.0.0");
			assertNotNull(model);
		} catch (PluginException | ModelException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test case Issue #590
	 * <p>
	 * The initial split event should not contain the exclusiveContiditions of task
	 * 1100!
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
			fail(e.getMessage());
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			fail();
			e.printStackTrace();
		}
		assertEquals(1, workItem.getItemValueInteger("runs"));
		assertEquals(1100, workItem.getTaskID());

		// Validate split Workitem....
		List<ItemCollection> splitWorkitems = workflowEngine.getWorkflowKernel().getSplitWorkitems();
		assertEquals(1, splitWorkitems.size());
		ItemCollection splitWorkitem = splitWorkitems.get(0);
		assertNotNull(splitWorkitem);
		assertEquals(2, splitWorkitem.getItemValueInteger("runs"));
		assertEquals(1200, splitWorkitem.getTaskID());

		// continue processing of main workitem
		try {
			workItem.event(80);
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			assertEquals(3, workItem.getItemValueInteger("runs"));
			assertEquals(1300, workItem.getTaskID());

		} catch (WorkflowException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			fail();
			e.printStackTrace();
		}

		// calling a processing cycle one again should fail as we have not more events
		// defined!
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			fail();
		} catch (WorkflowException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			logger.info(e.getMessage());
		}
	}

}