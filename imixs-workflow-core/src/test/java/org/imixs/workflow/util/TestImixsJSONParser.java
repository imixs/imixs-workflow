package org.imixs.workflow.util;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class test the parsing of a Imixs JSON file used by the workflowRestService
 * method postWorkitemJSON(InputStream requestBodyStream)
 * 
 * 
 * @author rsoika
 */
public class TestImixsJSONParser {
	
	
	

	@Test
	public void testSimple() throws ParseException {

		InputStream inputStream = getClass().getResourceAsStream("/json/simple.json");

		List<ItemCollection> result = null;
		try {
			 result = ImixsJSONParser.parse(inputStream);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		ItemCollection itemCol=result.get(0);

		Assert.assertEquals("Anna", itemCol.getItemValueString("$readaccess"));

		List<?> list = itemCol.getItemValue("txtLog");
		Assert.assertEquals(3, list.size());

		Assert.assertEquals("C", list.get(2));

		Assert.assertEquals(10, itemCol.getItemValueInteger("$ActivityID"));
	}
	
	

	@Test
	public void testMultiDocuments() throws ParseException {

		InputStream inputStream = getClass().getResourceAsStream("/json/multidocuments.json");

		List<ItemCollection> result = null;
		try {
			 result = ImixsJSONParser.parse(inputStream);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(result);
		Assert.assertEquals(2, result.size());

		ItemCollection itemCol=result.get(0);
		Assert.assertEquals("Anna", itemCol.getItemValueString("$readaccess"));
		List<?> list = itemCol.getItemValue("txtLog");
		Assert.assertEquals(3, list.size());
		Assert.assertEquals("C", list.get(2));
		Assert.assertEquals(10, itemCol.getItemValueInteger("$ActivityID"));

		itemCol=result.get(1);
		Assert.assertEquals("Tom", itemCol.getItemValueString("$readaccess"));
		list = itemCol.getItemValue("txtLog");
		Assert.assertEquals(3, list.size());
		Assert.assertEquals("F", list.get(2));
		Assert.assertEquals(20, itemCol.getItemValueInteger("$ActivityID"));

	
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

		List<ItemCollection> result = null;
		try {
			 result = ImixsJSONParser.parse(inputStream);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertNotNull(result);
		ItemCollection itemCol=result.get(0);

		Assert.assertNotNull(itemCol);

		Assert.assertEquals(10, itemCol.getEventID());
		Assert.assertEquals(100, itemCol.getTaskID());
	}
}
