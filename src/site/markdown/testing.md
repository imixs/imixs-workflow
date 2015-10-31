#The Imixs-Workflow Test Suite
Imixs Workflow provides a Test Suite to test and simulate various business cases. The Imixs WorkflowTestSuite is framework for building test classes based on the  test framework JUnit. To test or simulate a specific business scenario  different test users can be registered and used to perform workflow steps and testing the state of different workitems. 
 

##How to setup a test case
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
 
##How to test business cases
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
	 	ItemCollection registration=new ItemCollection();
		registration.replaceItemValue("type", "profile");
		registration.replaceItemValue("$ModelVersion", "system-de-0.0.1");
		registration.replaceItemValue("$processid", 200);
		registration.replaceItemValue("$activityid", 10);
		registration.replaceItemValue("txtName","Test");

		registration=testSuite.processWorkitem(registration, "anna");

		Assert.assertNotNull(registration);
		String uid= registration.getItemValueString("$UniqueID");
		WorkflowTestSuite.log(Level.INFO,"UID=" +uid);
		Assert.assertFalse(uid.isEmpty());
	}
 
  
  