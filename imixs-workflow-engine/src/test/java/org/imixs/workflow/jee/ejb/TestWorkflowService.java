package org.imixs.workflow.jee.ejb;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import junit.framework.Assert;

/**
 * Test class for WorkflowService
 * 
 * This test verifies specific method implementations of the workflowService by
 * mocking the WorkflowService with the @spy annotation.
 * 
 * 
 * @author rsoika
 */
public class TestWorkflowService extends AbstractWorkflowEnvironment {
	public static final String DEFAULT_MODEL_VERSION="1.0.0";

	@Before
	public void setup() throws PluginException {
		super.setup();
	}

	/**
	 * This test simulates a workflowService process call by mocking the entity
	 * and model service.
	 * 
	 * This is just a simple simulation...
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * 
	 */
	@Test
	public void testProcessSimple() throws AccessDeniedException, ProcessingErrorException, PluginException {
		// load test workitem
		ItemCollection workitem = database.get("W0000-00001");
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION,DEFAULT_MODEL_VERSION);
		workitem.replaceItemValue(WorkflowKernel.PROCESSID,100);
		workitem.replaceItemValue(WorkflowKernel.ACTIVITYID,10);
	
		workitem = workflowService.processWorkItem(workitem);

		Assert.assertEquals("1.0.0", workitem.getItemValueString("$ModelVersion"));

	}

	/**
	 * test if the method getEvents returns correct lists of workflow events.
	 */
	@Test
	public void testGetEvents() {

		// set restricted to
		ItemCollection event = database.get("A100-10");

		// create keyRestrictedVisibility
		event.replaceItemValue("keyRestrictedVisibility", "namteam");
		database.put("A100-10", event);

		// get workitem
		ItemCollection workitem = database.get("W0000-00001");
		workitem.replaceItemValue(WorkflowService.PROCESSID, 100);
		workitem.replaceItemValue(WorkflowService.ACTIVITYID, 10);
		workitem.replaceItemValue("namteam", "manfred");

		List<ItemCollection> eventList=null;
		try {
			eventList = workflowService.getEvents(workitem);
		} catch (ModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertEquals(3, eventList.size());
	}

	/**
	 * test if the method getEvents returns correct lists of workflow event,
	 * with a more complex setup
	 */
	@Test
	public void testGetEventsComplex() {

		// set restricted to
		ItemCollection event = database.get("A100-10");

		// create keyRestrictedVisibility
		Vector<String> keys = new Vector<String>();
		keys.add("namteam");
		keys.add("namManageR");
		keys.add("namassist");
		event.replaceItemValue("keyRestrictedVisibility", keys);
		database.put("A100-10", event);

		// get workitem
		ItemCollection workitem = database.get("W0000-00001");
		workitem.replaceItemValue(WorkflowService.PROCESSID, 100);
		workitem.replaceItemValue(WorkflowService.ACTIVITYID, 10);

		Vector<String> members = new Vector<String>();
		members.add("jo");
		members.add("");
		members.add("manfred");
		members.add("alex");
		workitem.replaceItemValue("nammteam", "tom");
		workitem.replaceItemValue("nammanager", members);
		workitem.replaceItemValue("namassist", "");

		List<ItemCollection> eventList=null;
		try {
			eventList = workflowService.getEvents(workitem);
		} catch (ModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertEquals(3, eventList.size());
	}

}
