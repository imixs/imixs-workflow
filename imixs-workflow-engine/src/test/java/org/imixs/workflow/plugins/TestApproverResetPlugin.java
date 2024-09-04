package org.imixs.workflow.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.ApproverPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for ApproverPlugin
 * 
 * @author rsoika
 */
public class TestApproverResetPlugin {
	final static String MODEL_VERSION = "1.0.0";
	ItemCollection event;

	protected WorkflowMockEnvironment workflowEngine;
	ItemCollection workitem = null;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {
		workflowEngine = new WorkflowMockEnvironment();
		workflowEngine.setUp();
		workflowEngine.loadBPMNModel("/bpmn/TestApproverPluginReset.bpmn");
		workitem = workflowEngine.getDocumentService().load("W0000-00001");
		workitem.model("1.0.0").task(100);
	}

	/**
	 * This simple test
	 * 
	 * @throws PluginException
	 * @throws ModelException
	 * 
	 */
	@Test
	public void testSimpleApproval() throws PluginException, ModelException {

		workitem = new ItemCollection();
		workitem.model(MODEL_VERSION).task(100).event(10);

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		workitem.replaceItemValue("ProcessManager", nameList);

		// test with eddy
		when(workflowEngine.getWorkflowService().getUserName()).thenReturn("eddy");
		// 100.10
		workitem = workflowEngine.getWorkflowService().processWorkItem(workitem);
		assertEquals(3, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		assertEquals(0, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

		// 200.20 eddy
		workitem.event(20);
		workitem = workflowEngine.getWorkflowService().processWorkItem(workitem);
		assertEquals(2, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		assertEquals(1, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

		// switch to anna
		when(workflowEngine.getWorkflowService().getUserName()).thenReturn("anna");
		workitem.event(20);
		workitem = workflowEngine.getWorkflowService().processWorkItem(workitem);
		assertEquals(1, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		assertEquals(2, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

		// switch to manfred
		when(workflowEngine.getWorkflowService().getUserName()).thenReturn("manfred");
		workitem.event(20);
		workitem = workflowEngine.getWorkflowService().processWorkItem(workitem);
		assertEquals(0, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		assertEquals(3, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());
		assertEquals(300, workitem.getTaskID());

	}

	/**
	 * This simple test
	 * 
	 * @throws PluginException
	 * @throws ModelException
	 * 
	 */
	@Test
	public void testNewApproverListWithReject() throws PluginException, ModelException {

		workitem = new ItemCollection();
		workitem.model(MODEL_VERSION).task(100).event(10);

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		workitem.replaceItemValue("ProcessManager", nameList);

		// test with eddy
		when(workflowEngine.getWorkflowService().getUserName()).thenReturn("eddy");
		// 100.10
		workitem = workflowEngine.getWorkflowService().processWorkItem(workitem);
		assertEquals(3, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		assertEquals(0, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

		// 200.20
		workitem.event(20);
		workitem = workflowEngine.getWorkflowService().processWorkItem(workitem);
		assertEquals(2, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		assertEquals(1, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

		// repeat with same user
		workitem.event(20);
		workitem = workflowEngine.getWorkflowService().processWorkItem(workitem);
		assertNotNull(workitem);
		assertEquals(2, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		assertEquals(1, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

		// switch to anna
		when(workflowEngine.getWorkflowService().getUserName()).thenReturn("anna");
		workitem.event(20);
		workitem = workflowEngine.getWorkflowService().processWorkItem(workitem);
		assertEquals(1, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		assertEquals(2, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

		// test manfred reject...
		when(workflowEngine.getWorkflowService().getUserName()).thenReturn("manfred");
		workitem.event(30);
		workitem = workflowEngine.getWorkflowService().processWorkItem(workitem);
		assertNotNull(workitem);
		assertEquals(3, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		assertEquals(0, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

	}

}
