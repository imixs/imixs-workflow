package org.imixs.workflow.jee.ejb;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
public class AbstractPluginTest extends AbstractWorkflowEnvironment {
	protected WorkflowService workflowService = new WorkflowService();

	@Before
	public void setup() throws PluginException {

		super.setup();

		// mock workflowService
		workflowService = Mockito.mock(WorkflowService.class);
		workflowContext = Mockito.mock(WorkflowContext.class);
		workflowService.entityService = entityService;
		workflowService.ctx = ctx;
		

		when(workflowContext.getModel()).thenReturn(modelService);
		when(workflowService.getModel()).thenReturn(modelService);

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

		// simulate a workitemService.getWorkListByRef call
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

	}

	

	
	
}
