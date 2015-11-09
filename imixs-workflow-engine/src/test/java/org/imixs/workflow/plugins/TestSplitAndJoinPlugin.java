package org.imixs.workflow.plugins;

import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.AbstractWorkflowServiceTest;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test the SlitAndJoin plug-in
 * 
 * @author rsoika
 * 
 */
public class TestSplitAndJoinPlugin extends AbstractWorkflowServiceTest {

	private final static Logger logger = Logger.getLogger(TestSplitAndJoinPlugin.class.getName());

	SplitAndJoinPlugin splitAndJoinPlugin = null;
	ItemCollection documentContext;
	ItemCollection documentActivity, documentProcess;

	@Before
	public void setup() throws PluginException {

		super.setup();

		splitAndJoinPlugin = new SplitAndJoinPlugin();
		try {
			splitAndJoinPlugin.init(workflowContext);
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
	public void testCreateSubProcess() {

		// create test result.....
		String activityResult = "<item name=\"subprocess_create\"> " + "<modelversion>1.0.0</modelversion>"
				+ "<processid>1000</processid>" + "<activityid>10</activityid>" + "<items>namTeam</items>" + "</item>";

		documentActivity = this.getActivityEntity(100, 10);

		documentActivity.replaceItemValue("txtActivityResult", activityResult);
		try {
			splitAndJoinPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(documentContext);

	}

	/**
	 * Test if the plugin exception in case of wrong xml content for create
	 * subprocess...
	 ***/
	@Test
	public void testCreateSubProcessParsingError() {

		// create test result.....
		String activityResult = "<item name=\"subprocess_create\"> " + "<modelversion>1.0.0</modelversion>"
				+ "<processid>1000</processid>" + "<activityid>10</acttyid>" // wrong
																				// end
																				// tag!
				+ "<items>namTeam</items>" + "</item>";

		documentActivity = this.getActivityEntity(100, 10);

		documentActivity.replaceItemValue("txtActivityResult", activityResult);
		try {
			splitAndJoinPlugin.run(documentContext, documentActivity);

			Assert.fail();
		} catch (PluginException e) {
			// Plugin exception is expected
			logger.info("Exprected exception message: " + e.getMessage());
			Assert.assertTrue(e.getMessage().startsWith("subprocess_create failed"));
		}

		Assert.assertNotNull(documentContext);

	}

}
