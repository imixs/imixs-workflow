package org.imixs.workflow.plugins;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test the ACL plugin.
 * 
 * Also test the fallback mode
 * 
 * @author rsoika
 * 
 */
public class TestOwnerPlugin {

	private final static Logger logger = Logger
			.getLogger(TestOwnerPlugin.class.getName());

	OwnerPlugin ownerPlugin = null;
	ItemCollection documentContext;
	ItemCollection documentActivity;
	WorkflowContext workflowContext;
	
	@Before
	public void setup() {
		
		workflowContext = Mockito.mock(WorkflowContext.class);
		
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

		documentActivity=new ItemCollection();
		documentActivity.replaceItemValue("keyupdateAcl", true);

		Vector<String> list = new Vector<String>();
		list.add("sam");
		list.add("joe");
		documentActivity.replaceItemValue("namOwnershipNames", list);

		
		try {
			ownerPlugin.run( documentContext,documentActivity);
		} catch (PluginException e) {
			
			e.printStackTrace();
			Assert.fail();
		}
		
		List writeAccess = documentContext.getItemValue("namOwner");

		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));
	}



	@SuppressWarnings({  "rawtypes" })
	@Test
	public void testNoUpdate() {

		documentActivity=new ItemCollection();
		Vector<String> list = new Vector<String>();
		list.add("sam");
		list.add("joe");
		documentActivity.replaceItemValue("namOwnershipNames", list);

		
		try {
			ownerPlugin.run( documentContext,documentActivity);
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

		documentActivity=new ItemCollection();
		Vector<String> list = new Vector<String>();
		list.add("sam");
		list.add("joe");
		documentActivity.replaceItemValue("namOwnershipNames", list);

		documentActivity.replaceItemValue("keyOwnershipMode", "0");
		
		
		try {
			ownerPlugin.run( documentContext,documentActivity);
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
