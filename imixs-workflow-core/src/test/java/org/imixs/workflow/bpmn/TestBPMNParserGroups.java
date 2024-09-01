package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.text.ParseException;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
	ModelManager openBPMNModelManager = null;

	@Before
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new ModelManager();
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
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/link-event-basic.bpmn"));

			model = openBPMNModelManager.getModel("1.0.0");

			Assert.assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		// Test Groups
		Set<String> groups = openBPMNModelManager.findAllGroups(model);

		Assert.assertTrue(groups.contains("Simple"));

	}

	/**
	 * This test tests the resolution of multiple groups in a collaboration diagram
	 * 
	 * The method findAllGroups should return only the group names from the Pools
	 * but not the default process name.
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
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/multi-groups.bpmn"));
			model = openBPMNModelManager.getModel("protokoll-de-1.0.0");
			Assert.assertNotNull(model);

		} catch (ModelException | BPMNModelException e) {
			Assert.fail();
		}

		// Test Groups
		Set<String> groups = openBPMNModelManager.findAllGroups(model);
		Assert.assertEquals(2, groups.size());
		Assert.assertTrue(groups.contains("Protokoll"));
		Assert.assertTrue(groups.contains("Protokollpunkt"));
		Assert.assertFalse(groups.contains("Default Process"));

		// Test tasks per group
		BPMNProcess process = null;
		try {
			process = model.findProcessByName("Protokoll");
		} catch (BPMNModelException e) {
			Assert.fail(e.getMessage());
		}
		Set<Activity> activities = process.getActivities();
		Assert.assertEquals(4, activities.size());

		try {
			process = model.findProcessByName("Protokollpunkt");
		} catch (BPMNModelException e) {
			Assert.fail(e.getMessage());
		}
		activities = process.getActivities();
		Assert.assertEquals(4, activities.size());

		// test the Default Group (Public Process)
		BPMNProcess defaultProcess = model.openDefaultProces();
		Assert.assertEquals("Default Process", defaultProcess.getName());

	}

}
