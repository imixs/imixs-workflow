package org.imixs.workflow.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import org.imixs.workflow.ItemCollection;
import org.junit.Test;

import org.junit.Assert; 

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
		Assert.assertEquals("b38b84614af36f874ba4f08dd4ea40c4e66e0a607", key);
	}

	@Test
	public void testWorkitem() throws ParseException {

		String json = "{\"item\":[{\"name\":\"price\",\"value\":{\"$\":\"199.99\",\"@type\":\"xs:float\"}},{\"name\":\"coverage\",\"value\":{\"@type\":\"xs:float\",\"$\":\"100000.00\"}},{\"name\":\"deductible\",\"value\":{\"$\":\"5000.00\",\"@type\":\"xs:float\"}}]}";
	
		InputStream responseDataStream = new ByteArrayInputStream(json.getBytes());
		ItemCollection resultWorkitem;
		try {
			resultWorkitem = JSONParser.parseWorkitem(responseDataStream, "UTF-8");
			
			Assert.assertEquals(new Float(199.99), resultWorkitem.getItemValueFloat("price"),0);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();
		}
		
	}

}
