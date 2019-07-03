package org.imixs.workflow.plugins;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.plugins.IntervalPlugin;
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

	@SuppressWarnings("static-access")
	@Before
	public void setUp() throws PluginException {
		intervalPlugin = new IntervalPlugin();
		intervalPlugin.init(null);

		Logger logger = Logger.getLogger(IntervalPlugin.class.getName());

		logger.setLevel(Level.FINEST);
		this.logger.setLevel(Level.FINEST);
	}

	/**
	 * This test verifies the montly interval
	 * 
	 * @throws PluginException
	 */
	@SuppressWarnings("unused")
	@Test
	public void testMonthly() throws PluginException {

		SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");

		ItemCollection adocumentContext = new ItemCollection();

		Calendar calNow = Calendar.getInstance();
		Calendar calTestDate = Calendar.getInstance();
		Calendar calExptectedDate = Calendar.getInstance();

		// move to past one day...
		calTestDate.add(Calendar.DATE, -1);
		calExptectedDate.add(Calendar.MONTH, +1);

		adocumentContext.replaceItemValue("keyIntervalDatDate", "monthly");
		adocumentContext.replaceItemValue("datDate", calTestDate.getTime());

		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyScheduledActivity", "1");

		String sOldDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));
		String sExpectedDate = dateFormater.format(calExptectedDate.getTime());

		logger.info("TestDate=" + sOldDate);
		logger.info("Expected=" + sExpectedDate);

		// run plugin
		adocumentContext = intervalPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		// test new values
		String sNewDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));

		Assert.assertEquals(sExpectedDate, sNewDate);

	}

	/**
	 * This test verifies the montly interval
	 * 
	 * @throws PluginException
	 */
	@SuppressWarnings("unused")
	@Test
	public void testYearly() throws PluginException {

		SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");

		ItemCollection adocumentContext = new ItemCollection();

		Calendar calNow = Calendar.getInstance();
		Calendar calTestDate = Calendar.getInstance();
		Calendar calExptectedDate = Calendar.getInstance();

		// move to past one day...
		calTestDate.add(Calendar.DATE, -1);
		calExptectedDate.add(Calendar.YEAR, +1);

		adocumentContext.replaceItemValue("keyIntervalDatDate", "yearly");
		adocumentContext.replaceItemValue("datDate", calTestDate.getTime());

		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyScheduledActivity", "1");

		String sOldDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));
		String sExpectedDate = dateFormater.format(calExptectedDate.getTime());

		logger.info("TestDate=" + sOldDate);
		logger.info("Expected=" + sExpectedDate);

		// run plugin
		adocumentContext = intervalPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		// test new values
		String sNewDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));

		Assert.assertEquals(sExpectedDate, sNewDate);

	}

	/**
	 * This test verifies the montly interval
	 * 
	 * @throws PluginException
	 */
	@SuppressWarnings("unused")
	@Test
	public void testWeekly() throws PluginException {

		SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");

		ItemCollection adocumentContext = new ItemCollection();

		Calendar calNow = Calendar.getInstance();
		Calendar calTestDate = Calendar.getInstance();
		Calendar calExptectedDate = Calendar.getInstance();

		// move to past one day...
		calTestDate.add(Calendar.DATE, -1);
		calExptectedDate.add(Calendar.DATE, 7);

		adocumentContext.replaceItemValue("keyIntervalDatDate", "weekly");
		adocumentContext.replaceItemValue("datDate", calTestDate.getTime());

		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyScheduledActivity", "1");

		String sOldDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));
		String sExpectedDate = dateFormater.format(calExptectedDate.getTime());

		logger.info("TestDate=" + sOldDate);
		logger.info("Expected=" + sExpectedDate);

		// run plugin
		adocumentContext = intervalPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		// test new values
		String sNewDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));

		Assert.assertEquals(sExpectedDate, sNewDate);

	}

	/**
	 * This test verifies the daily interval
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testDaily() throws PluginException {

		SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");

		ItemCollection adocumentContext = new ItemCollection();

		// Calendar calNow = Calendar.getInstance();
		Calendar calTestDate = Calendar.getInstance();
		Calendar calExptectedDate = Calendar.getInstance();

		// move to past one day...
		calTestDate.add(Calendar.DATE, -1);
		calExptectedDate.add(Calendar.DATE, +1);

		adocumentContext.replaceItemValue("keyIntervalDatDate", "daily");
		adocumentContext.replaceItemValue("datDate", calTestDate.getTime());

		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyScheduledActivity", "1");

		String sOldDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));
		String sExpectedDate = dateFormater.format(calExptectedDate.getTime());

		logger.info("TestDate=" + sOldDate);
		logger.info("Expected=" + sExpectedDate);

		// run plugin
		adocumentContext = intervalPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		// test new values
		String sNewDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));

		Assert.assertEquals(sExpectedDate, sNewDate);

	}

	/**
	 * This test verifies fixed number of days interval
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testNumberOfDays() throws PluginException {

		SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");

		ItemCollection adocumentContext = new ItemCollection();

		// Calendar calNow = Calendar.getInstance();
		Calendar calTestDate = Calendar.getInstance();
		Calendar calExptectedDate = Calendar.getInstance();

		// move to past one day...
		calTestDate.add(Calendar.DATE, -1);
		calExptectedDate.add(Calendar.DATE, +1);

		// first we test a string for 1 day
		adocumentContext.replaceItemValue("keyIntervalDatDate", "1");
		adocumentContext.replaceItemValue("datDate", calTestDate.getTime());

		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyScheduledActivity", "1");

		String sOldDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));
		String sExpectedDate = dateFormater.format(calExptectedDate.getTime());

		logger.info("TestDate=" + sOldDate);
		logger.info("Expected=" + sExpectedDate);

		// run plugin
		adocumentContext = intervalPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		// test new values
		String sNewDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));

		Assert.assertEquals(sExpectedDate, sNewDate);

		/*
		 * test string with 10 days
		 * 
		 */
		calTestDate = Calendar.getInstance();
		calExptectedDate = Calendar.getInstance();

		// move to past one day...
		calTestDate.add(Calendar.DATE, -1);
		calExptectedDate.add(Calendar.DATE, +10);

		// first we test a string for 1 day
		adocumentContext.replaceItemValue("keyIntervalDatDate", "10");
		adocumentContext.replaceItemValue("datDate", calTestDate.getTime());

		adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyScheduledActivity", "1");

		sOldDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));
		sExpectedDate = dateFormater.format(calExptectedDate.getTime());

		logger.info("TestDate=" + sOldDate);
		logger.info("Expected=" + sExpectedDate);

		// run plugin
		adocumentContext = intervalPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		// test new values
		sNewDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));

		Assert.assertEquals(sExpectedDate, sNewDate);

		/*
		 * test integer with 5 days
		 * 
		 */
		calTestDate = Calendar.getInstance();
		calExptectedDate = Calendar.getInstance();

		// move to past one day...
		calTestDate.add(Calendar.DATE, -1);
		calExptectedDate.add(Calendar.DATE, +5);

		// first we test a string for 1 day
		adocumentContext.replaceItemValue("keyIntervalDatDate", new Integer(5));
		adocumentContext.replaceItemValue("datDate", calTestDate.getTime());

		adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyScheduledActivity", "1");

		sOldDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));
		sExpectedDate = dateFormater.format(calExptectedDate.getTime());

		logger.info("TestDate=" + sOldDate);
		logger.info("Expected=" + sExpectedDate);

		// run plugin
		adocumentContext = intervalPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		// test new values
		sNewDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));

		Assert.assertEquals(sExpectedDate, sNewDate);

	}

	/**
	 * This test verifies the montly interval
	 * 
	 * @throws PluginException
	 */
	@SuppressWarnings("unused")
	@Test
	public void testMonthlyAndYearly() throws PluginException {

		SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");

		ItemCollection adocumentContext = new ItemCollection();

		Calendar calNow = Calendar.getInstance();
		Calendar calTestDate = Calendar.getInstance();
		Calendar calExptectedDate = Calendar.getInstance();

		// move to past one day...
		calTestDate.add(Calendar.DATE, -1);
		calExptectedDate.add(Calendar.MONTH, +1);
		calExptectedDate.add(Calendar.YEAR, +1);

		adocumentContext.replaceItemValue("keyIntervalDatDate", "monthly yearly");
		adocumentContext.replaceItemValue("datDate", calTestDate.getTime());

		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyScheduledActivity", "1");

		String sOldDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));
		String sExpectedDate = dateFormater.format(calExptectedDate.getTime());

		logger.info("TestDate=" + sOldDate);
		logger.info("Expected=" + sExpectedDate);

		// run plugin
		adocumentContext = intervalPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		// test new values
		String sNewDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));

		Assert.assertEquals(sExpectedDate, sNewDate);

	}

	/**
	 * This test verifies the montly interval with a date in the future
	 * 
	 * @throws PluginException
	 */
	@SuppressWarnings("unused")
	@Test
	public void testMonthlyInFuture() throws PluginException {

		SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");

		ItemCollection adocumentContext = new ItemCollection();

		Calendar calNow = Calendar.getInstance();
		Calendar calTestDate = Calendar.getInstance();
		Calendar calExptectedDate = Calendar.getInstance();

		// move to past one day...
		calTestDate.add(Calendar.DATE, +1);
		calExptectedDate.add(Calendar.DATE, +1);

		adocumentContext.replaceItemValue("keyIntervalDatDate", "monthly");
		adocumentContext.replaceItemValue("datDate", calTestDate.getTime());

		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyScheduledActivity", "1");

		String sOldDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));
		String sExpectedDate = dateFormater.format(calExptectedDate.getTime());

		logger.info("TestDate=" + sOldDate);
		logger.info("Expected=" + sExpectedDate);

		// run plugin
		adocumentContext = intervalPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		// test new values
		String sNewDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));

		Assert.assertEquals(sExpectedDate, sNewDate);

	}

	/**
	 * This test verifies the montly interval on not scheduled activity.
	 * 
	 * @throws PluginException
	 */
	@SuppressWarnings("unused")
	@Test
	public void testMonthlyOnNoScheduledActivity() throws PluginException {

		SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");

		ItemCollection adocumentContext = new ItemCollection();

		Calendar calNow = Calendar.getInstance();
		Calendar calTestDate = Calendar.getInstance();
		Calendar calExptectedDate = Calendar.getInstance();

		// move to past one day...
		calTestDate.add(Calendar.DATE, -1);
		calExptectedDate.add(Calendar.DATE, -1);
		// calExptectedDate.add(Calendar.MONTH, +1);

		adocumentContext.replaceItemValue("keyIntervalDatDate", "monthly");
		adocumentContext.replaceItemValue("datDate", calTestDate.getTime());

		ItemCollection adocumentActivity = new ItemCollection();
		// adocumentActivity.replaceItemValue("keyScheduledActivity", "1");

		String sOldDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));
		String sExpectedDate = dateFormater.format(calExptectedDate.getTime());

		logger.info("TestDate=" + sOldDate);
		logger.info("Expected=" + sExpectedDate);

		// run plugin
		adocumentContext = intervalPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		// test new values
		String sNewDate = dateFormater.format(adocumentContext.getItemValueDate("datDate"));

		Assert.assertEquals(sExpectedDate, sNewDate);

	}

	/**
	 * This test verifies wrong date fields
	 * 
	 * @throws PluginException
	 */
	@SuppressWarnings("unused")
	@Test
	public void testWrongDat() throws PluginException {

		SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");

		ItemCollection adocumentContext = new ItemCollection();

		Calendar calNow = Calendar.getInstance();
		Calendar calTestDate = Calendar.getInstance();
		Calendar calExptectedDate = Calendar.getInstance();

		// move to past one day...
		calTestDate.add(Calendar.DATE, -1);
		calExptectedDate.add(Calendar.DATE, -1);
		// calExptectedDate.add(Calendar.MONTH, +1);

		adocumentContext.replaceItemValue("keyIntervalDatDate", "monthly");
		adocumentContext.replaceItemValue("datDateWrong", calTestDate.getTime());

		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyScheduledActivity", "1");

		String sOldDate = dateFormater.format(adocumentContext.getItemValueDate("datDateWrong"));
		String sExpectedDate = dateFormater.format(calExptectedDate.getTime());

		logger.info("TestDate=" + sOldDate);
		logger.info("Expected=" + sExpectedDate);

		// run plugin
		adocumentContext = intervalPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		// test new values
		String sNewDate = dateFormater.format(adocumentContext.getItemValueDate("datDateWrong"));

		Assert.assertEquals(sExpectedDate, sNewDate);

		// 2) test with missing date value

		adocumentContext = new ItemCollection();

		calNow = Calendar.getInstance();
		calTestDate = Calendar.getInstance();
		calExptectedDate = Calendar.getInstance();

		// move to past one day...
		calTestDate.add(Calendar.DATE, -1);
		calExptectedDate.add(Calendar.DATE, -1);
		// calExptectedDate.add(Calendar.MONTH, +1);

		adocumentContext.replaceItemValue("keyIntervalDatDate", "monthly");

		adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyScheduledActivity", "1");

		// run plugin
		adocumentContext = intervalPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		// 3) test with wrong interval field

		adocumentContext = new ItemCollection();

		calNow = Calendar.getInstance();
		calTestDate = Calendar.getInstance();
		calExptectedDate = Calendar.getInstance();

		// move to past one day...
		calTestDate.add(Calendar.DATE, -1);
		calExptectedDate.add(Calendar.DATE, -1);
		// calExptectedDate.add(Calendar.MONTH, +1);

		adocumentContext.replaceItemValue("keyInterval", "monthly");

		adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyScheduledActivity", "1");

		// run plugin
		adocumentContext = intervalPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

	}

}
