package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Test;
import org.xml.sax.SAXException;

import junit.framework.Assert;

/**
 * Test class test the Imixs BPMNParser group resolution
 * 
 * @author rsoika
 */
public class TestBPMNParserGroups {

	/**
	 * This test test the resolution of a singel process group
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException
	 */
	@Test
	public void testSingleGroup()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/link-event.bpmn");

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

		// Test Groups
		Assert.assertTrue(model.getGroups().contains("Simple"));

	}

	/**
	 * This test tests the resolution of multiple groups in a collaboration diagram
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException
	 */
	@Test
	public void testMultiGroups()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/test-groups"
				+ ".bpmn");

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

		// Test Groups
		Assert.assertEquals(2, model.getGroups().size());
		Assert.assertTrue(model.getGroups().contains("Protokoll"));
		Assert.assertTrue(model.getGroups().contains("Protokoll~Protokollpunkt"));
		
		// Test tasks per group
		List<ItemCollection> taskGroup=model.findTasksByGroup("Protokoll");
		Assert.assertEquals(4, taskGroup.size());

		 taskGroup=model.findTasksByGroup("Protokoll~Protokollpunkt");
		Assert.assertEquals(4, taskGroup.size());
	}

}
