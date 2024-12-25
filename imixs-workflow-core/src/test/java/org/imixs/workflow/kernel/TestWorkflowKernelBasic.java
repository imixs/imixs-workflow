package org.imixs.workflow.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @BeforeEach
    public void setup() throws PluginException {
        workflowEngine = new MockWorkflowEngine();
        // load default model
        workflowEngine.loadBPMNModel("/bpmn/simple.bpmn");
    }

    /**
     * This test tests the basic behavior of the WorkflowKernel process method.
     */
    @Test
    public void testSimpleProcessingCycle() {

        ItemCollection workitemProcessed = null;
        ItemCollection workItem = new ItemCollection();
        workItem.model("1.0.0")
                .task(1000)
                .event(10);
        workItem.replaceItemValue("txtTitel", "Hello");

        assertEquals(workItem.getItemValueString("txttitel"), "Hello");

        try {
            workitemProcessed = workflowEngine.getWorkflowKernel().process(workItem);
        } catch (ModelException | ProcessingErrorException | PluginException e) {
            e.printStackTrace();
            fail();
        }
        assertEquals(1, workitemProcessed.getItemValueInteger("runs"));
        assertEquals(1000, workitemProcessed.getTaskID());

        assertEquals("1.0.0", workitemProcessed.getModelVersion());

        // initial and processed workitems should be the same and should be equals!
        assertSame(workItem, workitemProcessed);
        assertTrue(workItem.equals(workitemProcessed));

        // the workitem should not have a $eventid
        assertEquals(0, workItem.getEventID());
        // a new call of process should throw a ProcessingErrorException
        try {
            workitemProcessed = workflowEngine.getWorkflowKernel().process(workItem);
            fail(); // we expect an Exception here!
        } catch (ModelException e) {
            fail(e.getMessage());
            e.printStackTrace();
        } catch (WorkflowException e) {
            fail();
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
    public void testInvalidModelVersion() {
        ItemCollection workItem = new ItemCollection();
        workItem.model("A.B.C")
                .task(1000)
                .event(10);

        try {
            workItem = workflowEngine.getWorkflowKernel().process(workItem);
            fail();
        } catch (ModelException e) {
            // Expected Exception
            logger.info(e.getMessage());
        } catch (WorkflowException e) {
            fail();
            e.printStackTrace();
        } catch (ProcessingErrorException e) {
            fail();
            e.printStackTrace();
        }

    }

    /**
     * This test tests a $modelversion with a regular expression. The ModelManager
     * should resolve the version 1.0.0.
     **/
    @Test
    public void testModelVersionByRegex() {
        ItemCollection workItem = new ItemCollection();
        workItem.model("(^1.0)|(^2.0)")
                .task(1000)
                .event(10);

        try {
            workItem = workflowEngine.getWorkflowKernel().process(workItem);
            assertNotNull(workItem);
            // $modelversion should be 1.0.0
            assertEquals("1.0.0", workItem.getModelVersion());
        } catch (ModelException | ProcessingErrorException | PluginException e) {
            e.printStackTrace();
            fail();
        }

    }

    /**
     * This test tests a worktiem without $modelversion but with a $workflowgroup.
     **/
    @Test
    public void testByWorkflowGroup() {
        ItemCollection workItem = new ItemCollection();
        workItem.setItemValue(WorkflowKernel.WORKFLOWGROUP, "Simple");
        workItem.task(1000)
                .event(10);

        try {
            workItem = workflowEngine.getWorkflowKernel().process(workItem);
            assertNotNull(workItem);
            // $modelversion should be 1.0.0
            assertEquals("1.0.0", workItem.getModelVersion());
        } catch (ModelException | ProcessingErrorException | PluginException e) {
            e.printStackTrace();
            fail();
        }

    }

    /**
     * This test tests a worktIem without $modelversion and without $workflowgroup.
     * An Exception is expected!
     **/
    @Test
    public void testWithoutModelVersionANDWorkflowGroup() {
        ItemCollection workItem = new ItemCollection();
        // not group no version
        workItem.task(1000)
                .event(10);

        try {
            workItem = workflowEngine.getWorkflowKernel().process(workItem);
            fail();
        } catch (PluginException e) {
            e.printStackTrace();
            fail(e.getMessage());
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
    public void testProcessWithDeprecatedField() {
        ItemCollection itemCollectionProcessed = null;
        ItemCollection workItem = new ItemCollection();
        workItem.replaceItemValue("txtTitel", "Hello");
        workItem.model("1.0.0")
                .task(1000)
                .event(20);

        assertEquals(workItem.getItemValueString("txttitel"), "Hello");

        try {
            itemCollectionProcessed = workflowEngine.getWorkflowKernel().process(workItem);
        } catch (WorkflowException e) {
            fail();
            e.printStackTrace();
        } catch (ProcessingErrorException e) {
            fail();
            e.printStackTrace();
        }
        assertEquals(1, itemCollectionProcessed.getItemValueInteger("runs"));
        assertEquals(1100, itemCollectionProcessed.getTaskID());

        // initial and processed workitems are the same and equals!
        assertSame(workItem, itemCollectionProcessed);
        assertTrue(workItem.equals(itemCollectionProcessed));
    }

    /**
     * This if a plugin which returns null influences the workitem
     */
    @Test
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
            fail();
        } catch (PluginException e) {
            assertEquals(WorkflowKernel.PLUGIN_ERROR, e.getErrorCode());
        } catch (ProcessingErrorException e) {
            fail();
            e.printStackTrace();
        } catch (ModelException e) {
            fail();
            e.printStackTrace();
        }

        assertEquals("should not be null", workItem.getItemValueString("txtname"));
        assertEquals(1000, workItem.getTaskID());
    }

    /**
     * Test a simple process life cycle with a new target task.
     */
    @Test
    public void testNextTaskElement() {
        ItemCollection workItem = new ItemCollection();
        workItem.model("1.0.0")
                .task(1000)
                .event(20);

        workItem.replaceItemValue("title", "Hello");

        try {
            workItem = workflowEngine.getWorkflowKernel().process(workItem);
            assertEquals(workItem.getItemValueString("title"), "Hello");
        } catch (WorkflowException e) {
            fail();
            e.printStackTrace();
        } catch (ProcessingErrorException e) {
            fail();
            e.printStackTrace();
        }

        assertEquals(1, workItem.getItemValueInteger("runs"));
        // test next state
        assertEquals(1100, workItem.getTaskID());
    }

    /**
     * Test processing a follow up event. Trigger 1000.10 should immediately trigger
     * the follow up event 20
     * 
     * The test loads the model 'followup_001.bpmn'
     */
    @Test
    public void testFollowup() {

        // load followup model
        workflowEngine.loadBPMNModel("/bpmn/followup_001.bpmn");
        ItemCollection workItem = new ItemCollection();
        workItem.replaceItemValue("txtTitel", "Hello");
        workItem.model("1.0.0")
                .task(1000)
                .event(10);

        assertEquals(workItem.getItemValueString("txttitel"), "Hello");

        try {
            workItem = workflowEngine.getWorkflowKernel().process(workItem);
        } catch (WorkflowException e) {
            fail();
            e.printStackTrace();
        } catch (ProcessingErrorException e) {
            fail();
            e.printStackTrace();
        }

        // runs should be 2
        assertEquals(2, workItem.getItemValueInteger("runs"));
        // test next state
        assertEquals(1100, workItem.getTaskID());
    }

    /**
     * Test processing a follow up event. Trigger 1000.10 should immediately trigger
     * the follow up event 20
     * 
     * The test loads the model 'followup_002.bpmn'
     */
    @Test
    public void testFollowupMultipleGateways() {

        // load followup model
        workflowEngine.loadBPMNModel("/bpmn/followup_002.bpmn");
        ItemCollection workItem = new ItemCollection();
        workItem.replaceItemValue("txtTitel", "Hello");
        workItem.model("1.0.0")
                .task(1000)
                .event(10);

        assertEquals(workItem.getItemValueString("txttitel"), "Hello");

        try {
            workItem = workflowEngine.getWorkflowKernel().process(workItem);
        } catch (WorkflowException e) {
            fail();
            e.printStackTrace();
        } catch (ProcessingErrorException e) {
            fail();
            e.printStackTrace();
        }

        // runs should be 2
        assertEquals(2, workItem.getItemValueInteger("runs"));
        // test next state
        assertEquals(1100, workItem.getTaskID());
    }

    @Test
    public void testRegisterPlugin() {

        try {
            workflowEngine.getWorkflowKernel().unregisterPlugin(MockPlugin.class.getName());
        } catch (PluginException e1) {
            fail();
            e1.printStackTrace();
        }

        // unregister once again - exception expected

        try {
            workflowEngine.getWorkflowKernel().unregisterPlugin(MockPlugin.class.getName());
            // exception expected!
            fail();
        } catch (PluginException e1) {
            assertEquals(WorkflowKernel.PLUGIN_NOT_REGISTERED, e1.getErrorCode());
        }

        try {
            MockPlugin mokPlugin = new MockPlugin();
            workflowEngine.getWorkflowKernel().registerPlugin(mokPlugin);
        } catch (PluginException e) {
            fail();
            e.printStackTrace();
        }

    }

    /**
     * This method tests the generation of the $eventlog entries.
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testActivityLog() {
        ItemCollection workitem = new ItemCollection();
        workitem.model("1.0.0")
                .task(1000);
        workitem.replaceItemValue("txtTitel", "Hello");

        try {
            // simulate two steps
            workitem.event(10);
            workitem = workflowEngine.getWorkflowKernel().process(workitem);
            assertEquals(workitem.getItemValueString("txttitel"), "Hello");

            workitem.event(20);
            // simulate a Log Comment and editor...
            workitem.replaceItemValue("$eventlogComment", "comment");
            workitem.replaceItemValue("$editor", "userid");
            workitem = workflowEngine.getWorkflowKernel().process(workitem);
        } catch (PluginException | ProcessingErrorException | ModelException e) {
            fail();
            e.printStackTrace();
        }

        assertEquals(2, workitem.getItemValueInteger("runs"));
        // test next state
        assertEquals(1100, workitem.getTaskID());

        // test log
        List log = workitem.getItemValue("$eventlog");
        assertNotNull(log);
        assertEquals(2, log.size());
        logger.log(Level.INFO, "''$eventlog''={0}", log);

        // test log entries
        // Format:
        // timestamp+timezone|model-version|sourcetask|eventid|targettask|actor|comment
        String logEntry = (String) log.get(0);
        String[] parts = logEntry.split("\\|", -1); // -1 keep empty entries

        // Test first log entry
        assertEquals(7, parts.length);
        // test timestamp with time zone
        try {
            ZonedDateTime.parse(parts[0]);
            assertTrue(true);
        } catch (DateTimeParseException e) {
            fail("Invalid timestamp format: " + parts[0]);
        }

        assertEquals("1.0.0", parts[1]); // model-version
        assertEquals("1000", parts[2]); // sourcetask
        assertEquals("10", parts[3]); // eventid
        assertEquals("1000", parts[4]); // targettask
        assertEquals("", parts[5]); // actor (empty for first log)
        assertEquals("", parts[6]); // comment (empty for first log)

        // Test second log entry
        logEntry = (String) log.get(1);
        parts = logEntry.split("\\|", -1); // -1 keep empty entries

        // test timestamp
        try {
            // check date object (without timezone for comparison)
            String sDate = parts[0].substring(0, 23); // test until millis
            LocalDateTime date = LocalDateTime.parse(sDate);
            LocalDateTime now = LocalDateTime.now();

            assertEquals(now.getYear(), date.getYear());
            assertEquals(now.getMonth(), date.getMonth());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        assertEquals("1.0.0", parts[1]); // model-version
        assertEquals("1000", parts[2]); // sourcetask
        assertEquals("20", parts[3]); // eventid
        assertEquals("1100", parts[4]); // targettask
        assertEquals("userid", parts[5]); // actor
        assertEquals("comment", parts[6]); // comment
    }

    /**
     * This test verifies the migration of old eventlog entries into the new format
     * 
     * @See Issue 890
     */
    @Test
    public void testEventLogMigration() {
        ItemCollection workitem = new ItemCollection();
        workitem.model("1.0.0")
                .task(1000);
        workitem.replaceItemValue("txtTitel", "Hello");

        // Create old format log entries
        List<String> oldLogEntries = new ArrayList<>();
        oldLogEntries.add("2024-08-27T12:04:20.469|1.0.0|1000.10|1000|");
        oldLogEntries.add("2024-08-27T12:05:30.127|1.0.0|1000.20|1100|comment");
        workitem.replaceItemValue("$eventlog", oldLogEntries);

        // Add a new log entry which should trigger the migration
        try {
            workitem.replaceItemValue("$editor", "john");
            workitem.event(10);
            workitem = workflowEngine.getWorkflowKernel().process(workitem);
        } catch (Exception e) {
            fail();
            e.printStackTrace();
        }

        // Test if old entries were backed up
        assertTrue(workitem.hasItem("$eventlogdeprecated"));
        List oldBackupLog = workitem.getItemValue("$eventlogdeprecated");
        assertEquals(2, oldBackupLog.size());
        assertEquals(oldLogEntries.get(0), oldBackupLog.get(0));
        assertEquals(oldLogEntries.get(1), oldBackupLog.get(1));

        // Verify the new eventlog format
        List newLog = workitem.getItemValue("$eventlog");
        // We expect all entries to be in new format
        assertNotNull(newLog);

        // Test new log entry format
        String logEntry = (String) newLog.get(0);
        String[] parts = logEntry.split("\\|", -1);
        assertEquals(7, parts.length);

        try {
            ZonedDateTime.parse(parts[0]);
            assertTrue(true);
        } catch (DateTimeParseException e) {
            fail("Invalid timestamp format: " + parts[0]);
        }

        // Test if a second migration attempt has no effect
        int currentSize = newLog.size();
        try {
            workitem.event(10);
            workitem = workflowEngine.getWorkflowKernel().process(workitem);
        } catch (Exception e) {
            fail();
            e.printStackTrace();
        }
        List finalLog = workitem.getItemValue("$eventlog");
        assertEquals(currentSize + 1, finalLog.size());

        // Test if backup remained unchanged
        List finalBackupLog = workitem.getItemValue("$eventlogdeprecated");
        assertEquals(oldBackupLog, finalBackupLog);
    }

    /**
     * test generated UUID
     * 
     * @see https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html
     */
    @Test
    public void testUUID() {
        String uid = WorkflowKernel.generateUniqueID();
        logger.log(Level.INFO, "UUID={0}", uid);
        // expected length is 36
        assertEquals(36, uid.length());
    }

    /**
     * test generated transactionID
     * 
     */
    @Test
    public void testTransactionID() {
        String tid = null;
        tid = WorkflowKernel.generateTransactionID();
        logger.log(Level.INFO, "TransactionID={0}", tid);
        // expected length is > 8
        assertTrue(tid.length() > 8);
    }
}
