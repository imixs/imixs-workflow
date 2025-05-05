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

/**
 * Test the Owner plug-in concerning the settings in a process entity.
 * 
 * If Owner settings are provided by the next process entity than these settings
 * should be set per default and the activity entity can provide additional
 * setting.
 * 
 * e.g.
 * 
 * ProcessEntity 'namaddwriteaccess' = 'jo'
 * ActivityEntity 'namaddwriteaccess' = 'anna'
 * 
 * then '$owner' should be 'jo','anna'
 * 
 * These tests extend the JUnit test in TestAccessPlugin
 * 
 * 
 * @author rsoika
 */
public class TestOwnerPluginProcessEntity {

	private final static Logger logger = Logger.getLogger(TestOwnerPluginProcessEntity.class.getName());

	protected ItemCollection workitem;
	protected ItemCollection event;

	protected MockWorkflowEnvironment workflowEnvironment;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {

		workflowEnvironment = new MockWorkflowEnvironment();
		workflowEnvironment.setUp();
		workflowEnvironment.loadBPMNModel("/bpmn/TestOwnerAndACL.bpmn");

		// prepare data
		workitem = new ItemCollection().model("1.0.0").task(100)
				.event(10);
		logger.info("[TestOwnerPluginProcessEntity] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("manfred");
		list.add("anna");
		workitem.replaceItemValue("namTeam", list);
		workitem.replaceItemValue("namCreator", "ronny");
	}

	/**
	 * Test if the Owner settings will not be changed if no ACL is set be
	 * process or activity
	 * 
	 * @throws ModelException
	 ***/
	@Test
	public void testOwnerNoUpdate() throws ModelException {
		Vector<String> list = new Vector<String>();
		list.add("Kevin");
		list.add("Julian");
		workitem.replaceItemValue(OwnerPlugin.OWNER, list);
		workitem.setTaskID(100);
		workitem.event(10);
		try {
			workflowEnvironment.getWorkflowService().processWorkItem(workitem);
		} catch (PluginException e) {
			fail(e.getMessage());
		}
		@SuppressWarnings("unchecked")
		List<String> ownerList = workitem.getItemValue(OwnerPlugin.OWNER);
		assertEquals(2, ownerList.size());
		assertTrue(ownerList.contains("Kevin"));
		assertTrue(ownerList.contains("Julian"));

	}

	/**
	 * Test if the Owner settings from the activityEntity are injected into the
	 * workitem
	 * 
	 * @throws ModelException
	 **/
	@Test
	public void testOwnerfromActivityEntity() throws ModelException {

		workitem.event(20);
		try {
			workflowEnvironment.getWorkflowService().processWorkItem(workitem);
		} catch (PluginException e) {
			fail(e.getMessage());
		}

		@SuppressWarnings("unchecked")
		List<String> onwerList = workitem.getItemValue(OwnerPlugin.OWNER);
		assertEquals(3, onwerList.size());
		assertTrue(onwerList.contains("joe"));
		assertTrue(onwerList.contains("manfred"));
		assertTrue(onwerList.contains("anna"));

	}

	/**
	 * Test if the ACL settings from the next processEntity are injected into
	 * the workitem
	 * 
	 * @throws ModelException
	 **/
	@Test
	public void testOwnerfromProcessEntity() throws ModelException {

		workitem.task(300).event(10);
		try {
			workflowEnvironment.getWorkflowService().processWorkItem(workitem);
		} catch (PluginException e) {
			fail(e.getMessage());
		}
		@SuppressWarnings("unchecked")
		List<String> ownerList = workitem.getItemValue(OwnerPlugin.OWNER);
		assertEquals(2, ownerList.size());
		assertTrue(ownerList.contains("joe"));
		assertTrue(ownerList.contains("sam"));
	}

	/**
	 * Test if the Owner settings from the next processEntity are ignored in
	 * case the ActivityEnttiy provides settings. Merge is not supported!
	 * 
	 * @throws ModelException
	 * 
	 **/
	@SuppressWarnings("unchecked")
	@Test
	public void testOwnerfromProcessEntityAndActivityEntity() throws ModelException {

		// set some old values
		Vector<String> list = new Vector<String>();
		list.add("Kevin");
		list.add("Julian");
		logger.info("..id=" + workitem.getUniqueID());
		workitem.replaceItemValue(OwnerPlugin.OWNER, list);
		workitem.task(300).event(20);
		try {
			workflowEnvironment.getWorkflowService().processWorkItem(workitem);
		} catch (PluginException e) {
			fail(e.getMessage());
		}
		// $writeAccess= anna , manfred, joe, sam
		List<String> onwerList = workitem.getItemValue(OwnerPlugin.OWNER);
		assertEquals(3, onwerList.size());
		assertTrue(onwerList.contains("joe"));
		// assertTrue(onwerList.contains("sam"));
		assertTrue(onwerList.contains("manfred"));
		assertTrue(onwerList.contains("anna"));
	}

}
