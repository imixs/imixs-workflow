/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Calendar;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Test the WorkflowService method 'adaptText'
 * 
 * The class test the TextAdapter as also the ForEachAdapter classes
 * 
 * @author rsoika
 * 
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class TestAdaptText {
	private final static Logger logger = Logger.getLogger(TestAdaptText.class.getName());

	protected MockWorkflowEnvironment workflowEngine;
	ItemCollection workitem;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {
		workflowEngine = new MockWorkflowEnvironment();
		workflowEngine.setUp();
		workflowEngine.loadBPMNModelFromFile("/bpmn/TestWorkflowService.bpmn");
		workitem = workflowEngine.getDocumentService().load("W0000-00001");
		workitem.model("1.0.0").task(100);
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
			resultString = this.workflowEngine.workflowService.adaptText(testString, documentContext);

			assertEquals(expectedString, resultString);
		} catch (PluginException e) {
			fail(e.getMessage());
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
			resultString = this.workflowEngine.workflowService.adaptText(testString, documentContext);
			assertNotNull(resultString);
			assertEquals(testString, resultString);
		} catch (PluginException e) {
			fail();
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

		String resultString = this.workflowEngine.workflowService.adaptText(testString, documentContext);

		assertEquals(expectedString, resultString);

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

		String resultString = this.workflowEngine.workflowService.adaptText(testString, documentContext);

		assertEquals(expectedString, resultString);

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

		String resultString = this.workflowEngine.workflowService.adaptText(testString, documentContext);

		assertEquals(expectedString, resultString);

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

		String resultString = this.workflowEngine.workflowService.adaptText(testString, documentContext);

		assertEquals(expectedString, resultString);

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

		String resultString = this.workflowEngine.workflowService.adaptText(testString, documentContext);

		// we expect that only the first value is given, because no separator was
		// defined.
		assertEquals(expectedString, resultString);

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

		String resultString = this.workflowEngine.workflowService.adaptText(testString, documentContext);

		assertEquals(expectedStringLast, resultString);

		// test first....
		testString = "The value list is: <itemvalue position=\"FIRST\">_numbers</itemvalue>.";
		String expectedStringFirst = "The value list is: 1.";

		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestAdaptText] setup test data...");

		documentContext.replaceItemValue("_numbers", values);

		resultString = this.workflowEngine.workflowService.adaptText(testString, documentContext);

		assertEquals(expectedStringFirst, resultString);

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

		String resultString = this.workflowEngine.workflowService.adaptText(testString, documentContext);

		assertEquals(expectedStringLast, resultString);

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

		String resultString = this.workflowEngine.workflowService.adaptText(testString, documentContext);

		assertEquals(expectedStringLast, resultString);

	}

}
