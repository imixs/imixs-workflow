package org.imixs.workflow.plugins;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.AbstractWorkflowServiceTest;
import org.imixs.workflow.plugins.jee.SplitAndJoinPlugin;
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

	/**
	 * We use the provided test workflow model form the
	 * AbstractWorkflowServiceTest
	 */
	@Before
	public void setup() throws PluginException {

		super.setup();

		splitAndJoinPlugin = new SplitAndJoinPlugin();
		try {
			splitAndJoinPlugin.init(workflowService);
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
	 * Test creation of subprocess
	 ***/
	@SuppressWarnings("unchecked")
	@Test
	public void testCreateSubProcess() {

		// create test result.....
		String activityResult = "<item name=\"subprocess_create\"> " + "<modelversion>1.0.0</modelversion>"
				+ "<processid>100</processid>" + "<activityid>10</activityid>" + "<items>namTeam</items>" + "</item>";

		documentActivity = this.getActivityEntity(100, 10);

		documentActivity.replaceItemValue("txtActivityResult", activityResult);
		try {
			splitAndJoinPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(documentContext);

		List<String> workitemRefList = documentContext.getItemValue(SplitAndJoinPlugin.LINK_PROPERTY);

		Assert.assertEquals(1, workitemRefList.size());

		String subprocessUniqueid = workitemRefList.get(0);

		// get the subprocess...
		ItemCollection subprocess = this.entityService.load(subprocessUniqueid);

		// test data in subprocess
		Assert.assertNotNull(subprocess);

		Assert.assertEquals(100, subprocess.getProcessID());

		logger.info("Created Subprocess UniqueID=" + subprocess.getUniqueID());
		
		// test if the field namTeam is available
		List<String> team = subprocess.getItemValue("namTeam");
		Assert.assertEquals(2, team.size());
		Assert.assertTrue(team.contains("manfred"));
		Assert.assertTrue(team.contains("anna"));

	}

	/**
	 * Test multi creation of subprocesses
	 * 
	 ***/
	@SuppressWarnings("unchecked")
	@Test
	public void testCreateMultiSubProcess() {

		// create test result with two subprocess creations.....
		String activityResult = "<item name=\"subprocess_create\"> " + "<modelversion>1.0.0</modelversion>"
				+ "<processid>100</processid>" + "<activityid>10</activityid>" + "<items>namTeam</items>" + "</item>";

		// second subprocess....
		activityResult += "<item name=\"subprocess_create\"> " + "<modelversion>1.0.0</modelversion>"
				+ "<processid>100</processid>" + "<activityid>20</activityid>" + "<items>namTeam</items>" + "</item>";

		documentActivity = this.getActivityEntity(100, 10);

		documentActivity.replaceItemValue("txtActivityResult", activityResult);
		try {
			splitAndJoinPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(documentContext);

		List<String> workitemRefList = documentContext.getItemValue(SplitAndJoinPlugin.LINK_PROPERTY);

		// two subprocesses should be created...
		Assert.assertEquals(2, workitemRefList.size());

		// test first subprocess instance...
		String subprocessUniqueid = workitemRefList.get(0);
		ItemCollection subprocess = this.entityService.load(subprocessUniqueid);
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(100, subprocess.getProcessID());
		logger.info("Created Subprocess UniqueID=" + subprocess.getUniqueID());

		// test second subprocess instance... 100.20 -> $processId=200
		subprocessUniqueid = workitemRefList.get(1);
		subprocess = this.entityService.load(subprocessUniqueid);
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(200, subprocess.getProcessID());
		logger.info("Created Subprocess UniqueID=" + subprocess.getUniqueID());

	}

	/**
	 * Test if the plugin exception in case of wrong xml content for create
	 * subprocess...
	 **/
	// @Ignore
	@Test
	public void testCreateSubProcessParsingError() {

		// create test result with a wrong end tag....
		String activityResult = "<item name=\"subprocess_create\"> " + "<modelversion>1.0.0</modelversion>"
				+ "<processid>1000</processid>" + "<activityid>10</acttyid>" + "<items>namTeam</items>" + "</item>";

		documentActivity = this.getActivityEntity(100, 10);

		documentActivity.replaceItemValue("txtActivityResult", activityResult);
		try {
			splitAndJoinPlugin.run(documentContext, documentActivity);

			Assert.fail();
		} catch (PluginException e) {
			// Plugin exception is expected
			logger.info("Exprected exception message: " + e.getMessage());
			Assert.assertTrue(e.getMessage().startsWith("Parsing item content failed:"));
		}

		Assert.assertNotNull(documentContext);

	}

}
