package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import junit.framework.Assert;

/**
 * Test class test the Imixs BPMNParser
 * 
 * bpmn2:dataobject
 * 
 * @author rsoika
 */
public class TestBPMNParserDataObject {
	BPMNModel model = null;

	@Before
	public void setUp() throws PluginException {
		@SuppressWarnings("unused")
		String VERSION = "1.0.0";

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/dataobject_example1.bpmn");

		try {
			model = BPMNParser.parseModel(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ModelException | ParseException | ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertNotNull(model);

	}

	@SuppressWarnings({ "unchecked" })
	@Test
	public void testTask()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		// Test Environment
		ItemCollection profile = model.getDefinition();
		Assert.assertNotNull(profile);

		// test count of elements
		Assert.assertEquals(2, model.findAllTasks().size());

		// test task 1000
		ItemCollection task = model.getTask(1000);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Data Object Example", task.getItemValueString("txtworkflowgroup"));

		List<?> dataObjects = task.getItemValue("dataObjects");

		Assert.assertNotNull(dataObjects);
		Assert.assertEquals(1, dataObjects.size());
		List<String> data = (List<String>) dataObjects.get(0);
		Assert.assertNotNull(data);
		Assert.assertEquals(2, data.size());
		Assert.assertEquals("Invoice Template", data.get(0));
		Assert.assertEquals("Some data ...", data.get(1));

	}

	@SuppressWarnings({ "unchecked" })
	@Test
	public void testEvent()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		ItemCollection event = model.getEvent(1000, 10);
		// test event 1000.10

		Assert.assertNotNull(event);

		List<?> dataObjects = event.getItemValue("dataObjects");

		Assert.assertNotNull(dataObjects);
		Assert.assertEquals(1, dataObjects.size());
		List<String> data = (List<String>) dataObjects.get(0);
		Assert.assertNotNull(data);
		Assert.assertEquals(2, data.size());
		Assert.assertEquals("EventData", data.get(0));
		Assert.assertEquals("Some config-data ...", data.get(1));

	}

}
