package org.imixs.workflow.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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
public class TestWorkflowServiceNew2 extends AbstractWorkflowServiceTest {

	@Override
	// @BeforeEach
	public void setUp() throws PluginException {
		super.setUp();
	}

	/**
	 * This test simulates a workflowService process call by mocking the entity and
	 * model service.
	 * 
	 * This is just a simple simulation...
	 * 
	 */
	@Test
	public void testProcessSimple()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		ItemCollection workitem = documentService.load("W0000-00001");
		assertNotNull(workitem);
		// load test workitem
		workitem = documentService.load("W0000-00001");
		assertNotNull(workitem);
		assertEquals("W0000-00001", workitem.getUniqueID());

	}

	/**
	 * This test simulates a workflowService process call by mocking the entity and
	 * model service.
	 * 
	 * This is just a simple simulation...
	 */
	@Test
	public void testProcessSimple2()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {
		// load test workitem

		ItemCollection workitem = documentService.load("W0000-00001");
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, WorkflowMockEnvironment.DEFAULT_MODEL_VERSION);
		workitem.setTaskID(100);

		workitem = workflowServiceMock.processWorkItem(workitem);
		assertNotNull(workitem);

		assertEquals("1.0.0", workitem.getItemValueString("$ModelVersion"));
		assertEquals(10, workitem.getItemValueInteger("$lastEvent"));
		assertEquals(0, workitem.getEventID());

	}

	@Test
	public void testReplaceDynamicValues() throws PluginException {

		String testString = "Hello <itemvalue>txtname</itemvalue>!";
		String expectedString = "Hello Anna!";

		// prepare data
		logger.info("[TestAdaptText] setup test data...");
		ItemCollection documentContext = new ItemCollection();
		documentContext.replaceItemValue("txtName", "Anna");

		String ding = workflowServiceMock.adaptText(testString, documentContext);

		String resultString = this.workflowServiceMock.adaptText(testString, documentContext);

		Assert.assertEquals(expectedString, resultString);

	}

}
