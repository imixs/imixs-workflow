package org.imixs.workflow.jee.ejb;

import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.SessionContext;

import junit.framework.Assert;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Test class for WorkflowService
 * 
 * This test verifies if the correct next model version is set in workitem
 * processed by the workflow service.
 * 
 * 
 * @author rsoika
 */
public class TestWorkflowService {

	Map<String, ItemCollection> database = null;

	private EntityService entityService;
	private ModelService modelService;
	private WorkflowService workflowService = new WorkflowService();
	private SessionContext ctx;

	@Before
	public void setup() throws PluginException {

		// setup db
		database = new HashMap<String, ItemCollection>();
		createSimpleDatabase();

		workflowService = new WorkflowService();

		// mock EJBs and inject them into theworkflowService EJB
		entityService = Mockito.mock(EntityService.class);
		modelService = Mockito.mock(ModelService.class);
		ctx = Mockito.mock(SessionContext.class);
		workflowService.entityService = entityService;
		workflowService.ctx = ctx;

		// Simulate fineProfile("1.0.0") -> entityService.load()...
		when(entityService.load(Mockito.anyString())).thenAnswer(
				new Answer<ItemCollection>() {
					@Override
					public ItemCollection answer(InvocationOnMock invocation)
							throws Throwable {
						Object[] args = invocation.getArguments();
						String id = (String) args[0];
						ItemCollection result = database.get(id);
						return result;
					}
				});

		// simulate save() method
		when(entityService.save(Mockito.any(ItemCollection.class))).thenAnswer(
				new Answer<ItemCollection>() {
					@Override
					public ItemCollection answer(InvocationOnMock invocation)
							throws Throwable {
						Object[] args = invocation.getArguments();
						ItemCollection entity = (ItemCollection) args[0];
						database.put(entity
								.getItemValueString(EntityService.UNIQUEID),
								entity);
						return entity;
					}
				});

		// simulate SessionContext ctx.getCallerPrincipal().getName()
		Principal principal = Mockito.mock(Principal.class);
		when(principal.getName()).thenReturn("manfred");
		when(ctx.getCallerPrincipal()).thenReturn(principal);

		workflowService.modelService = modelService;

		// simulate  getActivityEntityByVersion
		when(
				modelService.getActivityEntityByVersion(Mockito.anyInt(),
						Mockito.anyInt(), Mockito.anyString())).thenAnswer(
				new Answer<ItemCollection>() {
					@Override
					public ItemCollection answer(InvocationOnMock invocation)
							throws Throwable {
						Object[] args = invocation.getArguments();
						int p = (Integer) args[0];
						int a = (Integer) args[1];
						ItemCollection entity = database.get("A" + p + "-" + a);
						return entity;

					}
				});

	}

	/**
	 * This test simulates a workflowService process call by mocking the entity
	 * and model service.
	 * 
	 * This is just a simple simulation...
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * 
	 */
	@Test
	public void testProcessSimple() throws AccessDeniedException,
			ProcessingErrorException, PluginException {
		// load test workitem
		ItemCollection workitem = database.get("W0000-00001");

		// simulate findModelProfile(String modelversion)
		when(
				entityService.findAllEntities(Mockito.anyString(),
						Mockito.anyInt(), Mockito.anyInt())).thenAnswer(
				new Answer<List<ItemCollection>>() {
					@Override
					public List<ItemCollection> answer(
							InvocationOnMock invocation) throws Throwable {
						Object[] args = invocation.getArguments();
						String query = (String) args[0];
						if (query.contains("1.0.0")) {
							ItemCollection result = database
									.get("ENV0000-0000");
							List<ItemCollection> resultList = new ArrayList<ItemCollection>();
							;
							resultList.add(result);
							return resultList;
						} else
							return null;
					}
				});

		workitem = workflowService.processWorkItem(workitem);

		Assert.assertEquals("1.0.0",
				workitem.getItemValueString("$ModelVersion"));

	}

	/**
	 * Create a test database with some workItems and a simple model
	 */
	private void createSimpleDatabase() {
		ItemCollection entity = null;
		String modelVersion = "1.0.0";

		// simulate profile entity
		entity = new ItemCollection();
		entity.replaceItemValue("type", "WorkflowEnvironmentEntity");
		entity.replaceItemValue("$ModelVersion", modelVersion);
		entity.replaceItemValue(EntityService.UNIQUEID, "ENV0000-0000");
		entity.replaceItemValue("txtName", "WorkflowEnvironmentEntity ");
		database.put(entity.getItemValueString(EntityService.UNIQUEID), entity);

		// simulate process/activities
		for (int i = 1; i <= 3; i++) {
			entity = new ItemCollection();
			entity.replaceItemValue("type", "ProcessEntity");
			entity.replaceItemValue("$ModelVersion", modelVersion);
			entity.replaceItemValue(EntityService.UNIQUEID, "P" + i * 100);
			entity.replaceItemValue("txtName", "Process " + 100 * i);
			entity.replaceItemValue("$ModelVersion", "1.0.0");
			entity.replaceItemValue("numProcessID", 100 * i);

			database.put(entity.getItemValueString(EntityService.UNIQUEID),
					entity);
			// activities
			for (int j = 1; j <= 3; j++) {
				entity = new ItemCollection();
				entity.replaceItemValue("type", "ActivityEntity");
				entity.replaceItemValue("$ModelVersion", modelVersion);
				entity.replaceItemValue(EntityService.UNIQUEID, "A" + 100 * i
						+ "-" + 10 * j);
				entity.replaceItemValue("txtName", "Activity " + 100 * j + "."
						+ 10 * j);
				entity.replaceItemValue("$ModelVersion", "1.0.0");
				entity.replaceItemValue("numProcessID", 100 * i);
				entity.replaceItemValue("numnextprocessid", 100 * i);
				entity.replaceItemValue("numActivityID", 10 * j);

				database.put(entity.getItemValueString(EntityService.UNIQUEID),
						entity);
			}

		}

		// create workitems
		for (int i = 1; i < 6; i++) {
			entity = new ItemCollection();
			entity.replaceItemValue("type", "workitem");
			entity.replaceItemValue(EntityService.UNIQUEID, "W0000-0000" + i);
			entity.replaceItemValue("txtName", "Workitem " + i);
			entity.replaceItemValue("$ModelVersion", "1.0.0");
			entity.replaceItemValue("$ProcessID", 100);
			entity.replaceItemValue("$ActivityID", 10);

			database.put(entity.getItemValueString(EntityService.UNIQUEID),
					entity);
		}

	}
}
