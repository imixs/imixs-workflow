package org.imixs.workflow.plugins;

import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowEngineMock;
import org.imixs.workflow.engine.plugins.ApplicationPlugin;
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
 * Test the Application plug-in.
 * 
 * The plug-in evaluates the next process entity
 * 
 * @author rsoika
 * 
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class TestApplicationPlugin {

	private final static Logger logger = Logger
			.getLogger(TestApplicationPlugin.class.getName());

	protected ApplicationPlugin applicationPlugin = null;
	protected ItemCollection workitem;
	protected ItemCollection event;
	protected WorkflowEngineMock workflowEngine;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {

		workflowEngine = new WorkflowEngineMock();
		workflowEngine.setUp();
		workflowEngine.loadBPMNModel("/bpmn/plugin-test.bpmn");

		applicationPlugin = new ApplicationPlugin();
		try {
			applicationPlugin.init(workflowEngine.getWorkflowService());
		} catch (PluginException e) {
			Assert.fail(e.getMessage());
		}

		// prepare data
		workitem = workflowEngine.getDocumentService().load("W0000-00001");
		workitem.model("1.0.0").task(100);
		logger.info("[TestApplicationPlugin] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("manfred");
		list.add("anna");
		workitem.replaceItemValue("namTeam", list);
		workitem.replaceItemValue("namCreator", "ronny");
	}

	/**
	 * Test if the txtEditorID form the next processEntity is associated to the
	 * current workitem by the ApplicationPlugin
	 * 
	 * @throws ModelException
	 */
	@Test
	public void simpleTest() throws ModelException {

		workitem.event(10);
		event = workflowEngine.getModelService().loadEvent(workitem);
		try {
			applicationPlugin.run(workitem, event);
		} catch (PluginException e) {
			Assert.fail(e.getMessage());
		}

		String newEditor = workitem
				.getItemValueString("txtWorkflowEditorID");

		Assert.assertEquals("test-data", newEditor);

	}

}
