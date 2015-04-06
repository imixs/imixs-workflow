package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.imixs.workflow.ItemCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Test class test the parsing of a json file used by the workflowRestService
 * method postWorkitemJSON(InputStream requestBodyStream)
 * 
 * 
 * @author rsoika
 */
public class TestBPMNParser {

	@Before
	public void setup() {

	}

	@After
	public void teardown() {

	}

	@Ignore
	@Test
	public void testSimple() throws ParseException,
			ParserConfigurationException, SAXException, IOException {

		InputStream inputStream = getClass().getResourceAsStream(
				"/bpmn/ticket.bpmn");

		List<ItemCollection> model = null;
		try {
			model = BPMNParser.parseModel(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(model);

	}

	@Ignore
	@Test(expected = ParseException.class)
	public void testCorrupted() throws ParseException,
			ParserConfigurationException, SAXException, IOException {

		InputStream inputStream = getClass().getResourceAsStream(
				"/bpmn/corrupted.bpmn");

		List<ItemCollection> model = null;
		try {
			model = BPMNParser.parseModel(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNull(model);
	}

}
