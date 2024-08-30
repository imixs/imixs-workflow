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
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.openbpmn.bpmn.BPMNModel;

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

	@InjectMocks
	protected AccessAdapter accessAdapter;

	protected ItemCollection workitem;
	protected ItemCollection event;
	protected WorkflowMockEnvironment workflowEnvironment;
	BPMNModel model = null;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {
		// Ensures that @Mock and @InjectMocks annotations are processed
		MockitoAnnotations.openMocks(this);
		workflowEnvironment = new WorkflowMockEnvironment();

		// register AccessAdapter Mock
		workflowEnvironment.registerAdapter(accessAdapter);

		// Setup Environment
		workflowEnvironment.setUp();
		workflowEnvironment.loadBPMNModel("/bpmn/TestAccessPlugin.bpmn");
		model = workflowEnvironment.getModelService().getModel("1.0.0");
		accessAdapter.workflowService = workflowEnvironment.getWorkflowService();

		// prepare data
		workitem = new ItemCollection().model("1.0.0").task(100);
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void simpleTest() throws ModelException {
		event = workflowEnvironment.getModelService().getOpenBPMNModelManager().findEventByID(model, 100,
				10);
		workitem.setEventID(10);
		try {
			accessAdapter.execute(workitem, event);
		} catch (AdapterException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);
		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testNoUpdate() throws ModelException {
		event = workflowEnvironment.getModelService().getOpenBPMNModelManager().findEventByID(model, 100,
				20);
		workitem.setEventID(20);
		try {
			accessAdapter.execute(workitem, event);
		} catch (AdapterException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);

		Assert.assertEquals(0, writeAccess.size());
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void fieldMappingTest() throws ModelException {

		event = workflowEnvironment.getModelService().getOpenBPMNModelManager().findEventByID(model, 100,
				10);

		// event = workflowMockEnvironment.getModel().getEvent(100, 10);
		workitem.setEventID(10);
		logger.info("[TestAccessPlugin] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("manfred");
		list.add("anna");
		workitem.replaceItemValue("namTeam", list);
		workitem.replaceItemValue("namCreator", "ronny");

		try {
			accessAdapter.execute(workitem, event);
		} catch (AdapterException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);
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

		event = workflowEnvironment.getModelService().getOpenBPMNModelManager().findEventByID(model, 100,
				30);
		workitem.setEventID(30);
		try {
			accessAdapter.execute(workitem, event);
		} catch (AdapterException e) {
			e.printStackTrace();
			Assert.fail();
		}
		List writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);
		Assert.assertEquals(4, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("tom"));
		Assert.assertTrue(writeAccess.contains("sam"));
		Assert.assertTrue(writeAccess.contains("anna"));
		Assert.assertTrue(writeAccess.contains("joe"));
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void fallbackTest() throws ModelException {
		event = workflowEnvironment.getModelService().getOpenBPMNModelManager().findEventByID(model, 100,
				10);

		event.replaceItemValue("keyaccessmode", "0");
		workitem.setEventID(10);
		try {
			accessAdapter.execute(workitem, event);
		} catch (AdapterException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);
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
		workitem.setTaskID(200);
		workitem.setEventID(20);
		workitem.replaceItemValue("_budget", 50);
		try {
			workitem = workflowEnvironment.getWorkflowService().processWorkItem(workitem);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);
		Assert.assertEquals(300, workitem.getTaskID());
		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));

		// case II.
		workitem.setTaskID(200);
		workitem.setEventID(20);
		workitem.replaceItemValue("_budget", 570);
		try {
			workitem = workflowEnvironment.getWorkflowService().processWorkItem(workitem);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
		writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);
		Assert.assertEquals(400, workitem.getTaskID());
		Assert.assertEquals(1, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("tom"));
	}
}
