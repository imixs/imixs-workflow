package org.imixs.workflow.plugins;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.AbstractWorkflowEnvironment;
import org.imixs.workflow.engine.plugins.AccessPlugin;
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
public class TestAccessPlugin extends AbstractWorkflowEnvironment {

	private final static Logger logger = Logger.getLogger(TestAccessPlugin.class.getName());

	AccessPlugin accessPlugin = null;
	ItemCollection documentContext;
	ItemCollection documentActivity;

	@Before
	public void setup() throws PluginException, ModelException {
		this.setModelPath("/bpmn/TestAccessPlugin.bpmn");
		
		super.setup();

		accessPlugin = new AccessPlugin();
		try {
			accessPlugin.init(workflowContext);
		} catch (PluginException e) {

			e.printStackTrace();
		}

		// prepare data
		documentContext = new ItemCollection();
		documentContext.replaceItemValue("$processid", 100);
		
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void simpleTest() throws ModelException {

		documentActivity = this.getModel().getEvent(100, 10);	
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
		documentActivity = this.getModel().getEvent(100, 20);
		
		
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

		documentActivity = this.getModel().getEvent(100, 10);
		
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
	 * This test verifies if a list of users provided by the fieldMapping is
	 * mapped correctly into the workItem
	 * @throws ModelException 
	 */
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void staticUserGroupMappingTest() throws ModelException {

		documentActivity = this.getModel().getEvent(100, 30);
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

		documentActivity = this.getModel().getEvent(100, 10);
		
		
		
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

}
