package org.imixs.workflow.plugins;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.AbstractWorkflowServiceTest;
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
public class TestOwnerPlugin extends AbstractWorkflowServiceTest {

	private final static Logger logger = Logger.getLogger(TestOwnerPlugin.class.getName());

	OwnerPlugin ownerPlugin = null;
	ItemCollection documentContext;
	ItemCollection documentActivity;
	
	@Before
	public void setup() throws PluginException {
		super.setup();
		
		ownerPlugin = new OwnerPlugin();
		try {
			ownerPlugin.init(workflowContext);
		} catch (PluginException e) {

			e.printStackTrace();
		}

		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestOwnerPlugin] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("manfred");
		list.add("anna");
		documentContext.replaceItemValue("namTeam", list);

		documentContext.replaceItemValue("namCreator", "ronny");
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void simpleTest() {
 
		documentActivity = new ItemCollection();
		documentActivity.replaceItemValue("keyupdateAcl", true);

		Vector<String> list = new Vector<String>();
		list.add("sam");
		list.add("joe");
		documentActivity.replaceItemValue("namOwnershipNames", list);

		try {
			ownerPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue("namOwner");

		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));
	}
	
	
	
	
	
	/**
	 * Test if the current value of namowner can be set as the new value
	 */
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testUpdateOfamOwner() {
 
		documentActivity = new ItemCollection();
		documentActivity.replaceItemValue("keyupdateAcl", true);

		Vector<String> list = new Vector<String>();
		list.add("sam");
		list.add("joe");
		documentActivity.replaceItemValue("namOwnershipNames", list);

		// set a current owner
		documentContext.replaceItemValue("namOwner", "ralph");
		documentActivity.replaceItemValue("keyOwnershipFields", "namowner");
		
		
		
		try {
			ownerPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue("namOwner");

		Assert.assertEquals(3, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));
		Assert.assertTrue(writeAccess.contains("ralph"));
	}
	
	
	

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testNoUpdate() {

		documentActivity = new ItemCollection();
		Vector<String> list = new Vector<String>();
		list.add("sam");
		list.add("joe");
		documentActivity.replaceItemValue("namOwnershipNames", list);

		try {
			ownerPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue("namOwner");

		Assert.assertEquals(0, writeAccess.size());
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void fallbackTest() {

		documentActivity = new ItemCollection();
		Vector<String> list = new Vector<String>();
		list.add("sam");
		list.add("joe");
		documentActivity.replaceItemValue("namOwnershipNames", list);

		documentActivity.replaceItemValue("keyOwnershipMode", "0");

		try {
			ownerPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue("namowner");

		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));
	}

}
