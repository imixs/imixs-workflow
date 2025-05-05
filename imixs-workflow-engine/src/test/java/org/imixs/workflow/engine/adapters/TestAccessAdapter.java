package org.imixs.workflow.engine.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.MockWorkflowEnvironment;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
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
	protected MockWorkflowEnvironment workflowEnvironment;
	BPMNModel model = null;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {
		// Ensures that @Mock and @InjectMocks annotations are processed
		MockitoAnnotations.openMocks(this);
		workflowEnvironment = new MockWorkflowEnvironment();

		// register AccessAdapter Mock
		workflowEnvironment.registerAdapter(accessAdapter);

		// Setup Environment
		workflowEnvironment.setUp();
		workflowEnvironment.loadBPMNModelFromFile("/bpmn/TestAccessPlugin.bpmn");
		model = workflowEnvironment.getModelManager().getModel("1.0.0");
		accessAdapter.workflowContextService = workflowEnvironment.getWorkflowContextService();

		// prepare data
		workitem = new ItemCollection().model("1.0.0").task(100);
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void simpleTest() throws ModelException {
		event = workflowEnvironment.getModelManager().findEventByID(model, 100,
				10);
		workitem.setEventID(10);
		try {
			accessAdapter.execute(workitem, event);
		} catch (AdapterException e) {
			fail(e.getMessage());
		}

		List writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);
		assertEquals(2, writeAccess.size());
		assertTrue(writeAccess.contains("joe"));
		assertTrue(writeAccess.contains("sam"));
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testNoUpdate() throws ModelException {
		event = workflowEnvironment.getModelManager().findEventByID(model, 100,
				20);
		workitem.setEventID(20);
		try {
			accessAdapter.execute(workitem, event);
		} catch (AdapterException e) {
			e.printStackTrace();
			fail();
		}

		List writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);

		assertEquals(0, writeAccess.size());
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void fieldMappingTest() throws ModelException {

		event = workflowEnvironment.getModelManager().findEventByID(model, 100,
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
			fail();
		}

		List writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);
		assertEquals(4, writeAccess.size());
		assertTrue(writeAccess.contains("joe"));
		assertTrue(writeAccess.contains("sam"));
		assertTrue(writeAccess.contains("manfred"));
		assertTrue(writeAccess.contains("anna"));
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

		event = workflowEnvironment.getModelManager().findEventByID(model, 100,
				30);
		workitem.setEventID(30);
		try {
			accessAdapter.execute(workitem, event);
		} catch (AdapterException e) {
			e.printStackTrace();
			fail();
		}
		List writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);
		assertEquals(4, writeAccess.size());
		assertTrue(writeAccess.contains("tom"));
		assertTrue(writeAccess.contains("sam"));
		assertTrue(writeAccess.contains("anna"));
		assertTrue(writeAccess.contains("joe"));
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void fallbackTest() throws ModelException {
		event = workflowEnvironment.getModelManager().findEventByID(model, 100,
				10);

		event.replaceItemValue("keyaccessmode", "0");
		workitem.setEventID(10);
		try {
			accessAdapter.execute(workitem, event);
		} catch (AdapterException e) {
			e.printStackTrace();
			fail();
		}

		List writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);
		assertEquals(2, writeAccess.size());
		assertTrue(writeAccess.contains("joe"));
		assertTrue(writeAccess.contains("sam"));
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
			fail();
		}

		List writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);
		assertEquals(300, workitem.getTaskID());
		assertEquals(2, writeAccess.size());
		assertTrue(writeAccess.contains("joe"));
		assertTrue(writeAccess.contains("sam"));

		// case II.
		workitem.setTaskID(200);
		workitem.setEventID(20);
		workitem.replaceItemValue("_budget", 570);
		try {
			workitem = workflowEnvironment.getWorkflowService().processWorkItem(workitem);
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}
		writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);
		assertEquals(400, workitem.getTaskID());
		assertEquals(1, writeAccess.size());
		assertTrue(writeAccess.contains("tom"));
	}
}
