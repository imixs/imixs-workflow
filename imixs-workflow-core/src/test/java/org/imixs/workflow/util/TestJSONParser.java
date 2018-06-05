package org.imixs.workflow.util;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;

import junit.framework.Assert;

import org.imixs.workflow.ItemCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class test the parsing of a json file used by the workflowRestService
 * method postWorkitemJSON(InputStream requestBodyStream)
 * 
 * 
 * @author rsoika
 */
public class TestJSONParser {

	@Before
	public void setup() {

	}

	@After
	public void teardown() {

	}

	@Test
	public void testSimple() throws ParseException {

		InputStream inputStream = getClass().getResourceAsStream("/json/simple.json");

		ItemCollection itemCol = null;
		try {
			itemCol = JSONParser.parseWorkitem(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(itemCol);

		Assert.assertEquals("Anna", itemCol.getItemValueString("$readaccess"));

		List<?> list = itemCol.getItemValue("txtLog");
		Assert.assertEquals(3, list.size());

		Assert.assertEquals("C", list.get(2));

		Assert.assertEquals(10, itemCol.getItemValueInteger("$ActivityID"));
	}

	/**
	 * test parsing of json number fields
	 * 
	 * e.g. {"name":"$processid", "value":{"@type":"xs:int","$":1100}},
	 * 
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testSimpleNumbers() throws ParseException {

		InputStream inputStream = getClass().getResourceAsStream("/json/simple_numbers.json");

		ItemCollection itemCol = null;
		try {
			itemCol = JSONParser.parseWorkitem(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(itemCol);

		Assert.assertEquals(10, itemCol.getEventID());
		Assert.assertEquals(100, itemCol.getProcessID());
	}

	@Test(expected = ParseException.class)
	public void testCorrupted() throws ParseException {

		InputStream inputStream = getClass().getResourceAsStream("/json/corrupted.json");

		ItemCollection itemCol = null;
		try {
			itemCol = JSONParser.parseWorkitem(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNull(itemCol);
	}

	@Test
	public void testComplexWorkitem() throws ParseException {

		InputStream inputStream = getClass().getResourceAsStream("/json/workitem.json");

		ItemCollection itemCol = null;
		try {
			itemCol = JSONParser.parseWorkitem(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(itemCol);

		Assert.assertEquals(20, itemCol.getProcessID());

		Assert.assertEquals("worklist", itemCol.getItemValueString("txtworkflowresultmessage"));

		Assert.assertEquals("14194929161-1003e42a", itemCol.getItemValueString("$UniqueID"));

		List<?> list = itemCol.getItemValue("txtworkflowpluginlog");
		Assert.assertEquals(7, list.size());

	}
}
