package org.imixs.workflow.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.plugins.ResultPlugin;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
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

    ItemCollection event;
    ItemCollection workitem;
    protected WorkflowMockEnvironment workflowEnvironment;

    @BeforeEach
    public void setUp() throws PluginException, ModelException {

        workflowEnvironment = new WorkflowMockEnvironment();
        workflowEnvironment.setUp();
        workflowEnvironment.loadBPMNModel("/bpmn/TestResultPlugin.bpmn");

        resultPlugin = new ResultPlugin();
        try {
            resultPlugin.init(workflowEnvironment.getWorkflowService());
        } catch (PluginException e) {

            e.printStackTrace();
        }
        workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
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

        workitem = new ItemCollection();
        workitem.replaceItemValue("txtName", "Anna");
        ItemCollection event = new ItemCollection();

        String sResult = "<item name=\"txtName\">Manfred</item>";
        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        event.replaceItemValue("txtActivityResult", sResult);
        // run plugin
        workitem = resultPlugin.run(workitem, event);
        assertNotNull(workitem);

        assertEquals("Manfred", workitem.getItemValueString("txtName"));

        // test with ' instead of "
        sResult = "<item name='txtName'>Manfred</item>";
        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        event.replaceItemValue("txtActivityResult", sResult);
        // run plugin
        workitem = resultPlugin.run(workitem, event);
        assertNotNull(workitem);

        assertEquals("Manfred", workitem.getItemValueString("txtName"));
    }

    @Test
    public void testBasicWithTypeBoolean() throws PluginException {

        ItemCollection workitem = new ItemCollection();
        workitem.replaceItemValue("txtName", "Anna");
        ItemCollection event = new ItemCollection();

        String sResult = "<item name='txtName' type='boolean'>true</item>";

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        event.replaceItemValue("txtActivityResult", sResult);

        // run plugin
        workitem = resultPlugin.run(workitem, event);
        assertNotNull(workitem);

        assertTrue(workitem.getItemValueBoolean("txtName"));

    }

    @Test
    public void testBasicWithTypeInteger() throws PluginException {

        workitem = new ItemCollection();
        event = new ItemCollection();

        String sResult = "<item name='numValue' type='integer'>47</item>";

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        event.replaceItemValue("txtActivityResult", sResult);

        // run plugin
        workitem = resultPlugin.run(workitem, event);
        assertNotNull(workitem);

        assertEquals(47, workitem.getItemValueInteger("numValue"));

    }

    @Test
    public void testBasicWithTypeDate() throws PluginException {

        workitem = new ItemCollection();
        event = new ItemCollection();

        String sResult = "<item name='datValue' type='date' format='yyyy-MM-dd'>2017-12-31</item>";

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        event.replaceItemValue("txtActivityResult", sResult);

        // run plugin
        workitem = resultPlugin.run(workitem, event);
        assertNotNull(workitem);

        Date dateTest = workitem.getItemValueDate("datvalue");
        assertNotNull(dateTest);

        Calendar cal = Calendar.getInstance();
        cal.setTime(dateTest);

        assertEquals(2017, cal.get(Calendar.YEAR));
        assertEquals(11, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));

        System.out.println(dateTest + "");
    }

    @Test
    public void testBasicWithTypeDateWithEmptyValue() throws PluginException {

        workitem = new ItemCollection();
        event = new ItemCollection();

        String sResult = "<item name='datValue' type='date' format='yyyy-MM-dd'></item>";

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        event.replaceItemValue("txtActivityResult", sResult);

        // run plugin
        workitem = resultPlugin.run(workitem, event);
        assertNotNull(workitem);

        Date dateTest = workitem.getItemValueDate("datvalue");
        assertNull(dateTest);

    }

    @Test
    public void testBasicWithTypeDateWithExistingDateValue() throws PluginException {

        workitem = new ItemCollection();
        event = new ItemCollection();

        Date datTest = new Date();
        workitem.replaceItemValue("$lastEventDate", datTest);

        String sResult = "<item name='datValue' type='date'><itemvalue>$lastEventDate</itemvalue></item>";

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        event.replaceItemValue("txtActivityResult", sResult);
        // run plugin
        workitem = resultPlugin.run(workitem, event);
        assertNotNull(workitem);

        Date datResult = workitem.getItemValueDate("datvalue");
        assertNotNull(datResult);

        Calendar calResult = Calendar.getInstance();
        calResult.setTime(datResult);

        Calendar calTest = Calendar.getInstance();
        calResult.setTime(datTest);

        assertEquals(calTest.get(Calendar.YEAR), calResult.get(Calendar.YEAR));
        assertEquals(calTest.get(Calendar.MONTH), calResult.get(Calendar.MONTH));
        assertEquals(calTest.get(Calendar.DAY_OF_MONTH), calResult.get(Calendar.DAY_OF_MONTH));

    }

    /**
     * This test verifies if the 'type' property can be changed...
     * 
     * @throws PluginException
     */
    @Test
    public void testTypeProperty() throws PluginException {

        workitem = new ItemCollection();
        workitem.setType("workitem");
        event = new ItemCollection();

        String sResult = "<item name='type' >workitemdeleted</item>";

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        event.replaceItemValue("txtActivityResult", sResult);

        // run plugin
        try {
            workitem = resultPlugin.run(workitem, event);
            assertEquals("workitemdeleted", workitem.getType());
        } catch (PluginException e) {
            fail();
        }
        assertNotNull(workitem);

    }

    /**
     * This test verifies if a pluginException is thronw if the format was invalid
     * 
     * @throws PluginException
     */
    @SuppressWarnings("unused")
    @Test
    public void testInvalidFormatException() {

        workitem = new ItemCollection();
        event = new ItemCollection();

        // wrong format
        String sResult = "<item name='txtName' >Anna<item>";

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        event.replaceItemValue("txtActivityResult", sResult);

        int result;
        try {
            // run plugin
            workitem = resultPlugin.run(workitem, event);

            fail();

        } catch (PluginException e) {
            logger.info(e.getMessage());
        }

        // wrong format missing "
        sResult = "<item name=\"txtName >Anna</itemxxxxx>";

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        event.replaceItemValue("txtActivityResult", sResult);

        try {
            // run plugin
            workitem = resultPlugin.run(workitem, event);
            fail();

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
        workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
        workitem.removeItem("type");
        workitem.replaceItemValue(WorkflowKernel.MODELVERSION, DEFAULT_MODEL_VERSION);
        workitem.setTaskID(100);

        // case 1 - no type attribute
        workitem.setEventID(10);
        workitem = workflowEnvironment.getWorkflowService().processWorkItem(workitem);
        assertEquals(100, workitem.getTaskID());
        assertEquals(WorkflowService.DEFAULT_TYPE, workitem.getType());

        // case 2 - workitem
        workitem.setEventID(20);
        workitem = workflowEnvironment.getWorkflowService().processWorkItem(workitem);
        assertEquals(200, workitem.getTaskID());
        assertEquals("workitemdeleted", workitem.getType());

        // case 3 - workitemdeleted
        workitem.setEventID(30);
        workitem = workflowEnvironment.getWorkflowService().processWorkItem(workitem);
        assertEquals(200, workitem.getTaskID());
        assertEquals("workitemdeleted", workitem.getType());
        assertEquals("deleted", workitem.getItemValueString("subtype"));

    }

    /**
     * This test verifies white space in the result (e.g. newline)
     * 
     * @see issue #255
     * @throws PluginException
     */
    @Test
    public void testWhiteSpace() throws PluginException {

        workitem = new ItemCollection();
        event = new ItemCollection();

        // test new line...
        String sResult = "  \r\n  some data \r\n <item name='subtype' >workitemdeleted</item> \r\n ";

        logger.log(Level.INFO, "txtActivityResult={0}", sResult);
        event.replaceItemValue("txtActivityResult", sResult);

        // run plugin
        workitem = resultPlugin.run(workitem, event);
        assertNotNull(workitem);

        assertEquals("workitemdeleted", workitem.getItemValueString("subType"));

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

        workitem = new ItemCollection();
        workitem.replaceItemValue("txtName", "Anna");
        event = new ItemCollection();

        // clear value...
        String sResult = "<item name=\"txtName\"></item>";
        logger.log(Level.INFO, "txtActivityResult={0}", sResult);

        event.replaceItemValue("txtActivityResult", sResult);
        // run plugin
        workitem = resultPlugin.run(workitem, event);
        assertNotNull(workitem);

        // item should be empty
        assertEquals("", workitem.getItemValueString("txtName"));

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

        workitem = new ItemCollection();
        workitem.replaceItemValue("txtName", "Anna");
        event = new ItemCollection();

        // clear value...
        String sResult = "<item name=\"txtName\">some data</item>";
        sResult = sResult + "<immediate>true</immediate>";
        sResult = sResult + "<item name=\"txtName2\">some other data</item>";
        logger.log(Level.INFO, "txtActivityResult={0}", sResult);

        event.replaceItemValue("txtActivityResult", sResult);
        // run plugin
        workitem = resultPlugin.run(workitem, event);
        assertNotNull(workitem);

        // item should be empty
        assertEquals("some data", workitem.getItemValueString("txtName"));

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

        workitem = new ItemCollection();
        workitem.replaceItemValue("name", "Anna");
        workitem.replaceItemValue("amount", 55.332);
        event = new ItemCollection();

        event.replaceItemValue("txtActivityResult",
                "<item name=\"count\" type=\"double\"><itemvalue>amount</itemvalue></item>");
        // run plugin
        workitem = resultPlugin.run(workitem, event);

        workitem = workflowEnvironment.getWorkflowService().evalWorkflowResult(event, "item", workitem);
        assertNotNull(workitem);
        assertTrue(workitem.hasItem("count"));
        assertEquals(55.332, workitem.getItemValueDouble("count"), 0);

    }
}
