package org.imixs.workflow.xml;

import java.util.List;
import java.util.Map;

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

		String test = "<date field=\"a\"   number=1 />";

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

		String test = "<date field=\"a\"   number=1 />";

		String result = XMLParser.findAttribute(test,"field");
		Assert.assertNotNull(result);
		Assert.assertEquals("a", result);

		result = XMLParser.findAttribute(test,"number");
		Assert.assertNotNull(result);
		Assert.assertEquals("1", result);
	}
	
	

	
	
	
	@Test
	public void testSingelTags() {

		// test simple tag <date>..</date>
		String test = "abc <date field=\"a\"   number=1 >def</date>ghi";
		List<String> result = XMLParser.findTags(test,"date");
		System.out.println(result);
		Assert.assertNotNull(result);
		Assert.assertEquals("<date field=\"a\"   number=1 >def</date>", result.get(0));

		// test simple singel tag <date/>
		test = "abc <date field=\"a\"   number=1 />def";
		result = XMLParser.findTags(test,"date");
		System.out.println(result);
		Assert.assertNotNull(result);
		Assert.assertEquals("<date field=\"a\"   number=1 />", result.get(0));

		
		
		// test simple singel tag lowercase <DATE/>
		test = "abc <DATE field=\"a\"   number=1 />def";
		result = XMLParser.findTags(test,"date");
		System.out.println(result);
		Assert.assertNotNull(result);
		Assert.assertEquals("<DATE field=\"a\"   number=1 />", result.get(0));

		
		// test simple tag <DATE>..</DATE>
				 test = "abc <DATE field=\"a\"   number=1 >def</DATE>ghi";
			 result = XMLParser.findTags(test,"date");
				System.out.println(result);
				Assert.assertNotNull(result);
				Assert.assertEquals("<DATE field=\"a\"   number=1 >def</DATE>", result.get(0));

	}

	@Test
	public void testMultiTags() {

		// test simple tag <date>..</date> <date />
		String test = "abc <date field=\"a\"   number=1 >def</date>ghi\n" + "<date>abc</date>";
		List<String> result = XMLParser.findTags(test,"date");
		System.out.println(result);
		Assert.assertNotNull(result);
		Assert.assertEquals(2, result.size());
		Assert.assertEquals("<date field=\"a\"   number=1 >def</date>", result.get(0));
		Assert.assertEquals("<date>abc</date>", result.get(1));

		// test complex list of tags
		test = "abc <date field=\"a\"   number=1 >def</date>ghi\n" + "<item name=\"test\">value</item>"
				+ "<date>abc</date>" + "<date field=\"abc\">xyz</date>";
		result = XMLParser.findTags(test,"date");
		System.out.println(result);
		Assert.assertNotNull(result);
		Assert.assertEquals(3, result.size());
		Assert.assertEquals("<date field=\"a\"   number=1 >def</date>", result.get(0));
		Assert.assertEquals("<date>abc</date>", result.get(1));
		Assert.assertEquals("<date field=\"abc\">xyz</date>", result.get(2));

	}

	
	
	@Test
	public void testFindTagValue() {

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
	
}
