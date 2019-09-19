package org.imixs.workflow.engine.adapters;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import junit.framework.Assert;

/**
 * Test the ACL plugin.
 * 
 * Also test the fallback mode
 * 
 * @author rsoika
 * 
 */
public class TestAccessAdapter {

	private final static Logger logger = Logger.getLogger(TestAccessAdapter.class.getName());

	protected ItemCollection documentContext;
	protected ItemCollection documentActivity;
	protected WorkflowMockEnvironment workflowMockEnvironment;
	@Spy
	protected AccessAdapter participantAdapter;

	
	
	@Before
	public void setUp() throws PluginException, ModelException {
		MockitoAnnotations.initMocks(this);
		workflowMockEnvironment = new WorkflowMockEnvironment();
		workflowMockEnvironment.setModelPath("/bpmn/TestAccessPlugin.bpmn");

		workflowMockEnvironment.setup();

		participantAdapter.workflowService = workflowMockEnvironment.getWorkflowService();
		
		// prepare data
		documentContext = new ItemCollection().model("1.0.0").task(100);
		
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void simpleTest() throws ModelException {

		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 10);
		documentContext.setEventID(10);
		try {
			participantAdapter.execute(documentContext, documentActivity);
		} catch (AdapterException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue(WorkflowService.WRITEACCESS);

		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testNoUpdate() throws ModelException {
		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 20);
		documentContext.setEventID(20);
		try {
			participantAdapter.execute(documentContext, documentActivity);
		} catch (AdapterException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue(WorkflowService.WRITEACCESS);

		Assert.assertEquals(0, writeAccess.size());
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void fieldMappingTest() throws ModelException {

		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 10);
		documentContext.setEventID(10);
		logger.info("[TestAccessPlugin] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("manfred");
		list.add("anna");
		documentContext.replaceItemValue("namTeam", list);

		documentContext.replaceItemValue("namCreator", "ronny");

		try {
			participantAdapter.execute(documentContext, documentActivity);
		} catch (AdapterException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue(WorkflowService.WRITEACCESS);

		Assert.assertEquals(4, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));
		Assert.assertTrue(writeAccess.contains("manfred"));
		Assert.assertTrue(writeAccess.contains("anna"));
	}

	/**
	 * This test verifies if a list of users provided by the fieldMapping is mapped
	 * correctly into the workItem
	 * 
	 * @throws ModelException
	 */
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void staticUserGroupMappingTest() throws ModelException {

		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 30);
		documentContext.setEventID(30);
		try {
			participantAdapter.execute(documentContext, documentActivity);
		} catch (AdapterException e) {
			e.printStackTrace();
			Assert.fail();
		}
		List writeAccess = documentContext.getItemValue(WorkflowService.WRITEACCESS);

		Assert.assertEquals(4, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("tom"));
		Assert.assertTrue(writeAccess.contains("sam"));
		Assert.assertTrue(writeAccess.contains("anna"));
		Assert.assertTrue(writeAccess.contains("joe"));
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void fallbackTest() throws ModelException {

		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 10);
		documentActivity.replaceItemValue("keyaccessmode", "0");
		documentContext.setEventID(10);
		try {
			participantAdapter.execute(documentContext, documentActivity);
		} catch (AdapterException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue(WorkflowService.WRITEACCESS);

		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));
	}

	/**
	 * Test Conditional event
	 * 
	 * issue #327
	 * 
	 * @throws ModelException
	 */
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testCondition() throws ModelException {

		// case I.
 
		documentContext.setTaskID(200);
		documentContext.setEventID(20);
		documentContext.replaceItemValue("_budget", 50);
		try {
			documentContext = workflowMockEnvironment.processWorkItem(documentContext);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue(WorkflowService.WRITEACCESS);

		Assert.assertEquals(300, documentContext.getTaskID());
		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));

		// case II.

		documentContext.setTaskID(200);
		documentContext.setEventID(20);
		documentContext.replaceItemValue("_budget", 570);
		try {
			documentContext = workflowMockEnvironment.processWorkItem(documentContext);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		writeAccess = documentContext.getItemValue(WorkflowService.WRITEACCESS);

		Assert.assertEquals(400, documentContext.getTaskID());
		Assert.assertEquals(1, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("tom"));

	}
}
