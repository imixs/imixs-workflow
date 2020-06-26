# Imixs-Workflow - Simulation and Testing

The Imixs-Workflow project provides a set of test environments allowing to simulate and to test the processing live cycle of a spcific model or workitem. 

## The Simulation Service

The SimulationService EJB can be used to simulate the processing live cycle of a workitem without storing any data into the database. The EJB can be used during runtime. See the following example: 

	public class MyBusinessService {	
		// inject simulationService
		@EJB 
		SimulationService simulationService;
		
		// simulate the processing life cycle of a given workitem
		public void simulate(ItemCollection workitem) throws PluginException, ModelException {		
			// test without plugins
			workitem=simulationService.processWorkItem(workitem, null);
		}		
		..
	}
	
The processWorkitem method of the SimulationService expects a list of Plugins to be called during the simulation. If no plugin list is provided no plugins will be called during the simulation. This is independent form the configuration of the BPMN model. 

## The WorkflowSimulationEnvironment

The Test class 'WorkflowSimulationEnvironment' can be used in jUnit Tests to simulate the processing life cycle. The environment mocks the workflow context and runs the SimulationService:


	public class TestSimulationService { 
	
		@Test
		public void testConditionalEvent1()
				throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {
	
			WorkflowSimulationEnvironment wse = new WorkflowSimulationEnvironment();
			wse.setup();
			wse.loadModel("/bpmn/conditional_event1.bpmn");
	
			// setup a test workitem
			ItemCollection workitem = new ItemCollection();
			workitem.replaceItemValue(WorkflowKernel.MODELVERSION,"1.0.0");
			workitem.replaceItemValue(WorkflowKernel.PROCESSID, 1000);
			workitem.replaceItemValue(WorkflowKernel.ACTIVITYID, 10);
			// setup test data for simulation
			workitem.replaceItemValue("_budget", 99);
			workitem = wse.simulationService.processWorkItem(workitem, null);
			// test result of simulation....
			Assert.assertEquals(1200, workitem.getProcessID());
		}
	
	}



## WorkflowMockEnvironment

The Test class 'WorkflowMockEnvironment' mocks a full workflow environment including a in-memory-database. The WorkflowMockEnvironment can be used for more complex integration tests. 

	
	public class TestWorkflow {
		WorkflowMockEnvironment workflowMockEnvironment;
	
		@Before
		public void setup() throws PluginException, ModelException {
			workflowMockEnvironment = new WorkflowMockEnvironment();
			workflowMockEnvironment.setup();
			workflowMockEnvironment.loadModel("/bpmn/conditional_event1.bpmn");
		}
	
		@Test
		public void testConditionalEvent1()
				throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {
	
			// load test workitem form database
			ItemCollection workitem = workflowMockEnvironment.database.get("W0000-00001");
			workitem.replaceItemValue(WorkflowKernel.MODELVERSION, "1.0.0");
			workitem.replaceItemValue(WorkflowKernel.PROCESSID, 1000);
			workitem.replaceItemValue(WorkflowKernel.ACTIVITYID, 10);
	
			// test _budget<100
			workitem.replaceItemValue("_budget", 99);
			workitem = workflowMockEnvironment.getWorkflowService().processWorkItem(workitem);
			Assert.assertEquals(1200, workitem.getProcessID());
	
			// load data form database...
			Assert.assertNotNull( workflowMockEnvironment.getDocumentService().load(workitem.getUniqueID())
		}
	}

##The Imixs-Workflow Test-Suite
Imixs Workflow provides a Test Suite to test and simulate various business cases. The Imixs WorkflowTestSuite is framework for building test classes based on the  test framework JUnit. To test or simulate a specific business scenario  different test users can be registered and used to perform workflow steps and testing the state of different workitems. 
 

### How to setup a test case
To setup a test case the Imixs WorkflowTestSuite provides a singleton pattern   which make the integration into a JUnit test very easy.  See the following example how to setup a test class with two different users.
  
	public class SimpleTest {
		WorkflowTestSuite testSuite = null;
	
		@Test
		public void registerUsers() {
			testSuite = WorkflowTestSuite.getInstance();
	
			testSuite.setHost("http://localhost:8080/wokflow-rest/");
			testSuite.joinParty("anna", "anna");
			testSuite.joinParty("Anonymous", null);
			
			Assert.assertNotNull(testSuite.getClient("anna"));
			Assert.assertNull(testSuite.getClient("xxx"));
		}
	}  

This class setup a WorkflowTestSuite for a workflow application on the web address
 http://localhost:8080/wokflow-rest/ . This is the Web Addres to access the Imxis Rest API which is used by the WorkflowTestSuite. The Test case register two users 'anna' and 'Anoymous'. These users can be used for further test cases. The Setup of a WorkflowTestSuite should be typically placed in the Setup method  of a jUnit Class:
 
	@Before
	public void setup() {
		testSuite = WorkflowTestSuite.getInstance();
		testSuite.setHost("http://localhost:8080/minutes-rest/");
		testSuite.joinParty("anna", "anna");
		testSuite.joinParty("Anonymous", null);
	}
 
### How to test business cases
The main goal of the Imixs WorkflowTestSuite is to test business logic of a specific workflow model on a running workflow instance. The next example is a test method which verifies if the worklist for the user anna  contains open workitems

	@Test
	public void worklistTest() throws Exception {
		Assert.assertNotNull(testSuite.getClient("anna"));
		List<ItemCollection> result = testSuite
				.getWorklist("anna");
		Assert.assertTrue(result.size() > 0);
	}
  
To create a new workitem the  WorkflowTestSuite provides method to create and process  a workitem with different business values. 
 
	@Test
	public void createNewWorkitemTest() throws Exception {
	 	ItemCollection registration=new ItemCollection().model("system-de-0.0.1").task(200).event(10);
		registration.replaceItemValue("type", "profile");
		registration.replaceItemValue("txtName","Test");

		registration=testSuite.processWorkitem(registration, "anna");

		Assert.assertNotNull(registration);
		String uid= registration.getItemValueString("$UniqueID");
		WorkflowTestSuite.log(Level.INFO,"UID=" +uid);
		Assert.assertFalse(uid.isEmpty());
	}
 
  
  