package org.imixs.workflow.plugins;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.IntervalPlugin;
import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class for IntervalPugin
 * 
 * @author rsoika
 */
public class TestIntervalPlugin {
    protected IntervalPlugin intervalPlugin = null;

    private static Logger logger = Logger.getLogger(TestIntervalPlugin.class.getName());
    WorkflowMockEnvironment workflowMockEnvironment;
    ItemCollection documentContext;
    ItemCollection documentActivity;

    @Before
    public void setup() throws PluginException, ModelException, AdapterException {

        workflowMockEnvironment = new WorkflowMockEnvironment();
        workflowMockEnvironment.setModelPath("/bpmn/TestIntervalPlugin.bpmn");

        workflowMockEnvironment.setup();

        intervalPlugin = new IntervalPlugin();
        try {
            intervalPlugin.init(workflowMockEnvironment.getWorkflowService());
        } catch (PluginException e) {

            e.printStackTrace();
        }

        // prepare test workitem
        documentContext = new ItemCollection();
        logger.info("[TestAccessPlugin] setup test data...");

        documentContext.replaceItemValue("reminder", new Date());

        workflowMockEnvironment.getDocumentService().save(documentContext);

    }

    /**
     * This test verifies the montly interval
     * 
     * @throws PluginException
     */
    @Test
    public void testCron() throws PluginException {

        logger.info("------------------ Ref Date   =" + documentContext.getItemValueDate("reminder"));

        try {
            documentActivity = workflowMockEnvironment.getModel().getEvent(100, 99);
            intervalPlugin.run(documentContext, documentActivity);
        } catch (PluginException | ModelException e) {

            e.printStackTrace();
            Assert.fail();
        }

        Date result = documentContext.getItemValueDate("reminder");
        logger.info("------------------  Result Date=" + result);

    }

    /**
     * This test verifies the montly interval
     * 
     * @throws PluginException
     */
    @Test
    public void testMonthly() throws PluginException {

        // @monthly
        String cron = "0 0 1 * *";
        Date date = intervalPlugin.evalCron(cron);
        logger.info("Result monthyl=" + date);

        Calendar calTestDate = Calendar.getInstance();
        calTestDate.setTime(date);
        Calendar calExptectedDate = Calendar.getInstance();
        // move to past one day...
        calExptectedDate.add(Calendar.MONTH, +1);
        Assert.assertEquals(calExptectedDate.get(Calendar.MONTH), calTestDate.get(Calendar.MONTH));
    }

    /**
     * This test verifies the montly interval
     * 
     * @throws PluginException
     */
    @Test
    public void testYearly() throws PluginException {

        // @monthly
        String cron = "0 0 1 1 *";
        Date date = intervalPlugin.evalCron(cron);
        logger.info("Result monthyl=" + date);

        Calendar calTestDate = Calendar.getInstance();
        calTestDate.setTime(date);
        Calendar calExptectedDate = Calendar.getInstance();
        // move to past one day...
        calExptectedDate.add(Calendar.YEAR, +1);
        Assert.assertEquals(calExptectedDate.get(Calendar.YEAR), calTestDate.get(Calendar.YEAR));

    }

    /**
     * This test verifies the weekly interval
     * 
     * @throws PluginException
     */
    @Test
    public void testWeekly() throws PluginException {
        // @weekly
        String cron = "0 0 * * 0 ";
        Date date = intervalPlugin.evalCron(cron);
        logger.info("Result monthyl=" + date);

        Calendar calTestDate = Calendar.getInstance();
        calTestDate.setTime(date);
        // move to past one day...
        Assert.assertEquals(2, calTestDate.get(Calendar.DAY_OF_WEEK));

    }

}
