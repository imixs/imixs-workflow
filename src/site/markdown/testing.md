# Imixs-Workflow - Simulation and Testing

The Imixs-Workflow project provides a services and test environments to simulate and test the processing life cycle of a specific model or process instance. 

## Simulate a Processing Live Cycle

The `WorkflowService` provides a method called `evalNextTask`. This method evaluates the next task for a process instance (workitem) based on the current model definition. A Workitem must at least provide the properties `$TASKID` and `$EVENTID`. The method call can be helpful in many cases when a business logic just need to compute the next logical BPMN Task Element which will be assigned with a given process instance. 


```java
	try {
		// simulate the processing life cycle of a given workitem
		ItemCollection nextTaskEnity = workflowService.evalNextTask(workitem);
	} catch (ModelException e) {
		throw new PluginException(DocumentComposerPlugin.class.getSimpleName(), e.getErrorCode(), e.getMessage());
	}
```

**Note:** During the evaluation life-cycle more than one events can be evaluated. This depends on the model definition which can define follow-up-events, split-events and conditional events. The `evalNextTask` method did not persist the process instance or execute any plugin or adapter classes.


## Testing with the WorkflowMockEnvironment

The Test class `WorkflowMockEnvironment`  mocks a full workflow environment including a in-memory-database. The `WorkflowMockEnvironment` can be used for more complex integration tests using JUnit 5 or higher.  

	
```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.*;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestTemplate {

	protected WorkflowMockEnvironment workflowEnvironment;

	/**
	 * Setup the Mock environment
	 */
	@BeforeEach
	public void setUp() throws PluginException, ModelException {
		workflowEnvironment = new WorkflowMockEnvironment();
		workflowEnvironment.setUp();
		workflowEnvironment.loadBPMNModel("/bpmn/TestWorkflowService.bpmn");
	}

	/**
	 * This test simulates a workflowService process call.
	 */
	@Test
	public void testProcessSimple() {
		try {
			// load a test workitem
			ItemCollection workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
			workitem.model("1.0.0").task(100).event(10);
			workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
			// expected new task is 200
			assertEquals(200, workitem.getTaskID());
		} catch (AccessDeniedException | ProcessingErrorException | PluginException | ModelException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Test a complex workflow process with conditional events
	 */
	@Test
	public void testConditionalEvent1()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		workflowEnvironment.loadBPMNModel("/bpmn/conditional_event1.bpmn");
		// load test workitem
		ItemCollection workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		workitem.replaceItemValue("_budget", 99);
		workitem.model("1.0.0").task(1000).event(10);
		// test _budget<100 => 1200
		workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
		Assert.assertEquals(1200, workitem.getTaskID());
		// test _budget>100 => 1100
		workitem.replaceItemValue("_budget", 9999);
		workitem.model("1.0.0").task(1000).event(10);
		workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
		Assert.assertEquals(1100, workitem.getTaskID());
	}
}

```


### How to setup a test case
To setup a test case the Imixs `WorkflowMockEnvironment`   provides a setup method to initialize the environment and a loadBPMNModel method to load a test model. To setup the environment it is recommended to call the setup() method in a `org.junit.jupiter.api.BeforeEach` annotated init method:

```java
	@BeforeEach
	public void setUp() throws PluginException, ModelException {
		workflowEnvironment = new WorkflowMockEnvironment();
		workflowEnvironment.setUp();
		workflowEnvironment.loadBPMNModel("/bpmn/TestWorkflowService.bpmn");
	}
```
  
 
### How to test business cases
The main goal of the  `WorkflowMockEnvironment` is to test business logic of a specific workflow model. A workflow instance can be created form a empty ItemCollection and tested with any kind of data.

```java
	// Load a test model
	workflowEnvironment.loadBPMNModel("/bpmn/myModel.bpmn");
	// Create a workflow instance with some business data
	ItemCollection workitem = new ItemCollection();
	workitem.replaceItemValue("_budget", 99);
	// process the workflow instance....
	workitem.model("1.0.0").task(1000).event(10);
	workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
	// evaluate the results....
	Assert.assertEquals(1200, workitem.getTaskID());
```

 
  
### Mock Imixs Adapter Classes

The  `WorkflowMockEnvironment` also allows you to test custom Imixs Workflow `GenericAdapter` or `SignalAdapter` classes. You just need to call the method `registerAdapter(...)` method before you call `setup()`.  See the following example:

```java

public class MyAdapterTest {

	@InjectMocks
	protected MyImixsAdapter myAdapter;

	protected WorkflowMockEnvironment workflowEnvironment;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {
		// Ensures that @Mock and @InjectMocks annotations are processed
		MockitoAnnotations.openMocks(this);
		workflowEnvironment = new WorkflowMockEnvironment();

		// register AccessAdapter Mock
		workflowEnvironment.registerAdapter(accessAdapter);

		// Setup Environment
		workflowEnvironment.setUp();
	}
	.....
}

```