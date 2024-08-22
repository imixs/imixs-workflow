package org.imixs.workflow.engine;

import org.imixs.workflow.bpmn.OpenBPMNModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
public class TestModelServiceNew {
	public static final String DEFAULT_MODEL_VERSION = "1.0.0";

	@Mock
	private DocumentService documentService;

	@InjectMocks
	ModelService modelServiceMock;

	/**
	 * This test
	 * 
	 * @throws ModelException
	 * 
	 */
	@Test
	public void testGetDataObject() throws ModelException {

		OpenBPMNModelManager openBPMNModelManager = modelServiceMock.getOpenBPMNModelManager();

		Assert.assertNotNull(openBPMNModelManager);

	}

}
