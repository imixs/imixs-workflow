/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow.engine;

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
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.exceptions.ModelException;
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
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jakarta.ejb.SessionContext;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;

/**
 * The {@code MockWorkflowEnvironment} is used as a base class for junit
 * tests to mock the Imixs WorkflowService, WorkflowContextService and
 * ModelService.
 * <p>
 * Junit tests can instantiate this class to verify specific
 * method implementations of the workflowService, Plugin classes or Adapters in
 * a easy way.
 * <p>
 * External projects should use the imixs-mock library instead
 * (https://github.com/imixs/imixs-mock)
 * 
 * @author rsoika
 */
@MockitoSettings(strictness = Strictness.WARN)
public class MockWorkflowEnvironment {
	protected final static Logger logger = Logger.getLogger(MockWorkflowEnvironment.class.getName());

	protected SessionContext sessionContext = null;
	protected Map<String, ItemCollection> database = null;

	@Mock
	protected DocumentService documentService; // Mock instance

	@InjectMocks
	protected ModelService modelService; // Injects mocks into ModelService

	@InjectMocks
	protected WorkflowService workflowService; // Injects mocks into WorkflowService

	protected List<Adapter> adapterList = new ArrayList<>();
	protected List<Plugin> pluginList = new ArrayList<>();

	protected ModelManager modelManager = null;

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

		sessionContext = Mockito.mock(SessionContext.class);
		setupSessionContext();

		// Link modelService to workflowServiceMock
		workflowService.modelService = modelService;

		modelManager = new ModelManager(workflowService);
		workflowService.ctx = sessionContext;

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
		injectMockIntoField(workflowService, "textEvents", mockTextEvents);

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

	public ModelManager getModelManager() {
		return modelManager;
	}

	public ModelService getModelService() {
		return modelService;
	}

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
	 * Can be used to register an plugin before Setup
	 * 
	 * @param adapter
	 * @throws PluginException
	 */
	public void registerPlugin(Plugin plugin) throws PluginException {
		pluginList.add(plugin);
		plugin.init(workflowService);
	}

	/**
	 * Helper method to load a model from internal cache (not thread save)
	 * 
	 * @param version
	 * @return
	 * @throws ModelException
	 */
	public BPMNModel fetchModel(String version) throws ModelException {
		return modelService.getBPMNModel(version);
	}

	/**
	 * Loads a new model
	 * 
	 * @param modelPath
	 */
	public void loadBPMNModelFromFile(String modelPath) {

		try {
			BPMNModel model = BPMNModelFactory.read(modelPath);
			modelService.addModelData(BPMNUtil.getVersion(model), model, null);

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
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
	}

	/**
	 * This method updates the plugin definitions for a registered Model. This can
	 * be helpful if a junit test needs a reduced plugin list to reduce complexity
	 * of a test.
	 */
	public void updatePluginDefinition(BPMNModel model, List<String> newPlugins) {

		Element rootElement = model.getDefinitions();
		String imixsNamespace = "http://www.imixs.org/bpmn2";
		Document doc = rootElement.getOwnerDocument();

		// search for txtplugins Item
		Element pluginItem = findPluginItem(rootElement, imixsNamespace);

		if (pluginItem != null) {
			// remove old values
			removeOldValues(pluginItem, imixsNamespace);

			// add new plugin list
			for (String plugin : newPlugins) {
				Element valueElement = doc.createElementNS(imixsNamespace, "imixs:value");
				CDATASection cdata = doc.createCDATASection(plugin);
				valueElement.appendChild(cdata);
				pluginItem.appendChild(valueElement);
			}
		}

		// Update model data
		this.getModelService().addModelData(BPMNUtil.getVersion(model), model, null);
	}

	private static Element findPluginItem(Element parent, String namespace) {
		NodeList children = parent.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);

			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) child;

				// check if imixs:item name="txtplugins"
				if ("item".equals(element.getLocalName()) &&
						namespace.equals(element.getNamespaceURI()) &&
						"txtplugins".equals(element.getAttribute("name"))) {
					return element;
				}

				// Rekursiv search
				Element found = findPluginItem(element, namespace);
				if (found != null) {
					return found;
				}
			}
		}

		return null;
	}

	private static void removeOldValues(Element pluginItem, String namespace) {
		NodeList children = pluginItem.getChildNodes();

		for (int i = children.getLength() - 1; i >= 0; i--) {
			Node child = children.item(i);

			if (child.getNodeType() == Node.ELEMENT_NODE &&
					"value".equals(child.getLocalName()) &&
					namespace.equals(child.getNamespaceURI())) {
				pluginItem.removeChild(child);
			}
		}
	}
}
