package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
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
 * @author rsoika
 */
public class TestBPMNParserTicket {

	BPMNModel model = null;
	ModelManager modelManager = null;

	@BeforeEach
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		try {
			modelManager = new ModelManager();
			model = BPMNModelFactory.read("/bpmn/ticket.bpmn");
			assertNotNull(model);
		} catch (BPMNModelException e) {
			fail(e.getMessage());
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
		// Test Environment
		ItemCollection profile = modelManager.loadDefinition(model);
		assertNotNull(profile);
		assertEquals("environment.profile",
				profile.getItemValueString("name"));
		assertEquals("WorkflowEnvironmentEntity",
				profile.getItemValueString("type"));
		assertEquals(VERSION,
				profile.getItemValueString("$ModelVersion"));
		List plugins = profile.getItemValue("txtplugins");
		assertNotNull(plugins);
		assertEquals(4, plugins.size());
		assertEquals("org.imixs.workflow.plugins.AccessPlugin", plugins.get(0));
		assertEquals("org.imixs.workflow.plugins.OwnerPlugin", plugins.get(1));
		assertEquals("org.imixs.workflow.plugins.HistoryPlugin", plugins.get(2));
		assertEquals("org.imixs.workflow.plugins.ResultPlugin", plugins.get(3));

		Set<String> groups = modelManager.findAllGroupsByModel(model);
		assertTrue(groups.contains("Ticket"));

		// test task 1000
		ItemCollection task = modelManager.findTaskByID(model, 1000);
		assertNotNull(task);

		assertEquals("<b>Create</b> a new ticket",
				task.getItemValueString(BPMNUtil.TASK_ITEM_DOCUMENTATION));

		// test activity 1000.10 submit
		ItemCollection activity = modelManager.findEventByID(model, 1000, 10);
		assertNotNull(activity);

		assertEquals("<b>Submitt</b> new ticket",
				activity.getItemValueString(BPMNUtil.EVENT_ITEM_DOCUMENTATION));

		// test activity 1100.20 accept
		activity = modelManager.findEventByID(model, 1100, 20);
		assertNotNull(activity);

		assertEquals("accept", activity.getItemValueString(BPMNUtil.TASK_ITEM_NAME));

		// test activity 1200.20 - follow-up activity solve =>40
		activity = modelManager.findEventByID(model, 1200, 20);
		assertNotNull(activity);

		assertEquals("reopen", activity.getItemValueString(BPMNUtil.TASK_ITEM_NAME));

		// test activity 1200.40 - follow-up activity should not be returned
		activity = modelManager.findEventByID(model, 1200, 40);
		assertNull(activity);

		// test activity 100.10
		activity = modelManager.findEventByID(model, 1000, 10);
		assertNotNull(activity);

		assertEquals("submit", activity.getItemValueString(BPMNUtil.TASK_ITEM_NAME));
		// Test Owner
		assertTrue(activity.getItemValueBoolean(BPMNUtil.EVENT_ITEM_ACL_UPDATE));

		List owners = activity.getItemValue(BPMNUtil.TASK_ITEM_ACL_OWNER_LIST_MAPPING);
		assertNotNull(owners);
		assertEquals(2, owners.size());
		assertTrue(owners.contains("namTeam"));
		assertTrue(owners.contains("namManager"));
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
		// Test Environment
		ItemCollection profile = modelManager.loadDefinition(model);
		assertNotNull(profile);
		assertEquals("environment.profile",
				profile.getItemValueString("txtname"));
		assertEquals("WorkflowEnvironmentEntity",
				profile.getItemValueString("type"));
		assertEquals(VERSION,
				profile.getItemValueString("$ModelVersion"));
		List plugins = profile.getItemValue("txtplugins");
		assertNotNull(plugins);
		assertEquals(4, plugins.size());
		assertEquals("org.imixs.workflow.plugins.AccessPlugin", plugins.get(0));
		assertEquals("org.imixs.workflow.plugins.OwnerPlugin", plugins.get(1));
		assertEquals("org.imixs.workflow.plugins.HistoryPlugin", plugins.get(2));
		assertEquals("org.imixs.workflow.plugins.ResultPlugin", plugins.get(3));

		Set<String> groups = modelManager.findAllGroupsByModel(model);
		assertTrue(groups.contains("Ticket"));

		// test task 1000
		ItemCollection task = modelManager.findTaskByID(model, 1000);
		assertNotNull(task);

		assertEquals("<b>Create</b> a new ticket",
				task.getItemValueString("rtfdescription"));

		// test activity 1000.10 submit
		ItemCollection activity = modelManager.findEventByID(model, 1000, 10);
		assertNotNull(activity);

		assertEquals("<b>Submitt</b> new ticket",
				activity.getItemValueString("rtfdescription"));

		// test activity 1100.20 accept
		activity = modelManager.findEventByID(model, 1100, 20);
		assertNotNull(activity);

		assertEquals("accept", activity.getItemValueString("txtName"));

		// test activity 1200.20 - follow-up activity solve =>40
		activity = modelManager.findEventByID(model, 1200, 20);
		assertNotNull(activity);

		assertEquals("reopen", activity.getItemValueString("txtName"));

		// test activity 1200.40 - follow-up activity should not be returned
		activity = modelManager.findEventByID(model, 1200, 40);
		assertNull(activity);

		// test activity 100.10
		activity = modelManager.findEventByID(model, 1000, 10);
		assertNotNull(activity);

		assertEquals("submit", activity.getItemValueString("txtName"));
		// Test Owner
		assertTrue(activity.getItemValueBoolean("keyupdateacl"));

		List owners = activity.getItemValue("keyownershipfields");
		assertNotNull(owners);
		assertEquals(2, owners.size());
		assertTrue(owners.contains("namTeam"));
		assertTrue(owners.contains("namManager"));
	}

	@Test
	public void testCorrupted() throws ParseException,
			ParserConfigurationException, SAXException, IOException {

		BPMNModel model = null;
		try {
			model = BPMNModelFactory.read("/bpmn/corrupted.bpmn");
			fail(); // exception expected
		} catch (BPMNModelException e) {
			e.printStackTrace();
		}

		assertNull(model);
	}

}
