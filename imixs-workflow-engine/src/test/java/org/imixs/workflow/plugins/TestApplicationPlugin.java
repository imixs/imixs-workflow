package org.imixs.workflow.plugins;

import java.util.Vector;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.AbstractWorkflowServiceTest;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the Application plug-in.
 * 
 * The plug-in evaluates the next process entity
 * 
 * @author rsoika
 * 
 */
public class TestApplicationPlugin extends AbstractWorkflowServiceTest {

	private final static Logger logger = Logger
			.getLogger(TestApplicationPlugin.class.getName());

	ApplicationPlugin applicationPlugin = null;
	ItemCollection documentContext;
	ItemCollection documentActivity, documentProcess;

	@Before
	public void setup() throws PluginException {

		super.setup();

		applicationPlugin = new ApplicationPlugin();
		try {
			applicationPlugin.init(workflowContext);
		} catch (PluginException e) {

			e.printStackTrace();
		}

		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestAccessPlugin] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("manfred");
		list.add("anna");
		documentContext.replaceItemValue("namTeam", list);

		documentContext.replaceItemValue("namCreator", "ronny");

	}

	/**
	 * Test if the txtEditorID form the next processEntity is associated to the
	 * current workitem by the ApplicationPlugin
	 */
	@Test
	public void simpleTest() {

		documentActivity = this.getActivityEntity(100, 10);
		documentActivity.replaceItemValue("numnextprocessid", 200);
		this.setActivityEntity(documentActivity);

		documentProcess = this.getProcessEntity(200);
		documentProcess.replaceItemValue("txtEditorID", "test-data");
		this.setProcessEntity(documentProcess);

		try {
			applicationPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		String newEditor = documentContext
				.getItemValueString("txtWorkflowEditorID");

		Assert.assertEquals("test-data", newEditor);

	}

}
