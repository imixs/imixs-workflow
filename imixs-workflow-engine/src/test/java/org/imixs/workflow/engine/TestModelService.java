package org.imixs.workflow.engine;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jakarta.enterprise.inject.Model;

/**
 * Test class for WorkflowService
 * 
 * This test verifies specific method implementations of the workflowService by
 * mocking the WorkflowService with the @spy annotation.
 * 
 * 
 * @author rsoika
 */
public class TestModelService {
	public static final String DEFAULT_MODEL_VERSION = "1.0.0";

	private WorkflowMockEnvironment workflowEngine;

	@Before
	public void setup() throws PluginException {
		workflowEngine = new WorkflowMockEnvironment();

	}

	/**
	 * This test validates the getDataObject method of the modelSerivce.
	 * <p>
	 * A BPMN Task or Event element can be associated with a DataObject. The method
	 * getDataObject extracts the data object value by a given name of a associated
	 * DataObject.
	 * 
	 * @throws ModelException
	 * 
	 */
	@Test
	public void testGetDataObject() throws ModelException {

		workflowEngine.loadModel("/bpmn/TestWorkflowService.bpmn");

		ItemCollection event = this.getModel().getEvent(100, 20);

		Assert.assertNotNull(event);

		ModelService modelService = new ModelService();
		String data = modelService.getDataObject(event, "MyObject");

		Assert.assertNotNull(data);
		Assert.assertEquals("My data", data);

	}

	/**
	 * This deprecated model version
	 * 
	 * 
	 */
	@Test
	public void testDeprecatedModelVersion() {
		this.setModelPath("/bpmn/TestWorkflowService.bpmn");
		this.loadModel();

		// load test workitem
		ItemCollection workitem = database.get("W0000-00001");
		workitem.setModelVersion("0.9.0");
		workitem.setTaskID(100);
		workitem.setEventID(10);
		workitem.replaceItemValue("txtWorkflowGroup", "Ticket");

		Model amodel = null;
		try {
			amodel = modelService.getModelByWorkitem(workitem);
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(amodel);
		Assert.assertEquals("1.0.0", amodel.getVersion());

	}

	/**
	 * This deprecated model version
	 * 
	 * 
	 */
	@Test
	public void testRegexModelVersion() {
		this.setModelPath("/bpmn/TestWorkflowService.bpmn");
		this.loadModel();

		// load test workitem
		ItemCollection workitem = database.get("W0000-00001");
		workitem.setModelVersion("(^1.)");
		workitem.setTaskID(100);
		workitem.setEventID(10);

		Model amodel = null;
		try {
			amodel = modelService.getModelByWorkitem(workitem);

		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertNotNull(amodel);
		Assert.assertEquals("1.0.0", amodel.getVersion());

	}

	/**
	 * This deprecated model version
	 * 
	 * 
	 */
	@Test
	public void testNoMatchModelVersion() {
		this.setModelPath("/bpmn/TestWorkflowService.bpmn");
		this.loadModel();

		// load test workitem
		ItemCollection workitem = database.get("W0000-00001");
		workitem.setModelVersion("(^5.)");
		workitem.setTaskID(100);
		workitem.setEventID(10);
		workitem.replaceItemValue("$WorkflowGroup", "Invoice");

		Model amodel = null;
		try {
			amodel = modelService.getModelByWorkitem(workitem);
			Assert.fail();
		} catch (ModelException e) {
			// expected
			e.printStackTrace();
		}
		Assert.assertNull(amodel);
	}

}
