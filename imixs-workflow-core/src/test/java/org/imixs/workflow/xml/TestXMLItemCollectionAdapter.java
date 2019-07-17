package org.imixs.workflow.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for xmlItemCollection object
 * 
 * @author rsoika
 * 
 */
public class TestXMLItemCollectionAdapter {

	@Test
	public void testItemCollection() {
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("txtTitel", "Hello");
		XMLDocument xmlItemCollection = null;
		try {
			xmlItemCollection = XMLDocumentAdapter.getDocument(itemCollection);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		ItemCollection col2 = XMLDocumentAdapter.putDocument(xmlItemCollection);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");

		Assert.assertEquals(col2.getItemValueString("txttitel"), "Hello");
	}

	/**
	 * test convertion of date values
	 */
	@Test
	public void testDateValue() {
		ItemCollection itemCollection = new ItemCollection();

		Date datTest = new Date();
		itemCollection.replaceItemValue("datDate", datTest);

		XMLDocument xmlItemCollection = null;
		try {
			xmlItemCollection = XMLDocumentAdapter.getDocument(itemCollection);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		ItemCollection col2 = XMLDocumentAdapter.putDocument(xmlItemCollection);

		// test if date is equals
		Assert.assertEquals(col2.getItemValueDate("datDate"), itemCollection.getItemValueDate("datDate"));

		Assert.assertEquals(col2.getItemValueDate("datDate"), datTest);

	}

	/**
	 * Issue #52
	 * 
	 * Test conversion of XMLGregorianCalener Instances
	 * 
	 * @see http://stackoverflow.com/questions/835889/java-util-date-to-
	 *      xmlgregoriancalendar
	 */
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testXMLGregrianCalendar() {

		// first we test normal date objects

		Date datTest = new Date();

		XMLDocument xmlItemCollection = new XMLDocument();
		XMLItem xmlItem = new XMLItem();
		xmlItem.setName("datdate");
		xmlItem.setValue(new Object[] { datTest });
		XMLItem[] xmlItemList = new XMLItem[] { xmlItem };
		xmlItemCollection.setItem(xmlItemList);

		ItemCollection itemCollection = XMLDocumentAdapter.putDocument(xmlItemCollection);
		Assert.assertEquals(itemCollection.getItemValueDate("datdate"), datTest);

		/*
		 * Test phase II.
		 */

		// now we repeat the test with an XMLGregorianCalneder Object...
		XMLGregorianCalendar xmlDate = null;
		try {
			GregorianCalendar c = new GregorianCalendar();
			c.setTime(datTest);
			xmlDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
		} catch (DatatypeConfigurationException e) {

			e.printStackTrace();
			Assert.fail();
		}
		xmlItemCollection = new XMLDocument();
		xmlItem = new XMLItem();
		xmlItem.setName("datdate");
		xmlItem.setValue(new Object[] { xmlDate });
		xmlItemList = new XMLItem[] { xmlItem };
		xmlItemCollection.setItem(xmlItemList);

		itemCollection = XMLDocumentAdapter.putDocument(xmlItemCollection);

		// now we expect that the XMLItemCollectionAdapter has converted
		// the XMLGregorianCalendar impl into a java.util.Date object

		List resultDate = itemCollection.getItemValue("datdate");
		Assert.assertNotNull(resultDate);
		Assert.assertEquals(1, resultDate.size());

		Assert.assertFalse(resultDate.get(0) instanceof XMLGregorianCalendar);
		Assert.assertTrue(resultDate.get(0) instanceof Date);

		Assert.assertEquals(itemCollection.getItemValueDate("datdate"), datTest);

	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testItemCollectionContainingMap() {
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("txtTitel", "Hello");

		Map map = new HashMap<>();
		map.put("_name", "some data");
		itemCollection.replaceItemValue("_mapdata", map);

		XMLDocument xmlItemCollection = null;
		try {
			xmlItemCollection = XMLDocumentAdapter.getDocument(itemCollection);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		ItemCollection col2 = XMLDocumentAdapter.putDocument(xmlItemCollection);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");

		Assert.assertEquals(col2.getItemValueString("txttitel"), "Hello");

		List listOfMap = col2.getItemValue("_mapdata");
		Assert.assertEquals(1, listOfMap.size());

		Object some = listOfMap.get(0);

		Assert.assertTrue(some instanceof Map);

	}

	/**
	 * Test conversion of a ItemCollection containing a Item which value is a List
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testItemCollectionContainingListOfList() {
		ItemCollection itemColSource = new ItemCollection();
		itemColSource.replaceItemValue("txtTitel", "Hello");

		List<List<String>> valueList = new ArrayList<List<String>>();

		List<String> list1 = new ArrayList<String>();
		list1.add("Berlin");
		list1.add("Munich");
		valueList.add(list1);

		List<String> list2 = new ArrayList<String>();
		list2.add("John");
		list2.add("Sam");
		valueList.add(list2);

		itemColSource.replaceItemValue("_listdata", valueList);

		XMLDocument xmlItemCollection = null;
		try {
			xmlItemCollection = XMLDocumentAdapter.getDocument(itemColSource);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		// now reconstruct the xmlItemCollection into a ItemCollection...
		ItemCollection itemColTest = XMLDocumentAdapter.putDocument(xmlItemCollection);

		Assert.assertEquals(itemColTest.getItemValueString("txttitel"), "Hello");

		List listOfList = itemColTest.getItemValue("_listdata");
		Assert.assertEquals(2, listOfList.size());

		// test list1
		Object o1 = listOfList.get(0);
		Assert.assertTrue(o1 instanceof List);
		List<String> resultlist1 = (List<String>) o1;
		Assert.assertEquals("Berlin", resultlist1.get(0));
		Assert.assertEquals("Munich", resultlist1.get(1));

		// test list2
		Object o2 = listOfList.get(1);
		Assert.assertTrue(o2 instanceof List);
		List<String> resultlist2 = (List<String>) o2;
		Assert.assertEquals("John", resultlist2.get(0));
		Assert.assertEquals("Sam", resultlist2.get(1));

	}

	/**
	 * Test conversion of a ItemCollection containing a Item which value is a array
	 * of raw types (String and long)
	 */
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testItemCollectionContainingListOfArray() {
		ItemCollection itemColSource = new ItemCollection();
		itemColSource.replaceItemValue("txtTitel", "Hello");

		String[] valueArray1 = { "ABC", "DEF", "GHI" };
		itemColSource.replaceItemValue("_stringArrayData", valueArray1);

		long[] valueArray2 = { 1, 2, 3 };
		itemColSource.replaceItemValue("_longArrayData", valueArray2);

		Long[] valueArray3 = { new Long(1), new Long(2), new Long(3) };
		itemColSource.replaceItemValue("_longObjectArrayData", valueArray3);

		XMLDocument xmlItemCollection = null;
		try {
			xmlItemCollection = XMLDocumentAdapter.getDocument(itemColSource);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		// now reconstruct the xmlItemCollection into a ItemCollection...
		ItemCollection itemColTest = XMLDocumentAdapter.putDocument(xmlItemCollection);
		Assert.assertEquals(itemColTest.getItemValueString("txttitel"), "Hello");

		// test String array...
		List listOfList = itemColTest.getItemValue("_stringArrayData");
		Assert.assertEquals(1, listOfList.size());
		String[] resultStringArray = (String[]) listOfList.get(0);
		Assert.assertNotNull(resultStringArray);
		Assert.assertEquals("ABC", resultStringArray[0]);
		Assert.assertEquals("DEF", resultStringArray[1]);
		Assert.assertEquals("GHI", resultStringArray[2]);

		// test long array...
		listOfList = itemColTest.getItemValue("_LongArrayData");
		Assert.assertEquals(1, listOfList.size());
		long[] resultLongArray = (long[]) listOfList.get(0);
		Assert.assertNotNull(resultStringArray);
		Assert.assertEquals(1, resultLongArray[0]);
		Assert.assertEquals(2, resultLongArray[1]);
		Assert.assertEquals(3, resultLongArray[2]);

		// test long object array...
		listOfList = itemColTest.getItemValue("_LongObjectArrayData");
		Assert.assertEquals(1, listOfList.size());
		Long[] resultLongObjectArray = (Long[]) listOfList.get(0);
		Assert.assertNotNull(resultStringArray);
		Assert.assertEquals(new Long(1), resultLongObjectArray[0]);
		Assert.assertEquals(new Long(2), resultLongObjectArray[1]);
		Assert.assertEquals(new Long(3), resultLongObjectArray[2]);

	}
	
	
	
	/**
	 * Test conversion of a ItemCollection containing a Item which value is a array
	 * of mixed raw types (String and long)
	 */
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testItemCollectionContainingListOfMixedArray() {
		ItemCollection itemColSource = new ItemCollection();
		itemColSource.replaceItemValue("txtTitel", "Hello");

		Object[] valueArray1 = { "ABC", 4, new Long(5) };
		itemColSource.replaceItemValue("_mixedArrayData", valueArray1);

		XMLDocument xmlItemCollection = null;
		try {
			xmlItemCollection = XMLDocumentAdapter.getDocument(itemColSource);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		// now reconstruct the xmlItemCollection into a ItemCollection...
		ItemCollection itemColTest = XMLDocumentAdapter.putDocument(xmlItemCollection);
		Assert.assertEquals(itemColTest.getItemValueString("txttitel"), "Hello");

		// test String array...
		List listOfList = itemColTest.getItemValue("_mixedArrayData");
		Assert.assertEquals(1, listOfList.size());
		Object[] resultStringArray = (Object[]) listOfList.get(0);
		Assert.assertNotNull(resultStringArray);
		Assert.assertEquals("ABC", resultStringArray[0]);
		Assert.assertEquals(4, resultStringArray[1]);
		Assert.assertEquals(new Long(5), resultStringArray[2]);

		
	}

	

	/**
	 * Test conversion of a ItemCollection containing a Item which value is a single
	 * Map object
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testItemCollectionContainingListOfMap() {
		ItemCollection itemColSource = new ItemCollection();
		itemColSource.replaceItemValue("txtTitel", "Hello");

		List<Map<String, List<Object>>> mapList = new ArrayList<Map<String, List<Object>>>();

		Map map1 = new HashMap<>();
		map1.put("_name", "some data");
		map1.put("_city", "Munich");
		

		mapList.add(map1);
		itemColSource.replaceItemValue("_mapdata", mapList);

		XMLDocument xmlItemCollection = null;
		try {
			xmlItemCollection = XMLDocumentAdapter.getDocument(itemColSource);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		// now reconstruct the xmlItemCollection into a ItemCollection...
		ItemCollection itemColTest = XMLDocumentAdapter.putDocument(xmlItemCollection);

		Assert.assertEquals(itemColTest.getItemValueString("txttitel"), "Hello");

		List listOfMap = itemColTest.getItemValue("_mapdata");
		Assert.assertEquals(1, listOfMap.size());

		Object o1 = listOfMap.get(0);
		Assert.assertTrue(o1 instanceof Map);
		Map<String, List<Object>> mapTest1 = (Map<String, List<Object>>) o1;
		Assert.assertEquals("some data", mapTest1.get("_name").get(0));

		Assert.assertEquals("Munich", mapTest1.get("_city").get(0));

	}

	

	/**
	 * Test conversion of a ItemCollection containing a Item which value is a single
	 * Map object containing Date objects
	 * 
	 * Issue #
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testItemCollectionContainingListOfMapWithDate() {
		ItemCollection itemColSource = new ItemCollection();
		
		Date date=new Date();

		List<Map<String, List<Object>>> mapList = new ArrayList<Map<String, List<Object>>>();

		Map map1 = new HashMap<>();
		map1.put("_date", date);
		map1.put("_amount", new Double(5.47));

		mapList.add(map1);
		itemColSource.replaceItemValue("_mapdata", mapList);

		XMLDocument xmlItemCollection = null;
		try {
			xmlItemCollection = XMLDocumentAdapter.getDocument(itemColSource);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		// now reconstruct the xmlItemCollection into a ItemCollection...
		ItemCollection itemColTest = XMLDocumentAdapter.putDocument(xmlItemCollection);

		List listOfMap = itemColTest.getItemValue("_mapdata");
		Assert.assertEquals(1, listOfMap.size());

		Object o1 = listOfMap.get(0);
		Assert.assertTrue(o1 instanceof Map);
		Map<String, List<Object>> mapTest1 = (Map<String, List<Object>>) o1;
		
		Assert.assertEquals(date, mapTest1.get("_date").get(0));

	}

	
	/**
	 * Test conversion of a ItemCollection containing a $file Item
	 */
	@Test
	public void testItemCollectionContainingFiles() {
		ItemCollection itemColSource = new ItemCollection();
		itemColSource.replaceItemValue("txtTitel", "Hello");

		byte[] empty = { 0 };
		// add the file name (with empty data) into the
		// parentWorkitem.
		itemColSource.addFile(empty, "test.txt", "png");

		XMLDocument xmlItemCollection = null;
		try {
			xmlItemCollection = XMLDocumentAdapter.getDocument(itemColSource);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		// now reconstruct the xmlItemCollection into a ItemCollection...
		ItemCollection itemColTest = XMLDocumentAdapter.putDocument(xmlItemCollection);

		Assert.assertEquals(itemColTest.getItemValueString("txttitel"), "Hello");

		// test file
		Map<String, List<Object>> files = itemColTest.getFiles();
		List<Object> testFile = files.get("test.txt");
		Assert.assertEquals(2, testFile.size());
		Assert.assertEquals("png", testFile.get(0).toString());
		// compare content
		Assert.assertTrue(Arrays.equals(empty, (byte[]) testFile.get(1)));

	}

	/**
	 * Test conversion of a ItemCollection containing a Item which value is a list
	 * of Map objects. The map objects where constructed from ItemCollection objects
	 * 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testItemCollectionContainingListOfMapConstructedOfItemCollections() {
		ItemCollection itemColSource = new ItemCollection();
		itemColSource.replaceItemValue("txtTitel", "Hello");

		List<Map<String, List<Object>>> mapList = new ArrayList<Map<String, List<Object>>>();

		ItemCollection i1 = new ItemCollection();
		i1.replaceItemValue("_name", "some data");
		i1.replaceItemValue("_city", "Berlin");
		ItemCollection i2 = new ItemCollection();
		i2.replaceItemValue("_name", "other data");
		i2.replaceItemValue("_city", "Munich");
		mapList.add(i1.getAllItems());
		mapList.add(i2.getAllItems());
		itemColSource.replaceItemValue("_mapdata", mapList);

		XMLDocument xmlItemCollection = null;
		try {
			xmlItemCollection = XMLDocumentAdapter.getDocument(itemColSource);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		// now reconstruct the xmlItemCollection into a ItemCollection...
		ItemCollection itemColTest = XMLDocumentAdapter.putDocument(xmlItemCollection);

		Assert.assertEquals(itemColTest.getItemValueString("txttitel"), "Hello");

		List listOfMap = itemColTest.getItemValue("_mapdata");
		Assert.assertEquals(2, listOfMap.size());

		Object o1 = listOfMap.get(0);
		Assert.assertTrue(o1 instanceof Map);
		Map<String, List<Object>> mapTest1 = (Map<String, List<Object>>) o1;
		Assert.assertEquals("some data", mapTest1.get("_name").get(0));
		Assert.assertEquals("Berlin", mapTest1.get("_city").get(0));

		Object o2 = listOfMap.get(1);
		Assert.assertTrue(o2 instanceof Map);
		Map<String, List<Object>> mapTest2 = (Map<String, List<Object>>) o2;
		Assert.assertEquals("other data", mapTest2.get("_name").get(0));
		Assert.assertEquals("Munich", mapTest2.get("_city").get(0));

	}

	/**
	 * Test marshaling and unmarshaling FileData Objects.
	 * 
	 */
	@Test
	public void testItemCollectionContainingFileData() {
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.setItemValue("txtTitel", "Hello");

		ItemCollection attributes = new ItemCollection();
		attributes.setItemValue("comment", "some data");
		attributes.setItemValue("size", 47);

		// add a dummy file
		byte[] empty = { 0 };
		itemCollection.addFileData(new FileData("test1.txt", empty, "application/xml", attributes.getAllItems()));

		// convert to xml
		XMLDocument xmlItemCollection = null;
		try {
			xmlItemCollection = XMLDocumentAdapter.getDocument(itemCollection);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		// reconvert inot itemcollection
		ItemCollection col2 = XMLDocumentAdapter.putDocument(xmlItemCollection);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");
		Assert.assertEquals(col2.getItemValueString("txttitel"), "Hello");

		// verify file data....
		List<FileData> files = col2.getFileData();
		Assert.assertNotNull(files);

	}

}
