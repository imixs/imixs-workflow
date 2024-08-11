package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;
import org.xml.sax.SAXException;

/**
 * Test class test the Imixs BPMNParser with an Adapter definition in a Event
 * (Signal Event)
 * 
 * @author rsoika
 */
public class TestBPMNParserAdapter {

	BPMNModel model = null;
	OpenBPMNModelManager openBPMNModelManager = null;

	@Before
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new OpenBPMNModelManager();
	}

	@Test
	public void testEventAdapter()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {
		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/adapter.bpmn"));
			model = openBPMNModelManager.getBPMNModel("1.0.0");
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertNotNull(model);
		// test activity 1000.20 submit
		ItemCollection event = openBPMNModelManager.findEventByID(model, 1000, 20);
		Assert.assertNotNull(event);
		Assert.assertEquals("submit", event.getItemValueString("name"));

		// test adapter class.....
		Assert.assertEquals("org.imixs.workflow.adapter.Example", event.getItemValueString("adapter.id"));

	}

	@Test
	public void testEventMulitAdapter()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/adapter_multi.bpmn"));
			model = openBPMNModelManager.getBPMNModel("1.0.0");
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(model);

		// test activity 1000.20 submit
		ItemCollection event = openBPMNModelManager.findEventByID(model, 1000, 20);
		Assert.assertNotNull(event);
		Assert.assertEquals("adapter A", event.getItemValueString("txtname"));
		// test adapter class.....
		Assert.assertEquals("org.imixs.workflow.adapter.Example", event.getItemValueString("adapter.id"));

		// test activity 1100.10 submit
		event = openBPMNModelManager.findEventByID(model, 1100, 10);
		Assert.assertNotNull(event);
		Assert.assertEquals("adapter A", event.getItemValueString("txtname"));
		// test adapter class.....
		Assert.assertEquals("org.imixs.workflow.adapter.Example", event.getItemValueString("adapter.id"));

		// test activity 1000.20 submit
		event = openBPMNModelManager.findEventByID(model, 1100, 20);
		Assert.assertNotNull(event);
		Assert.assertEquals("adapter B", event.getItemValueString("txtname"));
		// test adapter class.....
		Assert.assertEquals("com.imixs.test.AdapterB", event.getItemValueString("adapter.id"));

	}

}
