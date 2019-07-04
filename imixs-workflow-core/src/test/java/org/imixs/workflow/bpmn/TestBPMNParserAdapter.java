package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Test;
import org.xml.sax.SAXException;

import junit.framework.Assert;

/**
 * Test class test the Imixs BPMNParser with an Adapter definition in a Event
 * (Signal Event)
 * 
 * @author rsoika
 */
public class TestBPMNParserAdapter {

	
	@Test
	public void testEventAdapter()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/adapter.bpmn");

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

		// test activity 1000.20 submit
		ItemCollection event = model.getEvent(1000, 20);
		Assert.assertNotNull(event);
		Assert.assertEquals(1100, event.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("submit", event.getItemValueString("txtname"));

		// test adapter class.....
		Assert.assertEquals("org.imixs.workflow.adapter.Example", event.getItemValueString("adapter.id"));

	}

	@Test
	public void testEventMulitAdapter()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/adapter_multi.bpmn");

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

		// test activity 1000.20 submit
		ItemCollection event = model.getEvent(1000, 20);
		Assert.assertNotNull(event);
		Assert.assertEquals(1100, event.getItemValueInteger("numNextProcessID"));
		Assert.assertEquals("adapter A", event.getItemValueString("txtname"));
		// test adapter class.....
		Assert.assertEquals("org.imixs.workflow.adapter.Example", event.getItemValueString("adapter.id"));

		// test activity 1100.10 submit
		event = model.getEvent(1100, 10);
		Assert.assertNotNull(event);
		Assert.assertEquals("adapter A", event.getItemValueString("txtname"));
		// test adapter class.....
		Assert.assertEquals("org.imixs.workflow.adapter.Example", event.getItemValueString("adapter.id"));

		// test activity 1000.20 submit
		event = model.getEvent(1100, 20);
		Assert.assertNotNull(event);
		Assert.assertEquals("adapter B", event.getItemValueString("txtname"));
		// test adapter class.....
		Assert.assertEquals("com.imixs.test.AdapterB", event.getItemValueString("adapter.id"));

	}

}
