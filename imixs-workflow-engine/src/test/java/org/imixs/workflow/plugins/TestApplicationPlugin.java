package org.imixs.workflow.plugins;

import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.AbstractWorkflowEnvironment;
import org.imixs.workflow.engine.plugins.ApplicationPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test the Application plug-in.
 * 
 * The plug-in evaluates the next process entity
 * 
 * @author rsoika 
 * 
 */
public class TestApplicationPlugin extends AbstractWorkflowEnvironment {

	private final static Logger logger = Logger
			.getLogger(TestApplicationPlugin.class.getName());

	ApplicationPlugin applicationPlugin = null;
	ItemCollection documentContext;
	ItemCollection documentActivity, documentProcess;

	@Before
	public void setup() throws PluginException, ModelException {

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
	 * @throws ModelException 
	 */
	@Test
	public void simpleTest() throws ModelException {

		documentActivity = this.getModel().getEvent(100, 10);
		documentActivity.replaceItemValue("numnextprocessid", 200);


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
