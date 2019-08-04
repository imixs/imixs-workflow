package org.imixs.workflow.util;

import java.text.ParseException;

import org.junit.Test;

import junit.framework.Assert;

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

}
