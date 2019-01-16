package org.imixs.workflow.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for xmlItem object
 * 
 * @author rsoika
 * 
 */
public class TestXMLItem {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testSimpleValue() {
		XMLItem xmlItem = new XMLItem();
		xmlItem.setName("name");

		List values = new ArrayList<>();
		values.add("a");
		xmlItem.setValue(values.toArray());

		Assert.assertEquals("name", xmlItem.getName());

		Assert.assertEquals(1, xmlItem.getValue().length);
		Assert.assertEquals("a", xmlItem.getValue()[0]);
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testListValue() {
		XMLItem xmlItem = new XMLItem();
		xmlItem.setName("name");

		List values = new ArrayList<>();
		values.add("a");
		values.add("b");
		xmlItem.setValue(values.toArray());

		Assert.assertEquals("name", xmlItem.getName());

		Assert.assertEquals(2, xmlItem.getValue().length);
		Assert.assertEquals("a", xmlItem.getValue()[0]);
		Assert.assertEquals("b", xmlItem.getValue()[1]);
	}

	/**
	 * Test embedded list of Maps
	 * 
	 */
	@SuppressWarnings({ "unchecked" })
	@Test
	public void testEmbeddedListOfMaps() {
		XMLItem xmlItem = new XMLItem();
		xmlItem.setName("maps"); 

		List<Map<String, List<Object>>> mapList = new ArrayList<Map<String, List<Object>>>();

		// add a list of maps as the value object...
		ItemCollection i1 = new ItemCollection();
		i1.replaceItemValue("_name", "some data");
		i1.replaceItemValue("_city", "Berlin");
		ItemCollection i2 = new ItemCollection();
		i2.replaceItemValue("_name", "other data");
		i2.replaceItemValue("_city", "Munich");
		mapList.add(i1.getAllItems());
		mapList.add(i2.getAllItems());
		xmlItem.setValue(mapList.toArray());

		Assert.assertEquals("maps", xmlItem.getName());

		// test the value object.... force conversion to map interface
		Object[] testMaps = xmlItem.getValue(true);
		// we excpect 2 entries of maps ...
		Assert.assertEquals(2, testMaps.length);

		Object o1 = testMaps[0];
		Assert.assertTrue(o1 instanceof Map);
		Map<String,List<Object>> m1= (Map<String, List<Object>>) o1;
		Assert.assertEquals("some data", m1.get("_name").get(0));
		Assert.assertEquals("Berlin", m1.get("_city").get(0));
		
		Object o2 = testMaps[1];
		Assert.assertTrue(o2 instanceof Map);
		Map<String,List<Object>> m2= (Map<String, List<Object>>) o2;
		Assert.assertEquals("other data", m2.get("_name").get(0));
		Assert.assertEquals("Munich", m2.get("_city").get(0));

	}

	
	
	/**
	 * This method test the convertion of a file data structure which is a list with an embedded map
	 */
	@Test
	public void testFileDataStructure() {
		
		ItemCollection itemColSource = new ItemCollection();
		
		// first we test without custom attributes
		// add a dummy file
		byte[] empty = { 0 };
		itemColSource.addFileData(new FileData( "test1.txt", empty,"application/xml",null));
		XMLItem xmlItem = new XMLItem();
		xmlItem.setName("$file");
		xmlItem.setValue(itemColSource.getItemValue("$file").toArray());
		Assert.assertEquals("$file", xmlItem.getName());
		Assert.assertEquals(1, xmlItem.getValue().length);

		
		// second we test with custom attributes
		ItemCollection attributes=new ItemCollection();
		attributes.setItemValue("comment","some data");
		attributes.setItemValue("count", 47);
		itemColSource.addFileData(new FileData( "test1.txt", empty,"application/xml",attributes.getAllItems()));
		xmlItem = new XMLItem();
		xmlItem.setName("$file");
		xmlItem.setValue(itemColSource.getItemValue("$file").toArray());
		Assert.assertEquals("$file", xmlItem.getName());
		Assert.assertEquals(1, xmlItem.getValue().length);
		
	}

}
