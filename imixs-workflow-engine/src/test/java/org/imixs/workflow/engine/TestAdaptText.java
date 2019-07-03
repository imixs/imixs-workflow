package org.imixs.workflow.engine;

import java.util.Calendar;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test the WorkflowService method 'adaptText'
 * 
 * @author rsoika
 * 
 */
public class TestAdaptText {
	public ItemCollection documentContext;
	public ItemCollection documentActivity;
	private final static Logger logger = Logger.getLogger(TestAdaptText.class.getName());

	protected WorkflowMockEnvironment workflowMockEnvironment;

	@Before
	public void setUp() throws PluginException, ModelException {

		workflowMockEnvironment = new WorkflowMockEnvironment();
		workflowMockEnvironment.setup();

		//workflowMockEnvironment.loadModel("/bpmn/TestWorkflowService.bpmn");

	}
	

	/**
	 * Test replacement of dynamic item values
	 * 
	 * <itemvalue>xxx</itemvalue>
	 * 
	 * @throws PluginException
	 * 
	 */
	@Test
	public void testReplaceDynamicValues() throws PluginException {

		String testString = "Hello <itemvalue>txtname</itemvalue>!";
		String expectedString = "Hello Anna!";

	
		// prepare data
		logger.info("[TestAbstractPlugin] setup test data...");
		documentContext = new ItemCollection();
		documentContext.replaceItemValue("txtName", "Anna");

		String resultString = workflowMockEnvironment.getWorkflowService().adaptText(testString, documentContext);

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

	
		// prepare data
		logger.info("[TestAbstractPlugin] setup test data...");
		documentContext = new ItemCollection();
		documentContext.replaceItemValue("txtName", "Anna");

		String resultString = null;
		try {
			resultString = workflowMockEnvironment.getWorkflowService().adaptText(testString, documentContext);
			Assert.assertNotNull(resultString);
			Assert.assertEquals(testString, resultString);
		} catch (PluginException e) {
			Assert.fail();
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
	 * 
	 * @throws PluginException
	 * 
	 */
	@Test
	public void testDateFormat() throws PluginException {

		String testString = "The Date is: <itemvalue format=\"EEEE, d. MMMM yyyy\" locale=\"de_DE\">datdate</itemvalue>.";
		String expectedString = "The Date is: Sonntag, 27. April 2014.";

			// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestHisotryPlugin] setup test data...");

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DATE, 27);
		cal.set(Calendar.YEAR, 2014);
		cal.set(Calendar.MONTH, 3);

		documentContext.replaceItemValue("datDate", cal.getTime());

		String resultString = workflowMockEnvironment.getWorkflowService().adaptText(testString, documentContext);

		Assert.assertEquals(expectedString, resultString);

	}

	@Test
	public void testDateFormatEN() throws PluginException {

		String testString = "The Date is: <itemvalue locale=\"en_EN\" format=\"EEEE, d. MMMM yyyy\">datdate</itemvalue>.";
		String expectedString = "The Date is: Sunday, 27. April 2014.";

		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestHisotryPlugin] setup test data...");

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DATE, 27);
		cal.set(Calendar.YEAR, 2014);
		cal.set(Calendar.MONTH, 3);

		documentContext.replaceItemValue("datDate", cal.getTime());

		String resultString =workflowMockEnvironment.getWorkflowService().adaptText(testString, documentContext);

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
	 * @throws PluginException
	 * 
	 */
	@Test
	public void testMultiValueFormat() throws PluginException {

		String testString = "The Valuelist is: <itemvalue separator=\"/\">_numbers</itemvalue>.";
		String expectedString = "The Valuelist is: 1/20/300.";

	
		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestHisotryPlugin] setup test data...");

		Vector<Integer> value = new Vector<Integer>();
		value.add(1);
		value.add(20);
		value.add(300);
		documentContext.replaceItemValue("_numbers", value);

		String resultString = workflowMockEnvironment.getWorkflowService().adaptText(testString, documentContext);

		Assert.assertEquals(expectedString, resultString);

	}

	/**
	 * Test format string of multi value with out separator:
	 * 
	 * <code>
	 * 
	 * <itemvalue>_numbers</itemvalue>
	 * 
	 * </code>
	 * 
	 * @throws PluginException
	 * 
	 */
	@Test
	public void testMultiValueWithNoSeparator() throws PluginException {

		String testString = "The Valuelist is: <itemvalue>_numbers</itemvalue>.";
		String expectedString = "The Valuelist is: 1.";

		
		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestHisotryPlugin] setup test data...");

		Vector<Integer> value = new Vector<Integer>();
		value.add(1);
		value.add(20);
		value.add(300);
		documentContext.replaceItemValue("_numbers", value);

		String resultString = workflowMockEnvironment.getWorkflowService().adaptText(testString, documentContext);

		// we expect that only the first value is given, because no separator was
		// defined.
		Assert.assertEquals(expectedString, resultString);

	}

	/**
	 * Test position tag:
	 * 
	 * <code>
	 * 
	 * <itemvalue position="last">_numbers</itemvalue>
	 * 
	 * </code>
	 * 
	 * @throws PluginException
	 * 
	 */
	@Test
	public void testMultiValuePosition() throws PluginException {

		String testString = "The Valuelist is: <itemvalue position=\"LAST\">_numbers</itemvalue>.";
		String expectedStringLast = "The Valuelist is: 300.";

		
		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestHisotryPlugin] setup test data...");

		Vector<Integer> values = new Vector<Integer>();
		values.add(1);
		values.add(20);
		values.add(300);
		documentContext.replaceItemValue("_numbers", values);

		String resultString = workflowMockEnvironment.getWorkflowService().adaptText(testString, documentContext);

		Assert.assertEquals(expectedStringLast, resultString);

		// test first....
		testString = "The Valuelist is: <itemvalue position=\"FIRST\">_numbers</itemvalue>.";
		String expectedStringFirst = "The Valuelist is: 1.";

		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestHisotryPlugin] setup test data...");

		documentContext.replaceItemValue("_numbers", values);

		resultString = workflowMockEnvironment.getWorkflowService().adaptText(testString, documentContext);

		Assert.assertEquals(expectedStringFirst, resultString);

	}

	/**
	 * This is a test plugin extending the AbstractPlugion to be used for several
	 * tests in this jUnit test only
	 * 
	 * @author rsoika
	 *
	 */
	class TestPlugin extends AbstractPlugin {

		@Override
		public ItemCollection run(ItemCollection documentContext, ItemCollection documentActivity)
				throws PluginException {
			return documentContext;
		}

		@Override
		public void close(boolean rollbackTransaction) throws PluginException {
			// no op
		}

	}
}
