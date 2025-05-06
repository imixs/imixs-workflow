package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;

/**
 * Test class to test the behavior of the ModelManager.
 * 
 * This test verifies the linking an imixs-event with an imixs-task using a
 * intermediate catch and intermediate throw link-event.
 * 
 * @author rsoika
 */
public class TestBPMNParserSharedLinkEvent {

	BPMNModel model = null;
	ModelManager modelManager = null;
	MockWorkflowContext workflowContext;

	@BeforeEach
	public void setup() {
		try {
			workflowContext = new MockWorkflowContext();
			modelManager = new ModelManager(workflowContext);
		} catch (PluginException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * This test class tests intermediate link events
	 */
	@Test
	public void testLinkEventSimple() {
		try {
			workflowContext.loadBPMNModelFromFile("/bpmn/shared-link-event.bpmn");
			model = workflowContext.fetchModel("1.0.0");
			assertNotNull(model);
		} catch (ModelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// test count of elements
		assertEquals(3, model.findAllActivities().size());

		// test task 1000
		ItemCollection task = modelManager.findTaskByID(model, 1000);
		assertEquals("Task Shared Link Event1", task.getItemValueString("txtName"));

		// test shared events
		assertEquals(3, modelManager.findEventsByTask(model, 1000).size());

		ItemCollection event = modelManager.findEventByID(model, 1000, 99);
		assertEquals("cancel", event.getItemValueString("txtName"));

		// test shared events
		assertEquals(2, modelManager.findEventsByTask(model, 1100).size());
		assertEquals(0, modelManager.findEventsByTask(model, 1200).size());

	}

}
