package org.imixs.workflow.engine;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

/**
 * The {@code AbstractWorkflowServiceTest} can be used as a base class for junit
 * tests to mock the Imixs WorkflowService. The class mocks the WorkflowService
 * and a workflow environment including the ModelService.
 * 
 * Junit tests can extend the AbstractWorkflowServiceTest to verify specific
 * method implementations of the workflowService, Plugin classes or Adapters in
 * a easy way.
 * <p>
 * Because this is a abstract base test class we annotate the MockitoSettings
 * {@link Strictness} to avoid
 * org.mockito.exceptions.misusing.UnnecessaryStubbingException.
 * 
 * @author rsoika
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public abstract class AbstractWorkflowServiceTest {
	protected final static Logger logger = Logger.getLogger(AbstractWorkflowServiceTest.class.getName());

	protected Map<String, ItemCollection> database = null;

	@Mock
	protected DocumentService documentService;

	@InjectMocks
	protected ModelService modelService;

	@InjectMocks
	protected WorkflowService workflowServiceMock;

	protected MockWorkflowEngineContext workflowContext = null;

	/**
	 * The Setup method initializes a mock environment to test the imixs workflow
	 * service. It initializes a in-memory database and a model Service as also a
	 * Session context object.
	 * <p>
	 * You can overwrite this method in a junit test to add additional test
	 * settings.
	 * 
	 * @throws PluginException
	 */
	@BeforeEach
	public void setUp() throws PluginException {
		// Ensure all mocks are initialized properly
		MockitoAnnotations.openMocks(this);

		// Set up test environment
		createTestDatabase();
		loadBPMNModel("/bpmn/plugin-test.bpmn");

		// Link model service to the mock workflow service
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
	 * Helper method that loads a new model into the ModelService
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
