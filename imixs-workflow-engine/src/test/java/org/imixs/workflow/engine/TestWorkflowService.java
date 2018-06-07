package org.imixs.workflow.engine;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import junit.framework.Assert;

/**
 * Test class for WorkflowService
 * 
 * This test verifies specific method implementations of the workflowService by
 * mocking the WorkflowService with the @spy annotation.
 * 
 * 
 * @author rsoika
 */
public class TestWorkflowService {
	WorkflowMockEnvironment workflowMockEnvironment;

	@Before
	public void setup() throws PluginException, ModelException {

		workflowMockEnvironment = new WorkflowMockEnvironment();
		workflowMockEnvironment.setup();

		workflowMockEnvironment.loadModel("/bpmn/TestWorkflowService.bpmn");

	}

	/**
	 * This test simulates a workflowService process call by mocking the entity and
	 * model service.
	 * 
	 * This is just a simple simulation...
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * @throws ModelException
	 * 
	 */
	@Test
	public void testProcessSimple()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {
		// load test workitem
		ItemCollection workitem = workflowMockEnvironment.database.get("W0000-00001");
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, WorkflowMockEnvironment.DEFAULT_MODEL_VERSION);
		workitem.setTaskID(100);

		workitem = workflowMockEnvironment.workflowService.processWorkItem(workitem);

		Assert.assertEquals("1.0.0", workitem.getItemValueString("$ModelVersion"));

	}

	/**
	 * test if the method getEvents returns correct lists of public events.
	 */
	@Test
	public void testGetEventsSimple() {

		// get workitem
		ItemCollection workitem = workflowMockEnvironment.database.get("W0000-00001");
		workitem.setTaskID(200);

		List<ItemCollection> eventList = null;
		try {
			eventList = workflowMockEnvironment.workflowService.getEvents(workitem);
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		// only one event is public!
		Assert.assertEquals(1, eventList.size());
	}

	/**
	 * test if the method getEvents returns correct lists of public and restricted
	 * events. User "manfred" is listed in current workitem namTeam.
	 */
	@Test
	public void testGetEventsComplex() {

		// get workitem
		ItemCollection workitem = workflowMockEnvironment.database.get("W0000-00001");
		workitem.setTaskID(100);

		Vector<String> members = new Vector<String>();
		members.add("jo");
		members.add("");
		members.add("manfred");
		members.add("alex");
		workitem.replaceItemValue("nammteam", "tom");
		workitem.replaceItemValue("nammanager", members);
		workitem.replaceItemValue("namassist", "");

		List<ItemCollection> eventList = null;
		try {
			eventList = workflowMockEnvironment.workflowService.getEvents(workitem);
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertEquals(3, eventList.size());
	}

	/**
	 * test if the method getEvents returns correct lists of workflow events, with a
	 * more complex setup. User 'manfred' is not listed in namManger!
	 */
	@Test
	public void testGetEventsComplexRestricted() {

		// get workitem
		ItemCollection workitem = workflowMockEnvironment.database.get("W0000-00001");
		workitem.setTaskID(100);

		Vector<String> members = new Vector<String>();
		members.add("jo");
		members.add("alex");
		workitem.replaceItemValue("nammteam", "tom");
		workitem.replaceItemValue("nammanager", members);
		workitem.replaceItemValue("namassist", "");

		List<ItemCollection> eventList = null;
		try {
			eventList = workflowMockEnvironment.workflowService.getEvents(workitem);
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertEquals(2, eventList.size());
	}

	/**
	 * This test verifies if the method getEvents returns only events where the
	 * current user has read access! In this case, the user "manfred" is not granted
	 * to the event 300.20 which is restricted to rhe access role
	 * 'org.imixs.ACCESSLEVEL.MANAGERACCESS'
	 * 
	 * So we expect only one event!
	 */
	@Test
	public void testGetEventsReadRestrictedForSimpleUser() {

		when(workflowMockEnvironment.workflowService.getUserNameList()).thenAnswer(new Answer<List<String>>() {
			@Override
			public List<String> answer(InvocationOnMock invocation) throws Throwable {
				List<String> result = new ArrayList<>();
				result.add("manfred");
				return result;
			}
		});

		// get workitem
		ItemCollection workitem = workflowMockEnvironment.database.get("W0000-00001");
		workitem.setTaskID(300);

		Vector<String> members = new Vector<String>();
		members.add("jo");
		members.add("alex");
		workitem.replaceItemValue("nammteam", "tom");
		workitem.replaceItemValue("nammanager", members);
		workitem.replaceItemValue("namassist", "");

		List<ItemCollection> eventList = null;
		try {
			eventList = workflowMockEnvironment.workflowService.getEvents(workitem);
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertEquals(1, eventList.size());
	}

	/**
	 * This test verifies if the method getEvents returns only events where the
	 * current user has read access! In this case, the user "manfred" is in the role
	 * 'org.imixs.ACCESSLEVEL.MANAGERACCESS' and granted to the event 300.20
	 * 
	 * So we expect both events!
	 */
	@Test
	public void testGetEventsReadRestrictedForManagerAccess() {

		when(workflowMockEnvironment.workflowService.getUserNameList()).thenAnswer(new Answer<List<String>>() {
			@Override
			public List<String> answer(InvocationOnMock invocation) throws Throwable {
				List<String> result = new ArrayList<>();
				result.add("manfred");
				result.add("org.imixs.ACCESSLEVEL.MANAGERACCESS");
				return result;
			}
		});

		// get workitem
		ItemCollection workitem = workflowMockEnvironment.database.get("W0000-00001");
		workitem.setTaskID(300);

		Vector<String> members = new Vector<String>();
		members.add("jo");
		members.add("alex");
		workitem.replaceItemValue("nammteam", "tom");
		workitem.replaceItemValue("nammanager", members);
		workitem.replaceItemValue("namassist", "");

		List<ItemCollection> eventList = null;
		try {
			eventList = workflowMockEnvironment.workflowService.getEvents(workitem);
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertEquals(2, eventList.size());
	}

	/**
	 * This test evaluates a event result
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testEvaluateWorkflowResult() {
		ItemCollection activityEntity = new ItemCollection();

		try {
			activityEntity.replaceItemValue("txtActivityResult",
					"<item ignore=\"true\" name=\"comment\" >some data</item>");
			ItemCollection result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
					new ItemCollection());
			Assert.assertNotNull(result);
			Assert.assertTrue(result.hasItem("comment"));
			Assert.assertEquals("some data", result.getItemValueString("comment"));
			Assert.assertEquals("true", result.getItemValueString("comment.ignore"));
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		// test an empty item tag
		try {
			activityEntity.replaceItemValue("txtActivityResult", "<item ignore=\"true\" name=\"comment\" />");
			ItemCollection result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
					new ItemCollection());
			Assert.assertNotNull(result);
			Assert.assertTrue(result.hasItem("comment"));
			Assert.assertEquals("", result.getItemValueString("comment"));
			Assert.assertEquals("true", result.getItemValueString("comment.ignore"));
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/**
	 * This test verifies if multiple item tags with the same name will be evaluated
	 * and added into one single property
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testEvaluateWorkflowResultMultiValue() throws PluginException {
		String sResult = "<item name=\"txtName\">Manfred</item>";
		sResult += "\n<item name=\"txtName\">Anna</item>";
		sResult += "\n<item name=\"test\">XXX</item>";
		sResult += "\n<item name=\"txtname\">Sam</item>";

		ItemCollection activityEntity = new ItemCollection();
		activityEntity.replaceItemValue("txtActivityResult", sResult);

		// expeced txtname= Manfred,Anna,Sam
		ItemCollection evalItemCollection = new ItemCollection();
		evalItemCollection = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
				new ItemCollection());

		Assert.assertTrue(evalItemCollection.hasItem("txtName"));

		List<?> result = evalItemCollection.getItemValue("txtname");

		Assert.assertEquals(3, result.size());

		Assert.assertTrue(result.contains("Manfred"));
		Assert.assertTrue(result.contains("Sam"));
		Assert.assertTrue(result.contains("Anna"));

		// test test item
		Assert.assertEquals("XXX", evalItemCollection.getItemValueString("test"));
	}

	/**
	 * This test evaluates an embedded xml content with newline chars used by the
	 * split plugin
	 * 
	 * <code>
	 * <item name="subprocess_create">
		    <modelversion>controlling-analyse-de-1.0.0</modelversion>
		    <processid>1000</processid>
		    <activityid>100</activityid> 
		    <items>_subject,_sender,_receipients,$file</items>
		</item>
	 * </code>
	 * 
	 * The test also test string variants with different newlines!
	 */
	@Test
	public void testEvaluateWorkflowResultEmbeddedXML() {
		ItemCollection activityEntity = new ItemCollection();
		try {

			// 1) create test result single line mode.....
			String activityResult = "<item name=\"subprocess_create\">"
					+ "    <modelversion>analyse-1.0.0</modelversion>" + "	    <processid>1000</processid>"
					+ "	    <activityid>100</activityid>" + "	    <items>_subject,_sender,_receipients,$file</items>"
					+ "	</item>";

			activityEntity.replaceItemValue("txtActivityResult", activityResult);
			ItemCollection result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
					new ItemCollection());
			Assert.assertNotNull(result);
			Assert.assertTrue(result.hasItem("subprocess_create"));
			String xmlContent = result.getItemValueString("subprocess_create");
			Assert.assertFalse(xmlContent.isEmpty());
			Assert.assertTrue(xmlContent.contains("<modelversion>analyse-1.0.0</modelversion>"));

			// 2) create test result unix mode.....
			activityResult = "<item name=\"subprocess_create\">\n" + "    <modelversion>analyse-1.0.0</modelversion>\n"
					+ "	    <processid>1000</processid>\n" + "	    <activityid>100</activityid>\n"
					+ "	    <items>_subject,_sender,_receipients,$file</items>\n" + "	</item>";

			activityEntity.replaceItemValue("txtActivityResult", activityResult);
			result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
					new ItemCollection());
			Assert.assertNotNull(result);
			Assert.assertTrue(result.hasItem("subprocess_create"));
			xmlContent = result.getItemValueString("subprocess_create");
			Assert.assertFalse(xmlContent.isEmpty());
			Assert.assertTrue(xmlContent.contains("<modelversion>analyse-1.0.0</modelversion>"));

			// 3) create test result windows mode.....
			activityResult = "<item name=\"subprocess_create\">\r\n"
					+ "    <modelversion>analyse-1.0.0</modelversion>\r\n" + "	    <processid>1000</processid>\r\n"
					+ "	    <activityid>100</activityid>\r\n"
					+ "	    <items>_subject,_sender,_receipients,$file</items>\r\n" + "	</item>";

			activityEntity.replaceItemValue("txtActivityResult", activityResult);
			result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
					new ItemCollection());
			Assert.assertNotNull(result);
			Assert.assertTrue(result.hasItem("subprocess_create"));
			xmlContent = result.getItemValueString("subprocess_create");
			Assert.assertFalse(xmlContent.isEmpty());
			Assert.assertTrue(xmlContent.contains("<modelversion>analyse-1.0.0</modelversion>"));
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

	}

	/**
	 * testing invalid item tag formats
	 */
	@Test
	public void testEvaluateWorkflowResultInvalidFormat() {
		ItemCollection activityEntity = new ItemCollection();

		try {
			// test no name attribute
			activityEntity.replaceItemValue("txtActivityResult",
					"<item ignore=\"true\" noname=\"comment\" >some data</item>");
			workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity, new ItemCollection());
			Assert.fail();
		} catch (PluginException e) {
			// ok
		}

		try {
			// test wrong closing tag
			activityEntity.replaceItemValue("txtActivityResult",
					"<item ignore=\"true\" name=\"comment\" >some data</xitem>");
			workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity, new ItemCollection());
			Assert.fail();
		} catch (PluginException e) {
			// exception expected
		}

	}

	/**
	 * testing empty content, and empty lines (issue #372)
	 */
	@Test
	public void testEvaluateWorkflowResultEmptyString() {
		ItemCollection activityEntity = new ItemCollection();
		ItemCollection result = null;
		try {
			// test no content
			activityEntity.replaceItemValue("txtActivityResult", "");
			result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
					new ItemCollection());
			Assert.assertNull(result);

			// test whitespace
			activityEntity.replaceItemValue("txtActivityResult", " ");
			result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
					new ItemCollection());
			Assert.assertNull(result);

			// test empty lines
			activityEntity.replaceItemValue("txtActivityResult", " \n ");
			result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
					new ItemCollection());
			Assert.assertNull(result);

			// test empty lines with valid content between
			String s = "\n";
			s += "<item ignore=\"true\" name=\"comment\" >some data</item>";
			s += "\n ";

			activityEntity.replaceItemValue("txtActivityResult", s);
			result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
					new ItemCollection());
			Assert.assertNotNull(result);
			Assert.assertTrue(result.hasItem("comment"));
			Assert.assertEquals("some data", result.getItemValueString("comment"));
			Assert.assertEquals("true", result.getItemValueString("comment.ignore"));

			// test valid content over multiple lines
			s = "\n";
			s += "<item ignore=\"true\" \n";
			s += "name=\"comment\" >some data</item>";
			s += "\n ";

			activityEntity.replaceItemValue("txtActivityResult", s);
			result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
					new ItemCollection());
			Assert.assertNotNull(result);
			Assert.assertTrue(result.hasItem("comment"));
			Assert.assertEquals("some data", result.getItemValueString("comment"));
			Assert.assertEquals("true", result.getItemValueString("comment.ignore"));

		} catch (PluginException e) {
			// failed
			Assert.fail();
		}

	}
}
