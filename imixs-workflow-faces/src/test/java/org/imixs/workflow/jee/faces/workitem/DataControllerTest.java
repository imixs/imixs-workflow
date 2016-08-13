package org.imixs.workflow.jee.faces.workitem;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the DataController
 * 
 * @author rsoika
 * 
 */
public class DataControllerTest {

	DataController dataController = null;

	@Before
	public void before() {
		dataController = new DataController();
	}

	@Test
	public void testBasic() {
		ItemCollection workitem = new ItemCollection();
		// test is new
		dataController.setWorkitem(workitem);
		Assert.assertTrue(dataController.isNewWorkitem());

		workitem.replaceItemValue(WorkflowKernel.UNIQUEID, WorkflowKernel.generateUniqueID());
		Assert.assertFalse(dataController.isNewWorkitem());

	}

}
