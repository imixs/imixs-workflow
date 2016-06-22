package org.imixs.workflow.plugins;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.AbstractPluginTest;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the Owner plug-in concerning the settings in a process entity.
 * 
 * If Owner settings are provided by the next process entity than these settings
 * should be set per default and the activity entity can provide additional
 * setting.
 * 
 * 
 * e.g.
 * 
 * ProcessEntity 'namaddwriteaccess' = 'jo'
 * 
 * ActivityEntity 'namaddwriteaccess' = 'anna'
 * 
 * 
 * then namowner should be 'jo','anna'
 * 
 * 
 * 
 * These tests extend the JUnit test in TestAccessPlugin
 * 
 * 
 * @author rsoika
 * 
 */
public class TestOwnerPluginProcessEntity extends AbstractPluginTest {

	private final static Logger logger = Logger.getLogger(TestOwnerPluginProcessEntity.class.getName());

	OwnerPlugin ownerPlugin = null;
	ItemCollection documentContext;
	ItemCollection documentActivity, documentProcess;

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

	/**
	 * Test if the Owner settings will not be changed if no ACL is set be
	 * process or activity
	 ***/
	@Test
	public void testOwnerNoUpdate() {
		Vector<String> list = new Vector<String>();
		list.add("Kevin");
		list.add("Julian");
		documentContext.replaceItemValue("namowner", list);

		documentActivity = this.getActivityEntity(100, 10);

		try {
			ownerPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		@SuppressWarnings("unchecked")
		List<String> ownerList = documentContext.getItemValue("namowner");

		Assert.assertEquals(2, ownerList.size());
		Assert.assertTrue(ownerList.contains("Kevin"));
		Assert.assertTrue(ownerList.contains("Julian"));

	}

	/**
	 * Test if the ACL settings from the next processEntity are injected into
	 * the workitem
	 **/
	@Test
	public void testOwnerfromProcessEntity() {

		documentActivity = this.getActivityEntity(100, 10);
		documentActivity.replaceItemValue("numnextprocessid", 200);
		this.setActivityEntity(documentActivity);

		// prepare ACL setting for process entity 200
		documentProcess = this.getProcessEntity(200);
		documentProcess.replaceItemValue("keyupdateAcl", true);
		Vector<String> list = new Vector<String>();
		list.add("sam");
		list.add("joe");
		documentProcess.replaceItemValue("namOwnershipNames", list);
		this.setProcessEntity(documentProcess);

		try {
			ownerPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		@SuppressWarnings("unchecked")
		List<String> ownerList = documentContext.getItemValue("namowneR");

		Assert.assertEquals(2, ownerList.size());
		Assert.assertTrue(ownerList.contains("joe"));
		Assert.assertTrue(ownerList.contains("sam"));

	}

	/**
	 * Test if the Owner settings from the activityEntity are injected into the
	 * workitem
	 **/
	@Test
	public void testOwnerfromActivityEntity() {

		documentActivity = this.getActivityEntity(100, 10);

		documentActivity.replaceItemValue("keyupdateAcl", true);
		Vector<String> list = new Vector<String>();
		list.add("samy");
		list.add(""); // test also for empty entry
		list.add("joe");
		documentActivity.replaceItemValue("namOwnershipNames", list);
		this.setActivityEntity(documentActivity);

		try {
			ownerPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		@SuppressWarnings("unchecked")
		List<String> onwerList = documentContext.getItemValue("namOwner");

		Assert.assertEquals(2, onwerList.size());
		Assert.assertTrue(onwerList.contains("joe"));
		Assert.assertTrue(onwerList.contains("samy"));

	}

	/**
	 * Test if the Owner settings from the next processEntity are ignored in
	 * case the ActivityEnttiy provides settings. Merge is not supported!
	 * 
	 **/
	@SuppressWarnings("unchecked")
	@Test
	public void testOwnerfromProcessEntityAndActivityEntity() {

		// set some old values
		Vector<String> list = new Vector<String>();
		list.add("Kevin");
		list.add("Julian");
		documentContext.replaceItemValue("namOwner", list);

		documentActivity = this.getActivityEntity(100, 10);
		documentActivity.replaceItemValue("keyupdateAcl", true);
		documentActivity.replaceItemValue("numnextprocessid", 200);
		list = new Vector<String>();
		list.add("anna");
		list.add("manfred");
		list.add("joe"); // overlapped!
		documentActivity.replaceItemValue("namOwnershipNames", list);

		// prepare ACL setting for process entity 200
		documentProcess = this.getProcessEntity(200);
		documentProcess.replaceItemValue("keyupdateAcl", true);
		list = new Vector<String>();
		list.add("sam");
		list.add("joe"); // overlapped!
		documentProcess.replaceItemValue("namOwnershipNames", list);
		this.setProcessEntity(documentProcess);

		try {
			ownerPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		// $writeAccess= anna , manfred, joe, sam
		List<String> onwerList = documentContext.getItemValue("namOwner");
		Assert.assertEquals(3, onwerList.size());
		Assert.assertTrue(onwerList.contains("joe"));
		//Assert.assertTrue(onwerList.contains("sam"));
		Assert.assertTrue(onwerList.contains("manfred"));
		Assert.assertTrue(onwerList.contains("anna"));

	}

}
