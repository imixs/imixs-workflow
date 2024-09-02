package org.imixs.workflow.kernel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockPlugin;
import org.imixs.workflow.MockPluginNull;
import org.imixs.workflow.MockWorkflowEngine;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.exceptions.WorkflowException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test class for Imixs WorkflowKernel using a static default model. The test
 * class verifies basic functionality. See the test class
 * TestWorkflowKernelTestModels for more complex model tests.
 * 
 * @author rsoika
 */
public class TestWorkflowKernelBasic {

    private static final Logger logger = Logger.getLogger(TestWorkflowKernelBasic.class.getName());

    private MockWorkflowEngine workflowEngine;

    @Before
    public void setup() throws PluginException {
        workflowEngine = new MockWorkflowEngine();
        // load default model
        workflowEngine.loadBPMNModel("/bpmn/simple.bpmn");
    }

    /**
     * This test tests the basic behavior of the WorkflowKernel process method.
     */
    @Test
    @Category(org.imixs.workflow.WorkflowKernel.class)
    public void testSimpleProcessingCycle() {

        ItemCollection workitemProcessed = null;
        ItemCollection workItem = new ItemCollection();
        workItem.model("1.0.0")
                .task(1000)
                .event(10);
        workItem.replaceItemValue("txtTitel", "Hello");

        Assert.assertEquals(workItem.getItemValueString("txttitel"), "Hello");

        try {
            workitemProcessed = workflowEngine.getWorkflowKernel().process(workItem);
        } catch (ModelException | ProcessingErrorException | PluginException e) {
            e.printStackTrace();
            Assert.fail();
        }
        Assert.assertEquals(1, workitemProcessed.getItemValueInteger("runs"));
        Assert.assertEquals(1000, workitemProcessed.getTaskID());

        Assert.assertEquals("1.0.0", workitemProcessed.getModelVersion());

        // initial and processed workitems should be the same and should be equals!
        Assert.assertSame(workItem, workitemProcessed);
        Assert.assertTrue(workItem.equals(workitemProcessed));

        // the workitem should not have a $eventid
        Assert.assertEquals(0, workItem.getEventID());
        // a new call of process should throw a ProcessingErrorException
        try {
            workitemProcessed = workflowEngine.getWorkflowKernel().process(workItem);
            Assert.fail(); // we expect an Exception here!
        } catch (ModelException e) {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        } catch (WorkflowException e) {
            Assert.fail();
            e.printStackTrace();
        } catch (ProcessingErrorException e) {
            // expected Exception!
        }

    }

    /**
     * This test tests an invalid $modelversion. In this case a ModelException is
     * expected.
     **/
    @Test
    @Category(org.imixs.workflow.WorkflowKernel.class)
    public void testInvalidModelVersion() {
        ItemCollection workItem = new ItemCollection();
        workItem.model("A.B.C")
                .task(1000)
                .event(10);

        try {
            workItem = workflowEngine.getWorkflowKernel().process(workItem);
            Assert.fail();
        } catch (ModelException e) {
            // Expected Exception
            logger.info(e.getMessage());
        } catch (WorkflowException e) {
            Assert.fail();
            e.printStackTrace();
        } catch (ProcessingErrorException e) {
            Assert.fail();
            e.printStackTrace();
        }

    }

    /**
     * This test tests a $modelversion with a regular expression. The ModelManager
     * should resolve the version 1.0.0.
     **/
    @Test
    @Category(org.imixs.workflow.WorkflowKernel.class)
    public void testModelVersionByRegex() {
        ItemCollection workItem = new ItemCollection();
        workItem.model("(^1.0)|(^2.0)")
                .task(1000)
                .event(10);

        try {
            workItem = workflowEngine.getWorkflowKernel().process(workItem);
            Assert.assertNotNull(workItem);
            // $modelversion should be 1.0.0
            Assert.assertEquals("1.0.0", workItem.getModelVersion());
        } catch (ModelException | ProcessingErrorException | PluginException e) {
            e.printStackTrace();
            Assert.fail();
        }

    }

    /**
     * This test tests a worktiem without $modelversion but with a $workflowgroup.
     **/
    @Test
    @Category(org.imixs.workflow.WorkflowKernel.class)
    public void testByWorkflowGroup() {
        ItemCollection workItem = new ItemCollection();
        workItem.setItemValue(WorkflowKernel.WORKFLOWGROUP, "Simple");
        workItem.task(1000)
                .event(10);

        try {
            workItem = workflowEngine.getWorkflowKernel().process(workItem);
            Assert.assertNotNull(workItem);
            // $modelversion should be 1.0.0
            Assert.assertEquals("1.0.0", workItem.getModelVersion());
        } catch (ModelException | ProcessingErrorException | PluginException e) {
            e.printStackTrace();
            Assert.fail();
        }

    }

    /**
     * This test tests a worktIem without $modelversion and without $workflowgroup.
     * An Exception is expected!
     **/
    @Test
    @Category(org.imixs.workflow.WorkflowKernel.class)
    public void testWithoutModelVersionANDWorkflowGroup() {
        ItemCollection workItem = new ItemCollection();
        // not group no version
        workItem.task(1000)
                .event(10);

        try {
            workItem = workflowEngine.getWorkflowKernel().process(workItem);
            Assert.fail();
        } catch (PluginException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (ModelException e) {
            // expected exception as no model version was defined and also no workflowGroup
            e.getErrorCode().equals(ModelException.UNDEFINED_MODEL_VERSION);
        }

    }

    /**
     * This test verifies if the deprecated fileds "$taskid" and $activityID are
     * still working.
     * 
     * see issue #381
     */
    @Test
    @Category(org.imixs.workflow.WorkflowKernel.class)
    public void testProcessWithDeprecatedField() {
        ItemCollection itemCollectionProcessed = null;
        ItemCollection workItem = new ItemCollection();
        workItem.replaceItemValue("txtTitel", "Hello");
        workItem.model("1.0.0")
                .task(1000)
                .event(20);

        Assert.assertEquals(workItem.getItemValueString("txttitel"), "Hello");

        try {
            itemCollectionProcessed = workflowEngine.getWorkflowKernel().process(workItem);
        } catch (WorkflowException e) {
            Assert.fail();
            e.printStackTrace();
        } catch (ProcessingErrorException e) {
            Assert.fail();
            e.printStackTrace();
        }
        Assert.assertEquals(1, itemCollectionProcessed.getItemValueInteger("runs"));
        Assert.assertEquals(1100, itemCollectionProcessed.getTaskID());

        // initial and processed workitems are the same and equals!
        Assert.assertSame(workItem, itemCollectionProcessed);
        Assert.assertTrue(workItem.equals(itemCollectionProcessed));
    }

    /**
     * This if a plugin which returns null influences the workitem
     */
    @Test
    @Category(org.imixs.workflow.WorkflowKernel.class)
    public void testProcessNullPlugin() {
        ItemCollection workItem = new ItemCollection();
        workItem.replaceItemValue("txtTitel", "Hello");
        workItem.model("1.0.0")
                .task(1000)
                .event(10);

        try {
            // MokWorkflowContext ctx = new MokWorkflowContext();
            // kernel = new WorkflowKernel(workflowContext);

            MockPluginNull mokPlugin = new MockPluginNull();
            workflowEngine.getWorkflowKernel().registerPlugin(mokPlugin);
            workItem.replaceItemValue("txtname", "test");

            workflowEngine.getWorkflowKernel().process(workItem);
            // kernel should throw exception...
            Assert.fail();
        } catch (PluginException e) {
            Assert.assertEquals(WorkflowKernel.PLUGIN_ERROR, e.getErrorCode());
        } catch (ProcessingErrorException e) {
            Assert.fail();
            e.printStackTrace();
        } catch (ModelException e) {
            Assert.fail();
            e.printStackTrace();
        }

        Assert.assertEquals("should not be null", workItem.getItemValueString("txtname"));
        Assert.assertEquals(1000, workItem.getTaskID());
    }

    /**
     * Test a simple process life cycle with a new target task.
     */
    @Test
    @Category(org.imixs.workflow.WorkflowKernel.class)
    public void testNextTaskElement() {
        ItemCollection workItem = new ItemCollection();
        workItem.model("1.0.0")
                .task(1000)
                .event(20);

        workItem.replaceItemValue("title", "Hello");

        try {
            workItem = workflowEngine.getWorkflowKernel().process(workItem);
            Assert.assertEquals(workItem.getItemValueString("title"), "Hello");
        } catch (WorkflowException e) {
            Assert.fail();
            e.printStackTrace();
        } catch (ProcessingErrorException e) {
            Assert.fail();
            e.printStackTrace();
        }

        Assert.assertEquals(1, workItem.getItemValueInteger("runs"));
        // test next state
        Assert.assertEquals(1100, workItem.getTaskID());
    }

    /**
     * Test processing a follow up event. Trigger 1000.10 should immediately trigger
     * the follow up event 20
     * 
     * The test loads the model 'followup_001.bpmn'
     */
    @Test
    @Category(org.imixs.workflow.WorkflowKernel.class)
    public void testFollowup() {

        // load followup model
        workflowEngine.loadBPMNModel("/bpmn/followup_001.bpmn");
        ItemCollection workItem = new ItemCollection();
        workItem.replaceItemValue("txtTitel", "Hello");
        workItem.model("1.0.0")
                .task(1000)
                .event(10);

        Assert.assertEquals(workItem.getItemValueString("txttitel"), "Hello");

        try {
            workItem = workflowEngine.getWorkflowKernel().process(workItem);
        } catch (WorkflowException e) {
            Assert.fail();
            e.printStackTrace();
        } catch (ProcessingErrorException e) {
            Assert.fail();
            e.printStackTrace();
        }

        // runs should be 2
        Assert.assertEquals(2, workItem.getItemValueInteger("runs"));
        // test next state
        Assert.assertEquals(1100, workItem.getTaskID());
    }

    /**
     * Test processing a follow up event. Trigger 1000.10 should immediately trigger
     * the follow up event 20
     * 
     * The test loads the model 'followup_002.bpmn'
     */
    @Test
    @Category(org.imixs.workflow.WorkflowKernel.class)
    public void testFollowupMultipleGateways() {

        // load followup model
        workflowEngine.loadBPMNModel("/bpmn/followup_002.bpmn");
        ItemCollection workItem = new ItemCollection();
        workItem.replaceItemValue("txtTitel", "Hello");
        workItem.model("1.0.0")
                .task(1000)
                .event(10);

        Assert.assertEquals(workItem.getItemValueString("txttitel"), "Hello");

        try {
            workItem = workflowEngine.getWorkflowKernel().process(workItem);
        } catch (WorkflowException e) {
            Assert.fail();
            e.printStackTrace();
        } catch (ProcessingErrorException e) {
            Assert.fail();
            e.printStackTrace();
        }

        // runs should be 2
        Assert.assertEquals(2, workItem.getItemValueInteger("runs"));
        // test next state
        Assert.assertEquals(1100, workItem.getTaskID());
    }

    @Test
    @Category(org.imixs.workflow.WorkflowKernel.class)
    public void testRegisterPlugin() {

        try {
            workflowEngine.getWorkflowKernel().unregisterPlugin(MockPlugin.class.getName());
        } catch (PluginException e1) {
            Assert.fail();
            e1.printStackTrace();
        }

        // unregister once again - exception expected

        try {
            workflowEngine.getWorkflowKernel().unregisterPlugin(MockPlugin.class.getName());
            // exception expected!
            Assert.fail();
        } catch (PluginException e1) {
            Assert.assertEquals(WorkflowKernel.PLUGIN_NOT_REGISTERED, e1.getErrorCode());
        }

        try {
            MockPlugin mokPlugin = new MockPlugin();
            workflowEngine.getWorkflowKernel().registerPlugin(mokPlugin);
        } catch (PluginException e) {
            Assert.fail();
            e.printStackTrace();
        }

    }

    /**
     * This method tests the generation of the $eventlog entries.
     */
    @SuppressWarnings("rawtypes")
    @Test
    @Category(org.imixs.workflow.WorkflowKernel.class)
    public void testActivityLog() {
        ItemCollection workitem = new ItemCollection();
        workitem.model("1.0.0")
                .task(1000);
        workitem.replaceItemValue("txtTitel", "Hello");
        try {
            // simulate two steps
            workitem.event(10);
            workitem = workflowEngine.getWorkflowKernel().process(workitem);
            Assert.assertEquals(workitem.getItemValueString("txttitel"), "Hello");
            workitem.event(20);
            // simulate a Log Comment...
            workitem.replaceItemValue("txtworkflowactivitylogComment", "userid|comment");
            workitem = workflowEngine.getWorkflowKernel().process(workitem);
        } catch (PluginException e) {
            Assert.fail();
            e.printStackTrace();
        } catch (ProcessingErrorException e) {
            Assert.fail();
            e.printStackTrace();
        } catch (ModelException e) {
            Assert.fail();
            e.printStackTrace();
        }

        Assert.assertEquals(2, workitem.getItemValueInteger("runs"));
        // test next state
        Assert.assertEquals(1100, workitem.getTaskID());

        // test log
        List log = workitem.getItemValue("$eventlog");

        Assert.assertNotNull(log);
        Assert.assertEquals(2, log.size());

        logger.log(Level.INFO, "''$eventlog''={0}", log);

        // test log entries
        // Format: timestamp|model-version|1000.10|1000|userid|
        String logEntry = (String) log.get(0);
        StringTokenizer st = new StringTokenizer(logEntry, "|");
        st.nextToken();
        Assert.assertEquals("1.0.0", st.nextToken());
        Assert.assertEquals("1000.10", st.nextToken());
        Assert.assertEquals("1000", st.nextToken());
        Assert.assertFalse(st.hasMoreTokens());

        logEntry = (String) log.get(1);
        st = new StringTokenizer(logEntry, "|");
        try {
            // check date object
            String sDate = st.nextToken();

            SimpleDateFormat formatter = new SimpleDateFormat(WorkflowKernel.ISO8601_FORMAT);
            Date date = null;
            date = formatter.parse(sDate);

            Calendar cal = Calendar.getInstance();
            Calendar calNow = Calendar.getInstance();
            cal.setTime(date);

            Assert.assertEquals(calNow.get(Calendar.YEAR), cal.get(Calendar.YEAR));
            Assert.assertEquals(calNow.get(Calendar.MONTH), cal.get(Calendar.MONTH));

        } catch (ParseException e) {

            e.printStackTrace();
            Assert.fail();
        }

        Assert.assertEquals("1.0.0", st.nextToken());
        Assert.assertEquals("1000.20", st.nextToken());
        Assert.assertEquals("1100", st.nextToken());
        // test comment
        Assert.assertTrue(st.hasMoreTokens());
        Assert.assertEquals("userid", st.nextToken());
        Assert.assertEquals("comment", st.nextToken());
        Assert.assertFalse(st.hasMoreTokens());

    }

    /**
     * This method tests the generation of the $eventlog entries and the restriction
     * to a maximum length of 30 entries.
     * 
     * Issue https://github.com/imixs/imixs-workflow/issues/179
     * 
     */
    @SuppressWarnings("rawtypes")
    @Test
    @Category(org.imixs.workflow.WorkflowKernel.class)
    public void testActivityLogMaxLength() {
        ItemCollection workitem = new ItemCollection();
        workitem.model("1.0.0")
                .task(1000)
                .event(10);
        workitem.replaceItemValue("txtTitel", "Hello");

        // we create 40 dummy entries
        String dummyEntry = "" + new Date() + "|1.0.0|100.10|100";
        Vector<String> v = new Vector<String>();
        for (int i = 1; i <= 40; i++) {
            v.add(dummyEntry);
        }
        workitem.replaceItemValue("$eventlog", v);

        try {
            // simulate two steps
            workitem.setEventID(10);
            workitem = workflowEngine.getWorkflowKernel().process(workitem);
            workitem.setEventID(20);
            // simulate a log Comment...
            workitem.replaceItemValue("txtworkflowactivitylogComment", "userid|comment");
            workitem = workflowEngine.getWorkflowKernel().process(workitem);

        } catch (PluginException e) {
            Assert.fail();
            e.printStackTrace();
        } catch (ProcessingErrorException e) {
            Assert.fail();
            e.printStackTrace();
        } catch (ModelException e) {
            Assert.fail();
            e.printStackTrace();
        }

        Assert.assertEquals(2, workitem.getItemValueInteger("runs"));
        // test next state
        Assert.assertEquals(1100, workitem.getTaskID());

        // test log
        List log = workitem.getItemValue("$eventlog");

        Assert.assertNotNull(log);
        Assert.assertEquals(30, log.size());

        logger.log(Level.INFO, "''$eventlog''={0}", log);

        // test log entries
        // Format: timestamp|model-version|1000.10|1000|userid|
        String logEntry = (String) log.get(log.size() - 2);
        StringTokenizer st = new StringTokenizer(logEntry, "|");
        st.nextToken();
        Assert.assertEquals("1.0.0", st.nextToken());
        Assert.assertEquals("1000.10", st.nextToken());
        Assert.assertEquals("1000", st.nextToken());
        Assert.assertFalse(st.hasMoreTokens());

        // test last entry
        logEntry = (String) log.get(log.size() - 1);
        st = new StringTokenizer(logEntry, "|");
        try {
            // check date object
            String sDate = st.nextToken();

            SimpleDateFormat formatter = new SimpleDateFormat(WorkflowKernel.ISO8601_FORMAT);
            Date date = null;
            date = formatter.parse(sDate);

            Calendar cal = Calendar.getInstance();
            Calendar calNow = Calendar.getInstance();
            cal.setTime(date);

            Assert.assertEquals(calNow.get(Calendar.YEAR), cal.get(Calendar.YEAR));
            Assert.assertEquals(calNow.get(Calendar.MONTH), cal.get(Calendar.MONTH));

        } catch (ParseException e) {

            e.printStackTrace();
            Assert.fail();
        }

        Assert.assertEquals("1.0.0", st.nextToken());
        Assert.assertEquals("1000.20", st.nextToken());
        Assert.assertEquals("1100", st.nextToken());
        // test comment
        Assert.assertTrue(st.hasMoreTokens());
        Assert.assertEquals("userid", st.nextToken());
        Assert.assertEquals("comment", st.nextToken());
        Assert.assertFalse(st.hasMoreTokens());
    }

    /**
     * test generated UUID
     * 
     * @see https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html
     */
    @Test
    @Category(org.imixs.workflow.WorkflowKernel.class)
    public void testUUID() {
        String uid = WorkflowKernel.generateUniqueID();
        logger.log(Level.INFO, "UUID={0}", uid);
        // expected length is 36
        Assert.assertEquals(36, uid.length());
    }

    /**
     * test generated transactionID
     * 
     */
    @Test
    @Category(org.imixs.workflow.WorkflowKernel.class)
    public void testTransactionID() {
        String tid = null;
        tid = WorkflowKernel.generateTransactionID();
        logger.log(Level.INFO, "TransactionID={0}", tid);
        // expected length is > 8
        Assert.assertTrue(tid.length() > 8);
    }
}
