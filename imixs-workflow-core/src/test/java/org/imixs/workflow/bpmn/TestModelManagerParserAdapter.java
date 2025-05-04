package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
public class TestModelManagerParserAdapter {

	BPMNModel model = null;
	ModelManager openBPMNModelManager = null;

	@BeforeEach
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new ModelManager();
	}

	@Test
	public void testEventAdapter()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {
		try {
			model = BPMNModelFactory.read("/bpmn/adapter.bpmn");
		} catch (BPMNModelException e) {
			e.printStackTrace();
			fail();
		}
		assertNotNull(model);
		// test activity 1000.20 submit
		ItemCollection event = openBPMNModelManager.findEventByID(model, 1000, 20);
		assertNotNull(event);
		assertEquals("submit", event.getItemValueString("name"));

		// test adapter class.....
		assertEquals("org.imixs.workflow.adapter.Example", event.getItemValueString("adapter.id"));

	}

	@Test
	public void testEventMulitAdapter()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {
		try {
			model = BPMNModelFactory.read("/bpmn/adapter_multi.bpmn");
		} catch (BPMNModelException e) {
			e.printStackTrace();
			fail();
		}

		assertNotNull(model);

		// test activity 1000.20 submit
		ItemCollection event = openBPMNModelManager.findEventByID(model, 1000, 20);
		assertNotNull(event);
		assertEquals("adapter A", event.getItemValueString("txtname"));
		// test adapter class.....
		assertEquals("org.imixs.workflow.adapter.Example", event.getItemValueString("adapter.id"));

		// test activity 1100.10 submit
		event = openBPMNModelManager.findEventByID(model, 1100, 10);
		assertNotNull(event);
		assertEquals("adapter A", event.getItemValueString("txtname"));
		// test adapter class.....
		assertEquals("org.imixs.workflow.adapter.Example", event.getItemValueString("adapter.id"));

		// test activity 1000.20 submit
		event = openBPMNModelManager.findEventByID(model, 1100, 20);
		assertNotNull(event);
		assertEquals("adapter B", event.getItemValueString("txtname"));
		// test adapter class.....
		assertEquals("com.imixs.test.AdapterB", event.getItemValueString("adapter.id"));

	}

}
