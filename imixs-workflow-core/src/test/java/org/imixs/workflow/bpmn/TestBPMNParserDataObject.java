package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Test class test the Imixs BPMNParser
 * 
 * bpmn2:dataobject
 * 
 * @author rsoika
 */
public class TestBPMNParserDataObject {

	@Before
	public void setup() {

	}

	@After
	public void teardown() {

	}

	@SuppressWarnings({ "unused", "unchecked" })
	@Test
	public void testSimple() throws ParseException,
			ParserConfigurationException, SAXException, IOException, ModelException {

		String VERSION = "1.0.0";

		InputStream inputStream = getClass().getResourceAsStream(
				"/bpmn/dataobject_example1.bpmn");

		BPMNModel model = null;
		try {
			model = BPMNParser.parseModel(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertNotNull(model);

		// Test Environment
		ItemCollection profile = model.getDefinition();
		Assert.assertNotNull(profile);


		// test count of elements
		Assert.assertEquals(2, model.findAllTasks().size());

		// test task 1000
		ItemCollection task = model.getTask(1000);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Data Object Example",
				task.getItemValueString("txtworkflowgroup"));
		
		List<?> dataObjects = task.getItemValue("dataObjects");
		
		
		Assert.assertNotNull(dataObjects);
		Assert.assertEquals(1,dataObjects.size());
		List<String> data=(List<String>) dataObjects.get(0);
		Assert.assertNotNull(data);
		Assert.assertEquals(2,data.size());
		Assert.assertEquals("Invoice Template",data.get(0));
		Assert.assertEquals("Some data ...",data.get(1));
		

	}

}
