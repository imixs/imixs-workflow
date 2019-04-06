package org.imixs.workflow.plugins;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.AccessPlugin;
import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test the ACL plugin.
 * 
 * Also test the fallback mode
 * 
 * @author rsoika
 * 
 */
public class TestAccessPlugin {

	private final static Logger logger = Logger.getLogger(TestAccessPlugin.class.getName());

	AccessPlugin accessPlugin = null;
	ItemCollection documentContext;
	ItemCollection documentActivity;
	WorkflowMockEnvironment workflowMockEnvironment;

	@Before
	public void setup() throws PluginException, ModelException, AdapterException {

		workflowMockEnvironment = new WorkflowMockEnvironment();
		workflowMockEnvironment.setModelPath("/bpmn/TestAccessPlugin.bpmn");

		workflowMockEnvironment.setup();

		accessPlugin = new AccessPlugin();
		try {
			accessPlugin.init(workflowMockEnvironment.getWorkflowService());
		} catch (PluginException e) {

			e.printStackTrace();
		}

		// prepare data
		documentContext = new ItemCollection();
		documentContext.replaceItemValue("$processid", 100);
		documentContext.replaceItemValue("$modelversion", "1.0.0");

	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void simpleTest() throws ModelException {

 		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 10);
		try {
			accessPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue("$WriteAccess");

		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testNoUpdate() throws ModelException {
		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 20);

		try {
			accessPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue("$WriteAccess");

		Assert.assertEquals(0, writeAccess.size());
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void fieldMappingTest() throws ModelException {

		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 10);

		logger.info("[TestAccessPlugin] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("manfred");
		list.add("anna");
		documentContext.replaceItemValue("namTeam", list);

		documentContext.replaceItemValue("namCreator", "ronny");

		try {
			accessPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue("$WriteAccess");

		Assert.assertEquals(4, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));
		Assert.assertTrue(writeAccess.contains("manfred"));
		Assert.assertTrue(writeAccess.contains("anna"));
	}

	/**
	 * This test verifies if a list of users provided by the fieldMapping is mapped
	 * correctly into the workItem
	 * 
	 * @throws ModelException
	 */
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void staticUserGroupMappingTest() throws ModelException {

		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 30);
		try {
			accessPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue("$WriteAccess");

		Assert.assertEquals(4, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("tom"));
		Assert.assertTrue(writeAccess.contains("sam"));
		Assert.assertTrue(writeAccess.contains("anna"));
		Assert.assertTrue(writeAccess.contains("joe"));
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void fallbackTest() throws ModelException {

		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 10);

		documentActivity.replaceItemValue("keyaccessmode", "0");

		try {
			accessPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue("$WriteAccess");

		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));
	}

	/**
	 * Test Conditional event
	 * 
	 * issue #327
	 * 
	 * @throws ModelException
	 */
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testCondition() throws ModelException {

		// case I.
		
		documentContext.setTaskID(200);
		documentContext.setEventID(20);
		documentContext.replaceItemValue("_budget", 50);
		try {
			documentContext = workflowMockEnvironment.processWorkItem(documentContext);
		} catch (AdapterException | PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue("$WriteAccess");

		Assert.assertEquals(300, documentContext.getTaskID());
		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));

		// case II.

		documentContext.setTaskID(200);
		documentContext.setEventID(20);
		documentContext.replaceItemValue("_budget", 570);
		try {
			documentContext = workflowMockEnvironment.processWorkItem(documentContext);
		} catch (AdapterException | PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		writeAccess = documentContext.getItemValue("$WriteAccess");

		Assert.assertEquals(400, documentContext.getTaskID());
		Assert.assertEquals(1, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("tom"));

	}
}
