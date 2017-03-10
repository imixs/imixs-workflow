package org.imixs.workflow.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.xml.XMLItemCollection;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class  writes a complex ItemCollection into a xml file.
 * 
 * 
 * @author rsoika
 */
public class TestWriteXMLDataComplex {

	@Before
	public void setup() {

	}

	@After
	public void teardown() {

	}

	
	/**
	 * writes a itemCollection with the jaxb object  into a new xml file.
	 * 
	 * The ItemCollection contains a List of Map items and a List of List 
	 * 
	 */
	@Test
	//@Ignore
	public void testWrite() {
			
		ItemCollection itemColSource = new ItemCollection();
		itemColSource.replaceItemValue("txtTitel", "Hello");

		
		// create list of list...
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
				

		
		// create list of map...
		List<Map<String, List<Object>>> mapList = new ArrayList<Map<String, List<Object>>>();

		ItemCollection i1 = new ItemCollection();
		i1.replaceItemValue("_name", "some data");
		i1.replaceItemValue("_city", list1);
		ItemCollection i2 = new ItemCollection();
		i2.replaceItemValue("_name", "other data");
		i2.replaceItemValue("_city", "Munich");
		mapList.add(i1.getAllItems());
		mapList.add(i2.getAllItems());
		itemColSource.replaceItemValue("_mapdata", mapList);
		
		
		// create JAXB object
		XMLItemCollection xmlCol = null;
		try {
			xmlCol = XMLItemCollectionAdapter.putItemCollection(itemColSource);
		} catch (Exception e1) {

			e1.printStackTrace();
			Assert.fail();
		}

		// now write back to file
		File file = null;
		try {

			file = new File("src/test/resources/export-xml-itemcollection-test.xml");
			JAXBContext jaxbContext = JAXBContext
					.newInstance(XMLItemCollection.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			jaxbMarshaller.marshal(xmlCol, file);
			jaxbMarshaller.marshal(xmlCol, System.out);

		} catch (JAXBException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertNotNull(file);
	}

}
