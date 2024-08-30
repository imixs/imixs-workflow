package org.imixs.workflow.plugins;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.OwnerPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

	OwnerPlugin ownerPlugin = null;
	ItemCollection workitem;
	ItemCollection event;

	protected WorkflowMockEnvironment workflowEngine;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {

		workflowEngine = new WorkflowMockEnvironment();
		workflowEngine.setUp();
		workflowEngine.loadBPMNModel("/bpmn/TestOwnerPlugin.bpmn");

		ownerPlugin = new OwnerPlugin();
		try {
			ownerPlugin.init(workflowEngine.getWorkflowService());
		} catch (PluginException e) {

			e.printStackTrace();
		}
		workitem = workflowEngine.getDocumentService().load("W0000-00001");
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
			Assert.fail();
		}

		List writeAccess = workitem.getItemValue(OwnerPlugin.OWNER);

		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));
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
			Assert.fail();
		}

		List ownerList = workitem.getItemValue(OwnerPlugin.OWNER);

		Assert.assertEquals(3, ownerList.size());
		Assert.assertTrue(ownerList.contains("joe"));
		Assert.assertTrue(ownerList.contains("sam"));
		Assert.assertTrue(ownerList.contains("ralph"));
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

		BPMNModel model = workflowEngine.getModelService().getModel("1.0.0");
		event = workflowEngine.getModelService().getOpenBPMNModelManager().findEventByID(model, 100, 10);
		event.replaceItemValue("keyupdateAcl", true);
		event.replaceItemValue("keyOwnershipFields", "[sam, tom,  anna ,]");

		try {
			ownerPlugin.run(workitem, event);
		} catch (PluginException e) {
			Assert.fail(e.getMessage());
		}

		List writeAccess = workitem.getItemValue(OwnerPlugin.OWNER);
		// 3 values expected!
		Assert.assertEquals(3, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("tom"));
		Assert.assertTrue(writeAccess.contains("sam"));
		Assert.assertTrue(writeAccess.contains("anna"));
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testNoUpdate() throws ModelException {

		BPMNModel model = workflowEngine.getModelService().getModel("1.0.0");
		event = workflowEngine.getModelService().getOpenBPMNModelManager().findEventByID(model, 100, 20);

		try {
			ownerPlugin.run(workitem, event);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = workitem.getItemValue(OwnerPlugin.OWNER);
		Assert.assertEquals(0, writeAccess.size());
	}

}
