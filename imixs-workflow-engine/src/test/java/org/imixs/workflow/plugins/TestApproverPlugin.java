package org.imixs.workflow.plugins;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.ApproverPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Test class for ApproverPlugin
 * 
 * @author rsoika
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class TestApproverPlugin {
	ApproverPlugin approverPlugin = null;
	ItemCollection event;
	ItemCollection workitem;
	protected WorkflowMockEnvironment workflowEnvironment;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {

		workflowEnvironment = new WorkflowMockEnvironment();
		workflowEnvironment.setUp();
		workflowEnvironment.loadBPMNModel("/bpmn/TestApproverPlugin.bpmn");

		approverPlugin = new ApproverPlugin();
		try {
			approverPlugin.init(workflowEnvironment.getWorkflowService());
		} catch (PluginException e) {

			e.printStackTrace();
		}
		workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		workitem.model("1.0.0").task(100);

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

		workitem.setEventID(10);
		event = workflowEnvironment.getModelService().loadEvent(workitem); // .getModel().getEvent(100, 10);
		// change result
		event.replaceItemValue("txtActivityResult", "<item name='approvedby'>ProcessManager</item>");

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		workitem.replaceItemValue("ProcessManager", nameList);

		// test with ronny
		when(workflowEnvironment.getWorkflowService().getUserName()).thenReturn("ronny");
		workitem = approverPlugin.run(workitem, event);
		Assert.assertNotNull(workitem);

		Assert.assertEquals(3, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(0, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

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

		workitem.setEventID(10);
		event = workflowEnvironment.getModelService().loadEvent(workitem); // .getModel().getEvent(100, 10);

		// change result
		event.replaceItemValue("txtActivityResult", "<item name='approvedby'>ProcessManager</item>");

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

		workitem.replaceItemValue("ProcessManager", nameList);

		// test with ronny
		when(workflowEnvironment.getWorkflowService().getUserName()).thenReturn("ronny");
		workitem = approverPlugin.run(workitem, event);
		Assert.assertNotNull(workitem);

		// we expect that the null and empty values are removed and the name anna is
		// distinct.

		List<String> approvers = workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS);
		Assert.assertEquals(4, approvers.size());
		Assert.assertEquals(0, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

		Assert.assertTrue(approvers.contains("anna"));
		Assert.assertTrue(approvers.contains("manfred"));
		Assert.assertTrue(approvers.contains("eddy"));
		Assert.assertTrue(approvers.contains("ronny"));

		// test sortorder
		Assert.assertTrue(approvers.indexOf("anna") == 0);
		Assert.assertTrue(approvers.indexOf("ronny") == 3);

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

		workitem.setEventID(10);
		event = workflowEnvironment.getModelService().loadEvent(workitem);

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		workitem.replaceItemValue("ProcessManager", nameList);

		// test with manfred
		when(workflowEnvironment.getWorkflowService().getUserName()).thenReturn("manfred");
		workitem = approverPlugin.run(workitem, event);
		Assert.assertNotNull(workitem);

		Assert.assertEquals(3, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(0, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

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

		workitem.setEventID(10);
		event = workflowEnvironment.getModelService().loadEvent(workitem);

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		workitem.replaceItemValue("ProcessManager", nameList);

		// test with manfred
		when(workflowEnvironment.getWorkflowService().getUserName()).thenReturn("manfred");
		workitem = approverPlugin.run(workitem, event);
		Assert.assertNotNull(workitem);

		Assert.assertEquals(3, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(0, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

		// second run - change soruce list

		nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		nameList.add("ronny");
		workitem.replaceItemValue("ProcessManager", nameList);

		// test with manfred
		when(workflowEnvironment.getWorkflowService().getUserName()).thenReturn("manfred");
		workitem = approverPlugin.run(workitem, event);
		Assert.assertNotNull(workitem);

		Assert.assertEquals(4, workitem.getItemValue("ProcessManager").size());
		Assert.assertEquals(3, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(1, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

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

		workitem.setEventID(10);
		event = workflowEnvironment.getModelService().loadEvent(workitem);

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		workitem.replaceItemValue("ProcessManager", nameList);

		// test with manfred
		when(workflowEnvironment.getWorkflowService().getUserName()).thenReturn("manfred");
		workitem = approverPlugin.run(workitem, event);
		Assert.assertNotNull(workitem);

		Assert.assertEquals(3, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(0, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

		// second run - change soruce list

		nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		workitem.replaceItemValue("ProcessManager", nameList);

		// test with manfred
		when(workflowEnvironment.getWorkflowService().getUserName()).thenReturn("manfred");
		workitem = approverPlugin.run(workitem, event);
		Assert.assertNotNull(workitem);

		// list should not be changed!
		Assert.assertEquals(2, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(1, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

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

		workitem.setEventID(10);
		event = workflowEnvironment.getModelService().loadEvent(workitem);

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		workitem.replaceItemValue("ProcessManager", nameList);

		// test with manfred
		when(workflowEnvironment.getWorkflowService().getUserName()).thenReturn("manfred");
		workitem = approverPlugin.run(workitem, event);
		Assert.assertNotNull(workitem);

		Assert.assertEquals(3, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(0, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

		// second run - Anna
		when(workflowEnvironment.getWorkflowService().getUserName()).thenReturn("anna");
		workitem = approverPlugin.run(workitem, event);
		Assert.assertEquals(2, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(1, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

		// 3rd run - eddy
		when(workflowEnvironment.getWorkflowService().getUserName()).thenReturn("eddy");
		workitem = approverPlugin.run(workitem, event);
		Assert.assertEquals(1, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(2, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

		// 4th run - manfred
		when(workflowEnvironment.getWorkflowService().getUserName()).thenReturn("manfred");
		workitem = approverPlugin.run(workitem, event);
		Assert.assertEquals(0, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(3, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

		// 5th run - Anna (no effect)
		when(workflowEnvironment.getWorkflowService().getUserName()).thenReturn("anna");
		workitem = approverPlugin.run(workitem, event);
		Assert.assertEquals(0, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(3, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

		// 6th run - ronny (no effect)
		when(workflowEnvironment.getWorkflowService().getUserName()).thenReturn("ronny");
		workitem = approverPlugin.run(workitem, event);
		Assert.assertEquals(0, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVERS).size());
		Assert.assertEquals(3, workitem.getItemValue("ProcessManager" + ApproverPlugin.APPROVEDBY).size());

	}

}
