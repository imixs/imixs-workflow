package org.imixs.workflow.jee.faces.workitem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.faces.data.DocumentController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for the DataController
 * 
 * @author rsoika
 * 
 */
public class DataControllerTest {

	DocumentController dataController = null;

	@BeforeEach
	public void before() {
		dataController = new DocumentController();
	}

	@Test
	public void testBasic() {
		ItemCollection workitem = new ItemCollection();
		// test is new

		dataController.setDocument(workitem);
		assertTrue(dataController.isNewWorkitem());

		Date someDate = new Date();
		workitem.replaceItemValue("$Modified", someDate);
		workitem.replaceItemValue("$Created", someDate);
		dataController.setDocument(workitem);
		assertFalse(dataController.isNewWorkitem());

		someDate = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(someDate);
		cal.add(Calendar.SECOND, +1);
		workitem.replaceItemValue("$Modified", cal.getTime());
		dataController.setDocument(workitem);
		assertFalse(dataController.isNewWorkitem());

	}

}
