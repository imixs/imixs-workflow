package org.imixs.workflow.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.bpmn.BPMNEntityBuilder;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.engine.OldWorkflowMockEnvironment;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.SplitAndJoinPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;

/**
 * Test the SlitAndJoin plug-in
 * 
 * @author rsoika
 * 
 */
public class TestSplitAndJoinPlugin {

	private final static Logger logger = Logger.getLogger(TestSplitAndJoinPlugin.class.getName());

	ItemCollection event;
	ItemCollection workitem;
	protected WorkflowMockEnvironment workflowEnvironment;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {
		workflowEnvironment = new WorkflowMockEnvironment();
		workflowEnvironment.setUp();
		workflowEnvironment.loadBPMNModel("/bpmn/TestSplitAndJoinPlugin.bpmn");

		// prepare test workitem
		workitem = new ItemCollection();
		workitem.model("1.0.0").task(100);
		logger.info("[TestSplitAndJoinPlugin] setup test data...");
		List<String> list = new ArrayList<String>();
		list.add("manfred");
		list.add("anna");
		workitem.replaceItemValue("namTeam", list);
		workitem.replaceItemValue("namCreator", "ronny");
		workitem.replaceItemValue("$snapshotid", "11112222");
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, OldWorkflowMockEnvironment.DEFAULT_MODEL_VERSION);
		workitem.replaceItemValue(WorkflowKernel.UNIQUEID, WorkflowKernel.generateUniqueID());
		workflowEnvironment.getDocumentService().save(workitem);

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
			workitem.event(20);
			workflowEnvironment.getWorkflowService().processWorkItem(workitem);
		} catch (PluginException e) {
			Assert.fail(e.getMessage());
		}

		List<String> workitemRefList = workitem.getItemValue(SplitAndJoinPlugin.LINK_PROPERTY);
		Assert.assertEquals(1, workitemRefList.size());
		String subprocessUniqueid = workitemRefList.get(0);
		// get the subprocess...
		ItemCollection subprocess = workflowEnvironment.getDocumentService().load(subprocessUniqueid);
		// test data in subprocess
		Assert.assertNotNull(subprocess);

		// test the new action result based on the new subprocess uniqueid....
		Assert.assertEquals("/pages/workitems/workitem.jsf?id=" + subprocessUniqueid,
				workitem.getItemValueString("action"));
		Assert.assertEquals(100, subprocess.getTaskID());
		logger.log(Level.INFO, "Created Subprocess UniqueID={0}", subprocess.getUniqueID());

		// test if the field namTeam is available
		List<String> team = subprocess.getItemValue("namTeam");
		Assert.assertEquals(2, team.size());
		Assert.assertTrue(team.contains("manfred"));
		Assert.assertTrue(team.contains("anna"));

	}

	/**
	 * Test read of txtactivityresult form event 60
	 * 
	 * 
	 * @throws ModelException
	 ***/
	@Test
	public void testOpenBPMNBuilder() throws ModelException {
		BPMNModel model = workflowEnvironment.getModelService().getModel("1.0.0");
		BPMNElementNode eventElement = model.findElementNodeById("IntermediateCatchEvent_4");
		ItemCollection test = BPMNEntityBuilder.build(eventElement);
		String txtValue = test.getItemValueString("txtactivityresult");
		String workValue = test.getItemValueString(BPMNUtil.EVENT_ITEM_WORKFLOW_RESULT);
		Assert.assertTrue(txtValue.startsWith("<split"));
		Assert.assertTrue(workValue.startsWith("<split"));
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
			workitem.event(60);
			workflowEnvironment.getWorkflowService().processWorkItem(workitem);
		} catch (PluginException e) {
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(workitem);
		List<String> workitemRefList = workitem.getItemValue(SplitAndJoinPlugin.LINK_PROPERTY);
		Assert.assertEquals(1, workitemRefList.size());
		String subprocessUniqueid = workitemRefList.get(0);

		// get the subprocess...
		ItemCollection subprocess = workflowEnvironment.getDocumentService().load(subprocessUniqueid);

		// test data in subprocess
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(100, subprocess.getTaskID());
		logger.log(Level.INFO, "Created Subprocess UniqueID={0}", subprocess.getUniqueID());

		// test if the field namTeam is available
		List<String> team = subprocess.getItemValue("_sub_Team");
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
	public void testCreateSubProcessWithFile() throws ModelException {

		// add a dummy file
		byte[] empty = { 0 };
		FileData fileData = new FileData("test1.txt", empty, "application/xml", null);
		List<Object> textlist = new ArrayList<Object>();
		textlist.add("\n\n\n\n hello world");
		fileData.setAttribute("text", textlist);
		workitem.addFileData(fileData);
		try {
			workitem.event(61);
			workflowEnvironment.getWorkflowService().processWorkItem(workitem);
		} catch (PluginException e) {
			Assert.fail(e.getMessage());
		}

		Assert.assertNotNull(workitem);
		List<String> workitemRefList = workitem.getItemValue(SplitAndJoinPlugin.LINK_PROPERTY);
		Assert.assertEquals(1, workitemRefList.size());
		String subprocessUniqueid = workitemRefList.get(0);

		// get the subprocess...
		ItemCollection subprocess = workflowEnvironment.getDocumentService().load(subprocessUniqueid);
		// test data in subprocess
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(100, subprocess.getTaskID());
		logger.log(Level.INFO, "Created Subprocess UniqueID={0}", subprocess.getUniqueID());

		// test if the field namTeam is available
		List<String> team = subprocess.getItemValue("_sub_Team");
		Assert.assertEquals(2, team.size());
		Assert.assertTrue(team.contains("manfred"));
		Assert.assertTrue(team.contains("anna"));

		// verify file content....
		Assert.assertNotNull(subprocess);
		List<FileData> targetFileList = subprocess.getFileData();
		Assert.assertEquals(1, targetFileList.size());
		List<Object> result = (List<Object>) targetFileList.get(0).getAttribute("text");
		Assert.assertNotNull(result);
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
			workitem.event(30);
			workflowEnvironment.getWorkflowService().processWorkItem(workitem);
		} catch (PluginException e) {
			Assert.fail(e.getMessage());
		}

		Assert.assertNotNull(workitem);

		List<String> workitemRefList = workitem.getItemValue(SplitAndJoinPlugin.LINK_PROPERTY);

		// two subprocesses should be created...
		Assert.assertEquals(2, workitemRefList.size());

		// test first subprocess instance...
		String subprocessUniqueid = workitemRefList.get(0);
		ItemCollection subprocess = workflowEnvironment.getDocumentService().load(subprocessUniqueid);
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(100, subprocess.getTaskID());
		logger.log(Level.INFO, "Created Subprocess UniqueID={0}", subprocess.getUniqueID());

		// test second subprocess instance... 100.20 -> $processId=200
		subprocessUniqueid = workitemRefList.get(1);
		subprocess = workflowEnvironment.getDocumentService().load(subprocessUniqueid);
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(100, subprocess.getTaskID());
		logger.log(Level.INFO, "Created Subprocess UniqueID={0}", subprocess.getUniqueID());
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
			workitem.event(40);
			workflowEnvironment.getWorkflowService().processWorkItem(workitem);
			Assert.fail();
		} catch (PluginException e) {
			// Plugin exception is expected
			logger.log(Level.INFO, "Expected exception message: {0}", e.getMessage());
			Assert.assertTrue(e.getMessage().startsWith("Parsing item content failed:"));
		}
		Assert.assertNotNull(workitem);
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
		String originUniqueID = workitem.getUniqueID();

		/*
		 * 1.) create test result for new subprcoess.....
		 * The Origin Workitem will be assigned to $taskID=200
		 */
		try {
			workitem.event(20);
			workflowEnvironment.getWorkflowService().processWorkItem(workitem);
			Assert.assertNotNull(workitem);
			Assert.assertEquals(200, workitem.getTaskID());
		} catch (PluginException e) {
			Assert.fail(e.getMessage());
		}

		// now load the subprocess
		List<String> workitemRefList = workitem.getItemValue(SplitAndJoinPlugin.LINK_PROPERTY);
		String subprocessUniqueid = workitemRefList.get(0);
		ItemCollection subprocess = workflowEnvironment.getDocumentService().load(subprocessUniqueid);
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(100, subprocess.getTaskID());

		/*
		 * 2.) process the subprocess to test if the origin process is updated
		 * correctly
		 */

		subprocess.replaceItemValue("_sub_data", "some test data");
		// now we process the subprocess
		try {
			subprocess.event(50);
			workflowEnvironment.getWorkflowService().processWorkItem(subprocess);
			Assert.assertEquals("some test data", subprocess.getItemValueString("_sub_data"));
		} catch (PluginException e) {
			Assert.fail(e.getMessage());
		}

		// test the new action result based on the origin process uniqueid....
		Assert.assertEquals("/pages/workitems/workitem.jsf?id=" + originUniqueID,
				subprocess.getItemValueString("action"));
		// load origin document
		ItemCollection originWorkitem = workflowEnvironment.getDocumentService().load(originUniqueID);
		Assert.assertNotNull(originWorkitem);
		// test origin data
		Assert.assertEquals(200, originWorkitem.getTaskID());
		Assert.assertEquals("some test data", originWorkitem.getItemValueString("_sub_data"));
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
			workitem.event(20);
			workflowEnvironment.getWorkflowService().processWorkItem(workitem);
			Assert.assertNotNull(workitem);
		} catch (PluginException e) {
			Assert.fail(e.getMessage());
		}

		// load the new subprocess....
		List<String> workitemRefList = workitem.getItemValue(SplitAndJoinPlugin.LINK_PROPERTY);
		Assert.assertEquals(1, workitemRefList.size());
		String subprocessUniqueid = workitemRefList.get(0);
		ItemCollection subprocess = workflowEnvironment.getDocumentService().load(subprocessUniqueid);
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(100, subprocess.getTaskID());

		// 2.) now update the subprocess
		try {
			subprocess.event(20);
			// set new team member
			subprocess.replaceItemValue("namTeam", "Walter");
			workflowEnvironment.getWorkflowService().processWorkItem(subprocess);
		} catch (PluginException e) {
			Assert.fail(e.getMessage());
		}

		// now we load the subprocess and test if it was updated (new taskid
		// expected is 300)
		Assert.assertNotNull(subprocess);
		subprocess = workflowEnvironment.getDocumentService().load(subprocessUniqueid);
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(200, subprocess.getTaskID());
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
			workitem.event(70);
			workflowEnvironment.getWorkflowService().processWorkItem(workitem);
		} catch (PluginException e) {
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(workitem);

		// load the new subprocess....
		List<String> workitemRefList = workitem.getItemValue(SplitAndJoinPlugin.LINK_PROPERTY);
		Assert.assertEquals(1, workitemRefList.size());
		String subprocessUniqueid = workitemRefList.get(0);
		ItemCollection subprocess = workflowEnvironment.getDocumentService().load(subprocessUniqueid);

		Assert.assertEquals("manfred", subprocess.getItemValue("namTeam", String.class));
		Assert.assertEquals("ronny", subprocess.getItemValue("namcreator", String.class));
		Assert.assertEquals("", subprocess.getItemValueString("$snapshotid"));

		// test the deprecated LIst
		List<String> workitemRefListDeprecated = workitem.getItemValue("txtworkitemref");
		Assert.assertEquals(workitemRefList, workitemRefListDeprecated);
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
