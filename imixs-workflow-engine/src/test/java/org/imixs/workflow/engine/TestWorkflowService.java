package org.imixs.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.plugins.ApplicationPlugin;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.plugins.TestApplicationPlugin;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Test class for WorkflowService
 * 
 * This test verifies specific method implementations of the workflowService by
 * mocking the WorkflowService with the @spy annotation.
 * 
 * 
 * @author rsoika
 */
public class TestWorkflowService {

	private final static Logger logger = Logger
			.getLogger(TestApplicationPlugin.class.getName());

	protected ApplicationPlugin applicationPlugin = null;
	protected ItemCollection documentContext;
	protected ItemCollection documentActivity, documentProcess;
	protected WorkflowEngineMock workflowEnvironment;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {

		workflowEnvironment = new WorkflowEngineMock();
		workflowEnvironment.setUp();
		workflowEnvironment.loadBPMNModel("/bpmn/TestWorkflowService.bpmn");

	}

	/**
	 * This test simulates a workflowService process call by mocking the entity and
	 * model service.
	 * 
	 * This is just a simple simulation...
	 * 
	 */
	@Test
	public void testDatabase()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		ItemCollection workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		assertNotNull(workitem);
		// load test workitem
		workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		assertNotNull(workitem);
		assertEquals("W0000-00001", workitem.getUniqueID());

	}

	/**
	 * This test simulates a workflowService process call by mocking the entity and
	 * model service.
	 * 
	 * This is just a simple simulation...
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * @throws ModelException
	 */
	@Test
	public void testProcessSimple()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {
		// load test workitem
		ItemCollection workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, OldWorkflowMockEnvironment.DEFAULT_MODEL_VERSION);
		workitem.setTaskID(100);

		workitem = workflowEnvironment.workflowService.processWorkItem(workitem);

		Assert.assertEquals("1.0.0", workitem.getItemValueString("$ModelVersion"));
		assertEquals("1.0.0", workitem.getItemValueString("$ModelVersion"));
		assertEquals(10, workitem.getItemValueInteger("$lastEvent"));
		assertEquals(0, workitem.getEventID());
	}

	/**
	 * test if the method getEvents returns correct lists of public events.
	 */
	@Test
	public void testGetEventsSimple() {

		// get workitem
		ItemCollection workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		workitem.setTaskID(200);

		List<ItemCollection> eventList = null;
		try {
			eventList = workflowEnvironment.workflowService.getEvents(workitem);
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		// only one event is public!
		Assert.assertEquals(1, eventList.size());
	}

	/**
	 * test if the method getEvents returns correct lists of public and restricted
	 * events. User "manfred" is listed in current workitem namTeam.
	 */
	@Test
	public void testGetEventsComplex() {

		// get workitem
		ItemCollection workitem = workflowEnvironment.documentService.load("W0000-00001");
		workitem.setTaskID(100);

		Vector<String> members = new Vector<String>();
		members.add("jo");
		members.add("");
		members.add("manfred");
		members.add("alex");
		workitem.replaceItemValue("nammteam", "tom");
		workitem.replaceItemValue("nammanager", members);
		workitem.replaceItemValue("namassist", "");

		List<ItemCollection> eventList = null;
		try {
			eventList = workflowEnvironment.workflowService.getEvents(workitem);
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertEquals(3, eventList.size());
	}

	/**
	 * test if the method getEvents returns correct lists of workflow events, with a
	 * more complex setup. User 'manfred' is not listed in namManger!
	 */
	@Test
	public void testGetEventsComplexRestricted() {

		// get workitem
		ItemCollection workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		workitem.setTaskID(100);

		Vector<String> members = new Vector<String>();
		members.add("jo");
		members.add("alex");
		workitem.replaceItemValue("nammteam", "tom");
		workitem.replaceItemValue("nammanager", members);
		workitem.replaceItemValue("namassist", "");

		List<ItemCollection> eventList = null;
		try {
			eventList = workflowEnvironment.workflowService.getEvents(workitem);
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertEquals(2, eventList.size());
	}

	/**
	 * This test verifies if the method getEvents returns only events where the
	 * current user has read access! In this case, the user "manfred" is in the role
	 * 'org.imixs.ACCESSLEVEL.MANAGERACCESS' and granted to the event 300.20
	 * 
	 * So we expect both events!
	 */
	@Test
	public void testGetEventsReadRestrictedForManagerAccess() {

		when(workflowEnvironment.workflowService.getUserNameList()).thenAnswer(new Answer<List<String>>() {
			@Override
			public List<String> answer(InvocationOnMock invocation) throws Throwable {
				List<String> result = new ArrayList<>();
				result.add("manfred");
				result.add("org.imixs.ACCESSLEVEL.MANAGERACCESS");
				return result;
			}
		});

		// get workitem
		ItemCollection workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		workitem.setTaskID(300);

		Vector<String> members = new Vector<String>();
		members.add("jo");
		members.add("alex");
		workitem.replaceItemValue("nammteam", "tom");
		workitem.replaceItemValue("nammanager", members);
		workitem.replaceItemValue("namassist", "");

		List<ItemCollection> eventList = null;
		try {
			eventList = workflowEnvironment.workflowService.getEvents(workitem);
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertEquals(2, eventList.size());
	}
}
