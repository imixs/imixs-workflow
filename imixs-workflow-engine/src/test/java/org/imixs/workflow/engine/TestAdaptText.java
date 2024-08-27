package org.imixs.workflow.engine;

import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.List;
import java.util.Vector;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

/**
 * Test the WorkflowService method 'adaptText'
 * 
 * The class test the TextAdapter as also the ForEachAdapter classes
 * 
 * @author rsoika
 * 
 */
@ExtendWith(MockitoExtension.class)
public class TestAdaptText extends AbstractWorkflowServiceTest {

	@Mock
	private WorkflowService workflowService;

	@Override
	public void setUp() throws PluginException {
		super.setUp();
		// AdaptText
		when(workflowService.adaptText(Mockito.anyString(), Mockito.any(ItemCollection.class)))
				.thenAnswer(new Answer<String>() {
					@Override
					public String answer(InvocationOnMock invocation) throws Throwable {

						Object[] args = invocation.getArguments();
						String text = (String) args[0];
						ItemCollection document = (ItemCollection) args[1];

						TextEvent textEvent = new TextEvent(text, document);

						// for-each adapter
						TextForEachAdapter tfea = new TextForEachAdapter();
						tfea.onEvent(textEvent);

						// ItemValue adapter
						TextItemValueAdapter tiva = new TextItemValueAdapter();
						tiva.onEvent(textEvent);

						return textEvent.getText();
					}
				});

		when(workflowService.adaptTextList(Mockito.anyString(), Mockito.any(ItemCollection.class)))
				.thenAnswer(new Answer<List<String>>() {
					@Override
					public List<String> answer(InvocationOnMock invocation) throws Throwable, PluginException {

						Object[] args = invocation.getArguments();
						String text = (String) args[0];
						ItemCollection document = (ItemCollection) args[1];

						TextEvent textEvent = new TextEvent(text, document);

						TextItemValueAdapter tiva = new TextItemValueAdapter();
						tiva.onEvent(textEvent);

						return textEvent.getTextList();
					}
				});

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
	public void testReplaceDynamicValues() {

		String testString = "Hello <itemvalue>txtname</itemvalue>!";
		String expectedString = "Hello Anna!";

		// prepare data
		logger.info("[TestAdaptText] setup test data...");
		ItemCollection documentContext = new ItemCollection();
		documentContext.replaceItemValue("txtName", "Anna");

		String resultString;
		try {
			resultString = this.workflowService.adaptText(testString, documentContext);

			Assert.assertEquals(expectedString, resultString);
		} catch (PluginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		logger.info("[TestAdaptText] setup test data...");
		ItemCollection documentContext = new ItemCollection();
		documentContext.replaceItemValue("txtName", "Anna");

		String resultString = null;
		try {
			resultString = this.workflowService.adaptText(testString, documentContext);
			Assert.assertNotNull(resultString);
			Assert.assertEquals(testString, resultString);
		} catch (PluginException e) {
			Assert.fail();
		}

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
		ItemCollection documentContext = new ItemCollection();
		logger.info("[TestAdaptText] setup test data...");

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DATE, 27);
		cal.set(Calendar.YEAR, 2014);
		cal.set(Calendar.MONTH, 3);

		documentContext.replaceItemValue("datDate", cal.getTime());

		String resultString = this.workflowService.adaptText(testString, documentContext);

		Assert.assertEquals(expectedString, resultString);

	}

	@Test
	public void testDateFormatEN() throws PluginException {

		String testString = "The Date is: <itemvalue locale=\"en_EN\" format=\"EEEE, d. MMMM yyyy\">datdate</itemvalue>.";
		String expectedString = "The Date is: Sunday, 27. April 2014.";

		// prepare data
		ItemCollection documentContext = new ItemCollection();
		logger.info("[TestAdaptText] setup test data...");

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DATE, 27);
		cal.set(Calendar.YEAR, 2014);
		cal.set(Calendar.MONTH, 3);

		documentContext.replaceItemValue("datDate", cal.getTime());

		String resultString = this.workflowService.adaptText(testString, documentContext);

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

		String testString = "The value list is: <itemvalue separator=\"/\">_numbers</itemvalue>.";
		String expectedString = "The value list is: 1/20/300.";

		// prepare data
		ItemCollection documentContext = new ItemCollection();
		logger.info("[TestAdaptText] setup test data...");

		Vector<Integer> value = new Vector<Integer>();
		value.add(1);
		value.add(20);
		value.add(300);
		documentContext.replaceItemValue("_numbers", value);

		String resultString = this.workflowService.adaptText(testString, documentContext);

		Assert.assertEquals(expectedString, resultString);

	}

	/**
	 * Test format number string:
	 * 
	 * <code>
	 * 
	 * <itemvalue format="#,###,##0.00" locale="de_DE">price</itemvalue>
	 * 
	 * </code>
	 * 
	 * @throws PluginException
	 * 
	 */
	@Test
	public void testNumberFormat() throws PluginException {

		String testString = "The price is: <itemvalue format=\"#,###,##0.00\" locale=\"de_DE\">price</itemvalue> €.";
		String expectedString = "The price is: 1.199,99 €.";

		// prepare data
		ItemCollection documentContext = new ItemCollection();

		documentContext.replaceItemValue("price", Float.valueOf((float) 1199.99));

		String resultString = this.workflowService.adaptText(testString, documentContext);

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

		String testString = "The value list is: <itemvalue>_numbers</itemvalue>.";
		String expectedString = "The value list is: 1.";

		// prepare data
		ItemCollection documentContext = new ItemCollection();
		logger.info("[TestAdaptText] setup test data...");

		Vector<Integer> value = new Vector<Integer>();
		value.add(1);
		value.add(20);
		value.add(300);
		documentContext.replaceItemValue("_numbers", value);

		String resultString = this.workflowService.adaptText(testString, documentContext);

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

		String testString = "The value list is: <itemvalue position=\"LAST\">_numbers</itemvalue>.";
		String expectedStringLast = "The value list is: 300.";

		// prepare data
		ItemCollection documentContext = new ItemCollection();
		logger.info("[TestAdaptText] setup test data...");

		Vector<Integer> values = new Vector<Integer>();
		values.add(1);
		values.add(20);
		values.add(300);
		documentContext.replaceItemValue("_numbers", values);

		String resultString = this.workflowService.adaptText(testString, documentContext);

		Assert.assertEquals(expectedStringLast, resultString);

		// test first....
		testString = "The value list is: <itemvalue position=\"FIRST\">_numbers</itemvalue>.";
		String expectedStringFirst = "The value list is: 1.";

		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestAdaptText] setup test data...");

		documentContext.replaceItemValue("_numbers", values);

		resultString = this.workflowService.adaptText(testString, documentContext);

		Assert.assertEquals(expectedStringFirst, resultString);

	}

	/**
	 * Test simple value list :
	 * 
	 * <pre>
	 * {@code
	    <for-each item="_partid">
	        Order-No: <itemvalue>_orderid</itemvalue> - Part ID: <itemvalue>_partid</itemvalue><br />
	    </for-each>  
	   }
	 * </pre>
	 * 
	 * @throws PluginException
	 * 
	 */
	@Test
	public void testForEachSimpleValueList() throws PluginException {

		String testString = "<for-each item=\"_partid\">Order-No: <itemvalue>_orderid</itemvalue> - Part ID: <itemvalue>_partid</itemvalue><br /></for-each>";
		String expectedStringLast = "Order-No: 111222 - Part ID: A123<br />Order-No: 111222 - Part ID: B456<br />";

		// prepare data
		ItemCollection documentContext = new ItemCollection();
		documentContext.setItemValue("_orderid", "111222");
		documentContext.appendItemValue("_partid", "A123");
		documentContext.appendItemValue("_partid", "B456");

		String resultString = workflowService.adaptText(testString, documentContext);

		Assert.assertEquals(expectedStringLast, resultString);

	}

	/**
	 * Test the child item value tag:
	 * 
	 * <pre>
	 * {@code
	<for-each childitem="_childs">
	   <itemvalue>_orderid</itemvalue>: <itemvalue>_amount</itemvalue>
	</for-each>  
	   }
	 * </pre>
	 * 
	 * @throws PluginException
	 * 
	 */
	@Test
	public void testForEachEmbeddedChildItemValue() throws PluginException {

		String testString = "<for-each item=\"_childs\">Order ID: <itemvalue>_orderid</itemvalue>: <itemvalue>_amount</itemvalue><br /></for-each>";
		String expectedStringLast = "Order ID: A123: 50.55<br />Order ID: B456: 1500000.0<br />";

		// prepare data
		ItemCollection documentContext = new ItemCollection();
		// create 1st child
		ItemCollection child = new ItemCollection();
		child.setItemValue("_orderid", "A123");
		child.setItemValue("_amount", 50.55);
		documentContext.appendItemValue("_childs", child.getAllItems());
		// create 2nd child
		child = new ItemCollection();
		child.setItemValue("_orderid", "B456");
		child.setItemValue("_amount", 1500000.00);
		documentContext.appendItemValue("_childs", child.getAllItems());
		// create a fake value which should be ignored
		documentContext.replaceItemValue("_orderid", "not used");

		String resultString = workflowService.adaptText(testString, documentContext);

		Assert.assertEquals(expectedStringLast, resultString);

	}

}
