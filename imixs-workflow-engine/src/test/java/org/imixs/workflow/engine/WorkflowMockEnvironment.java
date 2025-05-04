package org.imixs.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.imixs.workflow.Adapter;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.PluginException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

import jakarta.ejb.SessionContext;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;

/**
 * The {@code WorkflowMockEnvironment} can be used as a base class for junit
 * tests to mock the Imixs WorkflowService. The class mocks the WorkflowService
 * and a workflow environment including the ModelService.
 * 
 * Junit tests can instantiate this class to verify specific
 * method implementations of the workflowService, Plugin classes or Adapters in
 * a easy way.
 * <p>
 * Because this is a abstract base test class we annotate the MockitoSettings
 * {@link Strictness} to avoid
 * org.mockito.exceptions.misusing.UnnecessaryStubbingException.
 * 
 * @author rsoika
 */
@MockitoSettings(strictness = Strictness.WARN)
public class WorkflowMockEnvironment {
	protected final static Logger logger = Logger.getLogger(WorkflowMockEnvironment.class.getName());

	protected SessionContext ctx = null;
	protected Map<String, ItemCollection> database = null;

	@Mock
	protected DocumentService documentService; // Mock instance

	@InjectMocks
	protected ModelService modelService; // Injects mocks into ModelService

	@InjectMocks
	protected WorkflowService workflowService; // Injects mocks into WorkflowService

	@InjectMocks
	protected WorkflowContextService workflowContextService; // Injects mocks into WorkflowService

	// protected WorkflowContextMock workflowContext = null;
	protected List<Adapter> adapterList = new ArrayList<>();

	public ModelService getModelService() {
		return modelService;
	}

	// public WorkflowContextMock getWorkflowContext() {
	// return workflowContext;
	// }

	public DocumentService getDocumentService() {
		return documentService;
	}

	public WorkflowService getWorkflowService() {
		return workflowService;
	}

	/**
	 * Can be used to register an Adapter before Setup
	 * 
	 * @param adapter
	 */
	public void registerAdapter(Adapter adapter) {
		adapterList.add(adapter);
	}

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
	public void setUp() throws PluginException {
		// Ensures that @Mock and @InjectMocks annotations are processed
		MockitoAnnotations.openMocks(this);

		// Set up test environment
		createTestDatabase();

		ctx = Mockito.mock(SessionContext.class);
		setupSessionContext();

		// Link modelService to workflowServiceMock
		workflowService.modelService = modelService;
		workflowService.workflowContextService = workflowContextService;
		workflowContextService.modelService = modelService;
		modelService.modelManager = new ModelManager();
		assertNotNull(modelService.getModelManager());

		// workflowContext = new WorkflowContextMock();
		workflowService.ctx = ctx; // workflowContext.getSessionContext();
		workflowContextService.ctx = ctx;

		// Mock Database Service with a in-memory database...
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

		/*
		 * Mock Event<TextEvent>
		 */
		Event<TextEvent> mockTextEvents = Mockito.mock(Event.class);
		// Set up behavior for the mock to simulate firing adapters
		Mockito.doAnswer(invocation -> {
			TextEvent event = invocation.getArgument(0);

			// Create and use the adapters
			TextItemValueAdapter tiva = new TextItemValueAdapter();
			TextForEachAdapter tfea = new TextForEachAdapter();

			// Invoke adapters
			tfea.onEvent(event);
			tiva.onEvent(event);

			return null;
		}).when(mockTextEvents).fire(Mockito.any(TextEvent.class));
		// Inject the mocked Event<TextEvent> into the workflowService
		injectMockIntoField(workflowContextService, "textEvents", mockTextEvents);

		/*
		 * Mock Instance<Adapter> for adapters field
		 */
		@SuppressWarnings("unchecked")
		Instance<Adapter> mockAdapters = Mockito.mock(Instance.class);
		// Set up behavior to return adapters from the adapterList
		when(mockAdapters.iterator()).thenAnswer(invocation -> adapterList.iterator());
		when(mockAdapters.get()).thenAnswer(invocation -> adapterList.isEmpty() ? null : adapterList.get(0));
		// Inject the mocked Instance<Adapter> into the workflowService
		injectMockIntoField(workflowService, "adapters", mockAdapters);

	}

	/**
	 * Helper method that loads a new model into the ModelService
	 * 
	 * @param modelPath
	 */
	public void loadBPMNModel(String modelPath) {
		try {
			BPMNModel model = BPMNModelFactory.read(modelPath);
			modelService.addModel(model);
		} catch (BPMNModelException e) {
			e.printStackTrace();
			fail();
		}
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
	 * Helper method to inject a mock into a private/protected field using
	 * reflection.
	 *
	 * @param targetObject The object into which the field is to be injected.
	 * @param fieldName    The name of the field to inject.
	 * @param value        The mock or object to inject into the field.
	 */
	public void injectMockIntoField(Object targetObject, String fieldName, Object value) {
		try {
			Field field = targetObject.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(targetObject, value);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException("Failed to inject mock into field: " + fieldName, e);
		}
	}

	/**
	 * Creates a Mock for a Session Context with a test principal 'manfred'
	 */
	private void setupSessionContext() {
		Principal principal = Mockito.mock(Principal.class);
		when(principal.getName()).thenReturn("manfred");
		when(ctx.getCallerPrincipal()).thenReturn(principal);
	}

	// /**
	// * Helper method to inject a mock into a private/protected field using
	// * reflection.
	// *
	// * @param targetObject The object into which the field is to be injected.
	// * @param fieldName The name of the field to inject.
	// * @param value The mock or object to inject into the field.
	// */
	// private void injectMockIntoField(Object targetObject, String fieldName,
	// Object value) {
	// try {
	// Field field = targetObject.getClass().getDeclaredField(fieldName);
	// field.setAccessible(true);
	// field.set(targetObject, value);
	// } catch (NoSuchFieldException | IllegalAccessException e) {
	// throw new RuntimeException("Failed to inject mock into field: " + fieldName,
	// e);
	// }
	// }
}
