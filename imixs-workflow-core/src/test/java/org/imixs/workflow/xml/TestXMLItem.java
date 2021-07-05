package org.imixs.workflow.xml;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.RuleEngine;
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

		// final marshaling test...
		try {
			testMarshaling(xmlItem);
		} catch (JAXBException e) {
			e.printStackTrace();
			Assert.fail();
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testDateValue() {
		XMLItem xmlItem = new XMLItem();
		xmlItem.setName("name");
		Date date = new Date();
		List values = new ArrayList<>();
		values.add(date);
		xmlItem.setValue(values.toArray());

		Assert.assertEquals("name", xmlItem.getName());

		Assert.assertEquals(1, xmlItem.getValue().length);
		Assert.assertEquals(date, xmlItem.getValue()[0]);
		// final marshaling test...
		try {
			testMarshaling(xmlItem);
		} catch (JAXBException e) {
			e.printStackTrace();
			Assert.fail();
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testListSimpleValues() {
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

		// final marshaling test...
		try {
			testMarshaling(xmlItem);
		} catch (JAXBException e) {
			e.printStackTrace();
			Assert.fail();
		}

	}

	/**
	 * Test conversion of Date objects in a list
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testListDateValues() {
		XMLItem xmlItem = new XMLItem();
		xmlItem.setName("name");

		Date date1 = new Date();
		Date date2 = new Date();

		List values = new ArrayList<>();
		values.add(date1);
		values.add(date2);
		xmlItem.setValue(values.toArray());

		Assert.assertEquals("name", xmlItem.getName());

		Assert.assertEquals(2, xmlItem.getValue().length);
		Assert.assertEquals(date1, xmlItem.getValue()[0]);
		Assert.assertEquals(date2, xmlItem.getValue()[1]);

		// final marshaling test...
		try {
			testMarshaling(xmlItem);
		} catch (JAXBException e) {
			e.printStackTrace();
			Assert.fail();
		}

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
		// Object[] testMaps = xmlItem.getValue(true);
		Object[] testMaps = xmlItem.transformValue();
		// we excpect 2 entries of maps ...
		Assert.assertEquals(2, testMaps.length);

		Object o1 = testMaps[0];
		Assert.assertTrue(o1 instanceof Map);
		Map<String, List<Object>> m1 = (Map<String, List<Object>>) o1;
		Assert.assertEquals("some data", m1.get("_name").get(0));
		Assert.assertEquals("Berlin", m1.get("_city").get(0));

		Object o2 = testMaps[1];
		Assert.assertTrue(o2 instanceof Map);
		Map<String, List<Object>> m2 = (Map<String, List<Object>>) o2;
		Assert.assertEquals("other data", m2.get("_name").get(0));
		Assert.assertEquals("Munich", m2.get("_city").get(0));

		// final marshaling test...
		try {
			testMarshaling(xmlItem);
		} catch (JAXBException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
	
	
	
	/**
	 * Test embedded list of HashTables
	 * 
	 * https://github.com/imixs/imixs-admin/issues/46
	 */
	@Test
	public void testEmbeddedListOfHashTables() {
		XMLItem xmlItem = new XMLItem();
		xmlItem.setName("maps");
		
		Hashtable<String, ArrayList<String>> hashTable = new Hashtable<String, ArrayList<String>>();
		ArrayList<String> arrayList=new ArrayList<String>();
		arrayList.add("a");
		arrayList.add("b");
		hashTable.put("mykey", arrayList);
		
		List<Object> values=new ArrayList<>();
		values.add(hashTable);
		xmlItem.setValue(values.toArray());

		// final marshaling test...
		try {
			testMarshaling(xmlItem);
		} catch (JAXBException e) {
			e.printStackTrace();
			Assert.fail();
		}
		
		// repeat the test with an ItemCollection
		ItemCollection workitem1=new ItemCollection();
		workitem1.replaceItemValue("somedata", hashTable);
		XMLDocument xmlDocument=XMLDocumentAdapter.getDocument(workitem1);
		ItemCollection workitem2=XMLDocumentAdapter.putDocument(xmlDocument);
		Assert.assertEquals(workitem1.getItemValue("somedata"), workitem2.getItemValue("somedata"));
		
	}

	
	

	/**
	 * Test embedded list of Maps with Objects from Type Date and Double
	 * 
	 * Issue #535
	 * 
	 */
	@Test
	public void testListOfMapsWithNonStringValues() {
		XMLItem xmlItem = new XMLItem();
		xmlItem.setName("maps");

		Date date = new Date();
		Double _double1= Double.valueOf(1.47);
		
		
		List<Map<String, Object>> commentList = new ArrayList<>();

		Map<String, Object> log = new HashMap<String, Object>();
		log.put("date", date);
		log.put("double1", _double1);
		commentList.add(log);
		xmlItem.setValue(commentList.toArray());
		// final marshaling test...
		try {
			testMarshaling(xmlItem);
		} catch (JAXBException e) {
			e.printStackTrace();
			Assert.fail();
		}

		// test values
		Object[] xmlValue = xmlItem.getValue();
		Assert.assertEquals(1, xmlValue.length);

		Object o1 = xmlValue[0];
		Assert.assertTrue(o1 instanceof Object[]);
	
		Object[] objectArrayValue = (Object[]) xmlValue[0];
		Assert.assertEquals(2, objectArrayValue.length);

		// check date
		XMLItem embeddedXMLItem = (XMLItem) objectArrayValue[0];
		Assert.assertEquals("date", embeddedXMLItem.getName());
		Object[] embeddedValue = embeddedXMLItem.getValue();
		Assert.assertEquals(1, embeddedValue.length);
		Object innerObject = embeddedValue[0];
		// we expect a Date object!
		Assert.assertTrue(innerObject instanceof Date);
		Assert.assertEquals(date, innerObject);

	
		// check double1
		 embeddedXMLItem = (XMLItem) objectArrayValue[1];
		Assert.assertEquals("double1", embeddedXMLItem.getName());
		embeddedValue = embeddedXMLItem.getValue();
		Assert.assertEquals(1, embeddedValue.length);
		innerObject = embeddedValue[0];
		// we expect a Double object!
		Assert.assertTrue(innerObject instanceof Double);
		Assert.assertEquals(_double1, innerObject);

	
	
	}

	/**
	 * Test embedded list of Maps with Date Object and a non serializeable object
	 * (in this example a RuleEngine).
	 * <p>
	 * Non serializable objects should not be part of the XML tree. So the value
	 * must be null
	 * 
	 */
	@Test
	public void testListOfMapsWithNonSerializableValues() {
		XMLItem xmlItem = new XMLItem();
		xmlItem.setName("maps");

		Date date = new Date();
		List<Map<String, Object>> commentList = new ArrayList<>();

		Map<String, Object> log = new HashMap<String, Object>();
		log.put("date", date);

		// ad a non serializeable object.....
		log.put("ruleengine", new RuleEngine());
		commentList.add(log);
		xmlItem.setValue(commentList.toArray());
		// final marshaling test...
		try {
			testMarshaling(xmlItem);
		} catch (JAXBException e) {
			e.printStackTrace();
			Assert.fail();
		}

		// test values
		Object[] xmlValue = xmlItem.getValue();

		Assert.assertEquals(1, xmlValue.length);

		Object o1 = xmlValue[0];
		Assert.assertTrue(o1 instanceof Object[]);

		Object[] objectArrayValue = (Object[]) xmlValue[0];
		Assert.assertEquals(2, objectArrayValue.length);

		// test Date object
		XMLItem embeddedXMLItem = (XMLItem) objectArrayValue[0];
		Assert.assertEquals("date", embeddedXMLItem.getName());
		Object[] embeddedValue = embeddedXMLItem.getValue();
		Assert.assertEquals(1, embeddedValue.length);
		Object innerObject = embeddedValue[0];
		// we expect a Date object!
		Assert.assertTrue(innerObject instanceof Date);
		Assert.assertEquals(date, innerObject);

		// test RuleEngine object
		embeddedXMLItem = (XMLItem) objectArrayValue[1];
		Assert.assertEquals("ruleengine", embeddedXMLItem.getName());
		embeddedValue = embeddedXMLItem.getValue();
		// expected a null value
		Assert.assertNull(embeddedValue);

	}

	/**
	 * This method test the convertion of a file data structure which is a list with
	 * an embedded map
	 */
	@Test
	public void testFileDataStructure() {

		ItemCollection itemColSource = new ItemCollection();

		// first we test without custom attributes
		// add a dummy file
		byte[] empty = { 0 };
		itemColSource.addFileData(new FileData("test1.txt", empty, "application/xml", null));
		XMLItem xmlItem = new XMLItem();
		xmlItem.setName("$file");
		xmlItem.setValue(itemColSource.getItemValue("$file").toArray());
		Assert.assertEquals("$file", xmlItem.getName());
		Assert.assertEquals(1, xmlItem.getValue().length);

		// second we test with custom attributes
		ItemCollection attributes = new ItemCollection();
		attributes.setItemValue("comment", "some data");
		attributes.setItemValue("count", 47);
		itemColSource.addFileData(new FileData("test1.txt", empty, "application/xml", attributes.getAllItems()));
		xmlItem = new XMLItem();
		xmlItem.setName("$file");
		xmlItem.setValue(itemColSource.getItemValue("$file").toArray());
		Assert.assertEquals("$file", xmlItem.getName());
		Assert.assertEquals(1, xmlItem.getValue().length);

		// final marshaling test...
		try {
			testMarshaling(xmlItem);
		} catch (JAXBException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/**
	 * This helper method simpliy tries to marshal the XMLItem object. It verifies
	 * if the object can be converted by Jaxb
	 * 
	 * @param xmlItem
	 * @throws JAXBException
	 */
	private void testMarshaling(XMLItem xmlItem) throws JAXBException {
		// marshal test...
		JAXBContext jaxbContext = JAXBContext.newInstance(XMLDocument.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

		// output pretty printed
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		jaxbMarshaller.marshal(xmlItem, System.out);
	}

}
