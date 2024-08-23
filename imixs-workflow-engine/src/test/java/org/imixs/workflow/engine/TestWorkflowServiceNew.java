package org.imixs.workflow.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

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
public class TestWorkflowServiceNew {
	private final static Logger logger = Logger.getLogger(TestWorkflowServiceNew.class.getName());

	protected Map<String, ItemCollection> database = null;

	@Mock
	private DocumentService documentService;

	@InjectMocks
	ModelService modelService;

	@InjectMocks
	WorkflowService workflowServiceMock;

	MockWorkflowEngineContext workflowContext = null;

	@BeforeEach
	public void setUp() {

		MockitoAnnotations.openMocks(this);

		createTestDatabase();

		loadBPMNModel("/bpmn/plugin-test.bpmn");
		workflowServiceMock.modelService = modelService;
		workflowContext = new MockWorkflowEngineContext();
		workflowServiceMock.ctx = workflowContext.getSessionContext();

		// Mock Database Service...
		when(documentService.load(Mockito.anyString())).thenAnswer(new Answer<ItemCollection>() {
			@Override
			public ItemCollection answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				String id = (String) args[0];
				ItemCollection result = database.get(id);
				if (result != null) {
					// set author access=true
					result.replaceItemValue(DocumentService.ISAUTHOR, true);
				}
				return result;
			}
		});
		when(documentService.save(Mockito.any())).thenAnswer(new Answer<ItemCollection>() {
			@Override
			public ItemCollection answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				ItemCollection data = (ItemCollection) args[0];
				if (data != null) {
					database.put(data.getUniqueID(), data);
				}
				return data;
			}
		});

	}

	/**
	 * This test simulates a workflowService process call by mocking the entity and
	 * model service.
	 * 
	 * This is just a simple simulation...
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * @throws ModelException
	 */
	@Test
	public void testProcessSimple() {

		try {
			ItemCollection workitem = documentService.load("W0000-00001");
			assertNotNull(workitem);
			// load test workitem
			workitem = workflowServiceMock.getDocumentService().load("W0000-00001");
			assertNotNull(workitem);
			assertEquals("W0000-00001", workitem.getUniqueID());

		} catch (AccessDeniedException | ProcessingErrorException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

	}

	/**
	 * This test simulates a workflowService process call by mocking the entity and
	 * model service.
	 * 
	 * This is just a simple simulation...
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * @throws ModelException
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

		Assert.assertEquals("1.0.0", workitem.getItemValueString("$ModelVersion"));
		Assert.assertEquals(10, workitem.getItemValueInteger("$lastEvent"));
		Assert.assertEquals(0, workitem.getEventID());

	}

	/**
	 * Create a test database with some workItems and a simple model
	 */
	protected void createTestDatabase() {

		database = new HashMap<String, ItemCollection>();
		ItemCollection entity = null;
		logger.info("createSimpleDatabase....");
		// create workitems
		for (int i = 1; i < 6; i++) {
			entity = new ItemCollection();
			entity.replaceItemValue("type", "workitem");
			entity.replaceItemValue(WorkflowKernel.UNIQUEID, "W0000-0000" + i);
			entity.replaceItemValue("txtName", "Workitem " + i);
			entity.setModelVersion("1.0.0");
			entity.setTaskID(100);
			entity.setEventID(10);
			entity.replaceItemValue(DocumentService.ISAUTHOR, true);
			database.put(entity.getItemValueString(WorkflowKernel.UNIQUEID), entity);
		}
	}

	/**
	 * Loads a new model
	 * 
	 * @param modelPath
	 */
	public void loadBPMNModel(String modelPath) {
		try {
			BPMNModel model = BPMNModelFactory.read(modelPath);
			modelService.getOpenBPMNModelManager().addModel(model);
		} catch (BPMNModelException | ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
}
