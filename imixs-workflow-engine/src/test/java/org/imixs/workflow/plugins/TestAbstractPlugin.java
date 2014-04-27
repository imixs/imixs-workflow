package org.imixs.workflow.plugins;

import java.util.Calendar;
import java.util.Vector;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.plugins.jee.HistoryPlugin;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the replaceDynamicValues method of abstractPugin
 * 
 * @author rsoika
 * 
 */
public class TestAbstractPlugin extends HistoryPlugin {

	private final static Logger logger = Logger
			.getLogger(TestAbstractPlugin.class.getName());

	@Before
	public void setup() throws PluginException {

	}

	/**
	 * Test format string:
	 * 
	 * <code>
	 * 
	 * <itemvalue format="EEEE, d. MMMM yyyy">datdate</itemvalue>
	 * 
	 * </code>
	 * 
	 */
	@Test
	public void testDateFormat() {

		String testString = "The Date is: <itemvalue format=\"EEEE, d. MMMM yyyy\" locale=\"de_DE\">datdate</itemvalue>.";
		String expectedString = "The Date is: Sonntag, 27. April 2014.";

		ApplicationPlugin applicationPlugin = new ApplicationPlugin();

	
		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestHisotryPlugin] setup test data...");

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DATE, 27);
		cal.set(Calendar.YEAR, 2014);
		cal.set(Calendar.MONTH, 3);

		documentContext.replaceItemValue("datDate", cal.getTime());

		String resultString = applicationPlugin.replaceDynamicValues(
				testString, documentContext);

		Assert.assertEquals(expectedString, resultString);
		
		
	}
	
	
	@Test
	public void testDateFormatEN() {

		String testString = "The Date is: <itemvalue locale=\"en_EN\" format=\"EEEE, d. MMMM yyyy\">datdate</itemvalue>.";
		String expectedString = "The Date is: Sunday, 27. April 2014.";

		ApplicationPlugin applicationPlugin = new ApplicationPlugin();

		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestHisotryPlugin] setup test data...");

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DATE, 27);
		cal.set(Calendar.YEAR, 2014);
		cal.set(Calendar.MONTH, 3);

		documentContext.replaceItemValue("datDate", cal.getTime());

		String resultString = applicationPlugin.replaceDynamicValues(
				testString, documentContext);

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
	 * 
	 */
	@Test
	public void testMultiValueFormat() {

	
		String testString="The Valuelist is: <itemvalue separator=\"/\">_numbers</itemvalue>.";
		String expectedString = "The Valuelist is: 1/20/300.";

		ApplicationPlugin applicationPlugin = new ApplicationPlugin();

	
		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestHisotryPlugin] setup test data...");

	
		Vector<Integer> value=new Vector<Integer>();
		value.add(1);
		value.add(20);
		value.add(300);
		documentContext.replaceItemValue("_numbers",value);

		String resultString = applicationPlugin.replaceDynamicValues(
				testString, documentContext);

		Assert.assertEquals(expectedString, resultString);
		
		
	}
	
	

}
