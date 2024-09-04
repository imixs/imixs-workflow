package org.imixs.workflow.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.junit.jupiter.api.Test;

/**
 * Test class test the Imixs JSONBuilder class to build a JSON String form an
 * ItemCollection.
 * 
 * 
 * @author rsoika
 */
public class TestJSONBuilder {
	private static final Logger logger = Logger.getLogger(TestJSONBuilder.class.getName());

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
		workitem.setItemValue("amount", Double.valueOf(100.99));
		workitem.appendItemValue("txtlog", "A");
		workitem.appendItemValue("txtlog", "B");
		workitem.appendItemValue("txtlog", "C");

		String jsonResult = null;
		try {
			jsonResult = ImixsJSONBuilder.build(workitem);
			assertNotNull(jsonResult);
		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();
			fail();
		}

		// now convert the json string back using the Imixs JSONParser
		InputStream jsonStream = new ByteArrayInputStream(jsonResult.getBytes());
		try {
			List<ItemCollection> testItemColList = ImixsJSONParser.parse(jsonStream);
			assertNotNull(testItemColList);
			ItemCollection testItemCol = testItemColList.get(0);
			// convert data....
			assertEquals(workitem.getItemValueBoolean("$isauthor"),
					testItemCol.getItemValueBoolean("$isauthor"));
			assertEquals(workitem.getItemValueString("$readaccess"),
					testItemCol.getItemValueString("$readaccess"));

			List valueList = testItemCol.getItemValue("count");
			assertTrue(valueList.get(0) instanceof Integer);
			assertEquals(workitem.getItemValueInteger("count"), testItemCol.getItemValueInteger("count"), 0);
			assertEquals(workitem.getItemValueDouble("amount"), testItemCol.getItemValueDouble("amount"), 0);

			// test value list
			valueList = testItemCol.getItemValue("txtlog");
			assertEquals(3, valueList.size());
			assertEquals("C", valueList.get(2));

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			fail();
		}

		logger.info(jsonResult);

	}

}
