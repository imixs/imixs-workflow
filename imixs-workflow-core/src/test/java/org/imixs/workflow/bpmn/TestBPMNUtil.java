package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

/**
 * Test class test the Imixs BPMNUtil basic behavior.
 * 
 * @author rsoika
 */
public class TestBPMNUtil {

	protected BPMNModel model = null;
	ModelManager openBPMNModelManager = null;

	/**
	 * Loads the default model
	 */
	@BeforeEach
	public void setUp() {
		// load default model
		openBPMNModelManager = new ModelManager();
		try {
			model = BPMNModelFactory.read("/bpmn/simple.bpmn");
		} catch (BPMNModelException e) {
			e.printStackTrace();
			fail();
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

		ItemCollection task = openBPMNModelManager.findTaskByID(model, 1000);
		assertNotNull(task);

		task = openBPMNModelManager.findTaskByID(model, 1100);
		assertNotNull(task);

		// test non existing task
		task = openBPMNModelManager.findTaskByID(model, 2000);
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

		ItemCollection event = openBPMNModelManager.findEventByID(model, 1000, 10);
		assertNotNull(event);

		event = openBPMNModelManager.findEventByID(model, 1000, 20);
		assertNotNull(event);

		// test non existing event
		event = openBPMNModelManager.findEventByID(model, 1100, 10);
		assertNull(event);

		event = openBPMNModelManager.findEventByID(model, 2000, 10);
		assertNull(event);
	}

}
