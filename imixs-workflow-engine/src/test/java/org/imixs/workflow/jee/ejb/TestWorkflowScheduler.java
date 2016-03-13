package org.imixs.workflow.jee.ejb;

import java.util.Calendar;
import java.util.Date;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class for WorkflowSchedulerService
 * 
 * This test verifies if the inDue methods
 * 
 * - workItemInDue
 * 
 * 
 * @author rsoika
 */
public class TestWorkflowScheduler {

	@Before
	public void setup() throws PluginException {

	}

	/**
	 * This test the date compare base on timWorkflowLastAccess in days
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * 
	 */
	@Test
	public void testDaysOnLastProcess() throws AccessDeniedException, ProcessingErrorException, PluginException {

		ItemCollection doc = new ItemCollection();
		ItemCollection activity = new ItemCollection();

		// delay unit = days
		activity.replaceItemValue("keyActivityDelayUnit", "3");
		// compare type = last process
		activity.replaceItemValue("keyScheduledBaseObject", "1");
		// delay
		activity.replaceItemValue("numActivityDelay", 10);

		Calendar workitemCal = Calendar.getInstance();
		workitemCal.setTime(new Date());
		// adjust -14
		workitemCal.add(Calendar.DAY_OF_MONTH, -14);
		// prepare doc
		doc.replaceItemValue("timWorkflowLastAccess", workitemCal.getTime());
		Assert.assertTrue(WorkflowSchedulerService.workItemInDue(doc, activity));

		// delay
		activity.replaceItemValue("numActivityDelay", 15);
		workitemCal.setTime(new Date());
		// adjust -14
		workitemCal.add(Calendar.DAY_OF_MONTH, -14);
		// prepare doc
		doc.replaceItemValue("timWorkflowLastAccess", workitemCal.getTime());
		Assert.assertFalse(WorkflowSchedulerService.workItemInDue(doc, activity));

	}
	
	
	
	
	/**
	 * This test the date compare base on timWorkflowLastAccess in workdays
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * 
	 */
	@Test
	public void testWorkdaysOnLastProcess() throws AccessDeniedException, ProcessingErrorException, PluginException {

		ItemCollection doc = new ItemCollection();
		ItemCollection activity = new ItemCollection();

		// delay unit = days
		activity.replaceItemValue("keyActivityDelayUnit", "4");
		// compare type = last process
		activity.replaceItemValue("keyScheduledBaseObject", "1");
		// delay
		activity.replaceItemValue("numActivityDelay", 10);

		Calendar workitemCal = Calendar.getInstance();
		workitemCal.setTime(new Date());
		// adjust -14
		workitemCal.add(Calendar.DAY_OF_MONTH, -14);
		// prepare doc
		doc.replaceItemValue("timWorkflowLastAccess", workitemCal.getTime());
		Assert.assertTrue(WorkflowSchedulerService.workItemInDue(doc, activity));

		// delay
		activity.replaceItemValue("numActivityDelay", 15);
		workitemCal.setTime(new Date());
		// adjust -14
		workitemCal.add(Calendar.DAY_OF_MONTH, -14);
		// prepare doc
		doc.replaceItemValue("timWorkflowLastAccess", workitemCal.getTime());
		Assert.assertFalse(WorkflowSchedulerService.workItemInDue(doc, activity));

	}

}
