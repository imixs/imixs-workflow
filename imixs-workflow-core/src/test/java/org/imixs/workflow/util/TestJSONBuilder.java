package org.imixs.workflow.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.junit.Test;

import org.junit.Assert;

/**
 * Test class test the Imixs JSONBuilder class to build a JSON String form an
 * ItemCollection.
 * 
 * 
 * @author rsoika
 */
public class TestJSONBuilder {
	private static Logger logger = Logger.getLogger(TestJSONBuilder.class.getName());

	/**
	 * Expected Output: <code>
	 *  {
		"item":[
				{"name":"$isauthor","value":{"@type":"xs:boolean","$":"true"}},
				{"name":"$readaccess","value":{"@type":"xs:string","$":"Anna"}},
				{"name":"count","value":{"@type":"xs:int","$":"42"}},
				{"name":"amount","value":{"@type":"xs:double","$":"100.99"}},
				{"name":"txtlog","value":[
					{"@type":"xs:string","$":"A"},
					{"@type":"xs:string","$":"B"},
					{"@type":"xs:string","$":"C"}]
				},
				{"name":"$eventid","value":{"@type":"xs:int","$":"0"}}
			]
		}
	 * </code>
	 * 
	 * @throws ParseException
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void testSimple() throws ParseException {

		ItemCollection workitem = new ItemCollection();
		workitem.setItemValue("$isauthor", true);
		workitem.setItemValue("$readaccess", "Anna");

		workitem.setItemValue("count", 42);
		workitem.setItemValue("amount", new Double(100.99));
		workitem.appendItemValue("txtlog", "A");
		workitem.appendItemValue("txtlog", "B");
		workitem.appendItemValue("txtlog", "C");

		String jsonResult = null;
		try {
			jsonResult = ImixsJSONBuilder.build(workitem);
			Assert.assertNotNull(jsonResult);
		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();
			Assert.fail();
		}

		// now convert the json string back using the Imixs JSONParser
		InputStream jsonStream = new ByteArrayInputStream(jsonResult.getBytes());
		try { 
			ItemCollection testItemCol = JSONParser.parseWorkitem(jsonStream, "UTF-8");
			Assert.assertNotNull(testItemCol);

			// convert data....
			Assert.assertEquals(workitem.getItemValueBoolean("$isauthor"),
					testItemCol.getItemValueBoolean("$isauthor"));
			Assert.assertEquals(workitem.getItemValueString("$readaccess"),
					testItemCol.getItemValueString("$readaccess"));

			List valueList = testItemCol.getItemValue("count");
			Assert.assertTrue(valueList.get(0) instanceof Integer);
			Assert.assertEquals(workitem.getItemValueInteger("count"), testItemCol.getItemValueInteger("count"),0);
			Assert.assertEquals(workitem.getItemValueDouble("amount"), testItemCol.getItemValueDouble("amount"),0);

			// test value list
			valueList = testItemCol.getItemValue("txtlog");
			Assert.assertEquals(3, valueList.size());
			Assert.assertEquals("C", valueList.get(2));

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		}

		logger.info(jsonResult);

	}

}
