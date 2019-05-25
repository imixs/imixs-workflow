package org.imixs.workflow.engine;

import java.util.Calendar;
import java.util.Date;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowSchedulerService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class for WorkflowSchedulerService
 * 
 * This test verifies if the inDue methods workItemInDue() and addWorkDays()
 * 
 * @author rsoika
 */
public class TestWorkflowScheduler {
	
	WorkflowSchedulerService workflowSchedulerService;

	@Before
	public void setup() throws PluginException {
		workflowSchedulerService=new WorkflowSchedulerService();
	}

	/**
	 * This test the date compare base on $lastProcessingDate in days
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
		doc.replaceItemValue("$lastProcessingDate", workitemCal.getTime());
		Assert.assertTrue(workflowSchedulerService.workItemInDue(doc, activity));

		// delay
		activity.replaceItemValue("numActivityDelay", 15);
		workitemCal.setTime(new Date());
		// adjust -14
		workitemCal.add(Calendar.DAY_OF_MONTH, -14);
		// prepare doc
		doc.replaceItemValue("$lastProcessingDate", workitemCal.getTime());
		Assert.assertFalse(workflowSchedulerService.workItemInDue(doc, activity));

	}

	
	

	/**
	 * This method tests the addWorkDays function in a weekday movement from
	 * Monday to Fiday
	 */
	@Test
	public void testAddWorkdaysFromMonday() {

		Calendar startDate = Calendar.getInstance();
		// adjust to FRIDAY
		startDate.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		System.out.println("Startdate=" + startDate.getTime());

		
		Assert.assertEquals(Calendar.TUESDAY,
				workflowSchedulerService.addWorkDays(startDate, 1).get(Calendar.DAY_OF_WEEK));
		Assert.assertEquals(Calendar.WEDNESDAY,
				workflowSchedulerService.addWorkDays(startDate, 2).get(Calendar.DAY_OF_WEEK));
		Assert.assertEquals(Calendar.FRIDAY,
				workflowSchedulerService.addWorkDays(startDate, 4).get(Calendar.DAY_OF_WEEK));
	
	
		Assert.assertEquals(Calendar.MONDAY,
				workflowSchedulerService.addWorkDays(startDate, 5).get(Calendar.DAY_OF_WEEK));
		
		
		
		
		
		Assert.assertEquals(Calendar.FRIDAY,
				workflowSchedulerService.addWorkDays(startDate, 9).get(Calendar.DAY_OF_WEEK));
		Assert.assertEquals(Calendar.MONDAY,
				workflowSchedulerService.addWorkDays(startDate, 10).get(Calendar.DAY_OF_WEEK));

	}


	/**
	 * This method tests the addWorkDays function in a weekday movement from
	 * Friday back to Monday
	 */
	@Test
	public void testAddWorkdaysFromFriday() {
	
		Calendar startDate = Calendar.getInstance();
		// adjust to FRIDAY
		startDate.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
		System.out.println("Startdate=" + startDate.getTime());
	
		// adjust -3 Workdays -> THUSEDAY
		Assert.assertEquals(Calendar.TUESDAY,
				workflowSchedulerService.addWorkDays(startDate, 2).get(Calendar.DAY_OF_WEEK));
	
		Assert.assertEquals(Calendar.WEDNESDAY,
				workflowSchedulerService.addWorkDays(startDate, 8).get(Calendar.DAY_OF_WEEK));
	
		Assert.assertEquals(Calendar.FRIDAY,
				workflowSchedulerService.addWorkDays(startDate, 10).get(Calendar.DAY_OF_WEEK));
	
		Assert.assertEquals(Calendar.THURSDAY,
				workflowSchedulerService.addWorkDays(startDate, 14).get(Calendar.DAY_OF_WEEK));
	
	}

	/**
	 * This method tests the addWorkDays function in a weekday movement from
	 * Friday back to Monday
	 */
	@Test
	public void testMinusWorkdaysFromFriday() {

		Calendar startDate = Calendar.getInstance();
		// adjust to FRIDAY
		startDate.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
		System.out.println("Startdate=" + startDate.getTime());

		// adjust -3 Workdays -> THUSEDAY
		Assert.assertEquals(Calendar.THURSDAY,
				workflowSchedulerService.addWorkDays(startDate, -1).get(Calendar.DAY_OF_WEEK));
		Assert.assertEquals(Calendar.WEDNESDAY,
				workflowSchedulerService.addWorkDays(startDate, -2).get(Calendar.DAY_OF_WEEK));
		Assert.assertEquals(Calendar.MONDAY,
				workflowSchedulerService.addWorkDays(startDate, -4).get(Calendar.DAY_OF_WEEK));
		// friday - 5
		Assert.assertEquals(Calendar.FRIDAY,
				workflowSchedulerService.addWorkDays(startDate, -5).get(Calendar.DAY_OF_WEEK));
		Assert.assertEquals(Calendar.MONDAY,
				workflowSchedulerService.addWorkDays(startDate, -9).get(Calendar.DAY_OF_WEEK));
		Assert.assertEquals(Calendar.FRIDAY,
				workflowSchedulerService.addWorkDays(startDate, -10).get(Calendar.DAY_OF_WEEK));

	}
	
	
	/**
	 * This method tests the addWorkDays function in a weekday movement from
	 * Friday back to Monday
	 */
	@Test
	public void testMinusWorkdaysFromMonday() {

		Calendar startDate = Calendar.getInstance();
		// adjust to FRIDAY
		startDate.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		System.out.println("Startdate=" + startDate.getTime());

		// adjust -3 Workdays -> THUSEDAY
		Assert.assertEquals(Calendar.THURSDAY,
				workflowSchedulerService.addWorkDays(startDate, -2).get(Calendar.DAY_OF_WEEK));

	}
	
	
	
	/**
	 * This method tests the addWorkDays function in a weekday movement from
	 * SATURDAY backwards
	 */
	@Test
	public void testMinusWorkdaysFromSaturday() {

		Calendar startDate = Calendar.getInstance();
		// adjust to SATURDAY
		startDate.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		System.out.println("Startdate=" + startDate.getTime());

		// adjust -1 Workdays -> THURSDAY
		Assert.assertEquals(Calendar.THURSDAY,
				workflowSchedulerService.addWorkDays(startDate, -1).get(Calendar.DAY_OF_WEEK));
		Assert.assertEquals(Calendar.FRIDAY,
				workflowSchedulerService.addWorkDays(startDate, -5).get(Calendar.DAY_OF_WEEK));

	}
	
	
	/**
	 * This method tests the addWorkDays function in a weekday movement from
	 * SATURDAY forwards
	 */
	@Test
	public void testAddWorkdaysFromSaturday() {

		Calendar startDate = Calendar.getInstance();
		// adjust to SATURDAY
		startDate.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		System.out.println("Startdate=" + startDate.getTime());

		// adjust -1 Workdays -> TUESDAY
		Assert.assertEquals(Calendar.TUESDAY,
				workflowSchedulerService.addWorkDays(startDate, 1).get(Calendar.DAY_OF_WEEK));

		Assert.assertEquals(Calendar.FRIDAY,
				workflowSchedulerService.addWorkDays(startDate, 4).get(Calendar.DAY_OF_WEEK));

		
		Assert.assertEquals(Calendar.MONDAY,
				workflowSchedulerService.addWorkDays(startDate, 5).get(Calendar.DAY_OF_WEEK));

	}

	/**
	 * This method tests the addWorkDays function in a weekday movement from
	 * SUNDAY backwards
	 */
	@Test
	public void testMinusWorkdaysFromSunday() {

		Calendar startDate = Calendar.getInstance();
		// adjust to SATURDAY
		startDate.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		System.out.println("Startdate=" + startDate.getTime());

		// adjust -1 Workdays -> THURSDAY
		Assert.assertEquals(Calendar.THURSDAY,
				workflowSchedulerService.addWorkDays(startDate, -1).get(Calendar.DAY_OF_WEEK));
		Assert.assertEquals(Calendar.FRIDAY,
				workflowSchedulerService.addWorkDays(startDate, -5).get(Calendar.DAY_OF_WEEK));

	}
	
	/**
	 * This method tests the addWorkDays function in a weekday movement from
	 * SUNDAY forwards
	 */
	@Test
	public void testAddWorkdaysFromSunday() {

		Calendar startDate = Calendar.getInstance();
		// adjust to SATURDAY
		startDate.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		System.out.println("Startdate=" + startDate.getTime());

		// adjust -1 Workdays -> TUESDAY
		Assert.assertEquals(Calendar.TUESDAY,
				workflowSchedulerService.addWorkDays(startDate, 1).get(Calendar.DAY_OF_WEEK));
	
		Assert.assertEquals(Calendar.FRIDAY,
				workflowSchedulerService.addWorkDays(startDate, 4).get(Calendar.DAY_OF_WEEK));

		
		Assert.assertEquals(Calendar.MONDAY,
				workflowSchedulerService.addWorkDays(startDate, 5).get(Calendar.DAY_OF_WEEK));

	}

}
