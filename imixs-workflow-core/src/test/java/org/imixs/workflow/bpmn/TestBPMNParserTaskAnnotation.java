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
 * Test class test the Imixs BPMNParser.
 * 
 * The test verifies if a annotation assigned to a task will update the
 * documentation field of the task (in case it is not explicit filled by the
 * task)
 * 
 * 
 * 
 * @author rsoika
 */
public class TestBPMNParserTaskAnnotation {

	/**
	 * Test if annotations are assigned to a task documentation.
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException
	 */
	@Test
	public void testSimple()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		String VERSION = "1.0.0";

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/annotation_example.bpmn");

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
		Assert.assertEquals(VERSION, profile.getItemValueString("$ModelVersion"));
		Assert.assertTrue(model.getGroups().contains("Annotation Example"));

		// test count of elements
		Assert.assertEquals(3, model.findAllTasks().size());

		// test task 1200
		ItemCollection task = model.getTask(1200);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("<b>inner sample text</b>", task.getItemValueString("rtfdescription"));

	
		// test Task 1100 (overwrite annotation)
		task = model.getTask(1100);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("<b>custom text task2</b>", task.getItemValueString("rtfdescription"));

		
		// test Task 1000 (annotation)
		task = model.getTask(1000);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("<b>sample text1</b>", task.getItemValueString("rtfdescription"));

	
	}

}
