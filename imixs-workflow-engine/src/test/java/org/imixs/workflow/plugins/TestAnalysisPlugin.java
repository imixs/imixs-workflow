package org.imixs.workflow.plugins;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.AnalysisPlugin;
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
 * Test class for AnalysisPlugin
 * 
 * @author rsoika
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class TestAnalysisPlugin {
	protected AnalysisPlugin analysisPlugin = null;
	private static final Logger logger = Logger.getLogger(TestAnalysisPlugin.class.getName());

	ItemCollection workitem;

	protected WorkflowMockEnvironment workflowEngine;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {

		workflowEngine = new WorkflowMockEnvironment();
		workflowEngine.setUp();
		workflowEngine.loadBPMNModel("/bpmn/plugin-test.bpmn");

		analysisPlugin = new AnalysisPlugin();
		try {
			analysisPlugin.init(workflowEngine.getWorkflowService());
		} catch (PluginException e) {
			Assert.fail(e.getMessage());
		}

		workitem = workflowEngine.getDocumentService().load("W0000-00001");
		workitem.model("1.0.0").task(100);

	}

	/**
	 * Verify the start mechanism
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testBasicTest() throws PluginException {

		workitem.replaceItemValue("txtName", "Anna");
		workitem.event(10);
		ItemCollection event;
		try {
			event = workflowEngine.getModelService().loadEvent(workitem);

			String sResult = "<item name='measurepoint' type='start'>M1</item>";
			logger.log(Level.INFO, "txtActivityResult={0}", sResult);
			event.replaceItemValue("txtActivityResult", sResult);

			workitem = analysisPlugin.run(workitem, event);
			Assert.assertNotNull(workitem);

			Assert.assertTrue(workitem.hasItem("datMeasurePointStart_M1"));

			logger.log(Level.INFO, "datMeasurePointStart_M1= {0}",
					workitem.getItemValueDate("datMeasurePointStart_M1"));
		} catch (ModelException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Verify the start mechanism
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testWrongStartTest() throws PluginException {

		workitem = new ItemCollection();
		workitem.replaceItemValue("txtName", "Anna");

		// Activity Entity Dummy
		ItemCollection event = new ItemCollection();

		String sResult = "<item name='measurepoint' type='start'>M1</item>";
		logger.log(Level.INFO, "txtActivityResult={0}", sResult);
		event.replaceItemValue("txtActivityResult", sResult);

		workitem = analysisPlugin.run(workitem, event);
		Assert.assertNotNull(workitem);

		Assert.assertTrue(workitem.hasItem("datMeasurePointStart_M1"));

		logger.log(Level.INFO, "datMeasurePointStart_M1= {0}",
				workitem.getItemValueDate("datMeasurePointStart_M1"));

		sResult = "<item name='measurepoint' type='start'>M1</item>";
		logger.log(Level.INFO, "txtActivityResult={0}", sResult);
		event.replaceItemValue("txtActivityResult", sResult);

		workitem = analysisPlugin.run(workitem, event);
		Assert.assertNotNull(workitem);

		Assert.assertTrue(workitem.hasItem("datMeasurePointStart_M1"));
	}

	/**
	 * Verify the start numMeasurePoint_
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testTotalTime() throws PluginException {

		workitem = new ItemCollection();
		workitem.replaceItemValue("txtName", "Anna");

		// Activity Entity Dummy
		ItemCollection event = new ItemCollection();

		String sResult = "<item name='measurepoint' type='start'>M1</item>";
		logger.log(Level.INFO, "txtActivityResult={0}", sResult);
		event.replaceItemValue("txtActivityResult", sResult);

		workitem = analysisPlugin.run(workitem, event);
		Assert.assertNotNull(workitem);

		Assert.assertTrue(workitem.hasItem("datMeasurePointStart_M1"));

		logger.log(Level.INFO, "datMeasurePointStart_M1= {0}",
				workitem.getItemValueDate("datMeasurePointStart_M1"));

		try {
			Thread.sleep(1000); // 1000 milliseconds is one second.
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

		sResult = "<item name='measurepoint' type='stop'>M1</item>";
		logger.log(Level.INFO, "txtActivityResult={0}", sResult);
		event.replaceItemValue("txtActivityResult", sResult);

		workitem = analysisPlugin.run(workitem, event);
		Assert.assertNotNull(workitem);

		Assert.assertTrue(workitem.hasItem("datMeasurePointStart_M1"));

		int time = workitem.getItemValueInteger("numMeasurePoint_M1");

		System.out.println("Time=" + time);
		Assert.assertTrue(time > 0);
	}

}
