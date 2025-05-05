package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;

/**
 * Test class test the Imixs BPMNUtil basic behavior.
 * 
 * @author rsoika
 */
public class TestBPMNUtil {

	BPMNModel model = null;
	ModelManager modelManager = null;
	MockWorkflowContext workflowContext;

	@BeforeEach
	public void setup() {
		try {
			workflowContext = new MockWorkflowContext();
			modelManager = new ModelManager(workflowContext);
			workflowContext.loadBPMNModelFromFile("/bpmn/simple.bpmn");
			model = workflowContext.fetchModel("1.0.0");
			assertNotNull(model);

		} catch (ModelException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test the find Task
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testFindTasks() {
		assertNotNull(model);

		ItemCollection task = modelManager.findTaskByID(model, 1000);
		assertNotNull(task);

		task = modelManager.findTaskByID(model, 1100);
		assertNotNull(task);

		// test non existing task
		task = modelManager.findTaskByID(model, 2000);
		assertNull(task);
	}

	/**
	 * Test the find event
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testFindEvents() {
		assertNotNull(model);

		ItemCollection event = modelManager.findEventByID(model, 1000, 10);
		assertNotNull(event);

		event = modelManager.findEventByID(model, 1000, 20);
		assertNotNull(event);

		// test non existing event
		event = modelManager.findEventByID(model, 1100, 10);
		assertNull(event);

		event = modelManager.findEventByID(model, 2000, 10);
		assertNull(event);
	}

}
