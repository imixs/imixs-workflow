package org.imixs.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbpmn.bpmn.BPMNModel;

/**
 * Test class for WorkflowService
 * 
 * This test verifies specific method implementations of the workflowService by
 * mocking the WorkflowService with the @spy annotation.
 * 
 * 
 * @author rsoika
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class TestModelService {

	protected WorkflowMockEnvironment workflowEngine;
	ItemCollection workitem;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {

		workflowEngine = new WorkflowMockEnvironment();
		workflowEngine.setUp();
		workflowEngine.loadBPMNModel("/bpmn/TestWorkflowService.bpmn");

		workitem = workflowEngine.getDocumentService().load("W0000-00001");
		workitem.model("1.0.0").task(100);

	}

	/**
	 * This test validates the getDataObject method of the modelService.
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
		workitem.event(20);
		ItemCollection event = this.workflowEngine.getModelService().getModelManager().loadEvent(workitem);

		assertNotNull(event);

		String data = workflowEngine.getModelService().getModelManager().findDataObject(event, "MyObject");

		assertNotNull(data);
		assertEquals("My data", data);

	}

	/**
	 * This deprecated model version
	 * 
	 * 
	 */
	@Test
	public void testDeprecatedModelVersion() {

		// load test workitem
		workitem.setModelVersion("0.9.0");
		workitem.setEventID(10);
		workitem.setWorkflowGroup("Ticket");

		BPMNModel amodel = null;
		try {
			amodel = workflowEngine.getModelService().getModelByWorkitem(workitem);
		} catch (ModelException e) {
			fail(e.getMessage());
		}

		assertNotNull(amodel);
		assertEquals("1.0.0", BPMNUtil.getVersion(amodel));
	}

	/**
	 * This deprecated model version
	 * 
	 * 
	 */
	@Test
	public void testRegexModelVersion() {

		// load test workitem

		workitem.setModelVersion("(^1.)");
		workitem.setTaskID(100);
		workitem.setEventID(10);

		BPMNModel amodel = null;
		try {
			amodel = workflowEngine.getModelService().getModelByWorkitem(workitem);

		} catch (ModelException e) {
			e.printStackTrace();
			fail();
		}
		assertNotNull(amodel);
		assertEquals("1.0.0", BPMNUtil.getVersion(amodel));

	}

	/**
	 * This deprecated model version
	 * 
	 * 
	 */
	@Test
	public void testNoMatchModelVersion() {
		workitem.removeItem(WorkflowKernel.MODELVERSION);
		workitem.setEventID(10);
		workitem.setWorkflowGroup("Invoice");

		BPMNModel amodel = null;
		try {
			amodel = workflowEngine.getModelService().getModelByWorkitem(workitem);
			fail();
		} catch (ModelException e) {
			// expected
		}
		assertNull(amodel);
	}

}
