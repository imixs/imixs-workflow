package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.DataObject;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;
import org.xml.sax.SAXException;

/**
 * Test class test the Imixs BPMNParser
 * 
 * bpmn2:dataobject
 * 
 * @author rsoika
 */
public class TestBPMNParserDataObject {
	BPMNModel model = null;
	ModelManager openBPMNModelManager = null;

	@Before
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		try {
			openBPMNModelManager = new ModelManager();
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/dataobject_example1.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			Assert.assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

	}

	@SuppressWarnings({ "unchecked" })
	@Test
	public void testTask()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		// test task 1000
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 1000);
		Assert.assertNotNull(task);
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

		// test event 1000.10
		ItemCollection event = openBPMNModelManager.findEventByID(model, 1000, 10);
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

	/**
	 * This test fetches the DataObject directly form the bpmnElementNode
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException
	 */
	@Test
	public void testGetDataObjectFromBPMNElementNode()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		// test event 1000.10
		ItemCollection event = openBPMNModelManager.findEventByID(model, 1000, 10);
		Assert.assertNotNull(event);

		BPMNElementNode bpmnElementNode = model.findElementNodeById(event.getItemValueString("id"));

		Assert.assertNotNull(bpmnElementNode);
		Set<DataObject> dataObjects = bpmnElementNode.getDataObjects();
		Assert.assertEquals(1, dataObjects.size());
		DataObject dataObject = dataObjects.iterator().next();
		Assert.assertNotNull(dataObject);
		Assert.assertEquals("EventData", dataObject.getName());
		Assert.assertEquals("Some config-data ...", dataObject.getDocumentation());

	}

}
