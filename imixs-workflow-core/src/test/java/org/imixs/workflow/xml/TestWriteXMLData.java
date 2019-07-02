package org.imixs.workflow.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.imixs.workflow.ItemCollection;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class writes a simple ItemCollection into a xml file.
 * 
 * Next we read the same data back and compare the result.
 * 
 * 
 * @author rsoika
 */
public class TestWriteXMLData {

	/**
	 * This test case is split into 3 phases.
	 * 
	 * PHASE I.
	 * 
	 * Create a source ItemColleciton
	 * 
	 * PHASE II.
	 * 
	 * writes the source itemCollection with the jaxb object into a new xml file.
	 * 
	 * PHASE III.
	 * 
	 * Read the file back and compare the result
	 * 
	 * 
	 * The source ItemCollection contains a List of Map items and a List of List
	 * 
	 */
	@Test
	// @Ignore
	public void testWrite() {
		byte[] empty = { 0 };
		// PHASE I.
		// Create example IemCollection....
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

		// add a file

		itemColSource.addFile(empty, "test.txt", null);

		// PHASE II.
		// write the write-example.xml....

		// create JAXB object
		XMLDocument xmlCol = null;
		try {
			xmlCol = XMLDocumentAdapter.getDocument(itemColSource);
		} catch (Exception e1) {

			e1.printStackTrace();
			Assert.fail();
		}

		// now write back to file
		File file = null;
		try {

			file = new File("src/test/resources/write-example.xml");
			JAXBContext jaxbContext = JAXBContext.newInstance(XMLDocument.class);
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

		// PHASE III.
		// Read the write-example.xml....

		try {
			FileInputStream fis = null;

			fis = new FileInputStream(file);
			ItemCollection resultItemCollection = XMLDocumentAdapter.readItemCollectionFromInputStream(fis);

			Assert.assertNotNull(resultItemCollection);

			Assert.assertEquals(itemColSource.getItemValue("_name"), resultItemCollection.getItemValue("_name"));
			Assert.assertEquals(itemColSource.getItemValue("_listdata"),
					resultItemCollection.getItemValue("_listdata"));
			Assert.assertEquals(itemColSource.getItemValue("_mapdata"), resultItemCollection.getItemValue("_mapdata"));

			List<String> fileNames = itemColSource.getFileNames();
			Assert.assertNotNull(fileNames);
			Assert.assertEquals(1, fileNames.size());
			Map<String, List<Object>> files = itemColSource.getFiles();

			Assert.assertNotNull(files);
			List<Object> testFile = files.get("test.txt");
			Assert.assertEquals(2, testFile.size());
			Assert.assertEquals("application/unknown", testFile.get(0).toString());
			Assert.assertEquals(empty, testFile.get(1));

		} catch (JAXBException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (IOException e) {
			Assert.fail();
		}

	}

}
