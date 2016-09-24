package org.imixs.workflow.plugins;

import java.util.Calendar;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test the replaceDynamicValues method of abstractPugin
 * 
 * @author rsoika
 * 
 */
public class TestAbstractPlugin {
	public ItemCollection documentContext;
	public ItemCollection documentActivity;
	private final static Logger logger = Logger.getLogger(TestAbstractPlugin.class.getName());

	@Before
	public void setup() throws PluginException {

	}

	/**
	 * Test replacement of dynamic item values
	 * 
	 * <itemvalue>xxx</itemvalue>
	 * @throws PluginException 
	 * 
	 */
	@Test
	public void testReplaceDynamicValues() throws PluginException {

		String testString = "Hello <itemvalue>txtname</itemvalue>!";
		String expectedString = "Hello Anna!";

		TestPlugin applicationPlugin = new TestPlugin();

		// prepare data
		logger.info("[TestAbstractPlugin] setup test data...");
		documentContext = new ItemCollection();
		documentContext.replaceItemValue("txtName", "Anna");

		String resultString = applicationPlugin.replaceDynamicValues(testString, documentContext);

		Assert.assertEquals(expectedString, resultString);

	}
	
	/**
	 * Test replacement of dynamic item values with a format error
	 * 
	 * @see issue #115
	 */
	@SuppressWarnings("unused")
	@Test
	public void testReplaceDynamicValuesFormatError() {

		String testString = "Hello <itemvalue>txtname!";
		String expectedString = "Hello Anna!";

		TestPlugin applicationPlugin = new TestPlugin();

		// prepare data
		logger.info("[TestAbstractPlugin] setup test data...");
		documentContext = new ItemCollection();
		documentContext.replaceItemValue("txtName", "Anna");

		String resultString=null;
		try {
			resultString = applicationPlugin.replaceDynamicValues(testString, documentContext);
			Assert.fail();
		} catch (PluginException e) {
			// expected
			Assert.assertNull(resultString);
		}
		
		// test wrong embeded tags... 

	}
	

	/**
	 * Test format string:
	 * 
	 * <code>
	 * 
	 * <itemvalue format="EEEE, d. MMMM yyyy">datdate</itemvalue>
	 * 
	 * </code>
	 * @throws PluginException 
	 * 
	 */
	@Test
	public void testDateFormat() throws PluginException {

		String testString = "The Date is: <itemvalue format=\"EEEE, d. MMMM yyyy\" locale=\"de_DE\">datdate</itemvalue>.";
		String expectedString = "The Date is: Sonntag, 27. April 2014.";

		TestPlugin applicationPlugin = new TestPlugin();

		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestHisotryPlugin] setup test data...");

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DATE, 27);
		cal.set(Calendar.YEAR, 2014);
		cal.set(Calendar.MONTH, 3);

		documentContext.replaceItemValue("datDate", cal.getTime());

		String resultString = applicationPlugin.replaceDynamicValues(testString, documentContext);

		Assert.assertEquals(expectedString, resultString);

	}

	@Test
	public void testDateFormatEN() throws PluginException {

		String testString = "The Date is: <itemvalue locale=\"en_EN\" format=\"EEEE, d. MMMM yyyy\">datdate</itemvalue>.";
		String expectedString = "The Date is: Sunday, 27. April 2014.";

		TestPlugin applicationPlugin = new TestPlugin();

		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestHisotryPlugin] setup test data...");

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DATE, 27);
		cal.set(Calendar.YEAR, 2014);
		cal.set(Calendar.MONTH, 3);

		documentContext.replaceItemValue("datDate", cal.getTime());

		String resultString = applicationPlugin.replaceDynamicValues(testString, documentContext);

		Assert.assertEquals(expectedString, resultString);

	}

	/**
	 * Test format string:
	 * 
	 * <code>
	 * 
	 * <itemvalue separator="/">_numbers</itemvalue>
	 * 
	 * </code>
	 * @throws PluginException 
	 * 
	 */
	@Test
	public void testMultiValueFormat() throws PluginException {

		String testString = "The Valuelist is: <itemvalue separator=\"/\">_numbers</itemvalue>.";
		String expectedString = "The Valuelist is: 1/20/300.";

		TestPlugin applicationPlugin = new TestPlugin();

		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestHisotryPlugin] setup test data...");

		Vector<Integer> value = new Vector<Integer>();
		value.add(1);
		value.add(20);
		value.add(300);
		documentContext.replaceItemValue("_numbers", value);

		String resultString = applicationPlugin.replaceDynamicValues(testString, documentContext);

		Assert.assertEquals(expectedString, resultString);

	}

	/**
	 * This is a test plugin extending the AbstractPlugion to be used for
	 * several tests in this jUnit test only
	 * 
	 * @author rsoika
	 *
	 */
	class TestPlugin extends AbstractPlugin {

		@Override
		public ItemCollection run(ItemCollection documentContext, ItemCollection documentActivity) throws PluginException {
			return documentContext;
		}

		@Override
		public void close(boolean rollbackTransaction) throws PluginException {
			// no op
		}

	}
}
