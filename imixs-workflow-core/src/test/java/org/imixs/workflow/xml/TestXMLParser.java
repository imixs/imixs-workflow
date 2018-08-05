package org.imixs.workflow.xml;

import java.util.List;
import java.util.Map;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.util.XMLParser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for XMLParser
 * 
 * @author rsoika
 * 
 */
public class TestXMLParser {

	@Test
	public void testAllAttributes() {

		String test = "<date field=\"a\"   number='1' />";

		Map<String, String> result = XMLParser.findAttributes(test);

		System.out.println(result);

		Assert.assertNotNull(result);

		Assert.assertTrue(result.containsKey("field"));
		Assert.assertTrue(result.containsKey("number"));

		Assert.assertEquals("a", result.get("field"));
		Assert.assertEquals("1", result.get("number"));
	}

	@Test
	public void testSingleAttribute() {

		String test = "<date field=\"a\"   number='1' />";

		String result = XMLParser.findAttribute(test, "field");
		Assert.assertNotNull(result);
		Assert.assertEquals("a", result);

		result = XMLParser.findAttribute(test, "number");
		Assert.assertNotNull(result);
		Assert.assertEquals("1", result);
	}

	@Test
	public void testAttributeWithDoubleQuotation() {

		String test = "<item name=\"txtName\" type=\"x\">true</item>";

		// verify attribute
		String result = XMLParser.findAttribute(test, "name");
		Assert.assertNotNull(result);
		Assert.assertEquals("txtName", result);

		result = XMLParser.findAttribute(test, "type");
		Assert.assertNotNull(result);
		Assert.assertEquals("x", result);
	}

	@Test
	public void testAttributeWithSingleQuotation() {

		String test = "<item name='txtName' type='x'>true</item>";

		// verify attribute
		String result = XMLParser.findAttribute(test, "name");
		Assert.assertNotNull(result);
		Assert.assertEquals("txtName", result);

		result = XMLParser.findAttribute(test, "type");
		Assert.assertNotNull(result);
		Assert.assertEquals("x", result);
	}

	@Test
	public void testSingleAttributeUppercase() {
		String test = "<date FIELD=\"a\"   nUMBER='1' />";
		String result = XMLParser.findAttribute(test, "FIELD");
		Assert.assertNotNull(result);
		Assert.assertEquals("a", result);

		result = XMLParser.findAttribute(test, "nUMBER");
		Assert.assertNotNull(result);
		Assert.assertEquals("1", result);
	}

	/**
	 * issue #201
	 */
	@Test
	public void testSingleAttributeWithDash() {

		String test = "<format aggreation-type=\"SUM\">some data</format>";

		// verify attribute
		String result = XMLParser.findAttribute(test, "aggreation-type");
		Assert.assertNotNull(result);
		Assert.assertEquals("SUM", result);

		// verify value
		List<String> values = XMLParser.findTagValues(test, "format");
		Assert.assertNotNull(values);
		Assert.assertEquals(1, values.size());
		Assert.assertEquals("some data", values.get(0));

	}

	@Test
	public void testBooleanAttribute() {

		String test = "<item name=\"txtName\" type=\"boolean\">true</item>";

		// verify attribute
		String result = XMLParser.findAttribute(test, "name");
		Assert.assertNotNull(result);
		Assert.assertEquals("txtName", result);

		result = XMLParser.findAttribute(test, "type");
		Assert.assertNotNull(result);
		Assert.assertEquals("boolean", result);

		// verify attribute
		List<String> values = XMLParser.findTagValues(test, "item");
		Assert.assertNotNull(values);
		Assert.assertEquals(1, values.size());
		Assert.assertEquals("true", values.get(0));

	}

	@Test
	public void testSingelTags() {

		// test simple tag <date>..</date>
		String test = "abc <date field=\"a\"   number=1 >def</date>ghi";
		List<String> result = XMLParser.findTags(test, "date");
		System.out.println(result);
		Assert.assertNotNull(result);
		Assert.assertEquals("<date field=\"a\"   number=1 >def</date>", result.get(0));

		// test simple singel tag <date/>
		test = "abc <date field=\"a\"   number=1 />def";
		result = XMLParser.findTags(test, "date");
		System.out.println(result);
		Assert.assertNotNull(result);
		Assert.assertEquals("<date field=\"a\"   number=1 />", result.get(0));

		// test simple singel tag lowercase <DATE/>
		test = "abc <DATE field=\"a\"   number=1 />def";
		result = XMLParser.findTags(test, "date");
		System.out.println(result);
		Assert.assertNotNull(result);
		Assert.assertEquals("<DATE field=\"a\"   number=1 />", result.get(0));

		// test simple tag <DATE>..</DATE>
		test = "abc <DATE field=\"a\"   number=1 >def</DATE>ghi";
		result = XMLParser.findTags(test, "date");
		System.out.println(result);
		Assert.assertNotNull(result);
		Assert.assertEquals("<DATE field=\"a\"   number=1 >def</DATE>", result.get(0));

	}

	@Test
	public void testMultiTags() {

		// test simple tag <date>..</date> <date />
		String test = "abc <date field=\"a\"   number=1 >def</date>ghi\n" + "<date>abc</date>";
		List<String> result = XMLParser.findTags(test, "date");
		System.out.println(result);
		Assert.assertNotNull(result);
		Assert.assertEquals(2, result.size());
		Assert.assertEquals("<date field=\"a\"   number=1 >def</date>", result.get(0));
		Assert.assertEquals("<date>abc</date>", result.get(1));

		// test complex list of tags
		test = "abc <date field=\"a\"   number=1 >def</date>ghi\n" + "<item name=\"test\">value</item>"
				+ "<date>abc</date>" + "<date field=\"abc\">xyz</date>";
		result = XMLParser.findTags(test, "date");
		System.out.println(result);
		Assert.assertNotNull(result);
		Assert.assertEquals(3, result.size());
		Assert.assertEquals("<date field=\"a\"   number=1 >def</date>", result.get(0));
		Assert.assertEquals("<date>abc</date>", result.get(1));
		Assert.assertEquals("<date field=\"abc\">xyz</date>", result.get(2));

	}

	@Test
	public void testFindTagValues() {

		String test = "<date field=\"a\"   number=1 >2016-12-31</date>";

		List<String> result = XMLParser.findTagValues(test, "date");
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals("2016-12-31", result.get(0));

		test = "<date>2016-12-31</date>  <date>2016-11-30</date>";

		result = XMLParser.findTagValues(test, "date");
		Assert.assertNotNull(result);
		Assert.assertEquals(2, result.size());
		Assert.assertEquals("2016-12-31", result.get(0));
		Assert.assertEquals("2016-11-30", result.get(1));

	}

	/**
	 * Find the value of a single tag
	 */
	@Test
	public void testFindTag1() {
		String test = "<itemvalue>namcreator</itemvalue>";
		String result = XMLParser.findTagValue(test, "itemvalue");
		Assert.assertNotNull(result);
		Assert.assertEquals("namcreator", result);

	}

	/**
	 * Find the value of a single tag
	 */
	@Test
	public void testFindTag2() {

		String test = "<itemvalue name=\"txtname\">anna</itemvalue>";
		String result = XMLParser.findTagValue(test, "itemvalue");
		Assert.assertNotNull(result);
		Assert.assertEquals("anna", result);

	}

	/**
	 * Find the value of a single tag
	 */
	@Test
	public void testFindTag3() {

		String test = "<date field=\"a\"   number=1 >2016-12-31</date>";

		String result = XMLParser.findTagValue(test, "date");
		Assert.assertNotNull(result);
		Assert.assertEquals("2016-12-31", result);

		test = "<date>2016-12-31</date>  <date>2016-11-30</date>";

		result = XMLParser.findTagValue(test, "date");
		Assert.assertNotNull(result);
		// we expect the first value
		Assert.assertEquals("2016-12-31", result);

	}

	/**
	 * Test a typical string used by the evaluateWorkflowResutl method.
	 ***/
	@Test
	public void testItemXMLContent() {

		// create test result.....
		String activityResult = "<modelversion>1.0.0</modelversion>" + "<processid>1000</processid>"
				+ "<activityid>10</activityid>" + "<items>namTeam</items>";

		try {
			ItemCollection result = XMLParser.parseItemStructure(activityResult);

			Assert.assertEquals("1.0.0", result.getItemValueString("modelversion"));
			Assert.assertEquals("1000", result.getItemValueString("processID"));
			Assert.assertEquals("10", result.getItemValueString("activityID"));
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}
	}

	/**
	 * Test a special xml string containing whitespace
	 ***/
	@Test
	public void testItemXMLContentWhitespace() {
		// create test result.....
		String activityResult = "\n" + 
				"    <processid>2000</processid>\n" + 
				"    <items>_subject|_parentsubject,$workflowsummary|_parentworkflowsummary</items>\n" + 
				"    <activityid>100</activityid>";
		try {
			ItemCollection result = XMLParser.parseItemStructure(activityResult);
			String s = result.getItemValueString("processid");
			Assert.assertFalse(s.isEmpty());
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
}
