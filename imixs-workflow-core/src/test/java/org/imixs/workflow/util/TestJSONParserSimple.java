package org.imixs.workflow.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import org.imixs.workflow.ItemCollection;
import org.junit.jupiter.api.Test;

/**
 * Test class test the parsing of different json structures.
 * 
 * 
 * @author rsoika
 */
public class TestJSONParserSimple {

	/**
	 * Parse a single key value.
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testFindValueByKey() throws ParseException {

		String json = "{\"key\":\"b38b84614af36f874ba4f08dd4ea40c4e66e0a607\"}";
		String key = JSONParser.getKey("key", json);
		assertEquals("b38b84614af36f874ba4f08dd4ea40c4e66e0a607", key);
	}

	@Test
	public void testWorkitem() throws ParseException {

		String json = "{\"item\":[{\"name\":\"price\",\"value\":{\"$\":\"199.99\",\"@type\":\"xs:float\"}},{\"name\":\"coverage\",\"value\":{\"@type\":\"xs:float\",\"$\":\"100000.00\"}},{\"name\":\"deductible\",\"value\":{\"$\":\"5000.00\",\"@type\":\"xs:float\"}}]}";

		InputStream responseDataStream = new ByteArrayInputStream(json.getBytes());
		ItemCollection resultWorkitem;
		try {
			resultWorkitem = ImixsJSONParser.parse(responseDataStream).get(0);

			assertEquals(Float.valueOf(199.99f), resultWorkitem.getItemValueFloat("price"), 0);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
	}

	/**
	 * Simple test showing how null values are handled by the JSONParser
	 */
	@Test
	public void testKeyNotPresent() {
		String json = "{\"key1\":\"value1\",\"key2\": 2}";
		String key = "randomKey";
		assertNull(JSONParser.getKey(key, json));
	}

	/**
	 * Simple test showing how null values are handled by the JSONParser
	 */
	@Test
	public void testNullValue() {
		String json = "{\"key1\":null}";
		assertNull(JSONParser.getKey("key1", json));
	}

}
