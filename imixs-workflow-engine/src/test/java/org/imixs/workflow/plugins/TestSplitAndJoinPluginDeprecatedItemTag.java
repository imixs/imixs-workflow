package org.imixs.workflow.plugins;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.SplitAndJoinPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openbpmn.bpmn.BPMNModel;

/**
 * Test the SlitAndJoin plug-in using the deprecated item tag
 * 
 * @author rsoika
 * 
 */
public class TestSplitAndJoinPluginDeprecatedItemTag {

	private final static Logger logger = Logger.getLogger(TestSplitAndJoinPluginDeprecatedItemTag.class.getName());

	protected SplitAndJoinPlugin splitAndJoinPlugin = null;
	ItemCollection event;
	ItemCollection workitem;
	protected WorkflowMockEnvironment workflowEnvironment;
	BPMNModel model;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {

		workflowEnvironment = new WorkflowMockEnvironment();
		workflowEnvironment.setUp();
		workflowEnvironment.loadBPMNModel("/bpmn/TestSplitAndJoinPlugin.bpmn");
		model = workflowEnvironment.getModelService().getModel("1.0.0");
		// mock abstract plugin class for the plitAndJoinPlugin
		splitAndJoinPlugin = Mockito.mock(SplitAndJoinPlugin.class, Mockito.CALLS_REAL_METHODS);
		when(splitAndJoinPlugin.getWorkflowService()).thenReturn(workflowEnvironment.getWorkflowService());
		try {
			splitAndJoinPlugin.init(workflowEnvironment.getWorkflowContext());
		} catch (PluginException e) {

			e.printStackTrace();
		}
		// prepare test workitem
		workitem = new ItemCollection();
		logger.info("[TestSplitAndJoinPluginDeprecatedItemTag] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("manfred");
		list.add("anna");
		workitem.replaceItemValue("namTeam", list);
		workitem.replaceItemValue("namCreator", "ronny");
		workitem.replaceItemValue("$snapshotid", "11112222");
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, "1.0.0");
		workitem.setTaskID(100);
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
			event = workflowEnvironment.getModelService().getModelManager().findEventByID(model, 100,
					20);
			splitAndJoinPlugin.run(workitem, event);
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
	 * Test creation of subprocess
	 * 
	 * @throws ModelException
	 ***/
	@SuppressWarnings("unchecked")
	@Test
	public void testCreateSubProcessTargetFieldName() throws ModelException {
		try {
			event = workflowEnvironment.getModelService().getModelManager().findEventByID(model, 100,
					60);
			splitAndJoinPlugin.run(workitem, event);
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
			event = workflowEnvironment.getModelService().getModelManager().findEventByID(model, 100,
					61);
			splitAndJoinPlugin.run(workitem, event);
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
			event = workflowEnvironment.getModelService().getModelManager().findEventByID(model, 100,
					30);
			splitAndJoinPlugin.run(workitem, event);
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
			event = workflowEnvironment.getModelService().getModelManager().findEventByID(model, 100,
					40);
			splitAndJoinPlugin.run(workitem, event);
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
		String orignUniqueID = workitem.getUniqueID();
		/*
		 * 1.) create test result for new subprcoess.....
		 */
		try {
			event = workflowEnvironment.getModelService().getModelManager().findEventByID(model, 100,
					20);
			splitAndJoinPlugin.run(workitem, event);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertNotNull(workitem);
		// now load the subprocess
		List<String> workitemRefList = workitem.getItemValue(SplitAndJoinPlugin.LINK_PROPERTY);
		String subprocessUniqueid = workitemRefList.get(0);
		ItemCollection subprocess = workflowEnvironment.getDocumentService().load(subprocessUniqueid);

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
			event = workflowEnvironment.getModelService().getModelManager().findEventByID(model, 100,
					50);
			splitAndJoinPlugin.run(subprocess, event);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		// test the new action result based on the origin process uniqueid....
		Assert.assertEquals("/pages/workitems/workitem.jsf?id=" + orignUniqueID,
				subprocess.getItemValueString("action"));
		// load origin document
		workitem = workflowEnvironment.getDocumentService().load(orignUniqueID);
		Assert.assertNotNull(workitem);
		// test data.... (new $processId=200 and _sub_data from subprocess
		Assert.assertEquals(100, workitem.getTaskID());
		Assert.assertEquals("some test data", workitem.getItemValueString("_sub_data"));

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
			event = workflowEnvironment.getModelService().getModelManager().findEventByID(model, 100,
					20);
			splitAndJoinPlugin.run(workitem, event);
		} catch (PluginException e) {
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(workitem);

		// load the new subprocess....
		List<String> workitemRefList = workitem.getItemValue(SplitAndJoinPlugin.LINK_PROPERTY);
		Assert.assertEquals(1, workitemRefList.size());
		String subprocessUniqueid = workitemRefList.get(0);
		ItemCollection subprocess = workflowEnvironment.getDocumentService().load(subprocessUniqueid);
		Assert.assertNotNull(subprocess);
		Assert.assertEquals(100, subprocess.getTaskID());

		// 2.) now update the subprocess
		try {
			event = workflowEnvironment.getModelService().getModelManager().findEventByID(model, 200,
					20);
			// set new team member
			workitem.replaceItemValue("namTeam", "Walter");
			splitAndJoinPlugin.run(workitem, event);
		} catch (PluginException e) {
			Assert.fail(e.getMessage());
		}

		// now we load the subprocess and test if it was updated (new processiD
		// expected is 300)
		Assert.assertNotNull(workitem);
		subprocess = workflowEnvironment.getDocumentService().load(subprocessUniqueid);
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
			event = workflowEnvironment.getModelService().getModelManager().findEventByID(model, 100,
					70);
			splitAndJoinPlugin.run(workitem, event);
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

}
