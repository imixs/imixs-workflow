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
	BPMNModel model = null;
	OpenBPMNModelManager openBPMNModelManager = null;

	@Before
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new OpenBPMNModelManager();
		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/annotation_example.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			Assert.assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Test if annotations are assigned to a task documentation.
	 * 
	 * 
	 */
	@Test
	public void testSimple() {

		String VERSION = "1.0.0";

		// Test Environment
		ItemCollection profile;
		try {
			profile = openBPMNModelManager.loadDefinition(model);
			Assert.assertNotNull(profile);
			Assert.assertEquals(VERSION, profile.getItemValueString("$ModelVersion"));
		} catch (ModelException e) {
			Assert.fail(e.getMessage());
		}

		// test count of elements
		Assert.assertEquals(3, model.findAllActivities().size());

		// test task 1200
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 1200);
		Assert.assertNotNull(task);
		Assert.assertEquals("<b>inner sample text</b>", task.getItemValueString("rtfdescription"));

		// test Task 1100 (overwrite annotation)
		task = openBPMNModelManager.findTaskByID(model, 1100);
		Assert.assertNotNull(task);
		Assert.assertEquals("<b>custom text task2</b>", task.getItemValueString("rtfdescription"));

		// test Task 1000 (annotation)
		// task = openBPMNModelManager.findTaskByID(model, 1000);
		// Assert.assertNotNull(task);
		// Assert.assertEquals("<b>sample text1</b>",
		// task.getItemValueString("rtfdescription"));

	}

}
