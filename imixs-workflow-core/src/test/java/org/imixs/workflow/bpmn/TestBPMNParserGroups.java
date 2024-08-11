package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.text.ParseException;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

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
	OpenBPMNModelManager openBPMNModelManager = null;

	@Before
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new OpenBPMNModelManager();

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
	public void testSingleGroup()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/link-event.bpmn"));
		} catch (ModelException | BPMNModelException e) {
			Assert.fail();
		}
		model = openBPMNModelManager.getBPMNModel("1.0.0");

		Assert.assertNotNull(model);

		// Test Groups
		Set<String> groups = openBPMNModelManager.findAllGroups(model);

		Assert.assertTrue(groups.contains("Simple"));

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

		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/multi-groups.bpmn"));
		} catch (ModelException | BPMNModelException e) {
			Assert.fail();
		}
		model = openBPMNModelManager.getBPMNModel("protokoll-de-1.0.0");
		Assert.assertNotNull(model);

		// Test Groups
		Set<String> groups = openBPMNModelManager.findAllGroups(model);
		Assert.assertEquals(3, groups.size());
		Assert.assertTrue(groups.contains("Protokoll"));
		Assert.assertTrue(groups.contains("Protokollpunkt"));
		Assert.assertTrue(groups.contains("Default Process"));

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
	}

}
