package org.imixs.workflow.bpmn;

import java.util.Set;

import org.imixs.workflow.MockWorkflowEnvironment;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbpmn.bpmn.BPMNModel;

/**
 * Test class
 * 
 * Special cases with collaboration diagram containing two workflow groups
 * (participants) with different workflow models.
 * 
 * 
 * 
 * @author rsoika
 */
public class TestBPMNParserCollaborationMinutes {

	private MockWorkflowEnvironment workflowEnvironment;

	@Before
	public void setup() throws PluginException {
		workflowEnvironment = new MockWorkflowEnvironment();
		// load test models
		workflowEnvironment.loadBPMNModel("/bpmn/minutes.bpmn");

	}

	@Test
	public void testSimple() throws ModelException {

		BPMNModel model = workflowEnvironment.getOpenBPMNModelManager().getModel("1.0.0");
		Assert.assertNotNull(model);

		Set<String> groups = workflowEnvironment.getOpenBPMNModelManager().findAllGroups(model);
		// Test Groups
		Assert.assertFalse(groups.contains("Collaboration"));
		Assert.assertTrue(groups.contains("Protokoll"));
		Assert.assertTrue(groups.contains("Protokollpunkt"));

		// test count of elements
		Assert.assertEquals(8, model.findAllActivities().size());

	}

}
