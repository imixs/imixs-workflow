package org.imixs.workflow.plugins;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.WorkflowEngineMock;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.plugins.ResultPlugin;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    protected ResultPlugin resultPlugin = null;
    public static final String DEFAULT_MODEL_VERSION = "1.0.0";
    private static final Logger logger = Logger.getLogger(TestResultPlugin.class.getName());

    // protected OldWorkflowMockEnvironment workflowMockEnvironment;

    // @Before
    // public void setUp() throws PluginException, ModelException {

    // workflowMockEnvironment = new OldWorkflowMockEnvironment();
    // workflowMockEnvironment.setModelPath("/bpmn/TestResultPlugin.bpmn");

    // workflowMockEnvironment.setup();

    // resultPlugin = new ResultPlugin();
    // try {
    // resultPlugin.init(workflowMockEnvironment.getWorkflowService());
    // } catch (PluginException e) {

    // e.printStackTrace();
    // }

    // }

    ItemCollection event;
    ItemCollection workitem;
    protected WorkflowEngineMock workflowEngine;

    @BeforeEach
    public void setUp() throws PluginException, ModelException {

        workflowEngine = new WorkflowEngineMock();
        workflowEngine.setUp();
        workflowEngine.loadBPMNModel("/bpmn/TestResultPlugin.bpmn");

        resultPlugin = new ResultPlugin();
        try {
            resultPlugin.init(workflowEngine.getWorkflowService());
        } catch (PluginException e) {

            e.printStackTrace();
        }
        workitem = workflowEngine.getDocumentService().load("W0000-00001");
        workitem.model("1.0.0").task(100);

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
        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        adocumentActivity.replaceItemValue("txtActivityResult", sResult);
        // run plugin
        adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
        Assert.assertNotNull(adocumentContext);

        Assert.assertEquals("Manfred", adocumentContext.getItemValueString("txtName"));

        // test with ' instead of "
        sResult = "<item name='txtName'>Manfred</item>";
        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
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

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
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

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        adocumentActivity.replaceItemValue("txtActivityResult", sResult);

        // run plugin
        adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
        Assert.assertNotNull(adocumentContext);

        Assert.assertEquals(47, adocumentContext.getItemValueInteger("numValue"));

    }

    @Test
    public void testBasicWithTypeDate() throws PluginException {

        ItemCollection adocumentContext = new ItemCollection();
        ItemCollection adocumentActivity = new ItemCollection();

        String sResult = "<item name='datValue' type='date' format='yyyy-MM-dd'>2017-12-31</item>";

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        adocumentActivity.replaceItemValue("txtActivityResult", sResult);

        // run plugin
        adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
        Assert.assertNotNull(adocumentContext);

        Date dateTest = adocumentContext.getItemValueDate("datvalue");
        Assert.assertNotNull(dateTest);

        Calendar cal = Calendar.getInstance();
        cal.setTime(dateTest);

        Assert.assertEquals(2017, cal.get(Calendar.YEAR));
        Assert.assertEquals(11, cal.get(Calendar.MONTH));
        Assert.assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));

        System.out.println(dateTest + "");
    }

    @Test
    public void testBasicWithTypeDateWithEmptyValue() throws PluginException {

        ItemCollection adocumentContext = new ItemCollection();
        ItemCollection adocumentActivity = new ItemCollection();

        String sResult = "<item name='datValue' type='date' format='yyyy-MM-dd'></item>";

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        adocumentActivity.replaceItemValue("txtActivityResult", sResult);

        // run plugin
        adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
        Assert.assertNotNull(adocumentContext);

        Date dateTest = adocumentContext.getItemValueDate("datvalue");
        Assert.assertNull(dateTest);

    }

    @Test
    public void testBasicWithTypeDateWithExistingDateValue() throws PluginException {

        ItemCollection adocumentContext = new ItemCollection();
        ItemCollection adocumentActivity = new ItemCollection();

        Date datTest = new Date();
        adocumentContext.replaceItemValue("$lastEventDate", datTest);

        String sResult = "<item name='datValue' type='date'><itemvalue>$lastEventDate</itemvalue></item>";

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        adocumentActivity.replaceItemValue("txtActivityResult", sResult);
        // run plugin
        adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
        Assert.assertNotNull(adocumentContext);

        Date datResult = adocumentContext.getItemValueDate("datvalue");
        Assert.assertNotNull(datResult);

        Calendar calResult = Calendar.getInstance();
        calResult.setTime(datResult);

        Calendar calTest = Calendar.getInstance();
        calResult.setTime(datTest);

        Assert.assertEquals(calTest.get(Calendar.YEAR), calResult.get(Calendar.YEAR));
        Assert.assertEquals(calTest.get(Calendar.MONTH), calResult.get(Calendar.MONTH));
        Assert.assertEquals(calTest.get(Calendar.DAY_OF_MONTH), calResult.get(Calendar.DAY_OF_MONTH));

    }

    /**
     * This test verifies if the 'type' property can be changed...
     * 
     * @throws PluginException
     */
    @Test
    public void testTypeProperty() throws PluginException {

        ItemCollection adocumentContext = new ItemCollection();
        adocumentContext.setType("workitem");
        ItemCollection adocumentActivity = new ItemCollection();

        String sResult = "<item name='type' >workitemdeleted</item>";

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        adocumentActivity.replaceItemValue("txtActivityResult", sResult);

        // run plugin
        try {
            adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
            Assert.assertEquals("workitemdeleted", adocumentContext.getType());
        } catch (PluginException e) {
            Assert.fail();
        }
        Assert.assertNotNull(adocumentContext);

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

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
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

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
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
     * This test simulates a workflowService process call.
     * <p>
     * The test validates the update of the type attribute
     * <p>
     * event 10 - no type defined - empty event 20 - type = "workitem" event 30 -
     * type = "workitemeleted"
     * 
     * @throws ProcessingErrorException
     * @throws AccessDeniedException
     * @throws ModelException
     */
    @Test
    public void testProcessTypeAttriubteComplex()
            throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {
        ItemCollection workitem = workflowEngine.getDocumentService().load("W0000-00001");
        workitem.removeItem("type");
        workitem.replaceItemValue(WorkflowKernel.MODELVERSION, DEFAULT_MODEL_VERSION);
        workitem.setTaskID(100);

        // case 1 - no type attribute
        workitem.setEventID(10);
        workitem = workflowEngine.getWorkflowService().processWorkItem(workitem);
        Assert.assertEquals(100, workitem.getTaskID());
        Assert.assertEquals(WorkflowService.DEFAULT_TYPE, workitem.getType());

        // case 2 - workitem
        workitem.setEventID(20);
        workitem = workflowEngine.getWorkflowService().processWorkItem(workitem);
        Assert.assertEquals(200, workitem.getTaskID());
        Assert.assertEquals("workitemdeleted", workitem.getType());

        // case 3 - workitemdeleted
        workitem.setEventID(30);
        workitem = workflowEngine.getWorkflowService().processWorkItem(workitem);
        Assert.assertEquals(200, workitem.getTaskID());
        Assert.assertEquals("workitemdeleted", workitem.getType());
        Assert.assertEquals("deleted", workitem.getItemValueString("subtype"));

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
        String sResult = "  \r\n  some data \r\n <item name='subtype' >workitemdeleted</item> \r\n ";

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        adocumentActivity.replaceItemValue("txtActivityResult", sResult);

        // run plugin
        adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
        Assert.assertNotNull(adocumentContext);

        Assert.assertEquals("workitemdeleted", adocumentContext.getItemValueString("subType"));

    }

    /**
     * This test verifies the evaluation of an empty item tag. Expected result: item
     * will be cleard.
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
        logger.log(Level.INFO, "txtActivityResult={0}", sResult);

        adocumentActivity.replaceItemValue("txtActivityResult", sResult);
        // run plugin
        adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
        Assert.assertNotNull(adocumentContext);

        // item should be empty
        Assert.assertEquals("", adocumentContext.getItemValueString("txtName"));

    }

    /**
     * This test verifies the evaluation of an item tag in case other unspecified
     * tags exits.
     * <p>
     * {@code<jsf-immediate>true</jsf-immediate>}
     * 
     * @see imixs-faces - workflow actions
     * @throws PluginException
     */
    @Test
    public void testImediateTag() throws PluginException {

        ItemCollection adocumentContext = new ItemCollection();
        adocumentContext.replaceItemValue("txtName", "Anna");
        ItemCollection adocumentActivity = new ItemCollection();

        // clear value...
        String sResult = "<item name=\"txtName\">some data</item>";
        sResult = sResult + "<immediate>true</immediate>";
        sResult = sResult + "<item name=\"txtName2\">some other data</item>";
        logger.log(Level.INFO, "txtActivityResult={0}", sResult);

        adocumentActivity.replaceItemValue("txtActivityResult", sResult);
        // run plugin
        adocumentContext = resultPlugin.run(adocumentContext, adocumentActivity);
        Assert.assertNotNull(adocumentContext);

        // item should be empty
        Assert.assertEquals("some data", adocumentContext.getItemValueString("txtName"));

    }

    /**
     * This test copies an item from the source workitem.
     * <p>
     * The test copies the value of 'amount' into the new item 'count'
     * 
     * @throws PluginException
     */
    @Test
    public void testCopyItemFromSource() throws PluginException {

        ItemCollection workitem = new ItemCollection();
        workitem.replaceItemValue("name", "Anna");
        workitem.replaceItemValue("amount", 55.332);
        ItemCollection event = new ItemCollection();

        event.replaceItemValue("txtActivityResult",
                "<item name=\"count\" type=\"double\"><itemvalue>amount</itemvalue></item>");
        // run plugin
        workitem = resultPlugin.run(workitem, event);

        workitem = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(event, "item", workitem);
        Assert.assertNotNull(workitem);
        Assert.assertTrue(workitem.hasItem("count"));
        Assert.assertEquals(55.332, workitem.getItemValueDouble("count"), 0);

    }
}
