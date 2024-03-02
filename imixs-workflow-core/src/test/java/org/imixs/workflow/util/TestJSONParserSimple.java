package org.imixs.workflow.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;

import org.imixs.workflow.ItemCollection;
import org.junit.Test;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test class test the parsing of different json structures.
 * 
 * 
 * @author rsoika
 */
@RunWith(Parameterized.class)
public class TestJSONParserSimple {

	private String jsonString;
	private String key;
	private String response;

	public TestJSONParserSimple(String jsonString, String key, String response) {
		this.jsonString = jsonString;
		this.key = key;
		this.response = response;
	}

	/**
	 * Parse a single key value.
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testFindValueByKey() throws ParseException {

		String json = "{\"key\":\"b38b84614af36f874ba4f08dd4ea40c4e66e0a607\"}";
		String key = JSONParser.getKey("key", json);
		Assert.assertEquals("b38b84614af36f874ba4f08dd4ea40c4e66e0a607", key);
	}

	@Test
	public void testWorkitem() throws ParseException {

		String json = "{\"item\":[{\"name\":\"price\",\"value\":{\"$\":\"199.99\",\"@type\":\"xs:float\"}},{\"name\":\"coverage\",\"value\":{\"@type\":\"xs:float\",\"$\":\"100000.00\"}},{\"name\":\"deductible\",\"value\":{\"$\":\"5000.00\",\"@type\":\"xs:float\"}}]}";
	
		InputStream responseDataStream = new ByteArrayInputStream(json.getBytes());
		ItemCollection resultWorkitem;
		try {
			resultWorkitem = ImixsJSONParser.parse(responseDataStream).get(0);
			
			Assert.assertEquals( Float.valueOf(199.99f), resultWorkitem.getItemValueFloat("price"),0);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();
		}
		
	}

	@Test
	public void testKeyNotPresent() {
		String json = "{\"key1\":\"value1\",\"key2\": 2}";
		String key = "randomKey";
		Assert.assertNull(JSONParser.getKey(key, json));
	}

	@Test
	public void testNullValue() {
		String json = "{\"key1\":null}";
		Assert.assertNull(JSONParser.getKey("key1", json));
	}

	// Arrange for different key types in the json
	@Parameterized.Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "{\"key\":{\"nestedKey\":\"nestedValue\"}}", "key", "{\"nestedKey\":\"nestedValue\"}" },
				{ "{\"key\":[1,2,3] }", "key", "[1,2,3]" },
				{ "{\"key\":true }", "key", "true" },
				{ "{\"key\":false }", "key", "false" },
				{ "{\"key\":42 }", "key", "42" },
		});
	}

	@Test
	public void testGettingDifferentKeyTypesFromJson() {
		// act
		String actual = JSONParser.getKey(key, jsonString);
		//assert
		Assert.assertEquals(response, actual);
	}

}
