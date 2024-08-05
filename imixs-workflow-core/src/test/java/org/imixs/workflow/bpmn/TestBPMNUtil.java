package org.imixs.workflow.bpmn;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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

	/**
	 * Loads the default model
	 */
	@Before
	public void setUp() {

		try {
			model = BPMNModelFactory.read("/bpmn/simple.bpmn");
		} catch (BPMNModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/**
	 * Test the find Task
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testFindTasks() {
		Assert.assertNotNull(model);

		ItemCollection task = OpenBPMNModelManager.findTaskByID(model, 1000);
		Assert.assertNotNull(task);

		task = OpenBPMNModelManager.findTaskByID(model, 1100);
		Assert.assertNotNull(task);

		// test non existing task
		task = OpenBPMNModelManager.findTaskByID(model, 2000);
		Assert.assertNull(task);
	}

	/**
	 * Test the find event
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testFindEvents() {
		Assert.assertNotNull(model);

		ItemCollection event = OpenBPMNModelManager.findEventByID(model, 1000, 10);
		Assert.assertNotNull(event);

		event = OpenBPMNModelManager.findEventByID(model, 1000, 20);
		Assert.assertNotNull(event);

		// test non existing event
		event = OpenBPMNModelManager.findEventByID(model, 1100, 10);
		Assert.assertNull(event);

		event = OpenBPMNModelManager.findEventByID(model, 2000, 10);
		Assert.assertNull(event);
	}

}
