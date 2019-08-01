package org.imixs.workflow.plugins;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.ApproverPlugin;
import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class for ApproverPlugin
 * 
 * @author rsoika
 */
public class TestApproverPlugin {
	ApproverPlugin approverPlugin = null;
	ItemCollection documentActivity;
	ItemCollection documentContext;
	Map<String, ItemCollection> database = new HashMap<String, ItemCollection>();

	WorkflowMockEnvironment workflowMockEnvironment;

	@Before
	public void setup() throws PluginException, ModelException, AdapterException {

		workflowMockEnvironment = new WorkflowMockEnvironment();
		workflowMockEnvironment.setModelPath("/bpmn/TestApproverPlugin.bpmn");

		workflowMockEnvironment.setup();

		approverPlugin = new ApproverPlugin();
		try {
			approverPlugin.init(workflowMockEnvironment.getWorkflowService());
		} catch (PluginException e) {

			e.printStackTrace();
		}

		documentContext = new ItemCollection();
	}

	/**
	 * This simple test verifies if a approver list is added correctly into the
	 * workitem. The current user IS NOT A MEMBER of the approvers.
	 * 
	 * @throws PluginException
	 * @throws ModelException
	 * 
	 */
	@Test
	public void testNewApproverList() throws PluginException, ModelException {

		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 10);
		// change result
		documentActivity.replaceItemValue("txtActivityResult", "<item name='approvedby'>ProcessManager</item>");

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		documentContext.replaceItemValue("ProcessManager", nameList);

		// test with ronny
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("ronny");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertNotNull(documentContext);

		Assert.assertEquals(3, documentContext.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(0, documentContext.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

	}

	/**
	 * This simple test verifies if a approver list containing null values, empty
	 * values and duplicates is cleared and distinct.
	 * 
	 * @throws PluginException
	 * @throws ModelException
	 * 
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testDistinctApproverList() throws PluginException, ModelException {

		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 10);
		// change result
		documentActivity.replaceItemValue("txtActivityResult", "<item name='approvedby'>ProcessManager</item>");

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		nameList.add("ronny");

		// add null values and empty values
		nameList.add(null);
		nameList.add("");
		nameList.add(null);
		
		// add duplicate
		nameList.add("anna");

		documentContext.replaceItemValue("ProcessManager", nameList);

		// test with ronny
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("ronny");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertNotNull(documentContext);

		// we expect that the null and empty values are removed and the name anna is
		// distinct.

		List<String> approvers = documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS);
		Assert.assertEquals(4, approvers.size());
		Assert.assertEquals(0, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());

		Assert.assertTrue(approvers.contains("anna"));
		Assert.assertTrue(approvers.contains("manfred"));
		Assert.assertTrue(approvers.contains("eddy"));
		Assert.assertTrue(approvers.contains("ronny"));
		
		// test sortorder
		Assert.assertTrue(approvers.indexOf("anna")==0);
		Assert.assertTrue(approvers.indexOf("ronny")==3);

	}

	/**
	 * This simple test verifies if a approver list is added correctly into the
	 * workitem. The current user IS A MEMBER of the approvers.
	 * 
	 * A approval for the current user should not be performed.
	 * 
	 * @throws PluginException
	 * @throws ModelException
	 * 
	 */
	@Test
	public void testNewApproverListImmediateApproval() throws PluginException, ModelException {

		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 10);

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		documentContext.replaceItemValue("ProcessManager", nameList);

		// test with manfred
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("manfred");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertNotNull(documentContext);

		Assert.assertEquals(3, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(0, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());

	}

	/**
	 * Complex test verifies if a approver list is updated in a second run, a new
	 * approver (which may be added by the deputy plug-in) is added correctly into
	 * the existing list
	 * 
	 * @throws PluginException
	 * @throws ModelException
	 * 
	 */
	@Test
	public void testUpdateApproverListNewApprover() throws PluginException, ModelException {

		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 10);

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		documentContext.replaceItemValue("ProcessManager", nameList);

		// test with manfred
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("manfred");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertNotNull(documentContext);

		Assert.assertEquals(3, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(0, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());

		// second run - change soruce list

		nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		nameList.add("ronny");
		documentContext.replaceItemValue("ProcessManager", nameList);

		// test with manfred
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("manfred");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertNotNull(documentContext);

		Assert.assertEquals(4, documentContext.getItemValue("ProcessManager").size());
		Assert.assertEquals(3, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(1, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());

	}

	/**
	 * Complex test verifies if a approver list is updated in a second run, if a new
	 * approver (which may be added by the deputy plug-in) is added correctly into
	 * the existing list (in this case a user which already approved)
	 * 
	 * @throws PluginException
	 * @throws ModelException
	 * 
	 */
	@Test
	public void testUpdateApproverListExistingApprover() throws PluginException, ModelException {

		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 10);

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		documentContext.replaceItemValue("ProcessManager", nameList);

		// test with manfred
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("manfred");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertNotNull(documentContext);

		Assert.assertEquals(3, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(0, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());

		// second run - change soruce list

		nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		documentContext.replaceItemValue("ProcessManager", nameList);

		// test with manfred
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("manfred");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertNotNull(documentContext);

		// list should not be changed!
		Assert.assertEquals(2, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(1, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());

	}

	/**
	 * Complex test verifies if a complete approval.
	 * 
	 * @throws PluginException
	 * @throws ModelException
	 * 
	 */
	@Test
	public void testCompleteApproval() throws PluginException, ModelException {

		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 10);

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		documentContext.replaceItemValue("ProcessManager", nameList);

		// test with manfred
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("manfred");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertNotNull(documentContext);

		Assert.assertEquals(3, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(0, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());

		// second run - Anna
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("anna");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(2, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(1, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());

		// 3rd run - eddy
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("eddy");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(1, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(2, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());

		// 4th run - manfred
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("manfred");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(0, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(3, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());

		// 5th run - Anna (no effect)
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("anna");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(0, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(3, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());

		// 6th run - ronny (no effect)
		when(workflowMockEnvironment.getWorkflowService().getUserName()).thenReturn("ronny");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(0, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(3, documentContext.getItemValue("ProcessManager"+ApproverPlugin.APPROVEDBY).size());

	}

}
