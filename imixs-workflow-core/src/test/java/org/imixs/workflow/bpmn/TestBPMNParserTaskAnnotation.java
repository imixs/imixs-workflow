package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;

/**
 * Test class to test the behavior of the ModelManager.
 * 
 * The test verifies if a annotation assigned to a task will update the
 * documentation field of the task (in case it is not explicit filled by the
 * task)
 * 
 * 
 * 
 * @author rsoika
 */
public class TestBPMNParserTaskAnnotation {
	BPMNModel model = null;
	ModelManager modelManager = null;
	MockWorkflowContext workflowContext;

	@BeforeEach
	public void setup() {
		try {
			workflowContext = new MockWorkflowContext();
			modelManager = new ModelManager(workflowContext);
			workflowContext.loadBPMNModelFromFile("/bpmn/annotation_example.bpmn");
			model = workflowContext.fetchModel("1.0.0");
			assertNotNull(model);

		} catch (ModelException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test if annotations are assigned to a task documentation.
	 * 
	 * 
	 */
	@Test
	public void testSimple() {

		String VERSION = "1.0.0";

		// Test Environment
		ItemCollection profile;
		try {
			profile = modelManager.loadDefinition(model);
			assertNotNull(profile);
			assertEquals(VERSION, profile.getItemValueString("$ModelVersion"));
		} catch (ModelException e) {
			fail(e.getMessage());
		}

		// test count of elements
		assertEquals(3, model.findAllActivities().size());

		// test task 1200
		ItemCollection task = modelManager.findTaskByID(model, 1200);
		assertNotNull(task);
		assertEquals("<b>inner sample text</b>", task.getItemValueString("rtfdescription"));

		// test Task 1100 (overwrite annotation)
		task = modelManager.findTaskByID(model, 1100);
		assertNotNull(task);
		assertEquals("<b>custom text task2</b>", task.getItemValueString("rtfdescription"));

	}

}
