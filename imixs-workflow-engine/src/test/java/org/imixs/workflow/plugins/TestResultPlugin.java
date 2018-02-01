package org.imixs.workflow.plugins;

import java.util.List;
import java.util.logging.Logger;

import javax.script.ScriptException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.ResultPlugin;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.util.XMLParser;
import org.junit.Before;
import org.junit.Test;

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
public class TestResultPlugin {
	ResultPlugin resultPlugin = null;
	public static final String DEFAULT_MODEL_VERSION = "1.0.0";
	private static Logger logger = Logger.getLogger(TestResultPlugin.class.getName());

	WorkflowMockEnvironment workflowMockEnvironment;

	@Before
	public void setup() throws PluginException, ModelException {

		workflowMockEnvironment = new WorkflowMockEnvironment();
		workflowMockEnvironment.setModelPath("/bpmn/TestResultPlugin.bpmn");

		workflowMockEnvironment.setup();

		resultPlugin = new ResultPlugin();
		try {
			resultPlugin.init(workflowMockEnvironment.getWorkflowService());
		} catch (PluginException e) {

			e.printStackTrace();
		}

	}

	/**
	 * This test verifies the evaluation of a item tag
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testBasic() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");
		ItemCollection adocumentActivity = new ItemCollection();

		String sResult = "<item name=\"txtName\">Manfred</item>";
		logger.info("txtActivityResult=" + sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);
		// run plugin
		adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertEquals("Manfred", adocumentContext.getItemValueString("txtName"));

		// test with ' instead of "
		sResult = "<item name='txtName'>Manfred</item>";
		logger.info("txtActivityResult=" + sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);
		// run plugin
		adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertEquals("Manfred", adocumentContext.getItemValueString("txtName"));
	}

	@Test
	public void testBasicWithTypeBoolean() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");
		ItemCollection adocumentActivity = new ItemCollection();

		String sResult = "<item name='txtName' type='boolean'>true</item>";

		logger.info("txtActivityResult=" + sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);

		// run plugin
		adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertTrue(adocumentContext.getItemValueBoolean("txtName"));

	}

	@Test
	public void testBasicWithTypeInteger() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		String sResult = "<item name='numValue' type='integer'>47</item>";

		logger.info("txtActivityResult=" + sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);

		// run plugin
		adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertEquals(47, adocumentContext.getItemValueInteger("numValue"));

	}

	/**
	 * This test verifies if the 'type' property can be changed...
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testTypeProperty() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		String sResult = "<item name='type' >workitemdeleted</item>";

		logger.info("txtActivityResult=" + sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);

		// run plugin
		adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertEquals("workitemdeleted", adocumentContext.getItemValueString("Type"));

	}

	/**
	 * This test verifies if a pluginException is thronw if the format was invalid
	 * 
	 * @throws PluginException
	 */
	@SuppressWarnings("unused")
	@Test
	public void testInvalidFormatException() {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		// wrong format
		String sResult = "<item name='txtName' >Anna<item>";

		logger.info("txtActivityResult=" + sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);

		int result;
		try {
			// run plugin
			adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);

			Assert.fail();

		} catch (PluginException e) {
			logger.info(e.getMessage());
		}

		// wrong format missing "
		sResult = "<item name=\"txtName >Anna</itemxxxxx>";

		logger.info("txtActivityResult=" + sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);

		try {
			// run plugin
			adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
			Assert.fail();

		} catch (PluginException e) {
			logger.info(e.getMessage());
		}

	}

	/**
	 * This test verifies if multiple item tags with the same name will be added
	 * into one single property
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testMultiValueEvaluation() throws PluginException {
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
	 * Test the itemParser method or activity
	 ***/
	@Test
	public void testItemXMLContent() {

		// create test result.....
		String activityResult = "<modelversion>1.0.0</modelversion>" + "<processid>1000</processid>"
				+ "<activityid>10</activityid>" + "<items>namTeam</items>";

		try {
			ItemCollection result = XMLParser.parseItemStructure(activityResult);

			Assert.assertEquals("1.0.0", result.getItemValueString("modelversion"));
			Assert.assertEquals("1000", result.getItemValueString("processID"));
			Assert.assertEquals("10", result.getItemValueString("activityID"));
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void testevaluateWorkflowResult() {
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
	public void testevaluateWorkflowResultEmbeddedXMLContent() {
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
	public void testevaluateWorkflowRestultInvalidFormat() {
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
	 * This test simulates a workflowService process call.
	 * 
	 * The test validates the update of the type attribute
	 * 
	 * event 10 - no type defined - empty event 20 - type = "workitem" event 30 -
	 * type = "workitemeleted"
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * @throws ModelException
	 * 
	 */
	@Test
	public void testProcessTypeAttriubteComplex()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {
		ItemCollection workitem = workflowMockEnvironment.getDatabase().get("W0000-00001");
		workitem.removeItem("type");
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, DEFAULT_MODEL_VERSION);
		workitem.replaceItemValue(WorkflowKernel.PROCESSID, 100);

		// case 1 - no type attribute
		workitem.replaceItemValue(WorkflowKernel.ACTIVITYID, 10);
		workitem = workflowMockEnvironment.processWorkItem(workitem);
		Assert.assertEquals(100, workitem.getProcessID());
		Assert.assertEquals("", workitem.getType());

		// case 2 - workitem
		workitem.replaceItemValue(WorkflowKernel.ACTIVITYID, 20);
		workitem = workflowMockEnvironment.processWorkItem(workitem);
		Assert.assertEquals(200, workitem.getProcessID());
		Assert.assertEquals("workitem", workitem.getType());

		// case 3 - workitemdeleted
		workitem.replaceItemValue(WorkflowKernel.ACTIVITYID, 30);
		workitem = workflowMockEnvironment.processWorkItem(workitem);
		Assert.assertEquals(200, workitem.getProcessID());
		Assert.assertEquals("workitemdeleted", workitem.getType());

	}

	/**
	 * This test verifies white space in the result (e.g. newline)
	 * 
	 * @see issue #255
	 * @throws PluginException
	 */
	@Test
	public void testWhiteSpace() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		// test new line...
		String sResult = "  \r\n  some data \r\n <item name='type' >workitemdeleted</item> \r\n ";

		logger.info("txtActivityResult=" + sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);

		// run plugin
		adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertEquals("workitemdeleted", adocumentContext.getItemValueString("Type"));

	}

	
	/**
	 * This test verifies the evaluation of an empty item tag.
	 * Expected result: item will be cleard.
	 * 
	 * issue #339
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testEmptyTag() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");
		ItemCollection adocumentActivity = new ItemCollection();

		// clear value...
		String sResult = "<item name=\"txtName\"></item>";
		logger.info("txtActivityResult=" + sResult);
		
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);
		// run plugin
		adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		// item should be empty
		Assert.assertEquals("", adocumentContext.getItemValueString("txtName"));

		
	}
}
