package org.imixs.workflow.jee.ejb;

import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.SessionContext;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.plugins.TestApplicationPlugin;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Abstract base class for jUnit tests using the WorkflowService.
 * 
 * This test class mocks the classes WorkflowContext, WorkflowService,
 * EntityService and ModelService. The test class generates a test database with
 * process entities and activity entities which can be accessed from a plug-in
 * or the workflowKernel.
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
 * @see TestApplicationPlugin
 * @author rsoika
 */
public class AbstractWorkflowServiceTest {
	private final static Logger logger = Logger.getLogger(AbstractWorkflowServiceTest.class.getName());

	Map<String, ItemCollection> database = null;

	protected EntityService entityService;
	protected ModelService modelService;
	protected WorkflowService workflowService = new WorkflowService();
	protected SessionContext ctx;
	protected WorkflowContext workflowContext;

	@Before
	public void setup() throws PluginException {

		// setup db
		database = new HashMap<String, ItemCollection>();
		createSimpleDatabase();

		// workflowService = new WorkflowService();
		workflowService = Mockito.mock(WorkflowService.class);

		// mock EJBs and inject them into the workflowService EJB
		entityService = Mockito.mock(EntityService.class);
		modelService = Mockito.mock(ModelService.class);
		ctx = Mockito.mock(SessionContext.class);

		workflowContext = Mockito.mock(WorkflowContext.class);

		workflowService.entityService = entityService;
		workflowService.ctx = ctx;

		// Simulate fineProfile("1.0.0") -> entityService.load()...
		when(entityService.load(Mockito.anyString())).thenAnswer(new Answer<ItemCollection>() {
			@Override
			public ItemCollection answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				String id = (String) args[0];
				ItemCollection result = database.get(id);
				return result;
			}
		});

		// simulate save() method
		when(entityService.save(Mockito.any(ItemCollection.class))).thenAnswer(new Answer<ItemCollection>() {
			@Override
			public ItemCollection answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				ItemCollection entity = (ItemCollection) args[0];
				database.put(entity.getItemValueString(EntityService.UNIQUEID), entity);
				return entity;
			}
		});

		// simulate SessionContext ctx.getCallerPrincipal().getName()
		Principal principal = Mockito.mock(Principal.class);
		when(principal.getName()).thenReturn("manfred");
		when(ctx.getCallerPrincipal()).thenReturn(principal);

		when(workflowContext.getModel()).thenReturn(modelService);
		when(workflowService.getModel()).thenReturn(modelService);

		// simulate getActivityEntity
		when(modelService.getActivityEntity(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString()))
				.thenAnswer(new Answer<ItemCollection>() {
					@Override
					public ItemCollection answer(InvocationOnMock invocation) throws Throwable {
						Object[] args = invocation.getArguments();
						int p = (Integer) args[0];
						int a = (Integer) args[1];
						ItemCollection entity = database.get("A" + p + "-" + a);
						return entity;

					}
				});

		// simulate getProcessEntity
		when(modelService.getProcessEntity(Mockito.anyInt(), Mockito.anyString()))
				.thenAnswer(new Answer<ItemCollection>() {
					@Override
					public ItemCollection answer(InvocationOnMock invocation) throws Throwable {
						Object[] args = invocation.getArguments();
						int p = (Integer) args[0];
						ItemCollection entity = database.get("P" + p);
						return entity;

					}
				});

		// simulate a workitemService.process call
		when(workflowService.processWorkItem(Mockito.any(ItemCollection.class)))
				.thenAnswer(new Answer<ItemCollection>() {
					@Override
					public ItemCollection answer(InvocationOnMock invocation) throws Throwable {

						Object[] args = invocation.getArguments();
						ItemCollection aWorkitem = (ItemCollection) args[0];

						WorkflowKernel workflowkernel = new WorkflowKernel(workflowService);
						// we do not register plugins....
						workflowkernel.process(aWorkitem);

						// save workitem in mock database
						entityService.save(aWorkitem);
						return aWorkitem;

					}
				});

		// simulate a workitemService.getWorkitem call
		when(workflowService.getWorkItem(Mockito.anyString())).thenAnswer(new Answer<ItemCollection>() {
			@Override
			public ItemCollection answer(InvocationOnMock invocation) throws Throwable {

				Object[] args = invocation.getArguments();
				String id = (String) args[0];
				return entityService.load(id);
			}
		});

	}

	/**
	 * returns a activity entity from the test database
	 * 
	 * @param processid
	 * @param activityid
	 * @return
	 */
	protected ItemCollection getActivityEntity(int processid, int activityid) {
		ItemCollection entity = database.get("A" + processid

				+ "-" + activityid);
		if (entity == null) {
			logger.warning("ActivityEntity " + processid + "." + activityid + " not defined!");
		}

		return entity;
	}

	/**
	 * Update an activity entity in the test database
	 * 
	 * @param activityEntity
	 */
	protected void setActivityEntity(ItemCollection entity) {
		int p = entity.getItemValueInteger("numProcessID");
		int a = entity.getItemValueInteger("numActivityID");

		String uniqueid = "A" + p + "-" + a;
		entity.replaceItemValue(EntityService.UNIQUEID, uniqueid);
		database.put(entity.getItemValueString(EntityService.UNIQUEID), entity);
		logger.fine("add activityEntity: " + uniqueid);
	}

	/**
	 * returns a activity entity from the test database
	 * 
	 * @param processid
	 * @return
	 */
	protected ItemCollection getProcessEntity(int processid) {
		ItemCollection entity = database.get("P" + processid);

		if (entity == null) {
			logger.warning("ProcessEntity " + processid + " not defined!");
		}

		return entity;
	}

	/**
	 * Update an process entity in the test database
	 * 
	 * @param activityEntity
	 */
	protected void setProcessEntity(ItemCollection entity) {
		int p = entity.getItemValueInteger("numProcessID");

		String uniqueid = "P" + p;
		entity.replaceItemValue(EntityService.UNIQUEID, uniqueid);
		database.put(entity.getItemValueString(EntityService.UNIQUEID), entity);
		logger.fine("add processEntity: " + uniqueid);
	}

	/**
	 * Create a test database with some workItems and a simple model
	 */
	private void createSimpleDatabase() {
		ItemCollection entity = null;
		String modelVersion = "1.0.0";

		logger.info("createSimpleDatabase....");
		// simulate profile entity
		entity = new ItemCollection();
		entity.replaceItemValue("type", "WorkflowEnvironmentEntity");
		entity.replaceItemValue("$ModelVersion", modelVersion);
		entity.replaceItemValue(EntityService.UNIQUEID, "ENV0000-0000");
		entity.replaceItemValue("txtName", "WorkflowEnvironmentEntity ");
		database.put(entity.getItemValueString(EntityService.UNIQUEID), entity);

		// simulate process/activities
		for (int i = 1; i <= 3; i++) {
			// process entities 100-300
			entity = new ItemCollection();
			entity.replaceItemValue("type", "ProcessEntity");
			entity.replaceItemValue("$ModelVersion", modelVersion);
			entity.replaceItemValue("txtName", "Process " + 100 * i);
			entity.replaceItemValue("$ModelVersion", "1.0.0");
			entity.replaceItemValue("numProcessID", 100 * i);

			this.setProcessEntity(entity);

			// activities
			// 100.10, 100.20, 100.30, 200.10, 200.20 , .....
			// nextProcessID is for each activity 100, 200, 300
			for (int j = 1; j <= 3; j++) {
				entity = new ItemCollection();
				entity.replaceItemValue("type", "ActivityEntity");
				entity.replaceItemValue("$ModelVersion", modelVersion);

				entity.replaceItemValue("txtName", "Activity " + 100 * j + "." + 10 * j);
				entity.replaceItemValue("$ModelVersion", "1.0.0");
				entity.replaceItemValue("numProcessID", 100 * i);
				entity.replaceItemValue("numnextprocessid", 100 * j);
				entity.replaceItemValue("numActivityID", 10 * j);

				this.setActivityEntity(entity);
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
			entity.replaceItemValue(WorkflowService.ISAUTHOR, true);
			database.put(entity.getItemValueString(EntityService.UNIQUEID), entity);
		}

	}
}
