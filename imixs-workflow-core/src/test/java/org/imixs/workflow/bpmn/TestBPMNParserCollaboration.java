package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.text.ParseException;
import java.util.Set;

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
 * Test class test the Imixs BPMNParser
 * 
 * Special cases with collaboration diagrams
 * 
 * @author rsoika
 */
public class TestBPMNParserCollaboration {
	BPMNModel model = null;
	OpenBPMNModelManager openBPMNModelManager = null;

	@Before
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new OpenBPMNModelManager();
		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/collaboration.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			Assert.assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void testSimple()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		ItemCollection workitem = new ItemCollection().model("1.0.0");

		// Test Environment
		ItemCollection profile = openBPMNModelManager.loadDefinition(workitem);
		Assert.assertNotNull(profile);
		Assert.assertEquals("environment.profile", profile.getItemValueString("txtname"));
		Assert.assertEquals("WorkflowEnvironmentEntity", profile.getItemValueString("type"));
		Assert.assertEquals("1.0.0", profile.getItemValueString("$ModelVersion"));

		Set<String> groups = openBPMNModelManager.findAllGroups(model);
		// List<String> groups = model.getGroups();
		// Test Groups
		Assert.assertFalse(groups.contains("Collaboration"));
		Assert.assertTrue(groups.contains("WorkflowGroup1"));
		Assert.assertTrue(groups.contains("WorkflowGroup2"));

		// test count of elements
		// Assert.assertEquals(2, model.findAllTasks().size());

		// test task 1000
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 1000);
		Assert.assertNotNull(task);
		// Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("WorkflowGroup1", task.getItemValueString("txtworkflowgroup"));

		// test activity for task 1000
		// Collection<ItemCollection> activities = model.findAllEventsByTask(1000);
		// Assert.assertNotNull(activities);
		// Assert.assertEquals(1, activities.size());

		// test activity 1000.10 submit
		ItemCollection activity = openBPMNModelManager.findEventByID(model, 1000, 10);
		Assert.assertNotNull(activity);
		Assert.assertEquals("submit", activity.getItemValueString("txtname"));
		Assert.assertEquals(2000, activity.getItemValueInteger("numNextProcessID"));

		// test task 1100
		task = openBPMNModelManager.findTaskByID(model, 2000);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("WorkflowGroup2", task.getItemValueString("txtworkflowgroup"));

	}

}
