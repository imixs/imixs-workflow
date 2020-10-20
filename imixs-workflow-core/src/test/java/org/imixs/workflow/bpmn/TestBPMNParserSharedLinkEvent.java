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

import org.junit.Assert;

/**
 * Test class test the Imixs BPMNParser.
 * 
 * This test verifies the linking an imixs-event with an imixs-task using a
 * intermediate catch and intermediate throw link-event.
 * 
 * @author rsoika
 */
public class TestBPMNParserSharedLinkEvent {

	/**
	 * This test test intermediate link events and also loop throw events
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException 
	 */
	@Test
	public void testLinkEventSimple() throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/shared-link-event.bpmn");

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
		Assert.assertTrue(model.getGroups().contains("Simple"));

		// test count of elements
		Assert.assertEquals(3, model.findAllTasks().size());

		// test task 1000
		ItemCollection task = model.getTask(1000 );
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Simple", task.getItemValueString("txtworkflowgroup"));

		
		Assert.assertEquals("Task Shared Link Event1", task.getItemValueString("txtName"));
		
		// test shared events
		Assert.assertEquals(3,model.findAllEventsByTask(1000).size());
		
		ItemCollection event=model.getEvent(1000, 99);
		Assert.assertEquals("cancel", event.getItemValueString("txtName"));
		
		// test shared events
		Assert.assertEquals(2,model.findAllEventsByTask(1100).size());
		Assert.assertEquals(0,model.findAllEventsByTask(1200).size());

	}
	
	
	
	

}
