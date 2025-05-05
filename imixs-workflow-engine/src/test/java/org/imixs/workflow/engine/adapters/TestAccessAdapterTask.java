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
import org.imixs.workflow.engine.plugins.OwnerPlugin;
import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.openbpmn.bpmn.BPMNModel;

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
public class TestAccessAdapterTask {

	private final static Logger logger = Logger.getLogger(TestAccessAdapterTask.class.getName());

	protected ItemCollection workitem;
	protected ItemCollection event;
	protected MockWorkflowEnvironment workflowEnvironment;
	BPMNModel model = null;

	@InjectMocks
	protected AccessAdapter accessAdapter;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {
		// Ensures that @Mock and @InjectMocks annotations are processed
		MockitoAnnotations.openMocks(this);
		workflowEnvironment = new MockWorkflowEnvironment();

		// register AccessAdapter Mock
		workflowEnvironment.registerAdapter(accessAdapter);

		// Setup Environment
		workflowEnvironment.setUp();
		workflowEnvironment.loadBPMNModelFromFile("/bpmn/TestAccessAdapterTask.bpmn");
		model = workflowEnvironment.getModelManager().getModel("1.0.0");
		accessAdapter.workflowContextService = workflowEnvironment.getWorkflowContextService();

		// // prepare data
		workitem = new ItemCollection().model("1.0.0").task(100);
		logger.info("[TestAccessAdapterProcessEntity] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("manfred");
		list.add("anna");
		workitem.replaceItemValue("namTeam", list);
		workitem.replaceItemValue("namCreator", "ronny");
	}

	/**
	 * Test if the ACL settings will not be changed if no ACL is set be process or
	 * activity
	 * 
	 * @throws ModelException
	 ***/
	@Test
	public void testACLNoUpdate() throws ModelException {
		Vector<String> list = new Vector<String>();
		list.add("Kevin");
		list.add("Julian");
		workitem.replaceItemValue(WorkflowService.WRITEACCESS, list);

		event = workflowEnvironment.getModelManager().findEventByID(model, 100,
				10);
		workitem.setEventID(10);
		try {
			accessAdapter.execute(workitem, event);
		} catch (AdapterException e) {
			e.printStackTrace();
			fail();
		}

		@SuppressWarnings("unchecked")
		List<String> writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);
		assertEquals(2, writeAccess.size());
		assertTrue(writeAccess.contains("Kevin"));
		assertTrue(writeAccess.contains("Julian"));
	}

	/**
	 * Test if the ACL settings from the next processEntity are injected into the
	 * workitem
	 * 
	 * @throws ModelException
	 **/
	@Test
	public void testACLfromProcessEntity() throws ModelException {

		event = workflowEnvironment.getModelManager().findEventByID(model, 300,
				10);
		workitem.setEventID(10);
		workitem.setTaskID(300);
		workitem.setEventID(10);

		try {
			accessAdapter.execute(workitem, event);
		} catch (AdapterException e) {
			e.printStackTrace();
			fail();
		}

		@SuppressWarnings("unchecked")
		List<String> writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);
		assertEquals(2, writeAccess.size());
		assertTrue(writeAccess.contains("joe"));
		assertTrue(writeAccess.contains("sam"));

	}

	/**
	 * Test if the ACL settings from the activityEntity are injected into the
	 * workitem
	 * 
	 * @throws ModelException
	 **/
	@Test
	public void testACLfromActivityEntity() throws ModelException {

		event = workflowEnvironment.getModelManager().findEventByID(model, 100,
				20);
		workitem.setEventID(20);
		try {
			accessAdapter.execute(workitem, event);
		} catch (AdapterException e) {
			e.printStackTrace();
			fail();
		}

		@SuppressWarnings("unchecked")
		List<String> writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);
		assertEquals(3, writeAccess.size());
		assertTrue(writeAccess.contains("joe"));
		assertTrue(writeAccess.contains("samy"));
		assertTrue(writeAccess.contains("anna"));
	}

	/**
	 * Test if the ACL settings from the next processEntity are ignored in case the
	 * ActivityEnttiy provides settings. Merge is not supported!
	 * 
	 * @throws ModelException
	 **/
	@SuppressWarnings("unchecked")
	@Test
	public void testACLfromProcessEntityAndActivityEntity() throws ModelException {

		// set some old values
		Vector<String> list = new Vector<String>();
		list.add("Kevin");
		list.add("Julian");
		workitem.replaceItemValue(OwnerPlugin.OWNER, list);
		workitem.setTaskID(300);
		event = workflowEnvironment.getModelManager().findEventByID(model, 300,
				20);
		workitem.setEventID(20);
		try {
			accessAdapter.execute(workitem, event);
		} catch (AdapterException e) {
			e.printStackTrace();
			fail();
		}

		// $writeAccess= anna , manfred, joe, sam
		List<String> writeAccess = workitem.getItemValue(WorkflowService.WRITEACCESS);
		assertEquals(3, writeAccess.size());
		assertTrue(writeAccess.contains("joe"));
		// assertTrue(writeAccess.contains("sam"));
		assertTrue(writeAccess.contains("manfred"));
		assertTrue(writeAccess.contains("anna"));

		// $readAccess= anna , manfred, joe, sam
		writeAccess = workitem.getItemValue(WorkflowService.READACCESS);
		assertEquals(2, writeAccess.size());
		assertTrue(writeAccess.contains("tom"));
		assertTrue(writeAccess.contains("manfred"));

	}

}
