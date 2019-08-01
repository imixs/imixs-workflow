package org.imixs.workflow.plugins;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.ApproverPlugin;
import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import junit.framework.Assert;

/**
 * Test class for ApproverPlugin
 * 
 * @author rsoika
 */
public class TestApproverResetPlugin {
	WorkflowMockEnvironment workflowMockEnvironment;

	final static String MODEL_PATH = "/bpmn/TestApproverPluginReset.bpmn";
	final static String MODEL_VERSION = "1.0.0";

	ItemCollection workitem = null;
  
	@Before 
	public void setup() throws PluginException, ModelException, AdapterException {


		// initialize @Mock annotations....
		MockitoAnnotations.initMocks(this);

		workflowMockEnvironment = new WorkflowMockEnvironment();
		workflowMockEnvironment.setModelPath(MODEL_PATH);
		workflowMockEnvironment.setup();

		// test model...
		Assert.assertNotNull(workflowMockEnvironment.getModel());
	}

	/**
	 * This simple test 
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
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("eddy");
		// 100.10
		workitem = workflowMockEnvironment.getWorkflowService().processWorkItem(workitem);
		Assert.assertEquals(3, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(0, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());
		
		
		// 200.20 eddy
		workitem.event(20);
		workitem = workflowMockEnvironment.getWorkflowService().processWorkItem(workitem);
		Assert.assertEquals(2, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(1, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());
	
		
		// switch to anna
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("anna");		
		workitem.event(20);
		workitem = workflowMockEnvironment.getWorkflowService().processWorkItem(workitem);
		Assert.assertEquals(1, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(2, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());
	

		// switch to manfred
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("manfred");		
		workitem.event(20);
		workitem = workflowMockEnvironment.getWorkflowService().processWorkItem(workitem);
		Assert.assertEquals(0, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(3, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());
		Assert.assertEquals(300, workitem.getTaskID());
		
	
		 
		
	}

	/**
	 * This simple test 
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
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("eddy");
		// 100.10
		workitem = workflowMockEnvironment.getWorkflowService().processWorkItem(workitem);
		Assert.assertEquals(3, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(0, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());
		
		
		// 200.20
		workitem.event(20);
		workitem = workflowMockEnvironment.getWorkflowService().processWorkItem(workitem);
		Assert.assertEquals(2, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(1, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());
	
		// repeat with same user
		workitem.event(20);
		workitem = workflowMockEnvironment.getWorkflowService().processWorkItem(workitem);
		Assert.assertNotNull(workitem);
		Assert.assertEquals(2, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(1, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());
	
		
		// switch to anna
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("anna");		
		workitem.event(20);
		workitem = workflowMockEnvironment.getWorkflowService().processWorkItem(workitem);
		Assert.assertEquals(1, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(2, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());
	
		// test manfred reject...
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("manfred");		
		workitem.event(30);
		workitem = workflowMockEnvironment.getWorkflowService().processWorkItem(workitem);
		Assert.assertNotNull(workitem);
		Assert.assertEquals(3, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(0, workitem.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());
	
	}

	

}
