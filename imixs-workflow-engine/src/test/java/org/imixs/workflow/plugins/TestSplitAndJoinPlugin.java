package org.imixs.workflow.plugins;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
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

		// prepare test workitem
		documentContext = new ItemCollection();
		logger.info("[TestAccessPlugin] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("manfred");
		list.add("anna");
		documentContext.replaceItemValue("namTeam", list);
		documentContext.replaceItemValue("namCreator", "ronny");
		documentContext.replaceItemValue(WorkflowKernel.PROCESSID, 100);
		documentContext.replaceItemValue(WorkflowKernel.UNIQUEID, WorkflowKernel.generateUniqueID());
		entityService.save(documentContext);

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

		try {
			documentActivity = this.getActivityEntity(100, 10);
			documentActivity.replaceItemValue("txtActivityResult", activityResult);
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

		try {
			documentActivity = this.getActivityEntity(100, 10);
			documentActivity.replaceItemValue("txtActivityResult", activityResult);
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
		try {
			documentActivity = this.getActivityEntity(100, 10);
			documentActivity.replaceItemValue("txtActivityResult", activityResult);
			splitAndJoinPlugin.run(documentContext, documentActivity);

			Assert.fail();
		} catch (PluginException e) {
			// Plugin exception is expected
			logger.info("Exprected exception message: " + e.getMessage());
			Assert.assertTrue(e.getMessage().startsWith("Parsing item content failed:"));
		}

		Assert.assertNotNull(documentContext);

	}

	/**
	 * Test update origin
	 * 
	 * First we create a subprocess and in a secon step we test if the
	 * subprocess can update the origin workitem.
	 * 
	 **/
	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateOriginProcess() {

		String orignUniqueID = documentContext.getUniqueID();

		/*
		 * 1.) create test result for new subprcoess.....
		 */
		String activityResult = "<item name=\"subprocess_create\"> " + "<modelversion>1.0.0</modelversion>"
				+ "<processid>100</processid>" + "<activityid>10</activityid>" + "<items>namTeam</items>" + "</item>";
		try {
			documentActivity = this.getActivityEntity(100, 10);
			documentActivity.replaceItemValue("txtActivityResult", activityResult);

			splitAndJoinPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertNotNull(documentContext);

		// now load the subprocess
		List<String> workitemRefList = documentContext.getItemValue(SplitAndJoinPlugin.LINK_PROPERTY);
		String subprocessUniqueid = workitemRefList.get(0);
		ItemCollection subprocess = this.entityService.load(subprocessUniqueid);

		// test data in subprocess
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(100, subprocess.getProcessID());

		/*
		 * 2.) process the subprocess to test if the origin process will be
		 * updated correctly
		 */
		activityResult = "<item name=\"origin_update\"> " + "<modelversion>1.0.0</modelversion>"
				+ "<processid>100</processid>" + "<activityid>20</activityid>" + "<items>namTeam,_sub_data</items>"
				+ "</item>";

		// add some custom data
		subprocess.replaceItemValue("_sub_data", "some test data");
		// now we process the subprocess
		try {
			documentActivity = this.getActivityEntity(100, 10);
			documentActivity.replaceItemValue("txtActivityResult", activityResult);
			splitAndJoinPlugin.run(subprocess, documentActivity);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		// load origin document
		documentContext = entityService.load(orignUniqueID);
		Assert.assertNotNull(documentContext);

		// test data.... (new $processId=200 and _sub_data from subprocess
		Assert.assertEquals(200, documentContext.getProcessID());
		Assert.assertEquals("some test data", documentContext.getItemValueString("_sub_data"));

	}

	/**
	 * Test update of an existing subprocess
	 ***/
	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateSubProcess() {

		// 1.) create test subprocess.....
		String activityResult = "<item name=\"subprocess_create\"> " + "<modelversion>1.0.0</modelversion>"
				+ "<processid>100</processid>" + "<activityid>10</activityid>" + "<items>namTeam</items>" + "</item>";
		try {
			documentActivity = this.getActivityEntity(100, 10);
			documentActivity.replaceItemValue("txtActivityResult", activityResult);
			splitAndJoinPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(documentContext);

		// load the new subprocess....
		List<String> workitemRefList = documentContext.getItemValue(SplitAndJoinPlugin.LINK_PROPERTY);
		Assert.assertEquals(1, workitemRefList.size());
		String subprocessUniqueid = workitemRefList.get(0);
		ItemCollection subprocess = this.entityService.load(subprocessUniqueid);
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(100, subprocess.getProcessID());

		// 2.) now update the subprocess
		activityResult = "<item name=\"subprocess_update\"> " + "<modelversion>1.0.0</modelversion>"
				+ "<processid>100</processid>" + "<activityid>30</activityid>" + "<items>namTeam</items>" + "</item>";
		try {
			documentActivity = this.getActivityEntity(100, 10);
			documentActivity.replaceItemValue("txtActivityResult", activityResult);
			splitAndJoinPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		// now we load the subprocess and test if it was updated (new processiD
		// expected is 300)

		Assert.assertNotNull(documentContext);

		subprocess = this.entityService.load(subprocessUniqueid);
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(300, subprocess.getProcessID());

	}

	/**
	 * Test the regex evuating the execution conditions
	 ***/
	@Test
	public void testRegex() {

		
		
		
		Assert.assertTrue(Pattern.compile("(^1000$|^1020$|^1050$)").matcher("1050").find());
		
		
		Assert.assertTrue(Pattern.compile("").matcher("1050").find());
		
		
		Assert.assertTrue(Pattern.compile("(^abc-rechnungsausgang|^abc-rechnungseingang)").matcher("abc-rechnungsausgang-1.0.0").find());
		
		Assert.assertTrue(Pattern.compile("(^abc-rechnungsausgang|^abc-rechnungseingang)").matcher("abc-rechnungseingang-1.0.0").find());
		
		
	
		
		
		// model
		Assert.assertTrue(Pattern.compile("(^abc-rechnungsausgang|^abc-rechnungseingang)").matcher("abc-rechnungseingang-1.0.0").find());
		// processid
		Assert.assertTrue(Pattern.compile("(^1000$|^1010$)").matcher("1000").find());

		Assert.assertTrue(Pattern.compile("(1\\d{3})").matcher("1456").find());
		Assert.assertFalse(Pattern.compile("(1\\d{3})").matcher("2456").find());
		
		Assert.assertTrue(Pattern.compile("(1\\d{3})").matcher("14566").find());

		
		
		
		
		Assert.assertFalse(Pattern.compile("(^1\\d{3}$)").matcher("21123").find());
		
		Assert.assertTrue(Pattern.compile("(^1\\d{3}$)").matcher("1123").find());
		Assert.assertFalse(Pattern.compile("(^1\\d{3}$)").matcher("11123").find());
		
		
		
		Assert.assertTrue(Pattern.compile("1000").matcher("11000").find());
		

	}

}
