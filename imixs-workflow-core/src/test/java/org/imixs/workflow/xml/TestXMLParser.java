package org.imixs.workflow.xml;

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
	public void testAttributes() {

		String test = "<date field=\"a\"   number=1 />";

		Map<String, String> result = XMLParser.parseAttributes(test);

		System.out.println(result);

		Assert.assertNotNull(result);

		Assert.assertTrue(result.containsKey("field"));
		Assert.assertTrue(result.containsKey("number"));

		Assert.assertEquals("a", result.get("field"));
		Assert.assertEquals("1", result.get("number"));

	}

}
