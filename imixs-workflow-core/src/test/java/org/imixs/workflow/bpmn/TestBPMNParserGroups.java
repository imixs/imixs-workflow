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

import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.xml.sax.SAXException;

/**
 * Test class to test the behavior of the ModelManager.
 * 
 * Test class test the Imixs BPMNParser group resolution
 * 
 * @author rsoika
 */
public class TestBPMNParserGroups {

	BPMNModel model = null;
	ModelManager modelManager = null;
	MockWorkflowContext workflowContext;

	@BeforeEach
	public void setup() {
		try {
			workflowContext = new MockWorkflowContext();
			modelManager = new ModelManager(workflowContext);
			workflowContext.loadBPMNModelFromFile("/bpmn/dataobject_example1.bpmn");
			model = workflowContext.fetchModel("1.0.0");
			assertNotNull(model);

		} catch (ModelException | PluginException e) {
			fail(e.getMessage());
		}
	}

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
	public void testSingleGroup() {
		try {
			workflowContext.loadBPMNModelFromFile("/bpmn/link-event-basic.bpmn");
			model = workflowContext.fetchModel("1.0.0");

			assertNotNull(model);
			// Test Groups
			Set<String> groups = modelManager.findAllGroupsByModel(model);
			assertTrue(groups.contains("Simple"));
		} catch (ModelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * This test tests the resolution of multiple groups in a collaboration diagram
	 * 
	 * The method findAllGroups should return only the group names from the Pools
	 * but not the default process name. This is because the default process does
	 * not contain a start event!
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

		try {
			workflowContext.loadBPMNModelFromFile("/bpmn/multi-groups.bpmn");
			model = workflowContext.fetchModel("protokoll-de-1.0.0");

			assertNotNull(model);

		} catch (ModelException e) {
			fail();
		}

		// Test Groups
		Set<String> groups = modelManager.findAllGroupsByModel(model);
		assertEquals(2, groups.size());
		assertTrue(groups.contains("Protokoll"));
		assertTrue(groups.contains("Protokollpunkt"));
		assertFalse(groups.contains("Default Process"));

		// Test tasks per group
		BPMNProcess process = null;
		try {
			process = model.findProcessByName("Protokoll");
		} catch (BPMNModelException e) {
			fail(e.getMessage());
		}
		Set<Activity> activities = process.getActivities();
		assertEquals(4, activities.size());

		try {
			process = model.findProcessByName("Protokollpunkt");
		} catch (BPMNModelException e) {
			fail(e.getMessage());
		}
		activities = process.getActivities();
		assertEquals(4, activities.size());

		// test the Default Group (Public Process)
		BPMNProcess defaultProcess = model.openDefaultProces();
		assertEquals("Default Process", defaultProcess.getName());

	}

}
