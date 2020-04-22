package org.imixs.workflow.plugins;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.IntervalPlugin;
import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class for IntervalPugin
 * 
 * @author rsoika
 */
public class TestIntervalPlugin {
	protected IntervalPlugin intervalPlugin = null;

	private static Logger logger = Logger.getLogger(TestIntervalPlugin.class.getName());
	WorkflowMockEnvironment workflowMockEnvironment;
	ItemCollection documentContext;
	ItemCollection documentActivity;

	@Before
	public void setup() throws PluginException, ModelException, AdapterException {

		workflowMockEnvironment = new WorkflowMockEnvironment();
		workflowMockEnvironment.setModelPath("/bpmn/TestIntervalPlugin.bpmn");

		workflowMockEnvironment.setup();

		intervalPlugin = new IntervalPlugin();
		try {
			intervalPlugin.init(workflowMockEnvironment.getWorkflowService());
		} catch (PluginException e) {

			e.printStackTrace();
		}

		// prepare test workitem
		documentContext = new ItemCollection();
		logger.info("[TestAccessPlugin] setup test data...");

		documentContext.replaceItemValue("reminder", new Date());

		workflowMockEnvironment.getDocumentService().save(documentContext);

	}

	/**
	 * This test verifies the model configuration
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testBPMNModel() throws PluginException {
		logger.info("------------------ Ref Date   =" + documentContext.getItemValueDate("reminder"));
		try {
			documentActivity = workflowMockEnvironment.getModel().getEvent(100, 99);
			intervalPlugin.run(documentContext, documentActivity);
		} catch (PluginException | ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Date result = documentContext.getItemValueDate("reminder");
		logger.info("------------------  Result Date=" + result);
	}

	/**
	 * This test verifies the 1st day of month interval
	 * 
	 * @throws PluginException
	 */
	@Test
	public void test1stDayInMonth() throws PluginException {
		// @monthly
		String cron = "0 0 1 * *";
		LocalDateTime date = intervalPlugin.evalCron(cron);
		logger.info("Result monthyl=" + date);
		LocalDateTime now = LocalDateTime.now();
		Assert.assertEquals(date.getMonthValue(), now.getMonthValue() + 1);
	}

	/**
	 * This test verifies the 1st day of year interval
	 * 
	 * @throws PluginException
	 */
	@Test
	public void test1stDayInYear() throws PluginException {
		// @monthly
		String cron = "0 0 1 1 *";
		LocalDateTime date = intervalPlugin.evalCron(cron);
		logger.info("Result monthly=" + date);
		LocalDateTime now = LocalDateTime.now();
		Assert.assertEquals(date.getYear(), now.getYear() + 1);
		Assert.assertEquals(1, date.getMonthValue());
	}

	/**
	 * This test verifies the 1 day of wee interval
	 * 
	 * @throws PluginException
	 */
	@Test
	public void test1stDayOfWeek() throws PluginException {
		// @weekly
		String cron = "0 0 * * 0 ";
		LocalDateTime date = intervalPlugin.evalCron(cron);
		logger.info("Result monthyl=" + date);
		// move to past one day...
		Assert.assertEquals(DayOfWeek.MONDAY, date.getDayOfWeek());
	}

	/**
	 * This test verifies the monthly interval.
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testMonthly() throws PluginException {
		// @monthly
		// 1.1.2020 - > 1.2.2020
		LocalDateTime ldt = LocalDateTime.now();
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime date = intervalPlugin.evalMacro("@monthly", ldt);
		logger.info("Now            =" + now);
		logger.info("Result @monthly=" + date);
		Assert.assertEquals(now.getDayOfMonth(), date.getDayOfMonth());
		Assert.assertEquals(now.getMonthValue() + 1, date.getMonthValue());
	}

	/**
	 * This test verifies the yearly interval.
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testYearly() throws PluginException {
		// @monthly
		// 1.1.2020 - > 1.1.2021
		LocalDateTime ldt = LocalDateTime.now();
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime date = intervalPlugin.evalMacro("@yearly", ldt);
		logger.info("Now           =" + now);
		logger.info("Result @yearly=" + date);
		Assert.assertEquals(now.getDayOfMonth(), date.getDayOfMonth());
		Assert.assertEquals(now.getMonthValue(), date.getMonthValue());
		Assert.assertEquals(now.getYear() + 1, date.getYear());
	}

	/**
	 * This test verifies the weekly interval.
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testWeekly() throws PluginException {
		// @Weekly
		// 1.1.2020 - > 7.1.2021
		LocalDateTime ldt = LocalDateTime.now();
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime date = intervalPlugin.evalMacro("@weekly", ldt);
		logger.info("Now           =" + now);
		logger.info("Result @weekly=" + date);
		Assert.assertTrue(now.getDayOfMonth() != date.getDayOfMonth());
		Assert.assertEquals(now.getDayOfWeek(), date.getDayOfWeek());
	}

}
