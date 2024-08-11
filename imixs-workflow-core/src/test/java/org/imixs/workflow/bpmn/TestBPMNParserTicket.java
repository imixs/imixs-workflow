package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
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
 * @author rsoika
 */
public class TestBPMNParserTicket {

	BPMNModel model = null;
	OpenBPMNModelManager openBPMNModelManager = null;

	@Before
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		try {
			openBPMNModelManager = new OpenBPMNModelManager();
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/ticket.bpmn"));
			model = openBPMNModelManager.getModel("1.0.0");
			Assert.assertNotNull(model);
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

	}

	/**
	 * Simple test of the attributes (items) returned by the ModelManager for Task
	 * and Event entities.
	 * 
	 * @throws ModelException
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void testSimple() throws ModelException {

		String VERSION = "1.0.0";

		Assert.assertNotNull(model);

		ItemCollection workitem = new ItemCollection().model(VERSION);
		// Test Environment
		ItemCollection profile = openBPMNModelManager.loadDefinition(workitem);
		Assert.assertNotNull(profile);
		Assert.assertEquals("environment.profile",
				profile.getItemValueString("name"));
		Assert.assertEquals("WorkflowEnvironmentEntity",
				profile.getItemValueString("type"));
		Assert.assertEquals(VERSION,
				profile.getItemValueString("$ModelVersion"));
		List plugins = profile.getItemValue("txtplugins");
		Assert.assertNotNull(plugins);
		Assert.assertEquals(4, plugins.size());
		Assert.assertEquals("org.imixs.workflow.plugins.AccessPlugin", plugins.get(0));
		Assert.assertEquals("org.imixs.workflow.plugins.OwnerPlugin", plugins.get(1));
		Assert.assertEquals("org.imixs.workflow.plugins.HistoryPlugin", plugins.get(2));
		Assert.assertEquals("org.imixs.workflow.plugins.ResultPlugin", plugins.get(3));

		Set<String> groups = openBPMNModelManager.findAllGroups(model);
		Assert.assertTrue(groups.contains("Ticket"));

		// test task 1000
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 1000);
		Assert.assertNotNull(task);

		Assert.assertEquals("<b>Create</b> a new ticket",
				task.getItemValueString(OpenBPMNUtil.TASK_ITEM_DOCUMENTATION));

		// test activity 1000.10 submit
		ItemCollection activity = openBPMNModelManager.findEventByID(model, 1000, 10);
		Assert.assertNotNull(activity);

		Assert.assertEquals("<b>Submitt</b> new ticket",
				activity.getItemValueString(OpenBPMNUtil.EVENT_ITEM_DOCUMENTATION));

		// test activity 1100.20 accept
		activity = openBPMNModelManager.findEventByID(model, 1100, 20);
		Assert.assertNotNull(activity);

		Assert.assertEquals("accept", activity.getItemValueString(OpenBPMNUtil.TASK_ITEM_NAME));

		// test activity 1200.20 - follow-up activity solve =>40
		activity = openBPMNModelManager.findEventByID(model, 1200, 20);
		Assert.assertNotNull(activity);

		Assert.assertEquals("reopen", activity.getItemValueString(OpenBPMNUtil.TASK_ITEM_NAME));

		// test activity 1200.40 - follow-up activity should not be returned
		activity = openBPMNModelManager.findEventByID(model, 1200, 40);
		Assert.assertNull(activity);

		// test activity 100.10
		activity = openBPMNModelManager.findEventByID(model, 1000, 10);
		Assert.assertNotNull(activity);

		Assert.assertEquals("submit", activity.getItemValueString(OpenBPMNUtil.TASK_ITEM_NAME));
		// Test Owner
		Assert.assertTrue(activity.getItemValueBoolean(OpenBPMNUtil.EVENT_ITEM_ACL_UPDATE));

		List owners = activity.getItemValue(OpenBPMNUtil.TASK_ITEM_ACL_OWNER_LIST_MAPPING);
		Assert.assertNotNull(owners);
		Assert.assertEquals(2, owners.size());
		Assert.assertTrue(owners.contains("namTeam"));
		Assert.assertTrue(owners.contains("namManager"));
	}

	/**
	 * Testing deprecated model items
	 * 
	 * @throws ParseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ModelException
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void testSimpleDeprecatedItemNames() throws ModelException {

		String VERSION = "1.0.0";

		Assert.assertNotNull(model);

		ItemCollection workitem = new ItemCollection().model(VERSION);
		// Test Environment
		ItemCollection profile = openBPMNModelManager.loadDefinition(workitem);
		Assert.assertNotNull(profile);
		Assert.assertEquals("environment.profile",
				profile.getItemValueString("txtname"));
		Assert.assertEquals("WorkflowEnvironmentEntity",
				profile.getItemValueString("type"));
		Assert.assertEquals(VERSION,
				profile.getItemValueString("$ModelVersion"));
		List plugins = profile.getItemValue("txtplugins");
		Assert.assertNotNull(plugins);
		Assert.assertEquals(4, plugins.size());
		Assert.assertEquals("org.imixs.workflow.plugins.AccessPlugin", plugins.get(0));
		Assert.assertEquals("org.imixs.workflow.plugins.OwnerPlugin", plugins.get(1));
		Assert.assertEquals("org.imixs.workflow.plugins.HistoryPlugin", plugins.get(2));
		Assert.assertEquals("org.imixs.workflow.plugins.ResultPlugin", plugins.get(3));

		Set<String> groups = openBPMNModelManager.findAllGroups(model);
		Assert.assertTrue(groups.contains("Ticket"));

		// test task 1000
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 1000);
		Assert.assertNotNull(task);

		Assert.assertEquals("<b>Create</b> a new ticket",
				task.getItemValueString("rtfdescription"));

		// test activity 1000.10 submit
		ItemCollection activity = openBPMNModelManager.findEventByID(model, 1000, 10);
		Assert.assertNotNull(activity);

		Assert.assertEquals("<b>Submitt</b> new ticket",
				activity.getItemValueString("rtfdescription"));

		// test activity 1100.20 accept
		activity = openBPMNModelManager.findEventByID(model, 1100, 20);
		Assert.assertNotNull(activity);

		Assert.assertEquals("accept", activity.getItemValueString("txtName"));

		// test activity 1200.20 - follow-up activity solve =>40
		activity = openBPMNModelManager.findEventByID(model, 1200, 20);
		Assert.assertNotNull(activity);

		Assert.assertEquals("reopen", activity.getItemValueString("txtName"));

		// test activity 1200.40 - follow-up activity should not be returned
		activity = openBPMNModelManager.findEventByID(model, 1200, 40);
		Assert.assertNull(activity);

		// test activity 100.10
		activity = openBPMNModelManager.findEventByID(model, 1000, 10);
		Assert.assertNotNull(activity);

		Assert.assertEquals("submit", activity.getItemValueString("txtName"));
		// Test Owner
		Assert.assertTrue(activity.getItemValueBoolean("keyupdateacl"));

		List owners = activity.getItemValue("keyownershipfields");
		Assert.assertNotNull(owners);
		Assert.assertEquals(2, owners.size());
		Assert.assertTrue(owners.contains("namTeam"));
		Assert.assertTrue(owners.contains("namManager"));
	}

	@Test
	public void testCorrupted() throws ParseException,
			ParserConfigurationException, SAXException, IOException {

		BPMNModel model = null;
		try {
			model = BPMNModelFactory.read("/bpmn/corrupted.bpmn");
			Assert.fail(); // exception expected
		} catch (BPMNModelException e) {
			e.printStackTrace();
		}

		Assert.assertNull(model);
	}

}
