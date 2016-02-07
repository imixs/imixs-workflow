package org.imixs.workflow.plugins;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.AbstractWorkflowServiceTest;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the ACL plug-in concerning the ACL settings in a process entity.
 * 
 * If ACL settings are provided by the next process entity than these settings
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
 * then $writeAccess should be 'jo','anna'
 * 
 * 
 * 
 * These tests extend the JUnit test in TestAccessPlugin
 * 
 * 
 * @author rsoika
 * 
 */
public class TestAccessPluginProcessEntity extends AbstractWorkflowServiceTest {

	private final static Logger logger = Logger.getLogger(TestAccessPluginProcessEntity.class.getName());

	AccessPlugin accessPlugin = null;
	ItemCollection documentContext;
	ItemCollection documentActivity, documentProcess;

	@Before
	public void setup() throws PluginException {

		super.setup();

		accessPlugin = new AccessPlugin();
		try {
			accessPlugin.init(workflowContext);
		} catch (PluginException e) {

			e.printStackTrace();
		}

		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestAccessPlugin] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("manfred");
		list.add("anna");
		documentContext.replaceItemValue("namTeam", list);

		documentContext.replaceItemValue("namCreator", "ronny");

	}

	/**
	 * Test if the ACL settings will not be changed if no ACL is set be process
	 * or activity
	 ***/
	@Test
	public void testACLNoUpdate() {
		Vector<String> list = new Vector<String>();
		list.add("Kevin");
		list.add("Julian");
		documentContext.replaceItemValue("$writeaccess", list);

		documentActivity = this.getActivityEntity(100, 10);

		try {
			accessPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		@SuppressWarnings("unchecked")
		List<String> writeAccess = documentContext.getItemValue("$WriteAccess");

		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("Kevin"));
		Assert.assertTrue(writeAccess.contains("Julian"));

	}

	/**
	 * Test if the ACL settings from the next processEntity are injected into
	 * the workitem
	 **/
	@Test
	public void testACLfromProcessEntity() {

		documentActivity = this.getActivityEntity(100, 10);
		documentActivity.replaceItemValue("numnextprocessid", 200);
		this.setActivityEntity(documentActivity);

		// prepare ACL setting for process entity 200
		documentProcess = this.getProcessEntity(200);
		documentProcess.replaceItemValue("keyupdateAcl", true);
		Vector<String> list = new Vector<String>();
		list.add("sam");
		list.add("joe");
		documentProcess.replaceItemValue("namaddwriteaccess", list);
		this.setProcessEntity(documentProcess);

		try {
			accessPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		@SuppressWarnings("unchecked")
		List<String> writeAccess = documentContext.getItemValue("$WriteAccess");

		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));

	}

	/**
	 * Test if the ACL settings from the activityEntity are injected into the
	 * workitem
	 **/
	@Test
	public void testACLfromActivityEntity() {

		documentActivity = this.getActivityEntity(100, 10);

		documentActivity.replaceItemValue("keyupdateAcl", true);
		Vector<String> list = new Vector<String>();
		list.add("samy");
		list.add("joe");
		documentActivity.replaceItemValue("namaddwriteaccess", list);
		this.setActivityEntity(documentActivity);

		try {
			accessPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		@SuppressWarnings("unchecked")
		List<String> writeAccess = documentContext.getItemValue("$WriteAccess");

		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("samy"));

	}

	/**
	 * Test if the ACL settings from the next processEntity are ignored in case
	 * the ActivityEnttiy provides settings. Merge is not supported!
	 **/
	@SuppressWarnings("unchecked")
	@Test
	public void testACLfromProcessEntityAndActivityEntity() {

		// set some old values
		Vector<String> list = new Vector<String>();
		list.add("Kevin");
		list.add("Julian");
		documentContext.replaceItemValue("$writeaccess", list);

		documentActivity = this.getActivityEntity(100, 10);
		documentActivity.replaceItemValue("keyupdateAcl", true);
		documentActivity.replaceItemValue("numnextprocessid", 200);
		list = new Vector<String>();
		list.add("anna");
		list.add("manfred");
		list.add("joe"); // overlapped!
		documentActivity.replaceItemValue("namaddwriteaccess", list);
		// read access
		list = new Vector<String>();
		list.add("manfred");
		list.add("tom"); // overlapped!
		documentActivity.replaceItemValue("namaddreadaccess", list);
		this.setActivityEntity(documentActivity);

		// prepare ACL setting for process entity 200
		documentProcess = this.getProcessEntity(200);
		documentProcess.replaceItemValue("keyupdateAcl", true);
		list = new Vector<String>();
		list.add("sam");
		list.add("joe"); // overlapped!
		documentProcess.replaceItemValue("namaddwriteaccess", list);
		documentProcess.replaceItemValue("namaddreadaccess", "silvia");
		this.setProcessEntity(documentProcess);

		try {
			accessPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		// $writeAccess= anna , manfred, joe, sam
		List<String> writeAccess = documentContext.getItemValue("$WriteAccess");
		Assert.assertEquals(3, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		//Assert.assertTrue(writeAccess.contains("sam"));
		Assert.assertTrue(writeAccess.contains("manfred"));
		Assert.assertTrue(writeAccess.contains("anna"));

		// $readAccess= anna , manfred, joe, sam
		writeAccess = documentContext.getItemValue("$readAccess");
		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("tom"));
		Assert.assertTrue(writeAccess.contains("manfred"));

	}

}
