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

import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;
import org.xml.sax.SAXException;

/**
 * Test class test the Imixs BPMNParser group resolution
 * 
 * @author rsoika
 */
public class TestBPMNParserGroups {
	BPMNModel model = null;
	ModelManager modelManager = null;

	@BeforeEach
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		modelManager = new ModelManager();
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
			model = BPMNModelFactory.read("/bpmn/link-event-basic.bpmn");

			assertNotNull(model);
			// Test Groups
			Set<String> groups = modelManager.findAllGroupsByModel(model);
			assertTrue(groups.contains("Simple"));
		} catch (ModelException | BPMNModelException e) {
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

			model = BPMNModelFactory.read("/bpmn/multi-groups.bpmn");
			assertNotNull(model);

		} catch (BPMNModelException e) {
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
