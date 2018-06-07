package org.imixs.workflow.engine;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.SessionContext;
import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.bpmn.BPMNParser;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xml.sax.SAXException;

/**
 * The WorkflowMockEnvironment provides a mocked database environment for jUnit
 * tests. The WorkflowMockEnvironment can be used to test the WorkflowService.
 * 
 * This test class mocks a complete workflow environment without the class
 * WorkflowService
 * 
 * The test class generates a test database with process entities and activity
 * entities which can be accessed from a plug-in or the workflowKernel.
 * 
 * A JUnit Test can save, load and process workitems.
 * 
 * JUnit tests can also manipulate the model by changing entities through
 * calling the methods:
 * 
 * getActivityEntity,setActivityEntity,getProcessEntity,setProcessEntity
 * 
 * 
 * @version 2.0
 * @see AbstractPluginTest, TestWorkflowService, ModelPluginMock
 * @author rsoika
 */
public class WorkflowMockEnvironment {
	private final static Logger logger = Logger.getLogger(WorkflowMockEnvironment.class.getName());
	public static final String DEFAULT_MODEL_VERSION = "1.0.0";

	Map<String, ItemCollection> database = null;
	protected DocumentService documentService;
	protected WorkflowService workflowService;

	@Spy
	protected ModelService modelService;
	protected SessionContext ctx;

	protected WorkflowContext workflowContext;
	private BPMNModel model = null;

	private String modelPath = null;// "/bpmn/plugin-test.bpmn";

	public String getModelPath() {
		return modelPath;
	}

	public void setModelPath(String modelPath) {
		this.modelPath = modelPath;
	}

	public WorkflowContext getWorkflowContext() {
		return workflowContext;
	}

	public WorkflowService getWorkflowService() {
		return workflowService;
	}

	public ModelService getModelService() {
		return modelService;
	}

	public DocumentService getDocumentService() {
		return documentService;
	}

	@Before
	public void setup() throws PluginException, ModelException {
		MockitoAnnotations.initMocks(this);
		// setup db
		createTestDatabase();

		// mock session context
		ctx = Mockito.mock(SessionContext.class);
		// simulate SessionContext ctx.getCallerPrincipal().getName()
		Principal principal = Mockito.mock(Principal.class);
		when(principal.getName()).thenReturn("manfred");
		when(ctx.getCallerPrincipal()).thenReturn(principal);

		// mock Entity service
		documentService = Mockito.mock(DocumentService.class);
		// Simulate fineProfile("1.0.0") -> entityService.load()...
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

		// simulate save() method
		when(documentService.save(Mockito.any(ItemCollection.class))).thenAnswer(new Answer<ItemCollection>() {
			@Override
			public ItemCollection answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				ItemCollection entity = (ItemCollection) args[0];
				// test if uniqueid is available
				if (entity.getUniqueID().isEmpty()) {
					entity.replaceItemValue(WorkflowKernel.UNIQUEID, WorkflowKernel.generateUniqueID());
				}
				database.put(entity.getItemValueString(WorkflowKernel.UNIQUEID), entity);
				return entity;
			}
		});

		// Mock modelService (using the @spy) annotation
		Mockito.doNothing().when(modelService).init();

		// load model
		loadModel();

		// Mock modelManager
		ModelManager modelManager = Mockito.mock(ModelManager.class);
		try {
			when(modelManager.getModel(Mockito.anyString())).thenReturn(this.getModel());
			when(modelManager.getModelByWorkitem(Mockito.any(ItemCollection.class))).thenReturn(this.getModel());
		} catch (ModelException e) {
			e.printStackTrace();
		}

		// Mock context
		workflowContext = Mockito.mock(WorkflowContext.class);
		when(workflowContext.getModelManager()).thenReturn(modelManager);

		// Mock WorkflowService
		workflowService = Mockito.mock(WorkflowService.class);
		workflowService.documentService = documentService;
		workflowService.ctx = ctx;

		workflowService.modelService = modelService;
		when(workflowService.getModelManager()).thenReturn(modelService);
		when(workflowService.getWorkListByRef(Mockito.anyString())).thenAnswer(new Answer<List<ItemCollection>>() {
			@Override
			public List<ItemCollection> answer(InvocationOnMock invocation) throws Throwable {

				List<ItemCollection> result = new ArrayList<>();
				Object[] args = invocation.getArguments();
				String id = (String) args[0];

				// iterate over all data and return matching workitems.
				Collection<ItemCollection> allEntities = database.values();
				for (ItemCollection aentity : allEntities) {
					if (aentity.getItemValueString(WorkflowService.UNIQUEIDREF).equals(id)) {
						result.add(aentity);
					}
				}

				return result;
			}
		});

		
		// AdaptText
		when(workflowService.adaptText(Mockito.anyString(),Mockito.any(ItemCollection.class))).thenAnswer(new Answer<String>(){
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable, PluginException {
				
				Object[] args = invocation.getArguments();
				String text=(String) args[0];
				ItemCollection document = (ItemCollection) args[1];

				TextEvent textEvent=new TextEvent(text, document);
				
				TextItemValueAdapter tiva=new TextItemValueAdapter();
				tiva.onEvent(textEvent);
				
				
				return textEvent.getText();
			}
		});

		
		// AdaptText
		when(workflowService.adaptTextList(Mockito.anyString(),Mockito.any(ItemCollection.class))).thenAnswer(new Answer<List<String>>(){
			@Override
			public List<String> answer(InvocationOnMock invocation) throws Throwable, PluginException {
				
				Object[] args = invocation.getArguments();
				String text=(String) args[0];
				ItemCollection document = (ItemCollection) args[1];

				TextEvent textEvent=new TextEvent(text, document);
				
				TextItemValueAdapter tiva=new TextItemValueAdapter();
				tiva.onEvent(textEvent);
				
				
				return textEvent.getTextList();
			}
		});

		
		
		
		when(workflowService.evalNextTask(Mockito.any(ItemCollection.class),Mockito.any(ItemCollection.class))).thenCallRealMethod();
		when(workflowService.evalWorkflowResult(Mockito.any(ItemCollection.class),Mockito.any(ItemCollection.class))).thenCallRealMethod();
		when(workflowService.evalWorkflowResult(Mockito.any(ItemCollection.class),Mockito.any(ItemCollection.class),Mockito.anyBoolean())).thenCallRealMethod();
		when(workflowService.processWorkItem(Mockito.any(ItemCollection.class))).thenCallRealMethod();
		when(workflowService.getUserName()).thenCallRealMethod();
		when(workflowService.getWorkItem(Mockito.anyString())).thenCallRealMethod();

		try {
			when(workflowService.getEvents(Mockito.any(ItemCollection.class))).thenCallRealMethod();
		} catch (ModelException e) {

			e.printStackTrace();
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

	public Model getModel() {
		return model;
	}

	/**
	 * loads a model from the given path
	 * 
	 * @param modelPath
	 */
	public void loadModel(String modelPath) {
		setModelPath(modelPath);
		loadModel();
	}

	/**
	 * loads the current model
	 */
	public void loadModel() {
		if (this.modelPath != null) {
			long lLoadTime = System.currentTimeMillis();
			InputStream inputStream = WorkflowMockEnvironment.class.getResourceAsStream(this.modelPath);
			try {
				logger.info("loading model: " + this.modelPath + "....");
				model = BPMNParser.parseModel(inputStream, "UTF-8");

				this.modelService.addModel(model);
				logger.fine("...loadModel processing time=" + (System.currentTimeMillis() - lLoadTime) + "ms");	
			} catch (ModelException | ParseException | ParserConfigurationException | SAXException | IOException e) {
				e.printStackTrace();
			}

		}

	}

	public Map<String, ItemCollection> getDatabase() {
		return database;
	}

	/**
	 * Mocks a processing life cycle
	 * 
	 * @param workitem
	 * @return
	 * @throws ModelException
	 * @throws PluginException
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 */
	public ItemCollection processWorkItem(ItemCollection workitem)
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {
		workitem = workflowService.processWorkItem(workitem);
		return workitem;
	}
}
