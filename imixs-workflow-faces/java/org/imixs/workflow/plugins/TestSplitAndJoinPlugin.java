package org.imixs.workflow.plugins;

import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.SplitAndJoinPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.junit.Assert;

/**
 * Test the SlitAndJoin plug-in
 * 
 * @author rsoika
 * 
 */
public class TestSplitAndJoinPlugin {

	private final static Logger logger = Logger.getLogger(TestSplitAndJoinPlugin.class.getName());

	protected SplitAndJoinPlugin splitAndJoinPlugin = null;
	protected ItemCollection documentContext;
	protected ItemCollection documentActivity, documentProcess;

	/**
	 * We use the provided test workflow model form the AbstractWorkflowServiceTest
	 * 
	 * @throws ModelException
	 */
	WorkflowMockEnvironment workflowMockEnvironment;

	@Before
	public void setUp() throws PluginException, ModelException {

		workflowMockEnvironment = new WorkflowMockEnvironment();
		workflowMockEnvironment.setModelPath("/bpmn/TestSplitAndJoinPlugin.bpmn");

		workflowMockEnvironment.setup();

		// mock abstract plugin class for the plitAndJoinPlugin
		splitAndJoinPlugin = Mockito.mock(SplitAndJoinPlugin.class, Mockito.CALLS_REAL_METHODS);
		when(splitAndJoinPlugin.getWorkflowService()).thenReturn(workflowMockEnvironment.getWorkflowService());
		try {
			splitAndJoinPlugin.init(workflowMockEnvironment.getWorkflowContext());
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
		documentContext.replaceItemValue("$snapshotid", "11112222");
		documentContext.replaceItemValue(WorkflowKernel.MODELVERSION, WorkflowMockEnvironment.DEFAULT_MODEL_VERSION);
		documentContext.setTaskID(100);
		documentContext.replaceItemValue(WorkflowKernel.UNIQUEID, WorkflowKernel.generateUniqueID());
		workflowMockEnvironment.getDocumentService().save(documentContext);

	}

	/**
	 * Test creation of subprocess
	 * 
	 * @throws ModelException
	 ***/
	@SuppressWarnings("unchecked")
	@Test
	public void testCreateSubProcess() throws ModelException {

		try {
			documentActivity = workflowMockEnvironment.getModel().getEvent(100, 20);
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
		ItemCollection subprocess = workflowMockEnvironment.getDocumentService().load(subprocessUniqueid);

		// test data in subprocess
		Assert.assertNotNull(subprocess);

		// test the new action result based on the new subprocess uniqueid....
		Assert.assertEquals("/pages/workitems/workitem.jsf?id=" + subprocessUniqueid,
				documentContext.getItemValueString("action"));

		Assert.assertEquals(100, subprocess.getTaskID());

		logger.info("Created Subprocess UniqueID=" + subprocess.getUniqueID());

		// test if the field namTeam is available
		List<String> team = subprocess.getItemValue("namTeam");
		Assert.assertEquals(2, team.size());
		Assert.assertTrue(team.contains("manfred"));
		Assert.assertTrue(team.contains("anna"));

	}

	/**
	 * Test creation of subprocess
	 * 
	 * @throws ModelException
	 ***/
	@SuppressWarnings("unchecked")
	@Test
	public void testCreateSubProcessTargetFieldName() throws ModelException {

		try {
			documentActivity = workflowMockEnvironment.getModel().getEvent(100, 60);
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
		ItemCollection subprocess = workflowMockEnvironment.getDocumentService().load(subprocessUniqueid);

		// test data in subprocess
		Assert.assertNotNull(subprocess);

		Assert.assertEquals(100, subprocess.getTaskID());

		logger.info("Created Subprocess UniqueID=" + subprocess.getUniqueID());

		// test if the field namTeam is available
		List<String> team = subprocess.getItemValue("_sub_Team");
		Assert.assertEquals(2, team.size());
		Assert.assertTrue(team.contains("manfred"));
		Assert.assertTrue(team.contains("anna"));

	}

	/**
	 * Test multi creation of subprocesses
	 * 
	 * @throws ModelException
	 * 
	 ***/
	@SuppressWarnings("unchecked")
	@Test
	public void testCreateMultiSubProcess() throws ModelException {

		try {
			documentActivity = workflowMockEnvironment.getModel().getEvent(100, 30);
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
		ItemCollection subprocess = workflowMockEnvironment.getDocumentService().load(subprocessUniqueid);
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(100, subprocess.getTaskID());
		logger.info("Created Subprocess UniqueID=" + subprocess.getUniqueID());

		// test second subprocess instance... 100.20 -> $processId=200
		subprocessUniqueid = workitemRefList.get(1);
		subprocess = workflowMockEnvironment.getDocumentService().load(subprocessUniqueid);
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(100, subprocess.getTaskID());
		logger.info("Created Subprocess UniqueID=" + subprocess.getUniqueID());

	}

	/**
	 * Test if the plugin exception in case of wrong xml content for create
	 * subprocess...
	 * 
	 * @throws ModelException
	 **/
	@Test
	public void testCreateSubProcessParsingError() throws ModelException {
		try {
			documentActivity = workflowMockEnvironment.getModel().getEvent(100, 40);
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
	 * First we create a subprocess and in a secon step we test if the subprocess
	 * can update the origin workitem.
	 * 
	 * @throws ModelException
	 * 
	 **/
	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateOriginProcess() throws ModelException {

		String orignUniqueID = documentContext.getUniqueID();

		/*
		 * 1.) create test result for new subprcoess.....
		 */
		try {
			documentActivity = workflowMockEnvironment.getModel().getEvent(100, 20);
			splitAndJoinPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertNotNull(documentContext);

		// now load the subprocess
		List<String> workitemRefList = documentContext.getItemValue(SplitAndJoinPlugin.LINK_PROPERTY);
		String subprocessUniqueid = workitemRefList.get(0);
		ItemCollection subprocess = workflowMockEnvironment.getDocumentService().load(subprocessUniqueid);

		// test data in subprocess
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(100, subprocess.getTaskID());

		/*
		 * 2.) process the subprocess to test if the origin process will be updated
		 * correctly
		 */
		// add some custom data
		subprocess.replaceItemValue("_sub_data", "some test data");
		// now we process the subprocess
		try {
			documentActivity = workflowMockEnvironment.getModel().getEvent(100, 50);
			splitAndJoinPlugin.run(subprocess, documentActivity);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		// test the new action result based on the origin process uniqueid....
		Assert.assertEquals("/pages/workitems/workitem.jsf?id=" + orignUniqueID,
				subprocess.getItemValueString("action"));

		// load origin document
		documentContext = workflowMockEnvironment.getDocumentService().load(orignUniqueID);
		Assert.assertNotNull(documentContext);

		// test data.... (new $processId=200 and _sub_data from subprocess
		Assert.assertEquals(100, documentContext.getTaskID());
		Assert.assertEquals("some test data", documentContext.getItemValueString("_sub_data"));

	}

	/**
	 * Test update of an existing subprocess
	 * 
	 * @throws ModelException
	 ***/
	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateSubProcess() throws ModelException {

		// 1.) create test subprocess.....
		try {
			documentActivity = workflowMockEnvironment.getModel().getEvent(100, 20);
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
		ItemCollection subprocess = workflowMockEnvironment.getDocumentService().load(subprocessUniqueid);
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(100, subprocess.getTaskID());

		// 2.) now update the subprocess
		try {
			documentActivity = workflowMockEnvironment.getModel().getEvent(200, 20);
			// set new team member
			documentContext.replaceItemValue("namTeam", "Walter");
			splitAndJoinPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		// now we load the subprocess and test if it was updated (new processiD
		// expected is 300)

		Assert.assertNotNull(documentContext);

		subprocess = workflowMockEnvironment.getDocumentService().load(subprocessUniqueid);
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(100, subprocess.getTaskID());
		Assert.assertEquals("Walter", subprocess.getItemValueString("namTEAM"));

	}

	/**
	 * Test ItemCopy with regex during the creation of subprocesses
	 * 
	 * @throws ModelException
	 * 
	 ***/
	@SuppressWarnings("unchecked")
	@Test
	public void testCreateSubProcessCopyItemByRegex() throws ModelException {

		try {
			documentActivity = workflowMockEnvironment.getModel().getEvent(100, 70);
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
		ItemCollection subprocess = workflowMockEnvironment.getDocumentService().load(subprocessUniqueid);

		Assert.assertEquals("manfred", subprocess.getItemValue("namTeam", String.class));
		Assert.assertEquals("ronny", subprocess.getItemValue("namcreator", String.class));

		Assert.assertEquals("", subprocess.getItemValueString("$snapshotid"));
		// ("$snapshotid", "11112222");

		// test the deprecated LIst
	    List<String> workitemRefListDeprecated = documentContext.getItemValue("txtworkitemref");
	    Assert.assertEquals(workitemRefList,workitemRefListDeprecated);
	
	}

	/**
	 * Test the regex evuating the execution conditions
	 ***/
	@Test
	public void testRegex() {

		Assert.assertTrue(Pattern.compile("(^1000$|^1020$|^1050$)").matcher("1050").find());

		Assert.assertTrue(Pattern.compile("").matcher("1050").find());

		Assert.assertTrue(Pattern.compile("(^abc-rechnungsausgang|^abc-rechnungseingang)")
				.matcher("abc-rechnungsausgang-1.0.0").find());

		Assert.assertTrue(Pattern.compile("(^abc-rechnungsausgang|^abc-rechnungseingang)")
				.matcher("abc-rechnungseingang-1.0.0").find());

		// model
		Assert.assertTrue(Pattern.compile("(^abc-rechnungsausgang|^abc-rechnungseingang)")
				.matcher("abc-rechnungseingang-1.0.0").find());
		// processid
		Assert.assertTrue(Pattern.compile("(^1000$|^1010$)").matcher("1000").find());

		Assert.assertTrue(Pattern.compile("(1\\d{3})").matcher("1456").find());
		Assert.assertFalse(Pattern.compile("(1\\d{3})").matcher("2456").find());

		Assert.assertTrue(Pattern.compile("(1\\d{3})").matcher("14566").find());

		Assert.assertFalse(Pattern.compile("(^1\\d{3}$)").matcher("21123").find());

		Assert.assertTrue(Pattern.compile("(^1\\d{3}$)").matcher("1123").find());
		Assert.assertFalse(Pattern.compile("(^1\\d{3}$)").matcher("11123").find());

		Assert.assertTrue(Pattern.compile("1000").matcher("11000").find());

		// test start with
		Assert.assertTrue(Pattern.compile("(^txt|^num)").matcher("txtTitle").find());
		Assert.assertTrue(Pattern.compile("(^txt|^num)").matcher("numTitle").find());
		Assert.assertTrue(Pattern.compile("(^txt|^num|^_)").matcher("_subject").find());

		Assert.assertFalse(Pattern.compile("(^txt|^num|^_)").matcher("$taskid").find());

		Assert.assertTrue(Pattern.compile("(^[a-z]|^num)").matcher("txtTitle").find());
		Assert.assertTrue(Pattern.compile("(^[a-zA-Z]|^_)").matcher("TXTTitle").find());
		Assert.assertTrue(Pattern.compile("(^[a-zA-Z]|^_)").matcher("_title").find());
		
		
		Assert.assertTrue(Pattern.compile("(^requester[a-zA-Z])").matcher("requesterName").find());
		Assert.assertFalse(Pattern.compile("(^requester[a-zA-Z])").matcher("creatorName").find());
		
		

	}

}
