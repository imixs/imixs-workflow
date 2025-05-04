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
	ModelManager openBPMNModelManager = null;

	@BeforeEach
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new ModelManager();
		try {
			model = BPMNModelFactory.read("/bpmn/annotation_example.bpmn");
			assertNotNull(model);
		} catch (BPMNModelException e) {
			fail(e.getMessage());
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
			assertNotNull(profile);
			assertEquals(VERSION, profile.getItemValueString("$ModelVersion"));
		} catch (ModelException e) {
			fail(e.getMessage());
		}

		// test count of elements
		assertEquals(3, model.findAllActivities().size());

		// test task 1200
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 1200);
		assertNotNull(task);
		assertEquals("<b>inner sample text</b>", task.getItemValueString("rtfdescription"));

		// test Task 1100 (overwrite annotation)
		task = openBPMNModelManager.findTaskByID(model, 1100);
		assertNotNull(task);
		assertEquals("<b>custom text task2</b>", task.getItemValueString("rtfdescription"));

		// test Task 1000 (annotation)
		// task = openBPMNModelManager.findTaskByID(model, 1000);
		// assertNotNull(task);
		// assertEquals("<b>sample text1</b>",
		// task.getItemValueString("rtfdescription"));

	}

}
