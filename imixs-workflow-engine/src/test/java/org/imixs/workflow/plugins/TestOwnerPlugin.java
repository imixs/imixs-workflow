package org.imixs.workflow.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.MockWorkflowEnvironment;
import org.imixs.workflow.engine.plugins.OwnerPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.openbpmn.bpmn.BPMNModel;

/**
 * Test the ACL plugin.
 * 
 * Also test the fallback mode
 * 
 * @author rsoika
 * 
 */
public class TestOwnerPlugin {

	private final static Logger logger = Logger.getLogger(TestOwnerPlugin.class.getName());

	@InjectMocks
	protected OwnerPlugin ownerPlugin;

	ItemCollection workitem;
	ItemCollection event;

	protected MockWorkflowEnvironment workflowEnvironment;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {
		// Ensures that @Mock and @InjectMocks annotations are processed
		MockitoAnnotations.openMocks(this);

		workflowEnvironment = new MockWorkflowEnvironment();
		ownerPlugin.setWorkflowService(workflowEnvironment.getWorkflowService());

		workflowEnvironment.setUp();
		workflowEnvironment.registerPlugin(ownerPlugin);

		workflowEnvironment.loadBPMNModelFromFile("/bpmn/TestOwnerPlugin.bpmn");

		workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		workitem.model("1.0.0").task(100);

		// prepare data
		workitem = new ItemCollection().model("1.0.0").task(100)
				.event(10);
		logger.info("[TestOwnerPlugin] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("manfred");
		list.add("anna");
		workitem.replaceItemValue("namTeam", list);
		workitem.replaceItemValue("namCreator", "ronny");

	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void simpleTest() {

		event = new ItemCollection();
		event.replaceItemValue("keyupdateAcl", true);
		event.replaceItemValue("numNextProcessID", 100);

		Vector<String> list = new Vector<String>();
		list.add("sam");
		list.add("joe");
		event.replaceItemValue("namOwnershipNames", list);

		try {
			ownerPlugin.run(workitem, event);
		} catch (PluginException e) {

			e.printStackTrace();
			fail();
		}

		List writeAccess = workitem.getItemValue(OwnerPlugin.OWNER);

		assertEquals(2, writeAccess.size());
		assertTrue(writeAccess.contains("joe"));
		assertTrue(writeAccess.contains("sam"));
	}

	/**
	 * Test if the current value of namowner can be set as the new value
	 */
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testUpdateOfnamOwner() {

		event = new ItemCollection();
		event.replaceItemValue("keyupdateAcl", true);
		event.replaceItemValue("numNextProcessID", 100);

		Vector<String> list = new Vector<String>();
		list.add("sam");
		list.add("joe");
		event.replaceItemValue("namOwnershipNames", list);

		// set a current owner
		workitem.replaceItemValue(OwnerPlugin.OWNER, "ralph");
		event.replaceItemValue("keyOwnershipFields", OwnerPlugin.OWNER);

		try {
			ownerPlugin.run(workitem, event);
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}

		List ownerList = workitem.getItemValue(OwnerPlugin.OWNER);

		assertEquals(3, ownerList.size());
		assertTrue(ownerList.contains("joe"));
		assertTrue(ownerList.contains("sam"));
		assertTrue(ownerList.contains("ralph"));
	}

	/**
	 * This test verifies if a list of users provided by the fieldMapping is
	 * mapped correctly into the workItem
	 * 
	 * @throws ModelException
	 */
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void staticUserGroupMappingTest() throws ModelException {

		BPMNModel model = workflowEnvironment.getModelService().getModel("1.0.0");
		event = workflowEnvironment.getModelManager().findEventByID(model, 100, 10);
		event.replaceItemValue("keyupdateAcl", true);
		event.replaceItemValue("keyOwnershipFields", "[sam, tom,  anna ,]");

		try {
			ownerPlugin.run(workitem, event);
		} catch (PluginException e) {
			fail(e.getMessage());
		}

		List writeAccess = workitem.getItemValue(OwnerPlugin.OWNER);
		// 3 values expected!
		assertEquals(3, writeAccess.size());
		assertTrue(writeAccess.contains("tom"));
		assertTrue(writeAccess.contains("sam"));
		assertTrue(writeAccess.contains("anna"));
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testNoUpdate() throws ModelException {

		BPMNModel model = workflowEnvironment.getModelService().getModel("1.0.0");
		event = workflowEnvironment.getModelManager().findEventByID(model, 100, 20);

		try {
			ownerPlugin.run(workitem, event);
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}

		List writeAccess = workitem.getItemValue(OwnerPlugin.OWNER);
		assertEquals(0, writeAccess.size());
	}

	/**
	 * This test simulates a complex gateway situation where the OwnerPlugin and the
	 * Kernel call the eval() method multiple times.
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testMultipleEvents() throws ModelException {

		workflowEnvironment.loadBPMNModelFromFile("/bpmn/TestOwnerPluginWithEval.bpmn");
		workitem.task(1000).event(10);
		try {
			workflowEnvironment.getWorkflowService().processWorkItem(workitem);
		} catch (PluginException e) {
			fail(e.getMessage());
		}

		assertEquals(1100, workitem.getTaskID());
	}

}
