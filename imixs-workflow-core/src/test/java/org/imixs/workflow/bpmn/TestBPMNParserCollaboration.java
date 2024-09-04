package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.ParseException;
import java.util.Set;

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
 * Test class test the Imixs BPMNParser
 * 
 * Special cases with collaboration diagrams
 * 
 * @author rsoika
 */
public class TestBPMNParserCollaboration {
	BPMNModel model = null;
	ModelManager openBPMNModelManager = null;

	@BeforeEach
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new ModelManager();
		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/collaboration.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testSimple()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		// Test Environment
		ItemCollection profile = openBPMNModelManager.loadDefinition(model);
		assertNotNull(profile);
		assertEquals("environment.profile", profile.getItemValueString("txtname"));
		assertEquals("WorkflowEnvironmentEntity", profile.getItemValueString("type"));
		assertEquals("1.0.0", profile.getItemValueString("$ModelVersion"));

		Set<String> groups = openBPMNModelManager.findAllGroupsByModel(model);
		// List<String> groups = model.getGroups();
		// Test Groups
		assertFalse(groups.contains("Collaboration"));
		assertTrue(groups.contains("WorkflowGroup1"));
		assertTrue(groups.contains("WorkflowGroup2"));

		// test count of elements
		// assertEquals(2, model.findAllTasks().size());

		// test task 1000
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 1000);
		assertNotNull(task);

		// test activity 1000.10 submit
		ItemCollection activity = openBPMNModelManager.findEventByID(model, 1000, 10);
		assertNotNull(activity);
		assertEquals("submit", activity.getItemValueString("txtname"));

		// test task 1100
		task = openBPMNModelManager.findTaskByID(model, 2000);
		assertNotNull(task);

	}

}
