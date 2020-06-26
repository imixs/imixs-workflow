package org.imixs.workflow.engine;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.SessionContext;
import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.WorkflowContext;
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
import org.xml.sax.SAXException;

/**
 * The WorkflowSimulationEnvironment provides a test environment for jUnit
 * tests. The WorkflowSimulationEnvironment uses the SimulationService EJB to
 * simulate the processing life cycle of a process instance.
 * 
 * This test class mocks the ModelService and WorkflowContext. The Environment
 * did not store any data, so a test class can not read processed workitems. The
 * enviroment is typical used to verify the process flow of a model.
 * 
 * A plugin definition list can be set to simulate also the execution of plugins
 * 
 * @version 1.0
 * @author rsoika
 */
public class WorkflowSimulationEnvironment {
	private final static Logger logger = Logger.getLogger(WorkflowSimulationEnvironment.class.getName());

	public static final String DEFAULT_MODEL_VERSION = "1.0.0";
	private BPMNModel model = null;
	private String modelPath = null;// "/bpmn/plugin-test.bpmn";

	@Spy
	protected ModelService modelService;
	protected SessionContext ctx;

	protected WorkflowContext workflowContext;
	protected SimulationService simulationService;
	protected List<String> plugins=null;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws PluginException, ModelException, AccessDeniedException, ProcessingErrorException {
		MockitoAnnotations.initMocks(this);

		// mock session context
		ctx = Mockito.mock(SessionContext.class);
		// simulate SessionContext ctx.getCallerPrincipal().getName()
		Principal principal = Mockito.mock(Principal.class);
		when(principal.getName()).thenReturn("manfred");
		when(ctx.getCallerPrincipal()).thenReturn(principal);

		// mock Entity service

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
		simulationService = Mockito.mock(SimulationService.class);
		// workflowService.documentService = documentService;
		simulationService.setCtx(ctx);

		simulationService.setModelService(modelService);
		when(simulationService.getModelManager()).thenReturn(modelService);

		when(simulationService.processWorkItem(Mockito.any(ItemCollection.class), Mockito.any(List.class)))
				.thenCallRealMethod();

	}

	
	public String getModelPath() {
		return modelPath;
	}

	public void setModelPath(String modelPath) {
		this.modelPath = modelPath;
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
			InputStream inputStream = getClass().getResourceAsStream(this.modelPath);
			try {
				logger.info("loading model: " + this.modelPath + "....");
				model = BPMNParser.parseModel(inputStream, "UTF-8");

				this.modelService.addModel(model);
			} catch (ModelException | ParseException | ParserConfigurationException | SAXException | IOException e) {
				e.printStackTrace();
			}

		}

	}

	public List<String> getPlugins() {
		return plugins;
	}


	public void setPlugins(List<String> plugins) {
		this.plugins = plugins;
	}


	/**
	 * Simulates a processing life cycle 
	 * @param workitem
	 * @return
	 * @throws ModelException 
	 * @throws PluginException 
	 * @throws ProcessingErrorException 
	 * @throws AccessDeniedException 
	 */
	public ItemCollection processWorkItem(ItemCollection workitem) throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {
		workitem = simulationService.processWorkItem(workitem, plugins);
		return workitem;
	}
	
}
