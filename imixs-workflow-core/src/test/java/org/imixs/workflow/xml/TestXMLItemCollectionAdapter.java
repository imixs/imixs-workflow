package org.imixs.workflow.xml;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

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
		XMLItemCollection xmlItemCollection = null;
		try {
			xmlItemCollection = XMLItemCollectionAdapter
					.putItemCollection(itemCollection);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		ItemCollection col2 = XMLItemCollectionAdapter
				.getItemCollection(xmlItemCollection);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"),
				"Hello");

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

		XMLItemCollection xmlItemCollection = null;
		try {
			xmlItemCollection = XMLItemCollectionAdapter
					.putItemCollection(itemCollection);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		ItemCollection col2 = XMLItemCollectionAdapter
				.getItemCollection(xmlItemCollection);

		// test if date is equals
		Assert.assertEquals(col2.getItemValueDate("datDate"),
				itemCollection.getItemValueDate("datDate"));

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

		XMLItemCollection xmlItemCollection = new XMLItemCollection();
		XMLItem xmlItem = new XMLItem();
		xmlItem.setName("datdate");
		xmlItem.setValue(new Object[] { datTest });
		XMLItem[] xmlItemList = new XMLItem[] { xmlItem };
		xmlItemCollection.setItem(xmlItemList);

		ItemCollection itemCollection = XMLItemCollectionAdapter
				.getItemCollection(xmlItemCollection);
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
		xmlItemCollection = new XMLItemCollection();
		xmlItem = new XMLItem();
		xmlItem.setName("datdate");
		xmlItem.setValue(new Object[] { xmlDate });
		xmlItemList = new XMLItem[] { xmlItem };
		xmlItemCollection.setItem(xmlItemList);

		itemCollection = XMLItemCollectionAdapter
				.getItemCollection(xmlItemCollection);

		// now we expect that the XMLItemCollectionAdapter has converted
		// the XMLGregorianCalendar impl into a java.util.Date object

		List resultDate = itemCollection.getItemValue("datdate");
		Assert.assertNotNull(resultDate);
		Assert.assertEquals(1, resultDate.size());
		
		Assert.assertFalse(resultDate.get(0) instanceof XMLGregorianCalendar);
		Assert.assertTrue(resultDate.get(0) instanceof java.util.Date);

		Assert.assertEquals(itemCollection.getItemValueDate("datdate"), datTest);

	}
	
	
	
	
	
	
	
	@Test
	public void testItemCollectionContainingMap() {
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("txtTitel", "Hello");
		
		Map map=new HashMap<>();
		map.put("_name", "some data");
		itemCollection.replaceItemValue("_mapdata", map);
		
		XMLItemCollection xmlItemCollection = null;
		try {
			xmlItemCollection = XMLItemCollectionAdapter
					.putItemCollection(itemCollection);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		ItemCollection col2 = XMLItemCollectionAdapter
				.getItemCollection(xmlItemCollection);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"),
				"Hello");

		Assert.assertEquals(col2.getItemValueString("txttitel"), "Hello");
		
		
		 List  listOfMap=col2.getItemValue("_mapdata");
		 Assert.assertEquals(1,listOfMap.size());
		 
		 Object some = listOfMap.get(0);
			
		 Assert.assertTrue(some instanceof Map);
		
	}

}
